import Foundation
import SharedCore

enum CameraChallengeState {
    case loading
    case showTarget(targetKanji: Kanji, challengeNumber: Int, totalChallenges: Int, successCount: Int, sessionXp: Int)
    case success(targetKanji: Kanji, challengeNumber: Int, totalChallenges: Int, successCount: Int, sessionXp: Int, xpGained: Int)
    case sessionComplete(totalChallenges: Int, successCount: Int, accuracy: Int, totalXp: Int)
    case error(message: String)
}

@MainActor
final class CameraChallengeViewModel: ObservableObject {
    @Published var state: CameraChallengeState = .loading
    @Published var isScanning = false

    private var kanjiRepository: KanjiRepository?
    private var userRepository: UserRepository?
    private var fieldJournalRepository: FieldJournalRepository?

    private var availableKanji: [Kanji] = []
    private var usedKanjiIds = Set<Int32>()
    private var challengeNumber = 0
    private let totalChallenges = 5
    private var successCount = 0
    private var sessionXp = 0
    private var foundKanjiLiterals: [String] = []

    func load(container: AppContainer) {
        kanjiRepository = container.kanjiRepository
        userRepository = container.userRepository
        fieldJournalRepository = container.fieldJournalRepository
    }

    func startSession(targetKanjiId: Int32? = nil) {
        Task {
            do {
                if let targetId = targetKanjiId {
                    if let kanji = try await kanjiRepository?.getKanjiById(id: targetId) {
                        availableKanji = [kanji]
                    } else {
                        state = .error(message: "Kanji not found")
                        return
                    }
                } else {
                    let grade1 = (try? await kanjiRepository?.getKanjiByGrade(grade: 1)) ?? []
                    let grade2 = (try? await kanjiRepository?.getKanjiByGrade(grade: 2)) ?? []
                    let grade3 = (try? await kanjiRepository?.getKanjiByGrade(grade: 3)) ?? []
                    availableKanji = (grade1 + grade2 + grade3).shuffled()
                }

                if availableKanji.isEmpty {
                    state = .error(message: "No kanji available for challenges")
                    return
                }

                nextChallenge()
            } catch {
                state = .error(message: "Failed to load kanji: \(error.localizedDescription)")
            }
        }
    }

    func nextChallenge() {
        if challengeNumber >= totalChallenges {
            completeSession()
            return
        }

        challengeNumber += 1

        let target = availableKanji
            .filter { !usedKanjiIds.contains($0.id) }
            .randomElement() ?? availableKanji.randomElement()!

        usedKanjiIds.insert(target.id)

        state = .showTarget(
            targetKanji: target,
            challengeNumber: challengeNumber,
            totalChallenges: totalChallenges,
            successCount: successCount,
            sessionXp: sessionXp
        )
        isScanning = false
    }

    func onTextRecognized(_ recognizedText: String) {
        guard case .showTarget(let target, _, _, _, _) = state, !isScanning else { return }
        isScanning = true

        if recognizedText.contains(target.literal) {
            onSuccess(target)
        } else {
            isScanning = false
        }
    }

    private func onSuccess(_ targetKanji: Kanji) {
        successCount += 1
        foundKanjiLiterals.append(targetKanji.literal)
        let xpGained = 50

        sessionXp += xpGained

        state = .success(
            targetKanji: targetKanji,
            challengeNumber: challengeNumber,
            totalChallenges: totalChallenges,
            successCount: successCount,
            sessionXp: sessionXp,
            xpGained: xpGained
        )
        isScanning = false
    }

    private func completeSession() {
        Task {
            do {
                if sessionXp > 0 {
                    let profile = try await userRepository?.getProfile()
                    if let p = profile {
                        let newXp = p.totalXp + Int32(sessionXp)
                        try await userRepository?.updateXpAndLevel(xp: newXp, level: p.level)
                    }
                }

                if !foundKanjiLiterals.isEmpty {
                    try await fieldJournalRepository?.insert(
                        imagePath: "",
                        locationLabel: "",
                        kanjiFound: foundKanjiLiterals,
                        capturedAt: Int64(Date().timeIntervalSince1970)
                    )
                }

                let accuracy = Int(Float(successCount) / Float(totalChallenges) * 100)

                state = .sessionComplete(
                    totalChallenges: totalChallenges,
                    successCount: successCount,
                    accuracy: accuracy,
                    totalXp: sessionXp
                )
            } catch {
                state = .error(message: "Failed to save progress: \(error.localizedDescription)")
            }
        }
    }

    func reset() {
        challengeNumber = 0
        successCount = 0
        sessionXp = 0
        usedKanjiIds.removeAll()
        foundKanjiLiterals.removeAll()
        state = .loading
    }
}
