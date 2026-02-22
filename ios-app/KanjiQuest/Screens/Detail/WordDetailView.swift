import SwiftUI
import SharedCore

/// Word detail screen. Mirrors Android's WordDetailScreen.kt.
/// Large kanji form, reading, info chips, meanings list, related kanji.
struct WordDetailView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = WordDetailViewModel()
    let wordId: Int64
    var onBack: () -> Void = {}
    var onKanjiClick: ((Int32) -> Void)? = nil

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Loading...").font(KanjiQuestTheme.bodyLarge)
                }
            } else if let vocab = viewModel.vocabulary {
                ScrollView {
                    VStack(spacing: 12) {
                        // Large kanji form
                        KanjiText(
                            text: vocab.word,
                            font: .system(size: 80, weight: .regular, design: .serif)
                        )
                        .padding(.vertical, 16)

                        // Reading
                        Text(vocab.reading)
                            .font(KanjiQuestTheme.headlineSmall)
                            .foregroundColor(KanjiQuestTheme.primary)

                        // Info chips
                        HStack(spacing: 8) {
                            if let jlpt = vocab.jlptLevel {
                                chipLabel("JLPT N\(jlpt)")
                            }
                            if let freq = vocab.frequency {
                                chipLabel("Freq #\(freq)")
                            }
                        }

                        Spacer().frame(height: 4)

                        // Meanings
                        sectionCard(title: "Meanings") {
                            VStack(alignment: .leading, spacing: 4) {
                                ForEach(Array(vocab.meanings.enumerated()), id: \.offset) { index, meaning in
                                    Text("\(index + 1). \(meaning)")
                                        .font(KanjiQuestTheme.bodyLarge)
                                }
                            }
                        }

                        // Related kanji
                        if !viewModel.relatedKanji.isEmpty {
                            sectionCard(title: "Related Kanji") {
                                VStack(spacing: 8) {
                                    ForEach(viewModel.relatedKanji, id: \.id) { kanji in
                                        Button {
                                            onKanjiClick?(kanji.id)
                                        } label: {
                                            HStack(spacing: 12) {
                                                KanjiText(
                                                    text: kanji.literal,
                                                    font: .system(size: 32, weight: .bold, design: .serif)
                                                )
                                                VStack(alignment: .leading) {
                                                    Text(kanji.meanings.joined(separator: ", "))
                                                        .font(KanjiQuestTheme.bodyMedium).fontWeight(.medium)
                                                        .foregroundColor(KanjiQuestTheme.onSurface)
                                                    if let grade = kanji.grade {
                                                        Text("Grade \(grade)")
                                                            .font(KanjiQuestTheme.bodySmall)
                                                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                                                    }
                                                }
                                                Spacer()
                                            }
                                            .padding(.vertical, 4)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer().frame(height: 16)
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
                Text("Word Detail").font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { viewModel.load(container: container, wordId: wordId) }
    }

    private func chipLabel(_ text: String) -> some View {
        Text(text)
            .font(KanjiQuestTheme.labelMedium)
            .padding(.horizontal, 12).padding(.vertical, 6)
            .background(KanjiQuestTheme.surfaceVariant)
            .cornerRadius(16)
    }

    private func sectionCard<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(KanjiQuestTheme.titleSmall).fontWeight(.bold)
                .foregroundColor(KanjiQuestTheme.primary)
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(KanjiQuestTheme.surface)
        .cornerRadius(KanjiQuestTheme.radiusM)
    }
}
