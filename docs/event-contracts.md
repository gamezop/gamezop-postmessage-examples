# Gamezop client event contracts

Gamezop sends an object or stringified JSON to the embedding host. The reference apps normalize both representations into one captured-event model containing an ID, receipt timestamp, raw JSON, detected family, parsed fields, and validation status.

## Platform callbacks

| Target | Callback |
| --- | --- |
| Android | `AndroidBridge.postMessage(stringifiedJson)` |
| iOS | `window.webkit.messageHandlers.observer.postMessage(payload)` |
| React Native | `ReactNativeWebView.postMessage(stringifiedJson)` |
| Flutter | `FlutterWebView.postMessage(stringifiedJson)` |
| Web | `window.parent.postMessage(payload, targetOrigin)` |

See the [callback reference](callback-configuration.md) for bridge versus method
names, receiver setup, and Web target-origin checks. The same receiver handles
Individual and Battles. URL generation remains a server/partner responsibility.

## Individual games

Individual payloads are identified by the `state` field.

| State | Meaning |
| --- | --- |
| `loaded` | The game finished loading. `score` is always `0`. |
| `start` | A new gameplay began. `score` and `duration` are `0`. |
| `playing` | The score changed while the user was playing. |
| `over` | The gameplay ended; `score` is the final score for that attempt. A session may contain multiple `over` events. |
| `levelup` | A level was completed in a level-based game. |

| Field | Typical type | Meaning |
| --- | --- | --- |
| `state` | String | Lifecycle discriminator from the table above |
| `score` | Number | Current score, or final score for `over` |
| `duration` | Number | Gameplay duration reported by the game; see the official contract for units |
| `gameCode` | String | Game identifier |
| `gamePlayId` | String | Identifier for a gameplay attempt |
| `leaderboard` | Object, when supplied | Leaderboard metadata such as `id`, `name`, and `description`; preserved without interpretation |

Presence depends on event and integration. Consumers must tolerate additional
fields. See the [official score contract](https://docs.platform.gamezop.com/publishers/gamezop/advanced/receive-scores#client-side-broadcasts)
for authoritative payload definitions.

`score` is `0` for `loaded` and both `score` and `duration` are `0` for `start`. A single session can contain several `over` events when the user retries.

## Battles

Battles payloads are identified by the `event` field.

| Event | Meaning |
| --- | --- |
| `match_found` | Opponents were found and the countdown began. |
| `match_not_found` | Matchmaking ended without enough players. |
| `match_start` | The match began. |
| `match_playing` | A player's score or rank changed. |
| `match_over` | The current user exhausted their attempts while another player may still be playing. |
| `match_result` | Final results are known for every player. |
| `go_home` | The user selected the in-game home action. |

Typical fields are `event`, `gameCode`, `matchId`, and `players`. Depending on the event, `players` contains either user IDs or objects with `firstName`, `photo`, `gzpId`, `score`, and `rank`.

| Field | Typical type | Meaning |
| --- | --- | --- |
| `event` | String | Callback discriminator |
| `gameCode` | String | Game identifier |
| `matchId` | String, when supplied | Match identifier |
| `players` | Array, when supplied | Matched user IDs or player/result objects |
| `players[].gzpId` | String | Player identifier in object-form payloads |
| `players[].firstName`, `players[].photo` | Strings, when supplied | Player display data; may identify a person |
| `players[].score`, `players[].rank` | Numbers, when supplied | Reported score and placement |

Do not require a results-shaped `players` array during matchmaking. Refer to the
[official Battles payload examples](https://docs.platform.gamezop.com/publishers/gamezop/advanced/multiplayer-games/client-side-callbacks)
for event-specific field presence.

## Forward compatibility

Implementations should preserve the raw JSON, accept missing optional fields, and display unknown states/events rather than discarding them. Malformed JSON should be isolated as a diagnostic entry instead of crashing the host app.

The examples retain only the newest 500 entries in process memory. They do not upload, persist, or interpret events as business commands. In particular, `go_home` remains visible but does not navigate automatically.

## Launch and navigation policy

- Launch URLs must use HTTPS, include a host, and contain no embedded username/password.
- Battles URLs must contain exactly one `roomDetails` parameter that is standard or URL-safe Base64 and decodes to a non-empty JSON object.
- This is envelope validation, not encryption, room authorization, or verification of every Battles field.
- The launch host and documented Gamezop hosts remain inside mobile WebViews; off-host top-level navigation opens externally.
- The Web iframe accepts `message` events only from its exact `contentWindow` and configured HTTPS origin.
- Iframe loading can be blocked by the target's CSP `frame-ancestors` or `X-Frame-Options` headers.
