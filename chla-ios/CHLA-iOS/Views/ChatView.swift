//
//  ChatView.swift
//  CHLA-iOS
//
//  Created for KiNDD Resource Navigator
//  AI-powered chat with streaming and message actions
//

import SwiftUI
import CoreLocation
import UIKit

struct ChatView: View {
    @StateObject private var llmService = LLMService.shared
    @StateObject private var locationService = LocationService()
    @EnvironmentObject var appState: AppState
    @State private var inputText = ""
    @FocusState private var isFocused: Bool
    @State private var showingClearConfirmation = false
    @State private var showingExportSheet = false
    @State private var exportText = ""
    @State private var userZipCode: String?

    // Quick prompt suggestions
    private let quickPrompts = [
        ("🔍", "Find providers", "Find ABA providers near me"),
        ("📋", "Assessment", "How do I get a Regional Center assessment?"),
        ("🏥", "My RC", "Which Regional Center serves my area?"),
        ("👶", "Early Start", "What is Early Start and who qualifies?"),
        ("💊", "Insurance", "What insurance covers ABA therapy?"),
        ("📅", "Waitlists", "How long are therapy waitlists?"),
        ("🎂", "Age 3 transition", "What happens when my child turns 3?"),
        ("🗣️", "Speech", "Find speech therapy providers")
    ]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Quick prompts capsules
                PromptCapsulesBar(prompts: quickPrompts) { prompt in
                    sendMessage(prompt)
                }

                // Messages
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            // Welcome message
                            if llmService.messages.isEmpty {
                                WelcomeCard(onSuggestionTap: { suggestion in
                                    sendMessage(suggestion)
                                })
                                .padding(.top, 20)
                            }

                            ForEach(llmService.messages) { message in
                                MessageBubble(
                                    message: message,
                                    onLike: { llmService.setFeedback(message.id, feedback: .liked) },
                                    onDislike: { llmService.setFeedback(message.id, feedback: .disliked) },
                                    onCopy: {
                                        if let text = llmService.copyMessage(message.id) {
                                            UIPasteboard.general.string = text
                                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                                        }
                                    },
                                    onShare: {
                                        exportText = message.content
                                        showingExportSheet = true
                                    },
                                    onAction: { action in
                                        handleAction(action)
                                    }
                                )
                                .id(message.id)
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 100)
                    }
                    .onChange(of: llmService.messages.count) { _, _ in
                        scrollToBottom(proxy: proxy)
                    }
                    .onChange(of: llmService.messages.last?.content) { _, _ in
                        scrollToBottom(proxy: proxy)
                    }
                }

                // Input bar
                ChatInputBar(
                    text: $inputText,
                    isFocused: $isFocused,
                    isLoading: llmService.isLoading,
                    isStreaming: llmService.messages.last?.isStreaming == true,
                    onSend: { sendMessage(inputText) },
                    onCancel: { llmService.cancelStreaming() }
                )
            }
            .navigationTitle("Ask KiNDD")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        if !llmService.messages.isEmpty {
                            Button {
                                // Get regional center from last message if available
                                let lastRC = llmService.messages.last(where: { $0.regionalCenter != nil })?.regionalCenter
                                exportText = llmService.exportConversation(
                                    userZipCode: userZipCode ?? appState.userZipCode,
                                    regionalCenter: lastRC
                                )
                                showingExportSheet = true
                            } label: {
                                Label("Share Chat", systemImage: "square.and.arrow.up")
                            }

                            Button {
                                let lastRC = llmService.messages.last(where: { $0.regionalCenter != nil })?.regionalCenter
                                let text = llmService.exportConversation(
                                    userZipCode: userZipCode ?? appState.userZipCode,
                                    regionalCenter: lastRC
                                )
                                UIPasteboard.general.string = text
                                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            } label: {
                                Label("Copy All", systemImage: "doc.on.doc")
                            }

                            Divider()

                            Button(role: .destructive) {
                                showingClearConfirmation = true
                            } label: {
                                Label("Clear Chat", systemImage: "trash")
                            }
                        }

                        Toggle(isOn: $llmService.useStreaming) {
                            Label("Streaming Mode", systemImage: "waveform")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .confirmationDialog("Clear Chat", isPresented: $showingClearConfirmation) {
                Button("Clear All Messages", role: .destructive) {
                    llmService.clearChat()
                }
                Button("Cancel", role: .cancel) {}
            }
            .sheet(isPresented: $showingExportSheet) {
                ShareSheet(items: [exportText])
            }
            .onAppear {
                fetchUserZipCode()
            }
            .onChange(of: locationService.currentLocation) { _, location in
                if let location = location {
                    Task {
                        await updateZipCodeFromLocation(location)
                    }
                }
            }
        }
    }

    private func fetchUserZipCode() {
        // Use saved ZIP if available
        if let savedZip = appState.userZipCode {
            userZipCode = savedZip
            return
        }

        // Request location to get ZIP
        if locationService.hasLocationPermission {
            locationService.requestLocation()
        } else if locationService.shouldRequestPermission {
            locationService.requestPermission()
        }
    }

    private func updateZipCodeFromLocation(_ location: CLLocation) async {
        do {
            let zip = try await locationService.getZipCode(for: location.coordinate)
            userZipCode = zip
            appState.saveUserContext(zipCode: zip)
            print("📍 Got user ZIP from location: \(zip)")
        } catch {
            print("❌ Failed to get ZIP from location: \(error)")
        }
    }

    private func scrollToBottom(proxy: ScrollViewProxy) {
        if let last = llmService.messages.last {
            withAnimation(.easeOut(duration: 0.2)) {
                proxy.scrollTo(last.id, anchor: .bottom)
            }
        }
    }

    private func handleAction(_ action: ChatAction) {
        switch action {
        case .searchProviders(let therapyType):
            // Set filter and navigate to provider list
            if let type = therapyType {
                appState.searchFilters.therapyTypes = [type + " therapy"]
            }
            appState.navigateToBrowse()
            // Dismiss chat sheet if presented as sheet

        case .viewMap(let zipCode):
            if let zip = zipCode {
                appState.saveUserContext(zipCode: zip)
            }
            appState.navigateToMap()

        case .viewRegionalCenters:
            appState.navigateToRegions()

        case .viewProviderList:
            appState.navigateToBrowse()

        case .callPhone(let number):
            let cleanNumber = number.replacingOccurrences(of: "[^0-9]", with: "", options: .regularExpression)
            if let url = URL(string: "tel:\(cleanNumber)") {
                UIApplication.shared.open(url)
            }

        case .openWebsite(let urlString):
            if let url = URL(string: urlString) {
                UIApplication.shared.open(url)
            }
        }
    }

    private func sendMessage(_ text: String) {
        let query = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return }

        inputText = ""
        isFocused = false

        // Build context from app state - prioritize local userZipCode from location
        let effectiveZip = userZipCode ?? appState.userZipCode
        print("📍 Sending query with ZIP: \(effectiveZip ?? "none")")

        let context = UserContext(
            zipCode: effectiveZip,
            childAge: appState.userChildAge,
            diagnosis: appState.userDiagnosis ?? appState.searchFilters.diagnosis,
            insurance: appState.userInsurance ?? appState.searchFilters.insurance,
            currentServices: nil
        )

        Task {
            await llmService.ask(query, context: context)
        }
    }
}

