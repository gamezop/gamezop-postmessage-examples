# Android Gamezop PostMessage reference app

This Kotlin/Jetpack Compose app demonstrates the complete Android side of Gamezop client broadcasts. It receives both individual score states and Battles callbacks through a WebView, parses them defensively, and displays them in a live inspector.

The app intentionally contains no advertising SDK, webhook, score-submission, analytics, or user-account code.

## Requirements

- Android Studio with Android SDK 36
- JDK 17 or newer for Gradle (source and target compatibility remain Java 17)
- Android device or emulator running API 24+

## Run it

Open this `android/` directory in Android Studio, or build from the command line:

```bash
./gradlew assembleDebug
```

Install the debug build through Android Studio or with:

```bash
./gradlew installDebug
```

The launcher uses separate **Individual** and **Battles** tabs. Switching tabs hides the other
mode's launch block and preserves each tab's URL independently. Both modes open the same
hardened WebView and use the same live event inspector.

### Test a real individual game

1. Ask your Gamezop Account Manager to enable client-side broadcasts for your property; this feature is plan-dependent.
2. Select **Individual**.
3. Paste the HTTPS game URL or Gamezop Unique Link supplied for your property.
4. Launch the game and play. `loaded`, `start`, `playing`, `over`, and applicable `levelup` states appear in the inspector.

After either mode launches, use the draggable **Logs** pill over the game. Its badge updates as
callbacks arrive. Tap it to show or hide a fixed-height event overlay without resizing or
reloading the active WebView. The pill and overlay are constrained to Android's safe drawing area.

### Test a real Battles game

The app accepts the final Battles URL; it does not generate `roomDetails`. Before launch, it
requires exactly one non-empty `roomDetails` query parameter, Base64-decodes it (standard or
URL-safe alphabet), and verifies that the decoded value is a non-empty JSON object. This prevents
an ordinary or malformed URL from being launched accidentally in Battles mode.
The same callback setup serves Individual and Battles. Example room details:

```json
{
  "roomId": "ABC01",
  "user": {
    "name": "John Doe",
    "photo": "https://example.com/photos/user-1.jpg",
    "sub": "user-123"
  },
  "maxPlayers": 2,
  "minPlayers": 2,
  "maxWait": 60,
  "rounds": 1,
  "text": "go_home",
  "allowBots": false
}
```

