import 'dart:io';

class UploadQueue {
  final List<PendingUpload> _items = [];

  List<PendingUpload> get items => List.unmodifiable(_items);

  void enqueue(PendingUpload upload) {
    _items.removeWhere((item) => item.clientMutationId == upload.clientMutationId);
    _items.add(upload);
  }

  void markCompleted(String clientMutationId) {
    _items.removeWhere((item) => item.clientMutationId == clientMutationId);
  }
}

class PendingUpload {
  const PendingUpload({
    required this.clientMutationId,
    required this.taskInstanceId,
    required this.file,
    required this.contentType,
  });

  final String clientMutationId;
  final String taskInstanceId;
  final File file;
  final String contentType;
}
