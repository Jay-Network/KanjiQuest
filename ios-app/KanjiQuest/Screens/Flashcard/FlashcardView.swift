import SwiftUI
import SharedCore

/// Flashcard deck management screen. Mirrors Android's FlashcardScreen.kt.
/// Deck tabs, card list, create/edit/delete deck dialogs.
struct FlashcardView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = FlashcardViewModel()
    var onBack: () -> Void = {}
    var onStudy: ((Int64) -> Void)? = nil
    var onKanjiClick: ((Int32) -> Void)? = nil

    @State private var showCreateSheet = false
    @State private var newDeckName = ""
    @State private var showEditSheet = false
    @State private var editDeckName = ""
    @State private var showDeleteConfirm = false

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Loading flashcards...").font(KanjiQuestTheme.bodyLarge)
                }
            } else {
                VStack(spacing: 0) {
                    // Deck tabs
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(viewModel.deckGroups, id: \.id) { deck in
                                let selected = viewModel.selectedDeckId == deck.id
                                Button { viewModel.selectDeck(deck) } label: {
                                    Text(deck.name)
                                        .font(KanjiQuestTheme.bodyMedium)
                                        .fontWeight(selected ? .bold : .regular)
                                        .foregroundColor(selected ? .white : KanjiQuestTheme.onSurfaceVariant)
                                        .padding(.horizontal, 16).padding(.vertical, 8)
                                        .background(selected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                                        .cornerRadius(20)
                                }
                                .onLongPressGesture {
                                    viewModel.editingDeck = deck
                                    editDeckName = deck.name
                                    showEditSheet = true
                                }
                            }

                            // Add deck button
                            Button { showCreateSheet = true } label: {
                                Image(systemName: "plus")
                                    .foregroundColor(KanjiQuestTheme.primary)
                                    .padding(8)
                                    .background(KanjiQuestTheme.surfaceVariant)
                                    .clipShape(Circle())
                            }
                        }
                        .padding(.horizontal, 16).padding(.vertical, 8)
                    }

                    if viewModel.items.isEmpty {
                        // Empty state
                        VStack(spacing: 16) {
                            Spacer()
                            AssetImage(filename: "empty-flashcards.png", contentDescription: nil)
                                .frame(width: 120, height: 120)
                            Text("No flashcards yet")
                                .font(KanjiQuestTheme.titleMedium)
                            Text("Add kanji from the browser or game results")
                                .font(KanjiQuestTheme.bodyMedium)
                                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                                .multilineTextAlignment(.center)
                            Spacer()
                        }
                        .padding(24)
                    } else {
                        // Study button
                        Button {
                            onStudy?(viewModel.selectedDeckId)
                        } label: {
                            Text("Study (\(viewModel.items.count) cards)")
                                .font(KanjiQuestTheme.labelLarge).fontWeight(.bold)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity).frame(height: 48)
                                .background(KanjiQuestTheme.primary)
                                .cornerRadius(12)
                        }
                        .padding(.horizontal, 16).padding(.vertical, 8)

                        // Card list
                        ScrollView {
                            LazyVStack(spacing: 8) {
                                ForEach(viewModel.items) { item in
                                    flashcardRow(item)
                                }
                            }
                            .padding(16)
                        }
                    }
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
                Text("Flashcards").font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .alert("Create Deck", isPresented: $showCreateSheet) {
            TextField("Deck name", text: $newDeckName)
            Button("Create") {
                if !newDeckName.isEmpty { viewModel.createDeck(name: newDeckName); newDeckName = "" }
            }
            Button("Cancel", role: .cancel) { newDeckName = "" }
        }
        .alert("Edit Deck", isPresented: $showEditSheet) {
            TextField("Deck name", text: $editDeckName)
            Button("Rename") {
                if let deck = viewModel.editingDeck, !editDeckName.isEmpty {
                    viewModel.renameDeck(deck, to: editDeckName)
                }
            }
            Button("Delete", role: .destructive) { showDeleteConfirm = true }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Delete Deck?", isPresented: $showDeleteConfirm) {
            Button("Delete", role: .destructive) {
                if let deck = viewModel.editingDeck { viewModel.deleteDeck(deck) }
            }
            Button("Cancel", role: .cancel) {}
        }
        .task { viewModel.load(container: container) }
    }

    private func flashcardRow(_ item: FlashcardItem) -> some View {
        HStack(spacing: 12) {
            if let kanji = item.kanji {
                KanjiText(
                    text: kanji.literal,
                    font: .system(size: 36, weight: .regular, design: .serif)
                )
                .frame(width: 56, height: 56)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(item.kanji?.meaningsEn.first ?? "Unknown")
                    .font(KanjiQuestTheme.bodyLarge).fontWeight(.medium)
                Text(item.kanji?.onReadings.joined(separator: ", ") ?? "")
                    .font(KanjiQuestTheme.bodySmall)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            }

            Spacer()

            Button { viewModel.removeFromDeck(item) } label: {
                Image(systemName: "xmark.circle")
                    .foregroundColor(KanjiQuestTheme.error.opacity(0.6))
            }
        }
        .padding(12)
        .background(KanjiQuestTheme.surface)
        .cornerRadius(KanjiQuestTheme.radiusM)
    }
}
