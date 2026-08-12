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

  Future<Map<String, Object?>> getJson(String path, {String? accessToken}) async {
    final uri = Uri.parse('${config.coreApiBaseUrl}$path');
    final request = await _httpClient.getUrl(uri);
    if (accessToken != null) {
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $accessToken');
    }
    final response = await request.close();
    final body = await response.transform(utf8.decoder).join();
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw WishPoolApiException(response.statusCode, body);
    }
    final decoded = jsonDecode(body);
    if (decoded is! Map<String, Object?>) {
      throw const FormatException('Expected a JSON object response.');
    }
    return decoded;
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
