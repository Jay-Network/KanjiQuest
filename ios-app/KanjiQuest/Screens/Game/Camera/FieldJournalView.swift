import SwiftUI
import SharedCore

/// Field journal screen. Mirrors Android's FieldJournalScreen.kt.
/// Grid of captured kanji entries, stats, detail view with delete.
struct FieldJournalView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = FieldJournalViewModel()
    var onBack: () -> Void = {}

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Loading journal...").font(KanjiQuestTheme.bodyLarge)
                }
            } else if let entry = viewModel.selectedEntry {
                entryDetail(entry)
            } else if viewModel.entries.isEmpty {
                emptyState
            } else {
                journalGrid
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
                Text("Field Journal").font(.headline).foregroundColor(.white)
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Text("\(viewModel.totalPhotos) captures")
                    .font(KanjiQuestTheme.bodyMedium)
                    .foregroundColor(.white)
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { viewModel.load(container: container) }
    }

    // MARK: - Journal Grid

    private var journalGrid: some View {
        VStack(spacing: 0) {
            // Stats bar
            HStack(spacing: 0) {
                VStack(spacing: 2) {
                    Text("\(viewModel.totalKanjiCaught)")
                        .font(KanjiQuestTheme.headlineSmall)
                        .fontWeight(.bold)
                        .foregroundColor(KanjiQuestTheme.primary)
                    Text("Kanji caught")
                        .font(KanjiQuestTheme.bodySmall)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity)

                VStack(spacing: 2) {
                    Text("\(viewModel.entries.count)")
                        .font(KanjiQuestTheme.headlineSmall)
                        .fontWeight(.bold)
                        .foregroundColor(KanjiQuestTheme.primary)
                    Text("Photos")
                        .font(KanjiQuestTheme.bodySmall)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(12)
            .background(KanjiQuestTheme.surfaceVariant)
            .cornerRadius(KanjiQuestTheme.radiusM)
            .padding(12)

            // Grid
            ScrollView {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(viewModel.entries, id: \.id) { entry in
                        journalEntryCard(entry)
                    }
                }
                .padding(12)
            }
        }
    }

    private func journalEntryCard(_ entry: FieldJournalEntry) -> some View {
        Button {
            viewModel.selectEntry(entry)
        } label: {
            ZStack(alignment: .bottom) {
                // Kanji display
                VStack(spacing: 4) {
                    Spacer()
                    let displayKanji = Array(entry.kanjiFound.prefix(4))
                    HStack(spacing: 2) {
                        ForEach(displayKanji, id: \.self) { kanji in
                            KanjiText(text: kanji)
                                .font(.system(size: 28))
                                .foregroundColor(KanjiQuestTheme.primary)
                        }
                    }
                    if entry.kanjiFound.count > 4 {
                        Text("+\(entry.kanjiFound.count - 4) more")
                            .font(KanjiQuestTheme.bodySmall)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                    }
                    Spacer()
                }
                .frame(maxWidth: .infinity)

                // Bottom overlay
                VStack(alignment: .leading, spacing: 2) {
                    Text(formatDate(entry.capturedAt))
                        .font(KanjiQuestTheme.bodySmall)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    Text("\(entry.kanjiCount) kanji")
                        .font(KanjiQuestTheme.bodySmall)
                        .foregroundColor(.white.opacity(0.8))
                    if !entry.locationLabel.isEmpty {
                        Text(entry.locationLabel)
                            .font(KanjiQuestTheme.bodySmall)
                            .foregroundColor(.white.opacity(0.7))
                            .lineLimit(1)
                    }
                }
                .padding(8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.black.opacity(0.6))
            }
            .frame(height: 160)
            .background(KanjiQuestTheme.surface)
            .cornerRadius(12)
        }
    }

    // MARK: - Entry Detail

    private func entryDetail(_ entry: FieldJournalEntry) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Back to gallery
                Button {
                    viewModel.clearSelection()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("Back to Gallery")
                    }
                    .foregroundColor(KanjiQuestTheme.primary)
                }

                // Date and location
                Text(formatDate(entry.capturedAt))
                    .font(KanjiQuestTheme.titleLarge)
                    .fontWeight(.bold)

                if !entry.locationLabel.isEmpty {
                    Text(entry.locationLabel)
                        .font(KanjiQuestTheme.bodyLarge)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                }

                // Detected kanji
                Text("Detected Kanji")
                    .font(KanjiQuestTheme.titleMedium)
                    .fontWeight(.bold)

                VStack(spacing: 0) {
                    if entry.kanjiFound.isEmpty {
                        Text("No kanji recorded")
                            .font(KanjiQuestTheme.bodyMedium)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            .padding(12)
                    } else {
                        ForEach(entry.kanjiFound, id: \.self) { kanji in
                            HStack(spacing: 12) {
                                KanjiText(text: kanji)
                                    .font(.system(size: 24))
                                    .foregroundColor(KanjiQuestTheme.primary)
                                    .frame(width: 48, height: 48)
                                    .background(KanjiQuestTheme.primary.opacity(0.12))
                                    .cornerRadius(8)

                                Text(kanji)
                                    .font(KanjiQuestTheme.bodyLarge)
                            }
                            .padding(.vertical, 6)
                        }
                    }
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(KanjiQuestTheme.surface)
                .cornerRadius(KanjiQuestTheme.radiusM)

                Spacer().frame(height: 8)

                // Delete button
                Button {
                    viewModel.deleteEntry(entry.id)
                } label: {
                    Text("Delete Entry")
                        .foregroundColor(KanjiQuestTheme.error)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(16)
        }
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 16) {
            AssetImage(filename: "empty-journal.png")
                .frame(width: 160, height: 160)
            Text("No captures yet")
                .font(KanjiQuestTheme.headlineSmall)
                .fontWeight(.bold)
            Text("Use Camera Challenge mode to find and capture wild kanji in the real world!")
                .font(KanjiQuestTheme.bodyLarge)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                .multilineTextAlignment(.center)
        }
        .padding(24)
    }

    // MARK: - Helpers

    private func formatDate(_ epochSeconds: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochSeconds))
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM d, yyyy"
        return fmt.string(from: date)
    }
}
