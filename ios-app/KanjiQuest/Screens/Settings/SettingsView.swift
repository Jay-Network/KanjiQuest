import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var container: AppContainer
    @AppStorage("brushSize") private var brushSize: Double = 24
    @AppStorage("showGhostStrokes") private var showGhostStrokes = true
    @AppStorage("aiEnabled") private var aiEnabled = true

    var body: some View {
        Form {
            Section("Calligraphy") {
                HStack {
                    Text("Max Brush Size")
                    Spacer()
                    Text("\(Int(brushSize))pt")
                        .foregroundColor(.secondary)
                }
                Slider(value: $brushSize, in: 12...48, step: 2)

                Toggle("Show Ghost Strokes", isOn: $showGhostStrokes)
                Toggle("AI Feedback", isOn: $aiEnabled)
            }

            Section("Account") {
                Button("Sign Out", role: .destructive) {
                    Task {
                        try? await container.authRepository.signOut()
                    }
                }
            }

            Section("About") {
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.0.0 (iPad)")
                        .foregroundColor(.secondary)
                }
                HStack {
                    Text("Build")
                    Spacer()
                    Text("Phase 1 MVP")
                        .foregroundColor(.secondary)
                }
            }
        }
        .navigationTitle("Settings")
    }
}
