//
//  ChatView.swift
//  CHLA-iOS
//
//  Created for KiNDD Resource Navigator
//  AI-powered chat for neurodevelopmental service questions
//

import SwiftUI

struct ChatView: View {
    @StateObject private var llmService = LLMService.shared
    @EnvironmentObject var appState: AppState
    @State private var inputText = ""
    @FocusState private var isFocused: Bool
    @State private var showingClearConfirmation = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Messages
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            // Welcome message
                            if llmService.messages.isEmpty {
                                WelcomeCard()
                                    .padding(.top, 20)
                                    .onTapGesture { _ in
                                        // Handled by suggestion chips
                                    }
                            }

                            ForEach(llmService.messages) { message in
                                MessageBubble(message: message)
                                    .id(message.id)
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 100)
                    }
                    .onChange(of: llmService.messages.count) { _, _ in
                        if let last = llmService.messages.last {
                            withAnimation(.easeOut(duration: 0.3)) {
                                proxy.scrollTo(last.id, anchor: .bottom)
                            }
                        }
                    }
                }

                // Input bar
                ChatInputBar(
                    text: $inputText,
                    isFocused: $isFocused,
                    isLoading: llmService.isLoading
                ) {
                    sendMessage()
                }
            }
            .navigationTitle("Ask KiNDD")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    if !llmService.messages.isEmpty {
                        Button {
                            showingClearConfirmation = true
                        } label: {
                            Image(systemName: "trash")
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .confirmationDialog("Clear Chat", isPresented: $showingClearConfirmation) {
                Button("Clear All Messages", role: .destructive) {
                    llmService.clearChat()
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }

    private func sendMessage() {
        let query = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return }

        inputText = ""
        isFocused = false

        // Build context from app state (no zip code available in AppState yet)
        let context = UserContext(
            zipCode: nil,
            childAge: nil,
            diagnosis: nil,
            insurance: nil,
            currentServices: nil
        )

        Task {
            await llmService.ask(query, context: context)
        }
    }
}

// MARK: - Welcome Card

struct WelcomeCard: View {
    @StateObject private var llmService = LLMService.shared

    let suggestions = [
        "What ABA providers near 90210 accept Medi-Cal?",
        "My child is turning 3, what changes?",
        "How do I get a Regional Center assessment?",
        "What's the difference between OT and PT?"
    ]

    var body: some View {
        VStack(spacing: 24) {
            // Header
            VStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 70, height: 70)

                    Image(systemName: "sparkles")
                        .font(.system(size: 32))
                        .foregroundColor(.white)
                }

                Text("Ask KiNDD")
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                    .foregroundColor(Color(hex: "1E293B"))

                Text("I can help you find developmental\nservices in Los Angeles County")
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "64748B"))
                    .multilineTextAlignment(.center)
            }

            // Suggestions
            VStack(alignment: .leading, spacing: 12) {
                Text("Try asking:")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(Color(hex: "64748B"))
                    .padding(.leading, 4)

                ForEach(suggestions, id: \.self) { suggestion in
                    SuggestionChip(text: suggestion) {
                        Task {
                            await llmService.ask(suggestion)
                        }
                    }
                }
            }
        }
        .padding(24)
        .background(Color(hex: "F8FAFC"))
        .cornerRadius(20)
    }
}

// MARK: - Suggestion Chip

struct SuggestionChip: View {
    let text: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: "arrow.right.circle.fill")
                    .foregroundColor(Color(hex: "8B5CF6"))

                Text(text)
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "1E293B"))
                    .multilineTextAlignment(.leading)

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: Color.black.opacity(0.05), radius: 4, y: 2)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Message Bubble

struct MessageBubble: View {
    let message: ChatMessage

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            if message.role == .user {
                Spacer(minLength: 60)
            }

            if message.role == .assistant || message.role == .system {
                // AI Avatar
                ZStack {
                    Circle()
                        .fill(
                            message.role == .system
                                ? AnyShapeStyle(Color(hex: "EF4444"))
                                : AnyShapeStyle(LinearGradient(
                                    colors: [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ))
                        )
                        .frame(width: 36, height: 36)

                    Image(systemName: message.role == .system ? "exclamationmark.triangle" : "sparkles")
                        .font(.system(size: 16))
                        .foregroundColor(.white)
                }
            }

            // Message content
            VStack(alignment: message.role == .user ? .trailing : .leading, spacing: 4) {
                if message.isLoading {
                    TypingIndicator()
                } else {
                    Text(message.content)
                        .font(.body)
                        .foregroundColor(message.role == .user ? .white : Color(hex: "1E293B"))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(
                            message.role == .user
                                ? AnyShapeStyle(
                                    LinearGradient(
                                        colors: [Color(hex: "6366F1"), Color(hex: "8B5CF6")],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                : AnyShapeStyle(Color(hex: "F1F5F9"))
                        )
                        .cornerRadius(18)
                        .cornerRadius(message.role == .user ? 18 : 4, corners: message.role == .user ? [.bottomRight] : [.topLeft])
                }

                Text(message.timestamp, style: .time)
                    .font(.caption2)
                    .foregroundColor(Color(hex: "94A3B8"))
            }

            if message.role == .user {
                // User Avatar
                ZStack {
                    Circle()
                        .fill(Color(hex: "E2E8F0"))
                        .frame(width: 36, height: 36)

                    Image(systemName: "person.fill")
                        .font(.system(size: 16))
                        .foregroundColor(Color(hex: "64748B"))
                }
            }

            if message.role != .user {
                Spacer(minLength: 60)
            }
        }
    }
}

// MARK: - Typing Indicator

struct TypingIndicator: View {
    @State private var animating = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3) { index in
                Circle()
                    .fill(Color(hex: "94A3B8"))
                    .frame(width: 8, height: 8)
                    .scaleEffect(animating ? 1.0 : 0.5)
                    .animation(
                        Animation.easeInOut(duration: 0.5)
                            .repeatForever()
                            .delay(Double(index) * 0.15),
                        value: animating
                    )
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(Color(hex: "F1F5F9"))
        .cornerRadius(18)
        .onAppear {
            animating = true
        }
    }
}

// MARK: - Chat Input Bar

struct ChatInputBar: View {
    @Binding var text: String
    var isFocused: FocusState<Bool>.Binding
    let isLoading: Bool
    let onSend: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Divider()

            HStack(spacing: 12) {
                // Text field
                HStack {
                    TextField("Ask about services...", text: $text, axis: .vertical)
                        .focused(isFocused)
                        .lineLimit(1...5)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                }
                .background(Color(hex: "F1F5F9"))
                .cornerRadius(24)

                // Send button
                Button(action: onSend) {
                    ZStack {
                        Circle()
                            .fill(
                                text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading
                                    ? AnyShapeStyle(Color(hex: "E2E8F0"))
                                    : AnyShapeStyle(LinearGradient(
                                        colors: [Color(hex: "6366F1"), Color(hex: "8B5CF6")],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ))
                            )
                            .frame(width: 44, height: 44)

                        if isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Image(systemName: "arrow.up")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(
                                    text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                        ? Color(hex: "94A3B8")
                                        : .white
                                )
                        }
                    }
                }
                .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.white)
        }
    }
}

// Note: cornerRadius(_:corners:) extension is defined in FullMapView.swift

#Preview {
    ChatView()
        .environmentObject(AppState())
}
