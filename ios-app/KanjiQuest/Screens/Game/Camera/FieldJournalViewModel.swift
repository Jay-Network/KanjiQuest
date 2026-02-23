import Foundation
import SharedCore

@MainActor
final class FieldJournalViewModel: ObservableObject {
    @Published var entries: [FieldJournalEntry] = []
    @Published var totalPhotos: Int64 = 0
    @Published var totalKanjiCaught: Int64 = 0
    @Published var isLoading = true
    @Published var selectedEntry: FieldJournalEntry? = nil

    private var repository: FieldJournalRepository?

    func load(container: AppContainer) {
        repository = container.fieldJournalRepository
        loadEntries()
    }

    private func loadEntries() {
        Task {
            let all = (try? await repository?.getAll()) ?? []
            let totalRaw = try? await repository?.countAll()
            let caughtRaw = try? await repository?.totalKanjiCaught()

            entries = all
            totalPhotos = totalRaw?.int64Value ?? 0
            totalKanjiCaught = caughtRaw?.int64Value ?? 0
            isLoading = false
        }
    }

    func selectEntry(_ entry: FieldJournalEntry) {
        selectedEntry = entry
    }

    func clearSelection() {
        selectedEntry = nil
    }

    func deleteEntry(_ id: Int64) {
        Task {
            try? await repository?.delete(id: id)
            loadEntries()
            selectedEntry = nil
        }
    }
}
