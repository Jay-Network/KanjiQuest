import SwiftUI

@main
struct KanjiQuestApp: App {
    @StateObject private var container = AppContainer()

    init() {
        NSLog("KanjiQuest [App]: init() START")
        KMPBridge.initialize()
        NSLog("KanjiQuest [App]: KMPBridge.initialize() OK")
    }

    var body: some Scene {
        WindowGroup {
            if let error = container.initError {
                // Show error screen instead of crashing
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
            } else {
                AppNavigation()
                    .environmentObject(container)
            }
        }
    }
}
