import Foundation
import SharedCore

enum PlacementStage: Int, CaseIterable {
    case hiragana = 0
    case katakana = 1
    case radical = 2
    case grade1 = 3
    case grade2 = 4
    case grade3 = 5
    case grade4 = 6
    case grade5 = 7
    case grade6 = 8

    var displayName: String {
        switch self {
        case .hiragana: return "Hiragana"
        case .katakana: return "Katakana"
        case .radical: return "Radicals"
        case .grade1: return "Grade 1"
        case .grade2: return "Grade 2"
        case .grade3: return "Grade 3"
        case .grade4: return "Grade 4"
        case .grade5: return "Grade 5"
        case .grade6: return "Grade 6"
        }
    }
}

struct PlacementQuestion {
    let displayCharacter: String
    let prompt: String
    let options: [String]
    let correctIndex: Int
}

struct StageResult {
    let stage: PlacementStage
    let correct: Int
    let total: Int
    var passed: Bool { correct >= 4 } // 4 of 5 to pass
}

enum PlacementPhase {
    case intro
    case loading
    case question
    case complete
}

@MainActor
final class PlacementTestViewModel: ObservableObject {
    @Published var phase: PlacementPhase = .intro
    @Published var currentStage: PlacementStage = .hiragana
    @Published var currentQuestion: PlacementQuestion? = nil
    @Published var questionIndex = 0
    @Published var stageCorrect = 0
    @Published var selectedAnswer: Int? = nil
    @Published var showResult = false
    @Published var stageResults: [StageResult] = []
    @Published var assignedLevel = 1
    @Published var timeRemaining = 180
    @Published var isTimerRunning = false

    private let questionsPerStage = 5
    private let passThreshold = 4

    private var kanjiRepository: KanjiRepository?
    private var userRepository: UserRepository?
    private var kanaRepository: KanaRepository?
    private var radicalRepository: RadicalRepository?

    private var questions: [PlacementQuestion] = []
    private var timerTask: Task<Void, Never>?

    func configure(container: AppContainer) {
        kanjiRepository = container.kanjiRepository
        userRepository = container.userRepository
        kanaRepository = container.kanaRepository
        radicalRepository = container.radicalRepository
    }

    func beginAssessment() {
        phase = .loading
        stageResults = []
        currentStage = .hiragana
        timeRemaining = 180
        Task {
            await prepareStage()
            phase = .question
            startTimer()
        }
    }

    private func prepareStage() async {
        questionIndex = 0
        stageCorrect = 0
        selectedAnswer = nil
        showResult = false

        switch currentStage {
        case .hiragana:
            questions = await generateKanaQuestions(isHiragana: true)
        case .katakana:
            questions = await generateKanaQuestions(isHiragana: false)
        case .radical:
            questions = await generateRadicalQuestions()
        default:
            let grade = Int32(currentStage.rawValue - 2) // grade1=3-2=1, etc.
            questions = await generateKanjiQuestions(grade: grade)
        }

        if !questions.isEmpty {
            currentQuestion = questions[0]
        }
    }

    private func generateKanaQuestions(isHiragana: Bool) async -> [PlacementQuestion] {
        let type: KanaType = isHiragana ? .hiragana : .katakana
        let allKana = (try? await kanaRepository?.getKanaByType(type: type)) ?? []
        guard allKana.count >= 4 else { return [] }
        let selected = Array(allKana.shuffled().prefix(questionsPerStage))
        return selected.map { kana in
            var options = [kana.romanization]
            let distractors = allKana.filter { $0.id != kana.id }.shuffled().prefix(3).map { $0.reading }
            options.append(contentsOf: distractors)
            options.shuffle()
            let correctIdx = options.firstIndex(of: kana.romanization) ?? 0
            return PlacementQuestion(
                displayCharacter: kana.literal,
                prompt: "What is the reading?",
                options: options,
                correctIndex: correctIdx
            )
        }
    }

