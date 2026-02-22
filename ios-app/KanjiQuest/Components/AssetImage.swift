import SwiftUI

/// Loads and displays a PNG image from the app bundle's images/ directory.
/// Mirrors Android's AssetImage composable.
struct AssetImage: View {
    let filename: String
    let contentDescription: String?
    var contentMode: ContentMode = .fit

    var body: some View {
        if let uiImage = UIImage(named: filename) ?? loadFromBundle(filename) {
            Image(uiImage: uiImage)
                .resizable()
                .aspectRatio(contentMode: contentMode)
                .accessibilityLabel(contentDescription ?? "")
        } else {
            Image(systemName: "photo")
                .foregroundColor(.gray)
                .accessibilityLabel(contentDescription ?? "Image not found")
        }
    }

    private func loadFromBundle(_ name: String) -> UIImage? {
        if let path = Bundle.main.path(forResource: "images/\(name)", ofType: nil) {
            return UIImage(contentsOfFile: path)
        }
        let nameWithoutExt = (name as NSString).deletingPathExtension
        let ext = (name as NSString).pathExtension
        if let path = Bundle.main.path(forResource: nameWithoutExt, ofType: ext.isEmpty ? "png" : ext, inDirectory: "images") {
            return UIImage(contentsOfFile: path)
        }
        return nil
    }
}

/// Displays a radical image from the radicals/ directory.
struct RadicalImage: View {
    let radicalId: Int32
    let contentDescription: String?
    var contentMode: ContentMode = .fit

    var body: some View {
        let filename = "radical_\(radicalId).png"
        if let uiImage = loadRadical(filename) {
            Image(uiImage: uiImage)
                .resizable()
                .aspectRatio(contentMode: contentMode)
                .accessibilityLabel(contentDescription ?? "")
        } else {
            Text(contentDescription ?? "?")
                .font(.system(size: 40))
        }
    }

    private func loadRadical(_ name: String) -> UIImage? {
        let nameWithoutExt = (name as NSString).deletingPathExtension
        if let path = Bundle.main.path(forResource: nameWithoutExt, ofType: "png", inDirectory: "radicals") {
            return UIImage(contentsOfFile: path)
        }
        return nil
    }
}
