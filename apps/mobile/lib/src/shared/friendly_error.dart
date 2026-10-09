import 'dart:async';
import 'dart:io';
import '../infrastructure/wishpool_api_client.dart';

String childFriendlyError(Object error) {
  if (error is WishPoolApiException) {
    if (error.statusCode == 409) {
      return '\u8fd9\u4e2a\u4efb\u52a1\u5df2\u7ecf\u6253\u5361\u8fc7\u5566';
    }
    if (error.statusCode == 401 || error.statusCode == 403) {
      return '\u914d\u5bf9\u7801\u65e0\u6548\u6216\u5df2\u8fc7\u671f\uff0c\u8bf7\u8ba9\u5bb6\u957f\u91cd\u65b0\u751f\u6210';
    }
    if (error.statusCode >= 500 ||
        error.statusCode == 408 ||
        error.statusCode == 429) {
      return '\u7f51\u7edc\u6682\u65f6\u4e0d\u7a33\u5b9a\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5';
    }
    return '\u64cd\u4f5c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5';
  }
  if (error is SocketException || error is HttpException) {
    return '\u7f51\u7edc\u4e0d\u53ef\u8fbe\uff0c\u8bf7\u68c0\u67e5\u7f51\u7edc\u8fde\u63a5\u540e\u91cd\u8bd5';
  }
  if (error is TimeoutException) {
    return '\u7f51\u7edc\u6682\u65f6\u4e0d\u7a33\u5b9a\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5';
  }
  return '\u64cd\u4f5c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5';
}
