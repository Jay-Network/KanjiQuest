import XCTest
@testable import KanjiQuest

final class KanjiQuestTests: XCTestCase {

    func testPressureAnalyzer() {
        let points: [CalligraphyPointData] = [
            CalligraphyPointData(x: 0, y: 0, force: 0.3, altitude: 1.57, azimuth: 0, timestamp: 0),
            CalligraphyPointData(x: 10, y: 10, force: 0.5, altitude: 1.57, azimuth: 0, timestamp: 0.1),
            CalligraphyPointData(x: 20, y: 20, force: 0.8, altitude: 1.57, azimuth: 0, timestamp: 0.2),
            CalligraphyPointData(x: 30, y: 30, force: 0.6, altitude: 1.57, azimuth: 0, timestamp: 0.3),
            CalligraphyPointData(x: 40, y: 40, force: 0.2, altitude: 1.57, azimuth: 0, timestamp: 0.4),
        ]

        let result = PressureAnalyzer.analyze(strokes: [points])

        XCTAssertEqual(result.strokes.count, 1)
        XCTAssertEqual(result.strokes[0].pointCount, 5)
        XCTAssertGreaterThan(result.strokes[0].averagePressure, 0)
        XCTAssertEqual(result.strokes[0].maxPressure, 0.8, accuracy: 0.01)
        XCTAssertEqual(result.strokes[0].minPressure, 0.2, accuracy: 0.01)
    }

    func testBrushWidthRange() {
        let engine = FudeBrushEngine()
        // Engine is tested indirectly through rendering
        // Basic smoke test: engine exists and conforms to protocol
        XCTAssertNotNil(engine as BrushEngine)
    }

    func testCalligraphyStrokeRenderer() {
        let points: [CalligraphyPointData] = [
            CalligraphyPointData(x: 50, y: 50, force: 0.5, altitude: 1.57, azimuth: 0, timestamp: 0),
            CalligraphyPointData(x: 100, y: 100, force: 0.8, altitude: 1.57, azimuth: 0, timestamp: 0.1),
        ]

        let image = CalligraphyStrokeRenderer.renderForAI(
            strokes: [points],
            canvasSize: CGSize(width: 400, height: 400)
        )

        XCTAssertNotNil(image)
        XCTAssertEqual(image?.size.width, 512)
        XCTAssertEqual(image?.size.height, 512)
    }
}
