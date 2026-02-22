import SwiftUI

/// Text view that forces Japanese locale for correct CJK glyph rendering.
/// Without this, iOS may render kanji using Chinese font variants.
struct KanjiText: View {
    let text: String
    var font: Font = .body
    var fontWeight: Font.Weight?
    var color: Color?
    var alignment: TextAlignment?

    var body: some View {
        Text(text)
            .font(font)
            .fontWeight(fontWeight)
            .foregroundColor(color)
            .multilineTextAlignment(alignment ?? .leading)
            .environment(\.locale, Locale(identifier: "ja"))
    }
}
