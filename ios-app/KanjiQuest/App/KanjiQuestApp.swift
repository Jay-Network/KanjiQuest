import SwiftUI

@main
struct KanjiQuestApp: App {
    /// Container is created lazily AFTER init completes — avoids KMP crash.
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            AppRootView()
                .environmentObject(appState)
        }
    }
}

/// Lightweight root state — no KMP dependencies.
/// Holds the AppContainer once initialization succeeds.
class AppState: ObservableObject {
    enum Phase {
        case loading
        case ready(AppContainer)
        case failed(String)
    }

    @Published var phase: Phase = .loading
    @Published var diagnosticLog: [String] = []

    func initialize() {
        guard case .loading = phase else { return }
        log("Starting initialization...")

        log("KMPBridge.initialize()...")
        KMPBridge.initialize()
        log("KMPBridge OK")

        log("Creating AppContainer...")
        CrashDiagnostic.begin()
        let container = AppContainer()
        if let error = container.initError {
            log("AppContainer FAILED: \(error)")
            phase = .failed(error)
        } else {
            log("AppContainer OK — app ready")
            phase = .ready(container)
        }
        CrashDiagnostic.complete()
    }

    private func log(_ msg: String) {
        let entry = "[\(formattedTime())] \(msg)"
        diagnosticLog.append(entry)
        NSLog("KanjiQuest: %@", msg)
    }

    private func formattedTime() -> String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss.SSS"
        return f.string(from: Date())
    }
}

/// Root view — shows a safe loading screen first, then initializes.
/// This guarantees the user sees SOMETHING before any KMP code runs.
struct AppRootView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        switch appState.phase {
        case .loading:
            loadingView
                .task {
                    // Small delay so the loading screen renders first
                    try? await Task.sleep(nanoseconds: 100_000_000) // 0.1s
                    appState.initialize()
                }

        case .ready(let container):
            AppNavigation()
                .environmentObject(container)

        case .failed(let error):
            errorView(error)
        }
    }

    private var loadingView: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 16) {
                Image("JWorksLogo")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 240, height: 240)

                Text("KanjiQuest")
                    .font(.system(size: 32, weight: .bold))
                    .tracking(1)
                    .foregroundColor(Color(red: 0.05, green: 0.58, blue: 0.53)) // teal

                Text("by JWorks")
                    .font(.system(size: 16, weight: .medium))
                    .tracking(0.5)
                    .foregroundColor(.white.opacity(0.6))

                ProgressView()
                    .tint(.white.opacity(0.5))
                    .padding(.top, 12)
            }
        }
    }

    private func errorView(_ error: String) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.orange)
                Text("KanjiQuest")
                    .font(.title)
                    .bold()
                Text("Failed to initialize")
                    .font(.headline)
                    .foregroundColor(.secondary)
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                // Show full diagnostic log
                VStack(alignment: .leading, spacing: 2) {
                    Text("Diagnostic Log:")
                        .font(.caption)
                        .bold()
                    ForEach(appState.diagnosticLog, id: \.self) { line in
                        Text(line)
                            .font(.system(.caption2, design: .monospaced))
                            .foregroundColor(.primary)
                    }
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemGray6))
                .cornerRadius(8)
                .padding(.horizontal, 16)
            }
            .padding(24)
        }
    }
}
