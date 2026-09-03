import XCTest

final class LauncherUITests: XCTestCase {
    @MainActor
    func testInvalidURLShowsValidation() {
        let app = XCUIApplication()
        app.launch()
        let input = app.textFields["url-input"]
        input.tap()
        input.typeText("http://insecure.example")
        app.buttons["launch-game"].tap()
        XCTAssertTrue(app.staticTexts["validation-error"].waitForExistence(timeout: 2))
    }
}
