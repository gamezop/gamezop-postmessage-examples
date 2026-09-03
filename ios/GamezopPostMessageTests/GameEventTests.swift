import XCTest
import WebKit
@testable import GamezopPostMessage

final class GameEventTests: XCTestCase {
    func testEveryIndividualState() {
        for state in GameEventParser.individualStates {
            let event = GameEventParser.parse(body: ["state": state, "extra": "kept"], id: 1)
            XCTAssertEqual(event.family, .individual)
            XCTAssertEqual(event.status, .valid)
            XCTAssertEqual(event.fields["extra"], .string("kept"))
        }
    }

    func testEveryBattlesEventFromString() {
        for name in GameEventParser.battlesEvents {
            let event = GameEventParser.parse(body: "{\"event\":\"\(name)\"}", id: 1)
            XCTAssertEqual(event.family, .battles)
            XCTAssertEqual(event.status, .valid)
        }
    }

    func testMalformedUnknownAndArrayPayloadsRemainVisible() {
        XCTAssertEqual(GameEventParser.parse(body: "nope", id: 1).status, .malformed)
        XCTAssertEqual(GameEventParser.parse(body: ["future": true], id: 2).status, .unknown)
        XCTAssertEqual(GameEventParser.parse(body: [1, 2], id: 3).status, .malformed)
    }

    @MainActor
    func testStoreCapsAtFiveHundredAndClears() {
        let store = EventStore()
        for index in 0..<505 { store.capture(["state": "playing", "score": index]) }
        XCTAssertEqual(store.events.count, 500)
        XCTAssertEqual(store.droppedEventCount, 5)
        XCTAssertEqual(store.events.first?.fields["score"], .number(5))
        store.clear()
        XCTAssertTrue(store.events.isEmpty)
        XCTAssertEqual(store.droppedEventCount, 0)
    }

    func testURLValidationAndBattlesRoomDetails() throws {
        XCTAssertNil(URLPolicy.validationError("https://123.play.gamezop.com/g/code", mode: .individual))
        XCTAssertNotNil(URLPolicy.validationError("http://example.com", mode: .individual))
        XCTAssertNotNil(URLPolicy.validationError("https://example.com/game", mode: .battles))

        let json = try JSONSerialization.data(withJSONObject: ["roomId": "A"])
        let encoded = json.base64EncodedString().replacingOccurrences(of: "=", with: "")
        XCTAssertNil(URLPolicy.validationError("https://123.play.gamezop.com/g/code?roomDetails=\(encoded)", mode: .battles))
        XCTAssertNotNil(URLPolicy.validationError("https://example.com?roomDetails=%%%", mode: .battles))
        XCTAssertNotNil(URLPolicy.validationError("https://example.com?roomDetails=\(encoded)&roomDetails=\(encoded)", mode: .battles))
    }

    func testNavigationPolicy() throws {
        let launch = try XCTUnwrap(URL(string: "https://partner.example/game"))
        XCTAssertTrue(URLPolicy.isAllowedNavigation(launchURL: launch, candidate: URL(string: "https://partner.example/next")!))
        XCTAssertTrue(URLPolicy.isAllowedNavigation(launchURL: launch, candidate: URL(string: "https://123.play.gamezop.com/g/a")!))
        XCTAssertFalse(URLPolicy.isAllowedNavigation(launchURL: launch, candidate: URL(string: "https://evil.example")!))
    }

    @MainActor
    func testRealWKWebViewObserverHandler() async throws {
        let received = expectation(description: "observer receives JavaScript payload")
        var body: Any?
        let session = WebViewSession()
        let coordinator = GameWebView.Coordinator(
            launchURL: try XCTUnwrap(URL(string: "https://example.com")),
            session: session,
            onEvent: {
                body = $0
                received.fulfill()
            },
            onLoadingChanged: { _ in },
            onError: { _ in }
        )
        let configuration = WKWebViewConfiguration()
        configuration.userContentController.add(coordinator, name: "observer")
        let webView = WKWebView(frame: .zero, configuration: configuration)
        coordinator.webView = webView
        webView.loadHTMLString(
            "<script>window.webkit.messageHandlers.observer.postMessage({state:'loaded',score:0})</script>",
            baseURL: URL(string: "https://example.com")
        )

        await fulfillment(of: [received], timeout: 3)
        let object = try XCTUnwrap(body as? [String: Any])
        XCTAssertEqual(object["state"] as? String, "loaded")
        XCTAssertEqual(object["score"] as? Int, 0)
        configuration.userContentController.removeScriptMessageHandler(forName: "observer")
    }
}
