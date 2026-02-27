import SwiftUI

@main
struct KanjiQuestApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            AppRootView()
                .environmentObject(appState)
        }
    }
}

/// App state — starts with splash, then tries KMP init on a background thread.
/// If KMP crashes, the app survives and shows a mock UI instead.
class AppState: ObservableObject {
    enum Phase {
        case splash
        case mockHome
        case ready(AppContainer)
        case failed(String)
    }

    @Published var phase: Phase = .splash
    @Published var diagnosticLog: [String] = []

    func onSplashComplete() {
        // Go straight to mock home — KMP init happens in background
        phase = .mockHome
    }

    func tryKMPInit() {
        log("KMPBridge.initialize()...")
        do {
            try ObjCExceptionCatcher.catchVoid { KMPBridge.initialize() }
        } catch {
            log("KMPBridge EXCEPTION: \(error.localizedDescription)")
            phase = .failed("KMP bridge init failed:\n\(error.localizedDescription)")
            return
        }
        log("KMPBridge OK")

        log("Creating AppContainer...")
        CrashDiagnostic.begin()

        // Catch ObjC/KMP exceptions that Swift can't normally catch
        let container: AppContainer
        do {
            container = try ObjCExceptionCatcher.catch { AppContainer() }
        } catch {
            CrashDiagnostic.step("AppContainer EXCEPTION: \(error.localizedDescription)")
            log("AppContainer EXCEPTION: \(error.localizedDescription)")
            phase = .failed("KMP initialization failed:\n\(error.localizedDescription)")
            return
        }

        CrashDiagnostic.complete()

        if let error = container.initError {
            log("AppContainer FAILED: \(error)")
            phase = .failed(error)
        } else {
            log("AppContainer OK — switching to full app")
            // Register background task — wrap in exception catcher too
            do {
                try ObjCExceptionCatcher.catchVoid { container.syncService.registerBackgroundTask() }
                log("BGTask registered OK")
            } catch {
                log("BGTask registration failed (non-fatal): \(error.localizedDescription)")
                // Non-fatal — app can still work without background sync
            }
            phase = .ready(container)
        }
    }

    func log(_ msg: String) {
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

struct AppRootView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        switch appState.phase {
        case .splash:
            SplashView(onSplashComplete: {
                appState.onSplashComplete()
            })

        case .mockHome:
            MockHomeView()
                .environmentObject(appState)

        case .ready(let container):
            AppNavigation()
                .environmentObject(container)

        case .failed(let error):
            errorView(error)
        }
    }

    private func errorView(_ error: String) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.orange)
                Text("KanjiQuest")
                    .font(.title).bold()
                Text("Database initialization failed")
                    .font(.headline)
                    .foregroundColor(.secondary)
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                VStack(alignment: .leading, spacing: 2) {
                    Text("Diagnostic Log:")
                        .font(.caption).bold()
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

                Button("Try Again") {
                    appState.phase = .mockHome
                }
                .buttonStyle(.borderedProminent)
            }
            .padding(24)
        }
    }
}

/// Mock home screen — NO KMP, NO database, NO shared-core.
/// Pure SwiftUI so we can confirm the app runs on iPad.
/// Has a button to attempt KMP initialization when ready.
struct MockHomeView: View {
    @EnvironmentObject var appState: AppState
    @State private var isInitializing = false

    private let teal = Color(red: 0.05, green: 0.58, blue: 0.53)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Welcome card
                    VStack(spacing: 8) {
                        Image("JWorksLogo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 80, height: 80)
                        Text("Welcome to KanjiQuest!")
                            .font(.title2).bold()
                        Text("Gamified Kanji Learning")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(24)
                    .background(teal.opacity(0.1))
                    .cornerRadius(16)

                    // Game modes (static preview)
                    Text("Game Modes")
                        .font(.title3).bold()

                    mockModeCard(title: "Recognition", desc: "Identify kanji from choices", color: Color(red: 0.13, green: 0.59, blue: 0.95), icon: "eye.fill")
                    mockModeCard(title: "Writing", desc: "Practice writing kanji", color: Color(red: 0.30, green: 0.69, blue: 0.31), icon: "pencil.tip")
                    mockModeCard(title: "Vocabulary", desc: "Learn words using kanji", color: Color(red: 1.0, green: 0.60, blue: 0.0), icon: "book.fill")
                    mockModeCard(title: "Camera Challenge", desc: "Find kanji in the real world", color: Color(red: 0.61, green: 0.15, blue: 0.69), icon: "camera.fill")

                    Text("Study")
                        .font(.title3).bold()

                    mockModeCard(title: "Kana", desc: "Learn Hiragana & Katakana", color: Color(red: 0.91, green: 0.12, blue: 0.39), icon: "textformat.abc")
                    mockModeCard(title: "Radicals", desc: "Master kanji building blocks", color: Color(red: 0.47, green: 0.33, blue: 0.28), icon: "square.grid.3x3.fill")

                    // Initialize button
                    Spacer().frame(height: 16)

                    Button(action: {
                        isInitializing = true
                        appState.tryKMPInit()
                    }) {
                        HStack {
                            if isInitializing {
                                ProgressView().tint(.white)
                            } else {
                                Image(systemName: "play.fill")
                            }
                            Text(isInitializing ? "Loading database..." : "Start Learning")
                        }
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(isInitializing ? .gray : teal)
                        .cornerRadius(16)
                    }
                    .disabled(isInitializing)

                    // Diagnostic log (shown when initializing)
                    if !appState.diagnosticLog.isEmpty {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Init Log:")
                                .font(.caption).bold()
                            ForEach(appState.diagnosticLog, id: \.self) { line in
                                Text(line)
                                    .font(.system(.caption2, design: .monospaced))
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                    }

                    Spacer().frame(height: 32)
                }
                .padding(16)
            }
            .background(Color(.systemBackground))
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("KanjiQuest")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .toolbarBackground(teal, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }

    private func mockModeCard(title: String, desc: String, color: Color, icon: String) -> some View {
        HStack {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(.white)
                .frame(width: 48, height: 48)
                .background(color)
                .cornerRadius(12)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 16, weight: .bold))
                Text(desc)
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
        }
        .padding(12)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}
