import SwiftUI

/// SwiftUI wrapper for the UIKit-based calligraphy canvas.
/// Uses UIViewRepresentable to bridge the custom touch-handling UIView.
struct CalligraphyCanvasView: UIViewRepresentable {
    @Binding var strokes: [[CalligraphyPointData]]
    @Binding var activeStroke: [CalligraphyPointData]
    let referenceStrokePaths: [String]  // SVG path strings for ghost overlay
    var canvasVersion: Int = 0
    var brushSize: BrushSize = .medium
    var inkConcentration: CGFloat = 1.0
    var isSoundMuted: Bool = false
    var onStrokeComplete: (([CalligraphyPointData]) -> Void)?

    func makeUIView(context: Context) -> CalligraphyCanvasUIView {
        let view = CalligraphyCanvasUIView()
        view.delegate = context.coordinator
        view.referenceStrokePaths = referenceStrokePaths
        view.brushSize = brushSize
        view.inkConcentration = inkConcentration
        view.isSoundMuted = isSoundMuted
        context.coordinator.lastCanvasVersion = canvasVersion
        return view
    }

    func updateUIView(_ uiView: CalligraphyCanvasUIView, context: Context) {
        if context.coordinator.lastCanvasVersion != canvasVersion {
            context.coordinator.lastCanvasVersion = canvasVersion
            uiView.clearDrawing()
        }
        uiView.referenceStrokePaths = referenceStrokePaths
        uiView.brushSize = brushSize
        uiView.inkConcentration = inkConcentration
        uiView.isSoundMuted = isSoundMuted
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    final class Coordinator: NSObject, CalligraphyCanvasDelegate {
        let parent: CalligraphyCanvasView
        var lastCanvasVersion: Int = 0

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
