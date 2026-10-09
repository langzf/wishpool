import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:just_audio/just_audio.dart';
import 'package:wishpool_mobile/src/features/memory_timeline_screen.dart';

void main() {
  testWidgets('initState immediately preloads the audio source',
      (tester) async {
    final fake = FakeAudioPlayer();
    await tester.pumpWidget(_host(fake));

    expect(fake.loadCalls, ['https://example.test/audio.mp3']);
  });

  testWidgets('shows placeholders while metadata is loading', (tester) async {
    final fake = FakeAudioPlayer(loadCompleter: Completer<void>());
    await tester.pumpWidget(_host(fake));

    expect(find.text('--:-- / --:--'), findsOneWidget);
    expect(find.text('00:00 / 00:00'), findsNothing);
  });

  testWidgets('shows the loaded duration with a zero position', (tester) async {
    final fake = FakeAudioPlayer(duration: const Duration(seconds: 12));
    await tester.pumpWidget(_host(fake));
    await tester.pump();

    expect(find.text('00:00 / 00:12'), findsOneWidget);
  });

  testWidgets('shows a Chinese error and retries loading after failure',
      (tester) async {
    final fake = FakeAudioPlayer(failFirstLoad: true);
    await tester.pumpWidget(_host(fake));
    await tester.pump();

    expect(find.textContaining('\u64cd\u4f5c\u5931\u8d25'),
        findsOneWidget);
    expect(find.textContaining('00:00 /'), findsNothing);
    expect(fake.loadCalls, hasLength(1));

    await tester.tap(find.text('\u91cd\u8bd5'));
    await tester.pump();
    expect(fake.loadCalls, hasLength(2));
  });

  testWidgets('uses durationSec as a fallback while metadata is unavailable',
      (tester) async {
    final fake = FakeAudioPlayer(loadCompleter: Completer<void>());
    await tester.pumpWidget(_host(fake, durationSec: 30));

    expect(find.text('00:00 / 00:30'), findsOneWidget);
  });
}

Widget _host(FakeAudioPlayer fake, {int? durationSec}) {
  return MaterialApp(
    home: Scaffold(
      body: AudioPlayerWidget(
        url: 'https://example.test/audio.mp3',
        durationSec: durationSec,
        playerFactory: () => fake,
      ),
    ),
  );
}

class FakeAudioPlayer implements AudioPlayerController {
  FakeAudioPlayer(
      {this.duration,
      Completer<void>? loadCompleter,
      this.failFirstLoad = false})
      : _loadCompleter = loadCompleter;

  @override
  final Duration? duration;
  final Completer<void>? _loadCompleter;
  final bool failFirstLoad;
  final List<String> loadCalls = [];
  final _playerState = StreamController<PlayerState>.broadcast();
  final _position = StreamController<Duration>.broadcast();
  int _loadCount = 0;

  @override
  Stream<PlayerState> get playerStateStream => _playerState.stream;
  @override
  Stream<Duration> get positionStream => _position.stream;
  @override
  bool get playing => false;
  @override
  ProcessingState get processingState => ProcessingState.ready;

  @override
  Future<void> load(String url) async {
    loadCalls.add(url);
    _loadCount++;
    if (failFirstLoad && _loadCount == 1) {
      throw StateError('metadata unavailable');
    }
    if (_loadCompleter != null) await _loadCompleter.future;
  }

  @override
  Future<void> play() async {}
  @override
  Future<void> pause() async {}
  @override
  Future<void> seek(Duration position) async {}
  @override
  Future<void> dispose() async {
    await _playerState.close();
    await _position.close();
  }
}
