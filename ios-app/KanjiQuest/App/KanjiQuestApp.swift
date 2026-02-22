import SwiftUI

@main
struct KanjiQuestApp: App {
    @StateObject private var container = AppContainer()

    init() {
        KMPBridge.initialize()
    }

    var body: some Scene {
        WindowGroup {
            AppNavigation()
                .environmentObject(container)
        }
    }
}