    private func generateRadicalQuestions() async -> [PlacementQuestion] {
        let allRadicals = (try? await radicalRepository?.getAllRadicals()) ?? []
        guard allRadicals.count >= 4 else { return [] }
        let selected = Array(allRadicals.shuffled().prefix(questionsPerStage))
        return selected.map { radical in
            var options = [radical.meaningEn]
            let distractors = allRadicals.filter { $0.id != radical.id }.shuffled().prefix(3).map { $0.meaning }
            options.append(contentsOf: distractors)
            options.shuffle()
            let correctIdx = options.firstIndex(of: radical.meaningEn) ?? 0
            return PlacementQuestion(
                displayCharacter: radical.literal,
                prompt: "What does this radical mean?",
                options: options,
                correctIndex: correctIdx
            )
        }
    }

    private func generateKanjiQuestions(grade: Int32) async -> [PlacementQuestion] {
        let kanjiList = (try? await kanjiRepository?.getKanjiByGrade(grade: grade)) ?? []
        guard kanjiList.count >= 4 else { return [] }
        let selected = Array(kanjiList.shuffled().prefix(questionsPerStage))
        return selected.map { kanji in
            let meaning = kanji.meaningsEn.first ?? "unknown"
            var options = [meaning]
            let distractors = kanjiList.filter { $0.id != kanji.id }.shuffled().prefix(3).compactMap { $0.meaningsEn.first }
            options.append(contentsOf: distractors)
            options.shuffle()
            let correctIdx = options.firstIndex(of: meaning) ?? 0
            return PlacementQuestion(
                displayCharacter: kanji.literal,
                prompt: "What does this kanji mean?",
                options: options,
                correctIndex: correctIdx
            )
        }
    }

    func selectAnswer(_ index: Int) {
        guard !showResult else { return }
        selectedAnswer = index
        showResult = true
        if index == currentQuestion?.correctIndex {
            stageCorrect += 1
        }
    }

    func nextQuestion() {
        questionIndex += 1

        if questionIndex >= questionsPerStage {
            // Stage complete
            let result = StageResult(stage: currentStage, correct: stageCorrect, total: questionsPerStage)
            stageResults.append(result)

            if !result.passed || currentStage == .grade6 {
                finishAssessment()
            } else {
                // Move to next stage
                if let nextStage = PlacementStage(rawValue: currentStage.rawValue + 1) {
                    currentStage = nextStage
                    Task { await prepareStage() }
                } else {
                    finishAssessment()
                }
            }
        } else {
            selectedAnswer = nil
            showResult = false
            if questionIndex < questions.count {
                currentQuestion = questions[questionIndex]
            }
        }
    }

    private func finishAssessment() {
        stopTimer()

        // Determine level based on highest passed stage
        let passedStages = stageResults.filter { $0.passed }
        if let highest = passedStages.last {
            assignedLevel = levelForStage(highest.stage)
        } else {
            assignedLevel = 1
        }

        // Save result
        Task {
            let profile = try? await userRepository?.getProfile()
            let currentXp = profile?.totalXp ?? 0
            try? await userRepository?.updateXpAndLevel(totalXp: currentXp, level: Int32(assignedLevel))
            UserDefaults.standard.set(true, forKey: "placement_completed")
        }

        phase = .complete
    }

    private func levelForStage(_ stage: PlacementStage) -> Int {
        switch stage {
        case .hiragana: return 2
        case .katakana: return 4
        case .radical: return 6
        case .grade1: return 10
        case .grade2: return 15
        case .grade3: return 20
        case .grade4: return 25
        case .grade5: return 30
        case .grade6: return 35
        }
    }

    // MARK: - Timer

    private func startTimer() {
        isTimerRunning = true
        timerTask = Task {
            while timeRemaining > 0 && !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if !Task.isCancelled {
                    timeRemaining -= 1
                    if timeRemaining <= 0 {
                        finishAssessment()
                    }
                }
            }
        }
    }

    private func stopTimer() {
        isTimerRunning = false
        timerTask?.cancel()
        timerTask = nil
    }

    var formattedTime: String {
        let min = timeRemaining / 60
        let sec = timeRemaining % 60
        return String(format: "%d:%02d", min, sec)
    }
}
