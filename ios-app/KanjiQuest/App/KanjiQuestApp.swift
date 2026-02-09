import SwiftUI

@main
struct KanjiQuestApp: App {
    @StateObject private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            AppNavigation()
                .environmentObject(container)
        }
    }
}
