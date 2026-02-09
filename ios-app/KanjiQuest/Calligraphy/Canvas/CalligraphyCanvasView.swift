import SwiftUI

/// SwiftUI wrapper for the UIKit-based calligraphy canvas.
/// Uses UIViewRepresentable to bridge the custom touch-handling UIView.
struct CalligraphyCanvasView: UIViewRepresentable {
    @Binding var strokes: [[CalligraphyPointData]]
    @Binding var activeStroke: [CalligraphyPointData]
    let referenceStrokePaths: [String]  // SVG path strings for ghost overlay
    var onStrokeComplete: (([CalligraphyPointData]) -> Void)?

    func makeUIView(context: Context) -> CalligraphyCanvasUIView {
        let view = CalligraphyCanvasUIView()
        view.delegate = context.coordinator
        view.referenceStrokePaths = referenceStrokePaths
        return view
    }

    func updateUIView(_ uiView: CalligraphyCanvasUIView, context: Context) {
        uiView.referenceStrokePaths = referenceStrokePaths
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    final class Coordinator: NSObject, CalligraphyCanvasDelegate {
        let parent: CalligraphyCanvasView

        init(_ parent: CalligraphyCanvasView) {
            self.parent = parent
        }

        func canvasDidUpdateActiveStroke(_ points: [CalligraphyPointData]) {
            parent.activeStroke = points
        }

        func canvasDidCompleteStroke(_ points: [CalligraphyPointData]) {
            parent.strokes.append(points)
            parent.activeStroke = []
            parent.onStrokeComplete?(points)
        }

        func canvasDidClear() {
            parent.strokes = []
            parent.activeStroke = []
        }
    }
}
