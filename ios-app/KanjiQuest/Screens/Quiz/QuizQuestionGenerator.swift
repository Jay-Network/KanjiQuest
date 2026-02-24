import Foundation
import SharedCore

/// Quiz question model matching Android's QuizQuestion.
struct QuizQuestion {
    let displayCharacter: String
    let prompt: String
    let options: [String]
    let correctIndex: Int
}

/// Test scope matching Android's TestScope enum (14 scopes).
enum TestScope: CaseIterable {
    case hiragana, katakana
    case radicals
    case grade1, grade2, grade3, grade4, grade5, grade6
    case jlptN5, jlptN4, jlptN3, jlptN2, jlptN1

    var displayName: String {
        switch self {
        case .hiragana: return "Hiragana"
        case .katakana: return "Katakana"
        case .radicals: return "Radicals"
        case .grade1: return "Grade 1"
        case .grade2: return "Grade 2"
        case .grade3: return "Grade 3"
        case .grade4: return "Grade 4"
        case .grade5: return "Grade 5"
        case .grade6: return "Grade 6"
        case .jlptN5: return "JLPT N5"
        case .jlptN4: return "JLPT N4"
        case .jlptN3: return "JLPT N3"
        case .jlptN2: return "JLPT N2"
        case .jlptN1: return "JLPT N1"
        }
    }

    var category: String {
        switch self {
        case .hiragana, .katakana: return "Kana"
        case .radicals: return "Radicals"
        case .grade1, .grade2, .grade3, .grade4, .grade5, .grade6: return "School Grade"
        case .jlptN5, .jlptN4, .jlptN3, .jlptN2, .jlptN1: return "JLPT"
        }
    }

    /// Group scopes by category, preserving order.
    static var groupedByCategory: [(String, [TestScope])] {
        var result: [(String, [TestScope])] = []
        var currentCategory = ""
        var currentGroup: [TestScope] = []
        for scope in allCases {
            if scope.category != currentCategory {
                if !currentGroup.isEmpty {
                    result.append((currentCategory, currentGroup))
                }
                currentCategory = scope.category
                currentGroup = [scope]
            } else {
                currentGroup.append(scope)
            }
        }
        if !currentGroup.isEmpty {
            result.append((currentCategory, currentGroup))
        }
        return result
    }
}

/// Quiz question generator matching Android's QuizQuestionGenerator.
@MainActor
class QuizQuestionGenerator {
    private let kanjiRepository: KanjiRepositoryImpl
    private let kanaRepository: KanaRepositoryImpl
    private let radicalRepository: RadicalRepositoryImpl

    init(container: AppContainer) {
        self.kanjiRepository = container.kanjiRepository
        self.kanaRepository = container.kanaRepository
        self.radicalRepository = container.radicalRepository
    }

    func generateQuestions(scope: TestScope, count: Int = 10) async -> [QuizQuestion] {
        switch scope {
        case .hiragana:
            let kanaList = (try? await kanaRepository.getKanaByType(type: .hiragana)) ?? []
            return generateKanaQuestions(kanaList: kanaList, prompt: "What is the reading?", count: count)
        case .katakana:
            let kanaList = (try? await kanaRepository.getKanaByType(type: .katakana)) ?? []
            return generateKanaQuestions(kanaList: kanaList, prompt: "What is the reading?", count: count)
        case .radicals:
            return await generateRadicalQuestions(count: count)
        case .grade1: return await generateGradeQuestions(grade: 1, count: count)
        case .grade2: return await generateGradeQuestions(grade: 2, count: count)
        case .grade3: return await generateGradeQuestions(grade: 3, count: count)
        case .grade4: return await generateGradeQuestions(grade: 4, count: count)
        case .grade5: return await generateGradeQuestions(grade: 5, count: count)
        case .grade6: return await generateGradeQuestions(grade: 6, count: count)
        case .jlptN5: return await generateJlptQuestions(level: 5, count: count)
        case .jlptN4: return await generateJlptQuestions(level: 4, count: count)
        case .jlptN3: return await generateJlptQuestions(level: 3, count: count)
        case .jlptN2: return await generateJlptQuestions(level: 2, count: count)
        case .jlptN1: return await generateJlptQuestions(level: 1, count: count)
        }
    }

    private func generateKanaQuestions(kanaList: [Kana], prompt: String, count: Int) -> [QuizQuestion] {
        guard kanaList.count >= count else { return [] }
        let basicKana = kanaList.filter { $0.variant == .basic }
        let pool = basicKana.count >= count ? basicKana : kanaList
        let selected = Array(pool.shuffled().prefix(count))
        let allRomanizations = Array(Set(pool.map { $0.romanization }))

        return selected.map { kana in
            let correct = kana.romanization
            let distractors = Array(allRomanizations.filter { $0 != correct }.shuffled().prefix(3))
            let options = (distractors + [correct]).shuffled()
            return QuizQuestion(
                displayCharacter: kana.literal,
                prompt: prompt,
                options: options,
                correctIndex: options.firstIndex(of: correct) ?? 0
            )
        }
    }

    private func generateRadicalQuestions(count: Int) async -> [QuizQuestion] {
        let radicals = (try? await radicalRepository.getAllRadicals()) ?? []
        guard radicals.count >= count else { return [] }
        let selected = Array(radicals.shuffled().prefix(count))
        let allMeanings = Array(Set(radicals.map { $0.meaningEn }))

        return selected.map { radical in
            let correct = radical.meaningEn
            let distractors = Array(allMeanings.filter { $0 != correct }.shuffled().prefix(3))
            let options = (distractors + [correct]).shuffled()
            return QuizQuestion(
                displayCharacter: radical.literal,
                prompt: "What does this radical mean?",
                options: options,
                correctIndex: options.firstIndex(of: correct) ?? 0
            )
        }
    }

    private func generateGradeQuestions(grade: Int, count: Int) async -> [QuizQuestion] {
        let kanjiList = (try? await kanjiRepository.getKanjiByGrade(grade: Int32(grade))) ?? []
        return await generateKanjiMeaningQuestions(kanjiList: kanjiList, count: count)
    }

    private func generateJlptQuestions(level: Int, count: Int) async -> [QuizQuestion] {
        let kanjiList = (try? await kanjiRepository.getKanjiByJlptLevel(level: Int32(level))) ?? []
        return await generateKanjiMeaningQuestions(kanjiList: kanjiList, count: count)
    }

    private func generateKanjiMeaningQuestions(kanjiList: [Kanji], count: Int) async -> [QuizQuestion] {
        guard kanjiList.count >= count else { return [] }
        let selected = Array(kanjiList.shuffled().prefix(count))
        let allMeanings = await buildMeaningPool()

        return selected.map { kanji in
            let meanings = kanji.meaningsEn as? [String] ?? []
            let correctMeaning = meanings.first ?? "unknown"
            let distractors = Array(allMeanings.filter { !meanings.contains($0) }.shuffled().prefix(3))
            let options = (distractors + [correctMeaning]).shuffled()
            return QuizQuestion(
                displayCharacter: kanji.literal,
                prompt: "What does this kanji mean?",
                options: options,
                correctIndex: options.firstIndex(of: correctMeaning) ?? 0
            )
        }
    }

    private func buildMeaningPool() async -> [String] {
        var meanings: [String] = []
        for grade: Int32 in 1...6 {
            let gradeKanji = (try? await kanjiRepository.getKanjiByGrade(grade: grade)) ?? []
            for kanji in gradeKanji {
                if let m = kanji.meaningsEn as? [String] {
                    meanings.append(contentsOf: m)
                }
            }
        }
        return Array(Set(meanings))
    }
}
