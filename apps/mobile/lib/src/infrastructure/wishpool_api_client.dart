import 'dart:convert';
import 'dart:io';

import 'runtime_config.dart';

class WishPoolApiClient {
  WishPoolApiClient({
    required this.config,
    HttpClient? httpClient,
  }) : _httpClient = httpClient ?? HttpClient();

  final WishPoolRuntimeConfig config;
  final HttpClient _httpClient;
  static const _requestTimeout = Duration(seconds: 12);

  Future<Map<String, Object?>> getJson(String path,
      {String? accessToken}) async {
    final decoded = await getRawJson(path, accessToken: accessToken);
    if (decoded is! Map<String, Object?>) {
      throw const FormatException('Expected a JSON object response.');
    }
    return decoded;
  }

  Future<Object?> getRawJson(String path, {String? accessToken}) async {
    final uri = Uri.parse('${config.coreApiBaseUrl}$path');
    final request = await _httpClient.getUrl(uri).timeout(_requestTimeout);
    if (accessToken != null) {
      request.headers
          .set(HttpHeaders.authorizationHeader, 'Bearer $accessToken');
    }
    final response = await request.close().timeout(_requestTimeout);
    final body =
        await response.transform(utf8.decoder).join().timeout(_requestTimeout);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw WishPoolApiException(response.statusCode, body);
    }
    return jsonDecode(body);
  }

  Future<Map<String, Object?>> postJson(
    String path,
    Map<String, Object?> body, {
    String? accessToken,
    String? idempotencyKey,
  }) =>
      _writeJson(
        method: 'POST',
        path: path,
        body: body,
        accessToken: accessToken,
        idempotencyKey: idempotencyKey,
      );

  Future<Map<String, Object?>> putJson(
    String path,
    Map<String, Object?> body, {
    String? accessToken,
    String? idempotencyKey,
  }) =>
      _writeJson(
        method: 'PUT',
        path: path,
        body: body,
        accessToken: accessToken,
        idempotencyKey: idempotencyKey,
      );

  Future<Map<String, Object?>> _writeJson({
    required String method,
    required String path,
    required Map<String, Object?> body,
    String? accessToken,
    String? idempotencyKey,
  }) async {
    final uri = Uri.parse('${config.coreApiBaseUrl}$path');
    final request =
        await _httpClient.openUrl(method, uri).timeout(_requestTimeout);
    request.headers.contentType = ContentType.json;
    if (accessToken != null) {
      request.headers
          .set(HttpHeaders.authorizationHeader, 'Bearer $accessToken');
    }
    if (idempotencyKey != null) {
      request.headers.set('Idempotency-Key', idempotencyKey);
    }
    request.write(jsonEncode(body));
    final response = await request.close().timeout(_requestTimeout);
    final responseBody =
        await response.transform(utf8.decoder).join().timeout(_requestTimeout);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw WishPoolApiException(response.statusCode, responseBody);
    }
    if (responseBody.isEmpty) return <String, Object?>{};
    final decoded = jsonDecode(responseBody);
    if (decoded is! Map<String, Object?>) {
      throw const FormatException('Expected a JSON object response.');
    }
    return decoded;
  }

  Future<void> putFileToUrl(
    String url,
    File file, {
    required String contentType,
  }) async {
    final request =
        await _httpClient.putUrl(Uri.parse(url)).timeout(_requestTimeout);
    request.headers.contentType = ContentType.parse(contentType);
    request.headers.contentLength = await file.length();
    request.headers.set(HttpHeaders.userAgentHeader,
        'Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36');
    await request.addStream(file.openRead()).timeout(_requestTimeout);
    final response = await request.close().timeout(_requestTimeout);
    final responseBody =
        await response.transform(utf8.decoder).join().timeout(_requestTimeout);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw WishPoolApiException(response.statusCode, responseBody);
    }
  }

  Future<void> close() async {
    _httpClient.close();
  }
}

class WishPoolApiException implements Exception {
  const WishPoolApiException(this.statusCode, this.body);

  final int statusCode;
  final String body;

  @override
  String toString() => 'WishPoolApiException($statusCode): $body';
}
