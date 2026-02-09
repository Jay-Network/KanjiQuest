import XCTest

final class KanjiQuestUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testLaunchAndLogin() throws {
        let app = XCUIApplication()
        app.launch()

        // Verify login screen appears
        XCTAssertTrue(app.staticTexts["KanjiQuest"].exists)
    }
}
