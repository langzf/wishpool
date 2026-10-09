Duration uploadBackoffForAttempt(int attempt) {
  if (attempt <= 0) return const Duration(seconds: 2);
  final seconds = (1 << attempt).clamp(2, 300).toInt();
  return Duration(seconds: seconds);
}
