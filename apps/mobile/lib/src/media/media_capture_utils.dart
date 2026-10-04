import 'dart:io';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:image/image.dart' as img;

const maxMediaBytes = 50 * 1024 * 1024;
const maxPhotoEdge = 1280;

String mediaContentType(String path, String kind) {
  if (kind == 'photo') return 'image/jpeg';
  if (kind == 'audio') return 'audio/m4a';
  if (kind == 'video') return 'video/mp4';
  return 'application/octet-stream';
}

String stableClientMutationId(String taskId, File file) {
  final material = '$taskId|${file.path}|${file.lengthSync()}';
  final digest = sha1.convert(material.codeUnits).toString().substring(0, 16);
  return 'mobile-media-$taskId-$digest';
}

Future<File> compressPhoto(File source) async {
  final decoded = img.decodeImage(await source.readAsBytes());
  if (decoded == null) throw const FormatException('无法读取照片');
  final largestEdge =
      decoded.width > decoded.height ? decoded.width : decoded.height;
  final resized = largestEdge > maxPhotoEdge
      ? img.copyResize(
          decoded,
          width: decoded.width >= decoded.height ? maxPhotoEdge : null,
          height: decoded.height > decoded.width ? maxPhotoEdge : null,
        )
      : decoded;
  final bytes = Uint8List.fromList(img.encodeJpg(resized, quality: 80));
  final target = File('${source.path}.wishpool-compressed.jpg');
  await target.writeAsBytes(bytes, flush: true);
  if (bytes.length < source.lengthSync()) return target;
  await target.delete();
  return source;
}

String? mediaLimitMessage(File file) {
  final bytes = file.lengthSync();
  if (bytes > maxMediaBytes) {
    return '文件过大（${(bytes / 1024 / 1024).toStringAsFixed(1)} MB），上限为 50 MB';
  }
  return null;
}

Future<String> checksumSha256(File file) async {
  final digest = await file
      .openRead()
      .fold<List<int>>(<int>[], (bytes, chunk) => bytes..addAll(chunk));
  return sha256.convert(digest).toString();
}
