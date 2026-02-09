import Foundation

/// Reads configuration values from Info.plist, which are injected via .xcconfig files.
/// Secrets are kept in a gitignored KanjiQuest.xcconfig.
struct Configuration {
    let supabaseUrl: String
    let supabaseAnonKey: String
    let geminiApiKey: String
    let supabaseServiceRoleKey: String

    init() {
        let bundle = Bundle.main
        supabaseUrl = bundle.infoPlistString(for: "SUPABASE_URL") ?? ""
        supabaseAnonKey = bundle.infoPlistString(for: "SUPABASE_ANON_KEY") ?? ""
        geminiApiKey = bundle.infoPlistString(for: "GEMINI_API_KEY") ?? ""
        supabaseServiceRoleKey = bundle.infoPlistString(for: "SUPABASE_SERVICE_ROLE_KEY") ?? ""
    }
}

private extension Bundle {
    func infoPlistString(for key: String) -> String? {
        object(forInfoDictionaryKey: key) as? String
    }
}
