import SwiftUI

@main
struct KanjiQuestApp: App {
    @StateObject private var container = AppContainer()
    @State private var previousCrash: String? = nil

    init() {
        // Check for previous crash BEFORE doing anything else
        let crash = CrashDiagnostic.checkPreviousCrash()
        if crash != nil {
            _previousCrash = State(initialValue: crash)
        }

        // Begin breadcrumb tracking for THIS launch
        CrashDiagnostic.begin()
        CrashDiagnostic.step("KanjiQuestApp.init() START")

        KMPBridge.initialize()
        CrashDiagnostic.step("KMPBridge.initialize() OK")
    }

    var body: some Scene {
        WindowGroup {
            if let crash = previousCrash {
                // Show what happened in the PREVIOUS crash
                crashReportView(crash)
            } else if let error = container.initError {
                errorView(error)
            } else {
                AppNavigation()
                    .environmentObject(container)
            }
        }
    }

    private func crashReportView(_ crash: String) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                Image(systemName: "ant.circle.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.red)
                Text("KanjiQuest Crash Report")
                    .font(.title2)
                    .bold()
                Text("The app crashed on the previous launch. Details below:")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                Text(crash)
                    .font(.system(.caption, design: .monospaced))
                    .foregroundColor(.primary)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)

                Button("Dismiss & Retry") {
                    CrashDiagnostic.clearCrashData()
                    // Force relaunch by exiting — user taps app icon again
                    exit(0)
                }
                .buttonStyle(.borderedProminent)
                .tint(.orange)

                Text("Please screenshot this and send to Jay")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .padding(24)
        }
    }

    private func errorView(_ error: String) -> some View {
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
        }
    }
}
