import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:webview_flutter/webview_flutter.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('FlutterWebView JavaScript channel reaches Dart', (tester) async {
    String? received;
    final controller =
        WebViewController()
          ..setJavaScriptMode(JavaScriptMode.unrestricted)
          ..addJavaScriptChannel(
            'FlutterWebView',
            onMessageReceived: (message) => received = message.message,
          )
          ..loadHtmlString('''
        <!doctype html><html><body><script>
          FlutterWebView.postMessage(JSON.stringify({state: "loaded", score: 0}));
        </script></body></html>
      ''');
    await tester.pumpWidget(
      MaterialApp(home: WebViewWidget(controller: controller)),
    );
    await tester.pumpAndSettle(const Duration(seconds: 2));
    expect(received, contains('"state":"loaded"'));
  });
}