// MARK: - Welcome Card

struct WelcomeCard: View {
    let onSuggestionTap: (String) -> Void

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
                        onSuggestionTap(suggestion)
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

// MARK: - In-App Action Links

enum ChatAction: Identifiable {
    case searchProviders(therapyType: String?)
    case viewMap(zipCode: String?)
    case viewRegionalCenters
    case viewProviderList
    case callPhone(number: String)
    case openWebsite(url: String)

    var id: String {
        switch self {
        case .searchProviders(let type): return "search_\(type ?? "all")"
        case .viewMap(let zip): return "map_\(zip ?? "current")"
        case .viewRegionalCenters: return "rc"
        case .viewProviderList: return "list"
        case .callPhone(let num): return "call_\(num)"
        case .openWebsite(let url): return "web_\(url)"
        }
    }

    var icon: String {
        switch self {
        case .searchProviders: return "magnifyingglass"
        case .viewMap: return "map"
        case .viewRegionalCenters: return "building.2"
        case .viewProviderList: return "list.bullet"
        case .callPhone: return "phone"
        case .openWebsite: return "safari"
        }
    }

    var label: String {
        switch self {
        case .searchProviders(let type):
            return type != nil ? "Find \(type!) Providers" : "Search Providers"
        case .viewMap(let zip):
            return zip != nil ? "View Map (\(zip!))" : "Open Map"
        case .viewRegionalCenters: return "Regional Centers"
        case .viewProviderList: return "Browse All"
        case .callPhone: return "Call Now"
        case .openWebsite: return "Visit Website"
        }
    }
}

struct ChatActionButton: View {
    let action: ChatAction
    let onTap: (ChatAction) -> Void

    var body: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            onTap(action)
        } label: {
            HStack(spacing: 6) {
                Image(systemName: action.icon)
                    .font(.system(size: 12, weight: .semibold))
                Text(action.label)
                    .font(.system(size: 12, weight: .medium))
            }
            .foregroundColor(Color(hex: "6366F1"))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(hex: "EEF2FF"))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(Color(hex: "C7D2FE"), lineWidth: 1)
            )
        }
        .buttonStyle(ScaleButtonStyle())
    }
}

