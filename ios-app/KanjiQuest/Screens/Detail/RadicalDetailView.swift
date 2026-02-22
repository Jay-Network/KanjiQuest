import SwiftUI
import SharedCore

/// Radical detail screen. Mirrors Android's RadicalDetailScreen.kt.
/// Radical display, info, example kanji grid.
struct RadicalDetailView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = RadicalDetailViewModel()
    let radicalId: Int32
    var onBack: () -> Void = {}
    var onKanjiClick: ((Int32) -> Void)? = nil

    private let radicalColor = Color(hex: 0x795548)

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView().tint(radicalColor)
                    Text("Loading...").font(KanjiQuestTheme.bodyLarge)
                }
            } else if let radical = viewModel.radical {
                ScrollView {
                    VStack(spacing: 16) {
                        // Radical display card
                        ZStack {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(radicalColor)
                                .frame(width: 180, height: 180)
                                .shadow(color: .black.opacity(0.15), radius: 4, y: 2)

                            Text(radical.literal)
                                .font(.system(size: 100))
                                .foregroundColor(.white)
                        }

                        // Japanese name
                        if let meaningJp = radical.meaningJp, !meaningJp.isEmpty {
                            Text(meaningJp)
                                .font(KanjiQuestTheme.headlineMedium).fontWeight(.bold)
                                .foregroundColor(KanjiQuestTheme.onSurface)
                        }

                        // English meaning
                        Text(radical.meaningEn)
                            .font(KanjiQuestTheme.titleLarge)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                        // Info row
                        Text("\(radical.strokeCount) strokes  •  Priority \(radical.priority)  •  Freq \(radical.frequency)")
                            .font(KanjiQuestTheme.bodyMedium)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                        if let position = radical.position, !position.isEmpty {
                            Text("Position: \(position)")
                                .font(KanjiQuestTheme.bodyMedium)
                                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        }

                        Spacer().frame(height: 8)

                        // Example kanji
                        if !viewModel.exampleKanji.isEmpty {
                            Text("Kanji using this radical (\(viewModel.exampleKanji.count))")
                                .font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                                ForEach(viewModel.exampleKanji, id: \.id) { kanji in
                                    Button {
                                        onKanjiClick?(kanji.id)
                                    } label: {
                                        VStack(spacing: 2) {
                                            Text(kanji.literal)
                                                .font(.system(size: 28))
                                                .foregroundColor(KanjiQuestTheme.onSurface)
                                            if let grade = kanji.grade {
                                                Text("G\(grade)")
                                                    .font(.system(size: 10))
                                                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                                            }
                                        }
                                        .frame(width: 72, height: 72)
                                        .background(KanjiQuestTheme.surface)
                                        .cornerRadius(8)
                                    }
                                }
                            }
                        } else {
                            Text("No kanji found for this radical")
                                .font(KanjiQuestTheme.bodyMedium)
                                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        }
                    }
                    .padding(16)
                }
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left"); Text("Back")
                }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) {
                let title = viewModel.radical.map { "\($0.literal) \($0.meaningJp ?? "")" } ?? "Radical"
                Text(title).font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(radicalColor, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { viewModel.load(container: container, radicalId: radicalId) }
    }
}
