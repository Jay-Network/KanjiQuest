import SwiftUI

/// Bottom tab navigation matching Android's MainScaffold.kt.
/// Four tabs: Main (Home), Study, Play (Games), Collect (Collection).
struct MainTabView: View {
    @EnvironmentObject var container: AppContainer
    let navigateTo: (NavRoute) -> Void

    @State private var selectedTab: BottomTab = .main

    var body: some View {
        TabView(selection: $selectedTab) {
            // Tab 1: Main (Home)
            HomeView(navigateTo: navigateTo)
                .tabItem {
                    Image(systemName: "house.fill")
                    Text("Main")
                }
                .tag(BottomTab.main)

            // Tab 2: Study
            StudyTabView(navigateTo: navigateTo)
                .tabItem {
                    Image(systemName: "pencil.line")
                    Text("Study")
                }
                .tag(BottomTab.study)

            // Tab 3: Play (Games)
            GamesTabView(navigateTo: navigateTo)
                .tabItem {
                    Image(systemName: "play.fill")
                    Text("Play")
                }
                .tag(BottomTab.play)

            // Tab 4: Collect
            CollectionHubView(navigateTo: navigateTo)
                .tabItem {
                    Image(systemName: "star.fill")
                    Text("Collect")
                }
                .tag(BottomTab.collect)
        }
        .accentColor(KanjiQuestTheme.primary)
    }
}

/// Bottom tab enum matching Android's BottomNavItem.
enum BottomTab: String, CaseIterable {
    case main
    case study
    case play
    case collect
}
