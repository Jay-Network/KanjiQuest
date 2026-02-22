import Foundation
import SharedCore

/// Tracks daily preview trial usage for free-tier users.
/// Free users can try premium modes a limited number of times per day.
/// Swift equivalent of Android PreviewTrialManager (SharedPreferences → UserDefaults).
final class PreviewTrialManager {

    private let defaults = UserDefaults.standard
    private let prefix = "kanjiquest_preview_trials_"

    /// Max daily preview trials per mode
    static let trialLimits: [String: Int] = [
        "WRITING": 3,
        "VOCABULARY": 3,
        "CAMERA_CHALLENGE": 1
    ]

    private func todayKey(mode: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let today = formatter.string(from: Date())
        return "\(prefix)\(mode)_\(today)"
    }

    /// Get remaining preview trials for a mode today
    func getRemainingTrials(mode: String) -> Int {
        let limit = Self.trialLimits[mode] ?? 0
        if limit == 0 { return 0 }
        let used = defaults.integer(forKey: todayKey(mode: mode))
        return max(limit - used, 0)
    }

    /// Use one preview trial. Returns true if trial was available and consumed.
    func usePreviewTrial(mode: String) -> Bool {
        let remaining = getRemainingTrials(mode: mode)
        if remaining <= 0 { return false }
        let key = todayKey(mode: mode)
        let used = defaults.integer(forKey: key)
        defaults.set(used + 1, forKey: key)
        return true
    }

    /// Check if a mode has any remaining preview trials today
    func hasTrialsRemaining(mode: String) -> Bool {
        return getRemainingTrials(mode: mode) > 0
    }

    /// Get total trial limit for a mode
    func getTrialLimit(mode: String) -> Int {
        return Self.trialLimits[mode] ?? 0
    }

    /// Get preview trial info for display
    func getPreviewTrialInfo(mode: String) -> (remaining: Int, limit: Int) {
        let limit = getTrialLimit(mode: mode)
        let remaining = getRemainingTrials(mode: mode)
        return (remaining, limit)
    }
}
