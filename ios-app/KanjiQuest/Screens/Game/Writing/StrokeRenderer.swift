import SwiftUI

/// Renders stroke order guides for kanji writing practice.
struct StrokeRenderer: View {
    let strokePaths: [String]
    var highlightIndex: Int? = nil

    var body: some View {
        Canvas { context, size in
            let scale = min(size.width, size.height) / 109.0 // KanjiVG viewBox = 109x109
            for (index, path) in strokePaths.enumerated() {
                let color: Color = {
                    if let hi = highlightIndex {
                        return index == hi ? .red : (index < hi ? .gray : .gray.opacity(0.3))
                    }
                    return .gray
                }()
                if let cgPath = parseSvgPath(path, scale: scale) {
                    context.stroke(Path(cgPath), with: .color(color), lineWidth: 2)
                }
            }
        }
    }

    private func parseSvgPath(_ d: String, scale: CGFloat) -> CGPath? {
        let path = CGMutablePath()
        let scanner = Scanner(string: d)
        scanner.charactersToBeSkipped = CharacterSet.whitespaces.union(.init(charactersIn: ","))
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0

        while !scanner.isAtEnd {
            var cmd: NSString?
            scanner.scanCharacters(from: CharacterSet.letters, into: &cmd)
            guard let command = cmd as? String, let c = command.first else { continue }

            switch c {
            case "M":
                if let x = scanDouble(scanner), let y = scanDouble(scanner) {
                    currentX = x * scale; currentY = y * scale
                    path.move(to: CGPoint(x: currentX, y: currentY))
                }
            case "m":
                if let dx = scanDouble(scanner), let dy = scanDouble(scanner) {
                    currentX += dx * scale; currentY += dy * scale
                    path.move(to: CGPoint(x: currentX, y: currentY))
                }
            case "L":
                if let x = scanDouble(scanner), let y = scanDouble(scanner) {
                    currentX = x * scale; currentY = y * scale
                    path.addLine(to: CGPoint(x: currentX, y: currentY))
                }
            case "l":
                if let dx = scanDouble(scanner), let dy = scanDouble(scanner) {
                    currentX += dx * scale; currentY += dy * scale
                    path.addLine(to: CGPoint(x: currentX, y: currentY))
                }
            case "C":
                if let x1 = scanDouble(scanner), let y1 = scanDouble(scanner),
                   let x2 = scanDouble(scanner), let y2 = scanDouble(scanner),
                   let x = scanDouble(scanner), let y = scanDouble(scanner) {
                    currentX = x * scale; currentY = y * scale
                    path.addCurve(to: CGPoint(x: currentX, y: currentY),
                                  control1: CGPoint(x: x1 * scale, y: y1 * scale),
                                  control2: CGPoint(x: x2 * scale, y: y2 * scale))
                }
            case "c":
                if let dx1 = scanDouble(scanner), let dy1 = scanDouble(scanner),
                   let dx2 = scanDouble(scanner), let dy2 = scanDouble(scanner),
                   let dx = scanDouble(scanner), let dy = scanDouble(scanner) {
                    let cp1 = CGPoint(x: currentX + dx1 * scale, y: currentY + dy1 * scale)
                    let cp2 = CGPoint(x: currentX + dx2 * scale, y: currentY + dy2 * scale)
                    currentX += dx * scale; currentY += dy * scale
                    path.addCurve(to: CGPoint(x: currentX, y: currentY), control1: cp1, control2: cp2)
                }
            case "Z", "z":
                path.closeSubpath()
            default:
                break
            }
        }
        return path
    }

    private func scanDouble(_ scanner: Scanner) -> CGFloat? {
        var value: Double = 0
        return scanner.scanDouble(&value) ? CGFloat(value) : nil
    }
}
