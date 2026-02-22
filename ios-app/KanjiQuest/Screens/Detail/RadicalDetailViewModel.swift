import Foundation
import SharedCore

@MainActor
final class RadicalDetailViewModel: ObservableObject {
    @Published var radical: Radical? = nil
    @Published var exampleKanji: [Kanji] = []
    @Published var isLoading = true

    private var radicalRepository: RadicalRepository?
    private var kanjiRepository: KanjiRepository?

    func load(container: AppContainer, radicalId: Int32) {
        radicalRepository = container.radicalRepository
        kanjiRepository = container.kanjiRepository

        Task {
            do {
                let rad = try await radicalRepository?.getRadicalById(id: radicalId)
                var kanji: [Kanji] = []
                if rad != nil {
                    let kanjiIds = (try? await radicalRepository?.getKanjiIdsForRadical(radicalId: radicalId)) ?? []
                    for id in kanjiIds {
                        if let k = try? await kanjiRepository?.getKanjiById(id: Int32(id)) {
                            kanji.append(k)
                        }
                    }
                    kanji.sort { lhs, rhs in
                        let lGrade = lhs.grade ?? 99
                        let rGrade = rhs.grade ?? 99
                        if lGrade != rGrade { return lGrade < rGrade }
                        let lFreq = lhs.frequency ?? 9999
                        let rFreq = rhs.frequency ?? 9999
                        return lFreq < rFreq
                    }
                }
                self.radical = rad
                self.exampleKanji = kanji
            } catch {
                self.radical = nil
                self.exampleKanji = []
            }
            self.isLoading = false
        }
    }
}
