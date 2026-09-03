import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gamezop_postmessage_flutter/launcher_screen.dart';

void main() {
  testWidgets('launcher validates URLs and preserves each mode input', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(home: LauncherScreen(onLaunch: (_, _) {})),
    );
    await tester.enterText(
      find.byKey(const Key('url-input')),
      'http://example.com',
    );
    await tester.tap(find.byKey(const Key('launch-game')));
    await tester.pump();
    expect(find.text('Only HTTPS URLs are accepted'), findsOneWidget);

    await tester.tap(find.text('Battles'));
    await tester.pump();
    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      isEmpty,
    );
    await tester.tap(find.text('Individual'));
    await tester.pump();
    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      'http://example.com',
    );
  });
}
