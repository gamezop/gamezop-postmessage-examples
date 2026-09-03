# Native iOS Gamezop PostMessage example

This SwiftUI reference app mirrors the Android sample with Individual and Battles launch modes,
strict Battles URL validation, a hardened `WKWebView`, and a draggable in-memory event inspector.

## Requirements and run

- Xcode 26 or newer
- iOS 15 or newer
- A Gamezop URL enabled for client-side broadcasts
- An installed iOS Simulator runtime, or a connected device and your own signing team

From this `ios/` folder, open `GamezopPostMessage.xcodeproj` in Xcode, choose
the `GamezopPostMessage` scheme and an available simulator, then press Run.
For a physical device, select your own development team in Signing & Capabilities.
Do not commit your personal signing configuration.

The project is checked in, so XcodeGen is not required for a normal first run.
Install [XcodeGen](https://github.com/yonaskolb/XcodeGen) only if you change
`project.yml`, then regenerate with `xcodegen generate`.

For command-line build/tests, list available destinations first and replace
`<simulator-id>` with an installed simulator's ID:

```bash
xcodebuild -project GamezopPostMessage.xcodeproj -scheme GamezopPostMessage \
  -showdestinations
xcodebuild -project GamezopPostMessage.xcodeproj -scheme GamezopPostMessage \
  -destination 'platform=iOS Simulator,id=<simulator-id>' build test
```

## Callback contract

Register the iOS message handler as `observer` before the first request:

```swift
let configuration = WKWebViewConfiguration()
configuration.defaultWebpagePreferences.allowsContentJavaScript = true
configuration.userContentController.add(coordinator, name: "observer")
let webView = WKWebView(frame: .zero, configuration: configuration)
```

Gamezop calls:

```javascript
window.webkit.messageHandlers.observer.postMessage(payload)
```

| Setting | Value in this sample |
| --- | --- |
| WebKit handler name | `observer` |
| Method | WebKit's `postMessage` |
| Received payload | `message.body`, as an object or JSON string |

Register `observer` before loading Gamezop Games. Once Gamezop enables callbacks,
this handler receives both Individual and Battles events without separate room-level
callback setup. See the [callback reference](../docs/callback-configuration.md).

The coordinator removes `observer`, clears its delegates, and stops loading when SwiftUI disposes
the WebView. Only HTTPS navigation to the launch host, `gamezop.com`, and `umogames.com` remains
inside the app; other top-level URLs open through the system.

## Event handling

Known Individual states and Battles events follow [`../docs/event-contracts.md`](../docs/event-contracts.md).
Unknown fields and values are retained, malformed messages become diagnostic entries, and only the
newest 500 events remain in memory. `go_home` is displayed but never navigates automatically.

The draggable **Logs** pill and its overlay remain within the SwiftUI safe area and do not reload or
resize the active `WKWebView` when toggled. Event logs are memory-only. The default
WKWebView data store can retain website cookies/cache; this is not a private-browsing app.

## Real-device check and troubleshooting

1. Confirm broadcasts and the desired states/events are enabled for your property.
2. Launch Individual, play, open **Logs**, select an entry, copy its JSON, and clear.
3. Launch final Battles URLs on multiple clients with the same room and distinct
   users. Check matchmaking, play, results, and `go_home` without automatic navigation.
4. Rotate, drag/toggle Logs, reload, go back, and close the game. The game should
   remain mounted while toggling the overlay.

If no events appear, check `observer` spelling and callback enablement first.
If the page fails to load, test the exact URL and network connection on that device.
Off-host links open externally by design. The XCTest suite covers parsing, URL
policy, retention, and a real WKWebView callback; UI tests cover launcher behavior.