// MARK: - Message Bubble with Actions

struct MessageBubble: View {
    let message: ChatMessage
    var onLike: (() -> Void)?
    var onDislike: (() -> Void)?
    var onCopy: (() -> Void)?
    var onShare: (() -> Void)?
    var onAction: ((ChatAction) -> Void)?

    @State private var showActions = false

    // Detect suggested actions from message content
    private var suggestedActions: [ChatAction] {
        guard message.role == .assistant && !message.isStreaming else { return [] }

        var actions: [ChatAction] = []
        let content = message.content.lowercased()

        // Detect therapy type mentions
        if content.contains("aba") || content.contains("applied behavior") {
            actions.append(.searchProviders(therapyType: "ABA"))
        }
        if content.contains("speech") || content.contains("slp") {
            actions.append(.searchProviders(therapyType: "Speech"))
        }
        if content.contains("occupational therapy") || content.contains(" ot ") {
            actions.append(.searchProviders(therapyType: "OT"))
        }

        // Detect Regional Center mentions
        if content.contains("regional center") && !content.contains("which regional center") {
            actions.append(.viewRegionalCenters)
        }

        // Detect map/location suggestions
        if content.contains("near you") || content.contains("in your area") || content.contains("providers") {
            if actions.isEmpty {
                actions.append(.viewMap(zipCode: nil))
            }
        }

        // Limit to 3 actions
        return Array(actions.prefix(3))
    }

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            if message.role == .user {
                Spacer(minLength: 40)  // Reduced for wider bubbles
            }

            if message.role == .assistant || message.role == .system {
                // AI Avatar - smaller for more content space
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
                        .frame(width: 32, height: 32)

                    Image(systemName: message.role == .system ? "exclamationmark.triangle" : "sparkles")
                        .font(.system(size: 14))
                        .foregroundColor(.white)
                }
            }

            // Message content
            VStack(alignment: message.role == .user ? .trailing : .leading, spacing: 4) {
                if message.isLoading {
                    TypingIndicator()
                } else if message.isStreaming && message.content.isEmpty {
                    TypingIndicator()
                } else {
                    VStack(alignment: .leading, spacing: 8) {
                        // Use Markdown-aware text with clickable links
                        MarkdownTextView(
                            content: message.content,
                            isUserMessage: message.role == .user
                        )

                        // Streaming indicator
                        if message.isStreaming {
                            HStack(spacing: 4) {
                                ProgressView()
                                    .scaleEffect(0.7)
                                Text("Typing...")
                                    .font(.caption2)
                                    .foregroundColor(Color(hex: "94A3B8"))
                            }
                            .transition(.opacity.combined(with: .scale(scale: 0.8)))
                        }

                        // Suggested action buttons
                        if !suggestedActions.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(suggestedActions) { action in
                                        ChatActionButton(action: action) { action in
                                            onAction?(action)
                                        }
                                    }
                                }
                            }
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                        }

                        // Actions for assistant messages (not while streaming)
                        if message.role == .assistant && !message.isStreaming && !message.content.isEmpty {
                            MessageActionsBar(
                                feedback: message.feedback,
                                onLike: onLike,
                                onDislike: onDislike,
                                onCopy: onCopy,
                                onShare: onShare
                            )
                            .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .animation(.easeOut(duration: 0.2), value: message.isStreaming)
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
                Spacer(minLength: 20)  // Reduced for wider AI bubbles
            }
        }
    }
}

// MARK: - Message Actions Bar

struct MessageActionsBar: View {
    let feedback: ChatMessage.MessageFeedback?
    var onLike: (() -> Void)?
    var onDislike: (() -> Void)?
    var onCopy: (() -> Void)?
    var onShare: (() -> Void)?

    var body: some View {
        HStack(spacing: 16) {
            // Like
            Button {
                onLike?()
            } label: {
                Image(systemName: feedback == .liked ? "hand.thumbsup.fill" : "hand.thumbsup")
                    .font(.system(size: 14))
                    .foregroundColor(feedback == .liked ? Color(hex: "22C55E") : Color(hex: "94A3B8"))
            }

            // Dislike
            Button {
                onDislike?()
            } label: {
                Image(systemName: feedback == .disliked ? "hand.thumbsdown.fill" : "hand.thumbsdown")
                    .font(.system(size: 14))
                    .foregroundColor(feedback == .disliked ? Color(hex: "EF4444") : Color(hex: "94A3B8"))
            }

            Divider()
                .frame(height: 16)

            // Copy
            Button {
                onCopy?()
            } label: {
                Image(systemName: "doc.on.doc")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "94A3B8"))
            }

