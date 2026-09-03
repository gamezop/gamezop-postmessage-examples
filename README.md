# Gamezop PostMessage Examples

Production-style reference applications showing how native, cross-platform, and browser hosts receive Gamezop client-side game events from an embedded game.

All implementations handle Gamezop client event payloads. Register the receiver
for your framework using the [callback reference](docs/callback-configuration.md).
The framework folders share the same goals:

- receive individual game states (`loaded`, `start`, `playing`, `over`, and `levelup`);
- receive Battles callbacks (`match_found`, `match_not_found`, `match_start`, `match_playing`, `match_over`, `match_result`, and `go_home`);
- display the parsed event and its original JSON without sending it anywhere;
- provide separate Individual and Battles launch flows while sharing the same event inspector.

## Implementations

| Framework | Status | Location |
| --- | --- | --- |
| Android (Kotlin + Jetpack Compose) | Implemented | [`android/`](android/) |
| iOS (SwiftUI + WKWebView) | Implemented | [`ios/`](ios/) |
| React Native 0.87 (TypeScript) | Implemented | [`react-native/`](react-native/) |
| Flutter 3.47 | Implemented | [`flutter/`](flutter/) |
| Web (HTML iframe) | Implemented | [`web/index.html`](web/index.html) |

Choose the relevant framework guide—[Android](android/README.md), [iOS](ios/README.md),
[React Native](react-native/README.md), [Flutter](flutter/README.md), or [Web](web/README.md).
Run each guide's commands from its framework folder unless otherwise stated.
Start with the [callback names and receiver setup](docs/callback-configuration.md)
and shared [event contract](docs/event-contracts.md).

## Gamezop Games callbacks

| Target | Gamezop Games call |
| --- | --- |
| Android | `AndroidBridge.postMessage(JSON.stringify(payload))` |
| iOS | `window.webkit.messageHandlers.observer.postMessage(payload)` |
| React Native | `ReactNativeWebView.postMessage(JSON.stringify(payload))` |
| Flutter | `FlutterWebView.postMessage(JSON.stringify(payload))` |
| Web | `window.parent.postMessage(payload, targetOrigin)` |

Each mobile app has independent Individual and Battles URL state, accepts only credential-free HTTPS launch URLs, defensively parses string or object messages, and retains the newest 500 events in memory. Battles launch requires exactly one `roomDetails` query value that decodes from standard or URL-safe Base64 to a non-empty JSON object.

The draggable **Logs** pill is safe-area constrained. Its fixed-height overlay sits over the active game instead of recreating the WebView. Unknown fields and future event values remain inspectable, malformed input becomes a visible diagnostic entry, and `go_home` is never treated as an automatic navigation command.

## Web iframe example

Serve [`web/index.html`](web/index.html) from HTTPS for real integration testing.
See the [Web setup guide](web/README.md) for local UI testing and HTTPS referrer
requirements. It also supports direct launch parameters:

```text
?mode=individual&url=<encoded-https-url>
?mode=battles&url=<encoded-final-battles-url>
```

The page accepts a message only when `event.source === iframe.contentWindow` and `event.origin` equals the configured iframe origin. A target can still refuse embedding through `Content-Security-Policy: frame-ancestors` or `X-Frame-Options`; that restriction cannot be bypassed by the parent page.

## Scope

This repository is deliberately limited to client-side WebView/iframe events. It
does not implement Google Mobile Ads integration, server-side webhooks, score
submission, event-log uploads, authentication, URL generation, database changes,
or production business actions. The embedded Gamezop website is a separate system
and may make network requests or use cookies and storage.

Supply your real launch URLs at runtime. Captured event history is memory-only;
that is not a promise that the entire app/browser is storage-free. WebView caches,
cookies, Android saved UI state, the Web page's URL/history, and the clipboard may
retain other data. Redact player IDs, names, photos, and room URLs before sharing
logs, screenshots, or issue reports.

Ask the Gamezop account manager to enable client-side broadcasts and the required
events once for the property. The same callback setup serves Individual and Battles.
For Battles, generate the final URL outside these examples and follow the
[Battles setup instructions](docs/callback-configuration.md#individual-and-battles-setup).
Base64 validates an encoding format; it does not encrypt or authorize a room.

Android release variants in this reference use debug signing for local testing.
Do not distribute them as production-signed applications. Configure your own
private signing credentials outside version control before distribution.

## Official documentation

- [Implement game-events listener](https://docs.platform.gamezop.com/publishers/gamezop/advanced/implement-game-events-listener)
- [Individual client-side score broadcasts](https://docs.platform.gamezop.com/publishers/gamezop/advanced/receive-scores#client-side-broadcasts)
- [Battles client-side callbacks](https://docs.platform.gamezop.com/publishers/gamezop/advanced/multiplayer-games/client-side-callbacks)
