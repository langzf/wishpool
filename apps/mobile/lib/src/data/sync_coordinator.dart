import 'dart:convert';
import 'dart:async';
import 'dart:io';

import 'package:shared_preferences/shared_preferences.dart';

import '../infrastructure/runtime_config.dart';

class SyncCoordinator {
  SyncCoordinator({required this.config});

  final WishPoolRuntimeConfig config;
  final _events = StreamController<FamilySyncEvent>.broadcast();
  WebSocket? _socket;
  Timer? _reconnectTimer;
  bool _disposed = false;
  bool _connecting = false;
  String? _familyId;
  String? _accessToken;
  int _afterSeq = 0;

  Stream<FamilySyncEvent> get events => _events.stream;

  Future<void> start({
    required String familyId,
    required String accessToken,
  }) async {
    if (_disposed || !config.hasRemoteContext) return;
    _familyId = familyId;
    _accessToken = accessToken;
    _afterSeq = await _loadCursor(familyId);
    await _connect();
  }

  Future<SyncConnectionState> connect({
    required String familyId,
    required int afterSeq,
    String? accessToken,
  }) async {
    final uri = Uri.parse(config.realtimeBaseUrl).replace(
      queryParameters: {
        'familyId': familyId,
        'afterSeq': '$afterSeq',
      },
    );
    final socket = await WebSocket.connect(
      uri.toString(),
      headers: accessToken == null ? null : {HttpHeaders.authorizationHeader: 'Bearer $accessToken'},
    );
    socket.add(jsonEncode({'type': 'sync.pull', 'afterSeq': afterSeq}));
    return SyncConnectionState(socket: socket, afterSeq: afterSeq);
  }

  Future<void> pullOnce() async {
    if (_socket == null) return;
    _socket?.add(jsonEncode({'type': 'sync.pull', 'afterSeq': _afterSeq}));
  }

  Future<void> dispose() async {
    _disposed = true;
    _reconnectTimer?.cancel();
    await _socket?.close();
    await _events.close();
  }

  Future<void> _connect() async {
    final familyId = _familyId;
    final accessToken = _accessToken;
    if (_connecting || _disposed || familyId == null || accessToken == null) return;
    _connecting = true;
    try {
      final state = await connect(familyId: familyId, afterSeq: _afterSeq, accessToken: accessToken);
      _socket = state.socket;
      _socket?.listen(_handleSocketMessage, onDone: _scheduleReconnect, onError: (_) => _scheduleReconnect());
    } catch (_) {
      _scheduleReconnect();
    } finally {
      _connecting = false;
    }
  }

  Future<void> _handleSocketMessage(dynamic message) async {
    if (message is! String) return;
    final decoded = jsonDecode(message);
    if (decoded is! Map<String, Object?>) return;

    final type = decoded['type'];
    if (type == 'realtime.connected' || type == 'realtime.heartbeat') {
      final latestSeq = decoded['latestSeq'];
      if (latestSeq is num) await _advanceCursor(latestSeq.toInt());
      return;
    }
    if (type != 'family.event') return;

    final event = decoded['event'];
    if (event is! Map<String, Object?>) return;
    final syncEvent = FamilySyncEvent.fromJson(event);
    await _advanceCursor(syncEvent.seq);
    _events.add(syncEvent);
  }

  void _scheduleReconnect() {
    if (_disposed) return;
    _socket = null;
    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(const Duration(seconds: 3), _connect);
  }

  Future<int> _loadCursor(String familyId) async {
    final preferences = await SharedPreferences.getInstance();
    return preferences.getInt(_cursorKey(familyId)) ?? 0;
  }

  Future<void> _advanceCursor(int seq) async {
    if (seq <= _afterSeq) return;
    _afterSeq = seq;
    final familyId = _familyId;
    if (familyId == null) return;
    final preferences = await SharedPreferences.getInstance();
    await preferences.setInt(_cursorKey(familyId), seq);
  }

  String _cursorKey(String familyId) => 'wishpool.sync.$familyId.afterSeq';
}

class FamilySyncEvent {
  const FamilySyncEvent({
    required this.seq,
    required this.type,
    required this.aggregateType,
    required this.aggregateId,
    required this.occurredAt,
    required this.payload,
  });

  final int seq;
  final String type;
  final String aggregateType;
  final String aggregateId;
  final DateTime? occurredAt;
  final Map<String, Object?> payload;

  factory FamilySyncEvent.fromJson(Map<String, Object?> json) {
    final occurredAtValue = json['occurredAt'];
    final payloadValue = json['payload'];
    return FamilySyncEvent(
      seq: (json['seq'] as num?)?.toInt() ?? 0,
      type: json['type']?.toString() ?? 'unknown',
      aggregateType: json['aggregateType']?.toString() ?? 'unknown',
      aggregateId: json['aggregateId']?.toString() ?? '',
      occurredAt: occurredAtValue is String ? DateTime.tryParse(occurredAtValue) : null,
      payload: payloadValue is Map ? payloadValue.cast<String, Object?>() : const {},
    );
  }
}

class SyncConnectionState {
  const SyncConnectionState({
    required this.socket,
    required this.afterSeq,
  });

  final WebSocket socket;
  final int afterSeq;
}