Base64-encode and URL-encode the serialized object as described in the [multiplayer guide](https://docs.platform.gamezop.com/publishers/gamezop/advanced/multiplayer-games), then append it as the `roomDetails` query parameter to the game URL. Use unique `user.sub` values on two clients but the same `roomId`, launch both final URLs, and complete a match.

## The required Android bridge

| Setting | Value in this sample |
| --- | --- |
| JavaScript bridge | `AndroidBridge` |
| Method | `postMessage` |
| Payload | Stringified JSON |

The names are case-sensitive. Register this bridge before loading Gamezop Games.
Ask your account manager to enable callbacks once for both Individual and Battles.
See the [cross-platform callback reference](../docs/callback-configuration.md).

```kotlin
@Keep
class GameEventBridge(
    private val onEvent: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Keep
    @JavascriptInterface
    fun postMessage(gameEvent: String) {
        mainHandler.post { onEvent(gameEvent) }
    }
}
```

Register it before loading the page:

```kotlin
webView.settings.javaScriptEnabled = true
webView.settings.domStorageEnabled = true
webView.addJavascriptInterface(
    GameEventBridge(onEvent = ::handleRawEvent),
    "AndroidBridge",
)
webView.loadUrl(gameUrl)
```

The release build also contains explicit R8 rules for `@JavascriptInterface` methods so `postMessage` cannot be renamed.

Games commonly render through a full-screen canvas or WebGL surface. The sample gives the
native `WebView` match-parent layout parameters, enables wide-viewport/overview handling,
uses an explicit hardware layer, and makes the view focusable. These settings prevent a page
from completing navigation while its game canvas remains visually blank. They do not require
Google Mobile Ads registration.

## Data flow

```text
Gamezop game JavaScript
  -> AndroidBridge.postMessage(stringifiedJson)
  -> background WebView bridge thread
  -> main-thread dispatcher
  -> GameEventParser
  -> in-memory GameEventViewModel (latest 500)
  -> Compose inspector
```

Payloads with `state` are classified as individual events; payloads with `event` are Battles events. The parser keeps the original string and renders all JSON keys, including fields added after this sample was released. An unknown discriminator remains visible with an `unknown` status. Invalid JSON becomes a `malformed_json` diagnostic entry rather than throwing into the WebView.

The log survives Activity configuration changes because it is owned by a `ViewModel`. It is never persisted to disk and disappears when the process ends. The oldest entry is discarded after 500 callbacks to bound memory during frequent `playing` or `match_playing` updates.

See the shared [event-contract reference](../docs/event-contracts.md) for event meanings and fields.

## WebView safety and lifecycle

`addJavascriptInterface` is injected into every frame and cannot reliably identify the frame origin. It should only expose minimal, non-privileged operations and should never be attached to arbitrary untrusted content. This sample:

- exposes one `String -> Unit` method and no Android `Context`;
- accepts only HTTPS live URLs;
- keeps the pasted launch host plus `gamezop.com` and `umogames.com` subdomains inside the WebView;
- sends off-host navigation to an external application;
- disables WebView file and content access;
- keeps cleartext traffic and mixed HTTPS/HTTP content blocked;
- removes the bridge, stops loading, and destroys the WebView when its Compose host is disposed;
- forwards callbacks to the main thread because WebView invokes JavaScript-interface methods on a private background thread.

Gamezop may supply a different domain for a partner. Pasting that URL trusts its exact host for the session. Review and hard-code the correct domain policy before adapting this sample for production. Also remember that an allowed top-level page can embed third-party frames, which is why the exposed bridge must remain harmless.

Read Android's [`addJavascriptInterface` security notes](https://developer.android.com/reference/android/webkit/WebView#addJavascriptInterface(java.lang.Object,%20java.lang.String)) before extending the bridge.

## Event behavior

The sample records `go_home` but deliberately does not navigate automatically. Production apps can observe that parsed event and perform their own navigation. The same applies to overlays, rewards, analytics, and other business actions.

For payload examples and lifecycle semantics, consult:

- [Gamezop game-events listener](https://docs.platform.gamezop.com/publishers/gamezop/advanced/implement-game-events-listener)
- [Individual client-side broadcasts](https://docs.platform.gamezop.com/publishers/gamezop/advanced/receive-scores#client-side-broadcasts)
- [Battles client-side callbacks](https://docs.platform.gamezop.com/publishers/gamezop/advanced/multiplayer-games/client-side-callbacks)

## Verification

Release variants use debug signing for local verification, not production
distribution. Configure your own private release signing outside version control
before distributing a derived app. Do not commit a production keystore.

Run the local checks:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

With an emulator or device connected, run the Compose/WebView instrumentation flow:

```bash
./gradlew connectedDebugAndroidTest
```

The instrumentation suite verifies the Individual/Battles tab switching and executes JavaScript through `AndroidBridge.postMessage` in a real, test-only WebView. No simulator page is bundled with the production app. Release assembly verifies that the minified build and bridge keep rules are valid.

## Troubleshooting

- **No events from a real game:** confirm client-side broadcasts are enabled for the property and that the page is being shown in this WebView rather than an external browser.
- **Battles loads but callbacks do not arrive:** confirm callbacks are enabled and `AndroidBridge.postMessage` is registered before loading the game.
- **A link opens outside the app:** check the destination host. Off-host navigation is intentional; update your production allowlist only after verifying the domain with Gamezop.
- **`ERR_NAME_NOT_RESOLVED`:** Android could not resolve the URL's hostname. Confirm the exact URL in the device browser, then check the device/emulator connection and DNS configuration. A cold boot often repairs stale emulator DNS. The app distinguishes a fully offline device from a connected device whose DNS lookup failed.
- **A payload is orange or red:** orange means the JSON is valid but its state/event is unknown; red means the payload is not a JSON object. Expand the inspector and copy the raw value for diagnosis.
