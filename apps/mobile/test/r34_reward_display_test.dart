import 'package:flutter_test/flutter_test.dart';
import 'package:wishpool_mobile/src/features/child_dashboard.dart';

void main() {
  test('R34: UI uses the server ledger amount without a hardcoded reward', () {
    expect(childStarLightLabel(1), '+1 星光');
    expect(childStarLightLabel(4), '+4 星光');
    expect(childStarLightLabel(0), '星光以账本发放为准');
  });
}
