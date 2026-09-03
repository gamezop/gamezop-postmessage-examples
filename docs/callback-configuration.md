# Gamezop Games callback reference

Use this reference when connecting a Gamezop property to one of these examples.
A working game URL alone does not enable callbacks: ask your Gamezop account
manager to enable the required events once. The same callback setup is used for
Individual and Battles; no per-room callback setup is needed.

## What the names mean

- **Bridge/channel/handler name:** the object the host exposes to game JavaScript.
- **Method name:** the function on that object which receives an event.
- **Payload:** the event data, usually serialized with `JSON.stringify(payload)`.
- **Web `targetOrigin`:** the receiving parent page's origin (scheme, hostname,
  and port). It is not a bridge name, method name, or game URL.

Names are case-sensitive. Register the bridge and method shown for your framework
before loading Gamezop Games. Use the same receiver for Individual and Battles.

## Exact contracts in this repository

| Host | Bridge or handler | Receiving method | Gamezop Games call | Received by the app |
| --- | --- | --- | --- | --- |
| Native Android | `AndroidBridge` | `postMessage` | `AndroidBridge.postMessage(JSON.stringify(payload))` | Kotlin `String` |
| Native iOS | `observer`, under `webkit.messageHandlers` | `postMessage` | `window.webkit.messageHandlers.observer.postMessage(payload)` | Swift `message.body`: object or JSON string |
| React Native, both OSes | `ReactNativeWebView` | `postMessage` | `ReactNativeWebView.postMessage(JSON.stringify(payload))` | `onMessage`: `event.nativeEvent.data` string |
| Flutter, both OSes | `FlutterWebView` JavaScript channel | `postMessage` | `FlutterWebView.postMessage(JSON.stringify(payload))` | `JavaScriptMessage.message` string |
| Web iframe | `window.parent` | `postMessage` | `window.parent.postMessage(payload, targetOrigin)` | Parent `message` listener: `event.data` object or JSON string |

Native Android can customize both names. Native iOS can customize the handler
name, but uses WebKit's fixed `postMessage` method. React Native provides
`ReactNativeWebView.postMessage`; a different page-facing name needs an adapter.
Flutter lets you choose the channel name, but `webview_flutter` uses the fixed
`postMessage` method. Changes must be coordinated on the host and Gamezop sides.

## Individual and Battles setup

Callback enablement is handled once by Gamezop. Integrators only need to register
the receiver for their framework using the names above. That receiver captures
both Individual states and Battles events. Flutter and React Native each use the same
page-facing bridge on Android and iOS.

Generate final Battles URLs in your own integration, outside these apps:

1. Create `roomDetails` following the [multiplayer guide](https://docs.platform.gamezop.com/publishers/gamezop/advanced/multiplayer-games).
2. Use the same room ID and different user IDs on the participating clients.
3. Serialize the object, Base64-encode its UTF-8 bytes, then URL-encode the value
   into exactly one `roomDetails` query parameter.
4. Paste the final HTTPS URL into **Battles** and join from multiple clients.

Base64 is encoding, **not encryption or authentication**. The examples validate
the envelope (HTTPS, no URL credentials, one Base64 non-empty JSON object), not
room authorization, user identity, or the entire server-side Battles schema.

## Copyable receiver examples

These are minimal receiver fragments, not replacements for the complete samples'
navigation, parsing, threading, or lifecycle code. `capture` represents your
defensive event parser; never evaluate a payload as code.

### Android

```kotlin
webView.addJavascriptInterface(GameEventBridge(onEvent = ::capture), "AndroidBridge")
// GameEventBridge exposes @JavascriptInterface fun postMessage(gameEvent: String).
// It forwards the callback from WebView's private bridge thread to the main thread.
```

See the [Android guide](../android/README.md#the-required-android-bridge) for the
class, settings, and release keep rules. Register before loading the URL.

### iOS

```swift
configuration.userContentController.add(coordinator, name: "observer")
// In coordinator: WKScriptMessageHandler
func userContentController(_ userContentController: WKUserContentController,
                           didReceive message: WKScriptMessage) {
    guard message.name == "observer" else { return }
    capture(message.body) // Handle both a dictionary and a JSON string.
}
```

Remove `observer` and clear delegates on disposal, as in the [iOS guide](../ios/README.md).

### React Native

```tsx
<WebView
  source={{uri: gameUrl}}
  javaScriptEnabled
  onMessage={event => capture(event.nativeEvent.data)}
/>
```

`onMessage` enables the page-facing bridge. See the [React Native guide](../react-native/README.md).

### Flutter

```dart
final controller = WebViewController()
  ..setJavaScriptMode(JavaScriptMode.unrestricted)
  ..addJavaScriptChannel(
    'FlutterWebView',
    onMessageReceived: (message) => capture(message.message),
  )
  ..loadRequest(Uri.parse(gameUrl));
```

Use the same channel on Android and iOS. See the [Flutter guide](../flutter/README.md).

### Web

The **parent page** registers the receiver, while Gamezop runs in the iframe:

```js
const frame = document.getElementById('game-frame');
const gameUrl = new URL(validatedHttpsGameUrl);
const expectedOrigin = gameUrl.origin;

function onGameMessage(event) {
  if (event.source !== frame.contentWindow) return;
  if (event.origin !== expectedOrigin) return;
  capture(event.data); // Object or JSON string; catch parsing errors.
}
window.addEventListener('message', onGameMessage);
frame.src = gameUrl.href;
// On host disposal: window.removeEventListener('message', onGameMessage).
```

Conceptually, the **iframe sender** uses:

```js
window.parent.postMessage(JSON.stringify(payload), 'https://partner.example');
```

Replace `https://partner.example` with the actual parent origin, including its
port if non-default. This is sender-side code: the parent cannot inject it into a
cross-origin Gamezop iframe. Gamezop must enable delivery. Never remove source or
origin validation to accommodate unrelated messages. See the [Web guide](../web/README.md).

## If the game loads but no events arrive

1. Confirm client broadcasts and the desired `events` are enabled for that property.
2. Match the bridge and method exactly to the table above, including letter case.
3. Confirm the final Battles URL and room details; changing the URL does not
   register a missing native bridge.
4. For Web, check the final iframe origin after redirects, HTTPS hosting, referrer
   policy, and embedding restrictions. A different origin is deliberately rejected.
5. Open **Logs** and check malformed/unknown entries before changing the parser.

All payloads are untrusted. `go_home` is captured, not executed. A client-side
score is not authoritative evidence for payments, prizes, or user authorization.
