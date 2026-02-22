import Foundation
import SharedCore

@MainActor
final class WordDetailViewModel: ObservableObject {
    @Published var vocabulary: Vocabulary? = nil
    @Published var relatedKanji: [Kanji] = []
    @Published var isLoading = true

    private var kanjiRepository: KanjiRepository?

    func load(container: AppContainer, wordId: Int64) {
        kanjiRepository = container.kanjiRepository

        Task {
            let vocab = try? await kanjiRepository?.getVocabularyById(id: wordId)
            var kanji: [Kanji] = []
            if vocab != nil {
                let kanjiIds = (try? await kanjiRepository?.getKanjiIdsForVocab(vocabId: wordId)) ?? []
                for id in kanjiIds {
                    if let k = try? await kanjiRepository?.getKanjiById(id: Int32(id)) {
                        kanji.append(k)
                    }
                }
            }

            self.vocabulary = vocab
            self.relatedKanji = kanji
            self.isLoading = false
        }
    }
}
