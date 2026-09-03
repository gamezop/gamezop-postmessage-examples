# Flutter example

This Flutter 3.47 mobile app runs on Android (API 24+) and iOS (15+). It launches Individual or Battles URLs in `webview_flutter` 4.14.1 and captures client-side events in an in-memory inspector.

## Callback contract

| Setting | Value on Android and iOS |
| --- | --- |
| JavaScript channel | `FlutterWebView` |
| Method | `postMessage` |
| Receiver | Channel `onMessageReceived`, reading `message.message` |

The same receiver serves both Flutter platforms and both game modes. The channel
name is `FlutterWebView`, and the plugin's method is `postMessage`.
The Dart callback parameter remains `onMessageReceived`; it is not the
JavaScript method name. See the [callback reference](../docs/callback-configuration.md).

The JavaScript channel is named `FlutterWebView`. Gamezop Games send stringified JSON through:

```js
FlutterWebView.postMessage(JSON.stringify(payload));
```

For Battles, generate the final URL outside this app. The launcher requires
exactly one `roomDetails` query value which must be standard or URL-safe Base64
decoding to a non-empty JSON object. Callback enablement is handled once by
Gamezop and applies to both Individual and Battles.

The app accepts Individual states `loaded`, `start`, `playing`, `over`, and `levelup`; and Battles events `match_found`, `match_not_found`, `match_start`, `match_playing`, `match_over`, `match_result`, and `go_home`. Unknown fields and event names remain visible. Malformed JSON becomes an error entry. `go_home` is displayed but intentionally has no business action.

## Run

Install Flutter 3.47 with Dart 3.13.2 or a compatible SDK satisfying `pubspec.yaml`.
For Android, install Android Studio, its SDK tools, and a compatible JDK for the
checked-in Gradle/Android plugin. For iOS, use macOS with Xcode and an installed
simulator runtime. Run `flutter doctor -v` to identify missing platform tools.

From this `flutter/` directory:

```sh
flutter doctor -v
flutter pub get
flutter devices
flutter run -d <device-id>
```

Choose an Android or iOS device ID, not Chrome or a desktop target: this sample
uses the mobile `webview_flutter` implementation. Do not run `flutter create .`
over the existing project. Flutter regenerates ignored host files during its
normal setup/build flow. The pub lockfile is checked in.

For a physical iOS device, configure your own team in `ios/Runner.xcworkspace`.
Keep signing credentials out of Git. Android release builds currently use debug
signing for local verification, not production distribution.

The launcher retains separate Individual and Battles input values. After launch, drag the floating **Logs** pill anywhere within the safe area. Toggling the fixed-height overlay does not reconstruct or resize the WebView. The latest 500 events are kept only in memory; use **Copy JSON** or **Clear** from the inspector.

## Security and lifecycle

- Only HTTPS launch URLs without embedded credentials are accepted.
- The original launch host and `*.gamezop.com` / `*.umogames.com` stay in-app; off-host top-level links open through the system browser.
- The channel exposes only the string-message callback. Payload parsing is defensive.
- `WebViewController` remains stateful for the lifetime of the game screen; leaving the screen releases it through Flutter/plugin lifecycle cleanup.
- Memory-only event history does not disable website cookies/cache or clear the
  clipboard. Redact user data before sharing inspector captures.

## Verify

```sh
dart format --output=none --set-exit-if-changed lib test integration_test
flutter analyze
flutter test
flutter test integration_test/webview_bridge_test.dart -d <device-id>
flutter build apk --debug
flutter build ios --debug --no-codesign
```

Ask the Gamezop account manager to enable client broadcasts once for Individual and Battles. Test Battles with multiple clients and generated final URLs for the same room and distinct users.

The integration test exercises the real JavaScript channel with test-only HTML;
it does not prove that a partner's live property has broadcasts enabled. On real
devices, also verify rotation, draggable Logs, selection/copy/clear, reload, back,
and close. If the game loads without events, confirm the channel name and callback
enablement; if builds fail, resolve `flutter doctor -v` issues first.
