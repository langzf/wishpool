import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';

import '../data/wishpool_scope.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/fixture_data.dart';

class SubmissionWaitingScreen extends StatefulWidget {
  const SubmissionWaitingScreen(
      {super.key, required this.task, required this.file, required this.scope});
  final ChildTask task;
  final File file;
  final WishPoolScope scope;

  @override
  State<SubmissionWaitingScreen> createState() =>
      _SubmissionWaitingScreenState();
}

class _SubmissionWaitingScreenState extends State<SubmissionWaitingScreen> {
  Timer? _timer;
  WishPoolSnapshot? _snapshot;
  late final DateTime _submittedAt;

  @override
  void initState() {
    super.initState();
    _submittedAt = DateTime.now();
    _refresh();
    _timer = Timer.periodic(const Duration(seconds: 3), (_) => _refresh());
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _refresh() async {
    final snapshot = await widget.scope.loadSnapshot();
    if (mounted) setState(() => _snapshot = snapshot);
  }

  @override
  Widget build(BuildContext context) {
    final matches = _snapshot?.childTasks
        .where((item) => item.id == widget.task.id)
        .toList();
    final task =
        matches != null && matches.isNotEmpty ? matches.first : widget.task;
    return Scaffold(
      appBar: AppBar(title: const Text('提交反馈')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (widget.task.submissionTypeCode == 'photo')
            ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: Image.file(widget.file, height: 220, fit: BoxFit.cover),
            )
          else
            Container(
              height: 150,
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.secondaryContainer,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Icon(widget.task.submissionTypeCode == 'video'
                  ? Icons.videocam_outlined
                  : Icons.mic_none),
            ),
          const SizedBox(height: 20),
          Text(widget.task.title,
              style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 8),
          Text(
              '提交时间：${_submittedAt.hour.toString().padLeft(2, '0')}:${_submittedAt.minute.toString().padLeft(2, '0')}'),
          const SizedBox(height: 20),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(_waitingText(task),
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  const Text('提交已保存，等家长确认。页面会自动刷新，无需重启应用。'),
                  if (task.feedback != null && task.feedback!.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    Text('家长反馈：${task.feedback}'),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _refresh,
            icon: const Icon(Icons.refresh),
            label: const Text('立即刷新'),
          ),
        ],
      ),
    );
  }

  String _waitingText(ChildTask task) => switch (task.status) {
        '已通过' => '已完成，获得 ${task.reward} 星光',
        '被退回' => '被退回了：${task.feedback ?? '请查看家长反馈'}',
        '待家长审核' => '已提交，等家长看看',
        _ => '已提交，正在智能检查…',
      };
}
