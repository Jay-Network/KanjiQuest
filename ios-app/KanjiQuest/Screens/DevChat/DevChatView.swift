import SwiftUI
import SharedCore

/// Dev chat screen. Mirrors Android's DevChatScreen.kt.
/// Chat bubbles, category filter chips, message input bar with send button.
struct DevChatView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = DevChatViewModel()
    var onBack: () -> Void = {}

    @State private var messageText = ""

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // Messages
                if viewModel.isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else if viewModel.messages.isEmpty {
                    Spacer()
                    Text("No messages yet.\nSend a message to the dev agent!")
                        .multilineTextAlignment(.center)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                    Spacer()
                } else {
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(spacing: 8) {
                                ForEach(viewModel.messages, id: \.id) { message in
                                    messageBubble(message)
                                        .id(message.id)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                        }
                        .onChange(of: viewModel.messages.count) { _ in
                            if let last = viewModel.messages.last {
                                withAnimation {
                                    proxy.scrollTo(last.id, anchor: .bottom)
                                }
                            }
                        }
                    }
                }

                // Category chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(MessageCategory.allCases, id: \.self) { category in
                            Button {
                                viewModel.selectCategory(
                                    viewModel.selectedCategory == category ? nil : category
                                )
                            } label: {
                                Text(category.label)
                                    .font(KanjiQuestTheme.labelSmall)
                                    .foregroundColor(viewModel.selectedCategory == category ? .white : KanjiQuestTheme.primary)
                                    .padding(.horizontal, 10).padding(.vertical, 6)
                                    .background(viewModel.selectedCategory == category ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                                    .cornerRadius(16)
                            }
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                }

                // Input bar
                HStack(spacing: 8) {
                    TextField("Type a message...", text: $messageText, axis: .vertical)
                        .textFieldStyle(.plain)
                        .padding(10)
                        .background(KanjiQuestTheme.surfaceVariant)
                        .cornerRadius(24)
                        .lineLimit(1...4)
                        .disabled(viewModel.isSending)

                    Button {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    } label: {
                        if viewModel.isSending {
                            ProgressView()
                                .frame(width: 24, height: 24)
                        } else {
                            Image(systemName: "paperplane.fill")
                                .foregroundColor(messageText.trimmingCharacters(in: .whitespaces).isEmpty ? KanjiQuestTheme.onSurfaceVariant : KanjiQuestTheme.primary)
                        }
                    }
                    .disabled(messageText.trimmingCharacters(in: .whitespaces).isEmpty || viewModel.isSending)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left"); Text("Back")
                }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) {
                Text("Dev Chat").font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { viewModel.load(container: container) }
        .alert("Error", isPresented: .init(get: { viewModel.error != nil }, set: { if !$0 { viewModel.clearError() } })) {
            Button("OK") { viewModel.clearError() }
        } message: {
            Text(viewModel.error ?? "")
        }
    }

    // MARK: - Message Bubble

    private func messageBubble(_ message: DevChatMessage) -> some View {
        let isFromUser = message.direction == .toAgent
        let bubbleColor = isFromUser ? KanjiQuestTheme.primaryContainer : KanjiQuestTheme.secondaryContainer
        let textColor = isFromUser ? KanjiQuestTheme.onPrimaryContainer : KanjiQuestTheme.onSecondaryContainer

        return VStack(alignment: isFromUser ? .trailing : .leading, spacing: 2) {
            Text(isFromUser ? "You" : "Dev Agent")
                .font(KanjiQuestTheme.labelSmall)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                .padding(.horizontal, 8)

            VStack(alignment: .leading, spacing: 4) {
                if let cat = message.category {
                    Text(cat.label)
                        .font(KanjiQuestTheme.labelSmall).fontWeight(.bold)
                        .foregroundColor(textColor.opacity(0.7))
                }
                Text(message.messageText)
                    .font(KanjiQuestTheme.bodyMedium)
                    .foregroundColor(textColor)
            }
            .padding(12)
            .background(bubbleColor)
            .cornerRadius(16)
            .frame(maxWidth: 300, alignment: isFromUser ? .trailing : .leading)
        }
        .frame(maxWidth: .infinity, alignment: isFromUser ? .trailing : .leading)
    }
}
