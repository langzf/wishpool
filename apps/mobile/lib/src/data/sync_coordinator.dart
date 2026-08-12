import 'dart:convert';
import 'dart:io';

import '../infrastructure/runtime_config.dart';

class SyncCoordinator {
  SyncCoordinator({required this.config});

  final WishPoolRuntimeConfig config;

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
}

class SyncConnectionState {
  const SyncConnectionState({
    required this.socket,
    required this.afterSeq,
  });

  final WebSocket socket;
  final int afterSeq;
}