            // Share
            Button {
                onShare?()
            } label: {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "94A3B8"))
            }

            Spacer()
        }
        .padding(.top, 8)
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
    let isStreaming: Bool
    let onSend: () -> Void
    let onCancel: () -> Void

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
                        .disabled(isStreaming)
                }
                .background(Color(hex: "F1F5F9"))
                .cornerRadius(24)

                // Send/Cancel button
                Button(action: isStreaming ? onCancel : onSend) {
                    ZStack {
                        Circle()
                            .fill(
                                isStreaming
                                    ? AnyShapeStyle(Color(hex: "EF4444"))
                                    : (text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading
                                        ? AnyShapeStyle(Color(hex: "E2E8F0"))
                                        : AnyShapeStyle(LinearGradient(
                                            colors: [Color(hex: "6366F1"), Color(hex: "8B5CF6")],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )))
                            )
                            .frame(width: 44, height: 44)

                        if isLoading && !isStreaming {
                            ProgressView()
                                .tint(.white)
                        } else if isStreaming {
                            Image(systemName: "stop.fill")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.white)
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
                .disabled(!isStreaming && (text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.white)
        }
    }
}

// MARK: - Prompt Capsules Bar

struct PromptCapsulesBar: View {
    let prompts: [(String, String, String)]  // (emoji, label, full prompt)
    let onTap: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(prompts, id: \.1) { emoji, label, prompt in
                    PromptCapsule(emoji: emoji, label: label) {
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        onTap(prompt)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .background(
            Color(hex: "F8FAFC")
                .shadow(color: Color.black.opacity(0.05), radius: 2, y: 2)
        )
    }
}

struct PromptCapsule: View {
    let emoji: String
    let label: String
    let action: () -> Void

    @State private var isPressed = false

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(emoji)
                    .font(.system(size: 14))
                Text(label)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(Color(hex: "475569"))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(Color.white)
                    .shadow(color: Color.black.opacity(0.08), radius: 3, y: 1)
            )
            .overlay(
                Capsule()
                    .strokeBorder(Color(hex: "E2E8F0"), lineWidth: 1)
            )
        }
        .buttonStyle(ScaleButtonStyle())
    }
}

struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

// MARK: - Markdown Text View with Clickable Links

struct MarkdownTextView: View {
    let content: String
    let isUserMessage: Bool

    var body: some View {
        // Use LinkDetectingText which preserves formatting better
        // and makes phone numbers/URLs clickable
        LinkDetectingText(content: content, isUserMessage: isUserMessage)
    }
}

// MARK: - Link Detecting Text

struct LinkDetectingText: View {
    let content: String
    let isUserMessage: Bool

    var body: some View {
        Text(parseContent())
            .font(.body)
            .foregroundColor(isUserMessage ? .white : Color(hex: "1E293B"))
            .tint(isUserMessage ? .white.opacity(0.9) : Color(hex: "6366F1"))
            .textSelection(.enabled)
            .fixedSize(horizontal: false, vertical: true)  // Preserve line breaks
    }

    private func parseContent() -> AttributedString {
        // Preserve line breaks by keeping newlines intact
        var result = AttributedString(content)

        // Detect URLs
        let urlPattern = #"https?://[^\s\)\]\>\"\']+"#
        if let regex = try? NSRegularExpression(pattern: urlPattern, options: []) {
            let range = NSRange(content.startIndex..., in: content)
            let matches = regex.matches(in: content, options: [], range: range)

            for match in matches.reversed() {
                if let stringRange = Range(match.range, in: content),
                   let attributedRange = Range(stringRange, in: result),
                   let url = URL(string: String(content[stringRange])) {
                    result[attributedRange].link = url
                    result[attributedRange].underlineStyle = .single
                    result[attributedRange].foregroundColor = isUserMessage ? .white : UIColor(Color(hex: "6366F1"))
                }
            }
        }

        // Detect phone numbers (various formats)
        let phonePattern = #"\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}"#
        if let regex = try? NSRegularExpression(pattern: phonePattern, options: []) {
            let range = NSRange(content.startIndex..., in: content)
            let matches = regex.matches(in: content, options: [], range: range)

            for match in matches.reversed() {
                if let stringRange = Range(match.range, in: content),
                   let attributedRange = Range(stringRange, in: result) {
                    let phoneNumber = String(content[stringRange]).replacingOccurrences(of: "[^0-9]", with: "", options: .regularExpression)
                    if let url = URL(string: "tel:\(phoneNumber)") {
                        result[attributedRange].link = url
                        result[attributedRange].underlineStyle = .single
                        result[attributedRange].foregroundColor = isUserMessage ? .white : UIColor(Color(hex: "22C55E"))  // Green for phone
                    }
                }
            }
        }

        return result
    }
}

// Note: ShareSheet is defined in ContentView.swift

#Preview {
    ChatView()
        .environmentObject(AppState())
}
