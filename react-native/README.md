# React Native example

This bare TypeScript React Native 0.87 app uses the New Architecture, Node 22+, `react-native-webview` 16.0.0, and Android/iOS application ID `com.gamezop.postmessageexample.reactnative`.

## Callback contract

| Setting | Value on Android and iOS |
| --- | --- |
| Page-facing bridge | `ReactNativeWebView` |
| Method | `postMessage` |
| Receiver | WebView `onMessage`, reading `event.nativeEvent.data` |

The bridge is provided by `react-native-webview` when `onMessage` is set. The
same receiver serves both OSes and both game modes. See the
[callback reference](../docs/callback-configuration.md).

`react-native-webview` makes `ReactNativeWebView` available to Gamezop Games, which send stringified JSON through:

```js
ReactNativeWebView.postMessage(JSON.stringify(payload));
```

For Battles, generate the final URL outside this sample. Callback enablement is
handled once by Gamezop and applies to both Individual and Battles.

The Battles launcher requires exactly one standard or URL-safe Base64 `roomDetails` value which decodes to a non-empty JSON object. It intentionally validates the object, not partner-specific fields, so future Battles fields remain compatible.

## Run

Requirements: Node 22.11+, npm, JDK 17+, Android Studio with SDK/build tools 37
and NDK 27.1.12297006 for Android; macOS, Xcode, CocoaPods, and an installed iOS
simulator for iOS. Minimum app OS versions are Android API 24 and iOS 15.1.
Use `react-native doctor` through the locally installed CLI (`npx react-native doctor`)
to diagnose missing host tools after installing dependencies.

From this `react-native/` directory, install the lockfile's versions and start Metro:

```sh
npm ci
npm start
```

Leave Metro running. In a second terminal in the same directory, with an Android
emulator running or a USB-debugging-enabled device connected:

```sh
npm run android
```

For iOS, install pods after `npm ci`, then launch the simulator app:

```sh
cd ios
pod install
cd ..
npm run ios
```

For a physical iOS device, open `ios/GamezopPostMessageReactNative.xcworkspace`
(not the `.xcodeproj`) and select your own signing team and device. Never commit
personal signing credentials. The Android release build is debug-signed for
local verification; configure private production signing before distribution.

The launcher keeps independent Individual and Battles URL values. The WebView accepts only HTTPS URLs without credentials. It keeps the launch host and documented Gamezop hosts in-app and opens off-host HTTPS navigation externally.

## Event inspector

Drag the floating **Logs** pill inside the safe area. Opening its fixed-height overlay does not remount or resize the WebView, including during orientation changes. The inspector:

- recognizes all documented Individual states and Battles events;
- preserves extra fields, unknown values, and malformed payloads;
- stores the newest 500 entries only in memory;
- supports selection, formatted JSON, copy, and clear;
- displays `go_home` without triggering partner navigation.

## Verify

```sh
npm test
npm run lint
npx tsc --noEmit
cd android && ./gradlew assembleDebug assembleRelease
cd ../ios && pod install
xcodebuild -workspace GamezopPostMessageReactNative.xcworkspace -scheme GamezopPostMessageReactNative -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build
```

Client broadcasts must be enabled for the partner by the Gamezop account manager. Validate Battles with multiple clients and generated final URLs for the same room and distinct users.

The native `onMessage` handler receives WebView messages on the React Native side. Keep message handling defensive and avoid treating payload fields as commands until the host app explicitly validates and maps them to business behavior.

## Troubleshooting and data handling

- If the app cannot connect to Metro, check that Metro is running and the device
  can reach your development machine; this is separate from Gamezop callbacks.
- If games load without logs, confirm property enablement and the exact bridge
  configuration above. Unknown/malformed messages remain visible in **Logs**.
- Toggle/drag Logs, rotate, reload, select/copy/clear events, and complete a
  multi-client match as part of real-device verification. Jest mocks the WebView
  and does not prove that a remote Gamezop page sends events.
- Event history stays in memory, but shared cookies, normal WebView cache/storage,
  and explicitly copied clipboard values are not erased by **Clear**.
- Dependency security: run `npm audit` before publishing or updating the sample.
  Do not apply `npm audit fix --force` without reviewing the resulting framework
  changes. Keep Metro restricted to trusted development networks.
