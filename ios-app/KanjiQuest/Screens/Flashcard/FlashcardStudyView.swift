import SwiftUI
import SharedCore

/// Flashcard study session screen. Mirrors Android's FlashcardStudyScreen.kt.
/// Card flip animation, Again/Hard/Good/Easy grading, completion summary.
struct FlashcardStudyView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = FlashcardStudyViewModel()
    let deckId: Int64
    var onBack: () -> Void = {}

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Loading cards...").font(KanjiQuestTheme.bodyLarge)
                }
            } else if viewModel.cards.isEmpty {
                Text("No cards to study")
                    .font(KanjiQuestTheme.bodyLarge)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            } else if viewModel.isComplete {
                studyComplete
            } else if let card = viewModel.currentCard {
                studyCardContent(card)
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
                Text("Study").font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { viewModel.load(container: container, deckId: deckId) }
    }

    // MARK: - Study Card Content

    private func studyCardContent(_ card: StudyCard) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                // Progress
                Text("\(viewModel.currentIndex + 1) of \(viewModel.cards.count)")
                    .font(KanjiQuestTheme.bodyMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                // Flashcard
                VStack {
                    if !viewModel.isFlipped {
                        // Front: kanji only
                        VStack(spacing: 16) {
                            Spacer()
                            KanjiText(text: card.kanji.literal)
                                .font(.system(size: 96))
                            Text("Tap to reveal")
                                .font(KanjiQuestTheme.bodyMedium)
                                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            Spacer()
                        }
                    } else {
                        // Back: meanings, readings, vocabulary
                        VStack(spacing: 8) {
                            Spacer()
                            KanjiText(text: card.kanji.literal)
                                .font(.system(size: 48))

                            Text(card.kanji.meaningsEn.joined(separator: ", "))
                                .font(KanjiQuestTheme.titleMedium)
                                .fontWeight(.bold)
                                .multilineTextAlignment(.center)

                            if !card.kanji.primaryOnReading.isEmpty {
                                Text("On: \(card.kanji.primaryOnReading)")
                                    .font(KanjiQuestTheme.bodyLarge)
                                    .foregroundColor(KanjiQuestTheme.primary)
                            }
                            if !card.kanji.primaryKunReading.isEmpty {
                                Text("Kun: \(card.kanji.primaryKunReading)")
                                    .font(KanjiQuestTheme.bodyLarge)
                                    .foregroundColor(KanjiQuestTheme.tertiary)
                            }

                            if !card.vocabulary.isEmpty {
                                ForEach(card.vocabulary, id: \.kanjiForm) { vocab in
                                    Text("\(vocab.kanjiForm) (\(vocab.reading)) - \(vocab.primaryMeaning)")
                                        .font(KanjiQuestTheme.bodySmall)
                                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                                        .multilineTextAlignment(.center)
                                }
                            }
                            Spacer()
                        }
                        .padding(16)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 300)
                .background(KanjiQuestTheme.surface)
                .cornerRadius(12)
                .shadow(radius: 4)
                .onTapGesture {
                    if !viewModel.isFlipped { viewModel.flip() }
                }

                // Grade buttons
                if viewModel.isFlipped {
                    HStack(spacing: 8) {
                        ForEach(StudyGrade.allCases, id: \.self) { grade in
                            Button {
                                viewModel.grade(grade)
                            } label: {
                                Text(grade.label)
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(gradeColor(grade))
                                    .cornerRadius(8)
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }

    // MARK: - Study Complete

    private var studyComplete: some View {
        VStack(spacing: 16) {
            Spacer()

            Text("Study Complete!")
                .font(KanjiQuestTheme.headlineMedium)
                .fontWeight(.bold)

            Text("\(viewModel.totalStudied) cards studied")
                .font(KanjiQuestTheme.titleMedium)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

            // Grade breakdown
            VStack(spacing: 8) {
                let again = viewModel.gradeResults[.again] ?? 0
                let hard = viewModel.gradeResults[.hard] ?? 0
                let good = viewModel.gradeResults[.good] ?? 0
                let easy = viewModel.gradeResults[.easy] ?? 0

                if again > 0 { gradeRow("Again", count: again, color: Color(hex: 0xF44336)) }
                if hard > 0 { gradeRow("Hard", count: hard, color: Color(hex: 0xFF9800)) }
                if good > 0 { gradeRow("Good", count: good, color: Color(hex: 0x4CAF50)) }
                if easy > 0 { gradeRow("Easy", count: easy, color: Color(hex: 0x2196F3)) }
            }
            .padding(16)
            .background(KanjiQuestTheme.surface)
            .cornerRadius(KanjiQuestTheme.radiusM)

            Spacer()

            Button {
                onBack()
            } label: {
                Text("Done")
                    .font(.system(size: 16))
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(KanjiQuestTheme.primary)
                    .cornerRadius(12)
            }
        }
        .padding(24)
    }

    private func gradeRow(_ label: String, count: Int, color: Color) -> some View {
        HStack {
            Text(label)
                .font(KanjiQuestTheme.bodyLarge)
                .fontWeight(.bold)
                .foregroundColor(color)
            Spacer()
            Text("\(count)")
                .font(KanjiQuestTheme.bodyLarge)
                .fontWeight(.bold)
        }
        .padding(.vertical, 4)
    }

    private func gradeColor(_ grade: StudyGrade) -> Color {
        switch grade {
        case .again: return Color(hex: 0xF44336)
        case .hard: return Color(hex: 0xFF9800)
        case .good: return Color(hex: 0x4CAF50)
        case .easy: return Color(hex: 0x2196F3)
        }
    }
}
