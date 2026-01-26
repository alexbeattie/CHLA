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
import PhotosUI

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
    
    // Attachment flow state (Step 1: Type, Step 2: Source)
    @State private var showingAttachmentTypeSheet = false  // Step 1: Choose analysis type
    @State private var showingSourceOptions = false         // Step 2: Choose source
    @State private var pendingAnalysisType: ImageAnalysisType = .document
    
    // Image/Document picker state
    @State private var showingImagePicker = false
    @State private var showingCamera = false
    @State private var showingDocumentPicker = false
    @State private var selectedImage: UIImage?

    // Quick prompt suggestions - computed property for localization
    private var quickPrompts: [(String, String, String)] {
        [
            ("🔍", L10n.Chat.findProviders, L10n.Chat.suggestion1),
            ("📋", L10n.Chat.assessment, "chat.assessmentPrompt".localized),
            ("🏥", L10n.Chat.myRC, L10n.Chat.suggestion2),
            ("👶", L10n.Chat.earlyStart, L10n.Chat.suggestion4),
            ("💊", L10n.Chat.insurance, L10n.Chat.suggestion3),
            ("📅", L10n.Chat.waitlists, "chat.waitlistsPrompt".localized),
            ("🎂", L10n.Chat.age3Transition, "chat.age3Prompt".localized),
            ("🗣️", L10n.Chat.speech, "chat.speechPrompt".localized)
        ]
    }

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
                    onCancel: { llmService.cancelStreaming() },
                    onAttachment: { showingAttachmentTypeSheet = true }
                )
            }
            .navigationTitle(L10n.Chat.title)
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
                                Label(L10n.Chat.clearChat, systemImage: "trash")
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
            .confirmationDialog(L10n.Chat.clearConfirmTitle, isPresented: $showingClearConfirmation) {
                Button(L10n.Chat.clearChat, role: .destructive) {
                    llmService.clearChat()
                }
                Button(L10n.Common.cancel, role: .cancel) {}
            }
            .sheet(isPresented: $showingExportSheet) {
                ShareSheet(items: [exportText])
            }
            // Step 1: Choose what type of document to analyze
            .sheet(isPresented: $showingAttachmentTypeSheet) {
                AttachmentTypeSheet(onTypeSelected: { type in
                    pendingAnalysisType = type
                    // Small delay to allow sheet to dismiss before showing dialog
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showingSourceOptions = true
                    }
                })
            }
            // Step 2: Choose source (Photo Library, Camera, Files)
            .confirmationDialog("Choose Source", isPresented: $showingSourceOptions) {
                Button("Photo Library") {
                    showingImagePicker = true
                }
                Button("Take Photo") {
                    showingCamera = true
                }
                Button("Choose File") {
                    showingDocumentPicker = true
                }
                Button("Cancel", role: .cancel) {}
            }
            .sheet(isPresented: $showingImagePicker) {
                ImagePicker(image: $selectedImage, sourceType: .photoLibrary)
            }
            .fullScreenCover(isPresented: $showingCamera) {
                ImagePicker(image: $selectedImage, sourceType: .camera)
            }
            .onChange(of: selectedImage) { _, newImage in
                if newImage != nil {
                    // Directly analyze with pre-selected type - no need for second sheet
                    analyzeSelectedImage(type: pendingAnalysisType)
                }
            }
            .sheet(isPresented: $showingDocumentPicker) {
                // Capture analysis type at the moment picker is shown to avoid race condition
                let capturedType = pendingAnalysisType
                DocumentPicker { data, fileExtension in
                    // Handle different file types
                    if let image = UIImage(data: data),
                       let imageData = image.jpegData(compressionQuality: 0.8) {
                        // It's an image - analyze directly with captured type
                        Task {
                            await llmService.analyzeImage(imageData, type: capturedType)
                        }
                    } else {
                        // It's a document (PDF, Word, etc.) - use document analysis endpoint
                        Task {
                            await llmService.analyzeDocument(data, fileType: fileExtension)
                        }
                    }
                }
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
    
    // MARK: - Image Analysis
    
    private func analyzeSelectedImage(type: ImageAnalysisType) {
        guard let image = selectedImage,
              let imageData = image.jpegData(compressionQuality: 0.8) else {
            return
        }
        
        selectedImage = nil
        
        Task {
            await llmService.analyzeImage(imageData, type: type)
        }
    }
}

// MARK: - Identifiable Image Wrapper

struct IdentifiableImage: Identifiable {
    let id = UUID()
    let image: UIImage
}

// MARK: - Image Picker

struct ImagePicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    let sourceType: UIImagePickerController.SourceType
    @Environment(\.dismiss) private var dismiss
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.delegate = context.coordinator
        picker.allowsEditing = false
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ImagePicker
        
        init(_ parent: ImagePicker) {
            self.parent = parent
        }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.image = image
            }
            parent.dismiss()
        }
        
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

// MARK: - Image Analysis Sheet

struct ImageAnalysisSheet: View {
    let image: IdentifiableImage
    let onAnalyze: (ImageAnalysisType) -> Void
    let onCancel: () -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                // Image preview
                Image(uiImage: image.image)
                    .resizable()
                    .scaledToFit()
                    .frame(maxHeight: 300)
                    .cornerRadius(16)
                    .shadow(color: .black.opacity(0.1), radius: 8, y: 4)
                    .padding(.horizontal)
                
                // Analysis type selection
                VStack(alignment: .leading, spacing: 12) {
                    Text("What would you like to analyze?")
                        .font(.headline)
                        .foregroundColor(Color.primary)
                    
                    ForEach(ImageAnalysisType.allCases, id: \.rawValue) { type in
                        AnalysisTypeButton(type: type) {
                            dismiss()
                            onAnalyze(type)
                        }
                    }
                }
                .padding(.horizontal)
                
                Spacer()
            }
            .padding(.top, 20)
            .navigationTitle("Analyze Image")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                        onCancel()
                    }
                }
            }
        }
    }
}

struct AnalysisTypeButton: View {
    let type: ImageAnalysisType
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: type.icon)
                    .font(.system(size: 24))
                    .foregroundColor(Color(hex: "6366F1"))
                    .frame(width: 44, height: 44)
                    .background(Color(hex: "6366F1").opacity(0.15))
                    .cornerRadius(12)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(type.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color(uiColor: .label))
                    
                    Text(typeDescription)
                        .font(.caption)
                        .foregroundColor(Color(uiColor: .secondaryLabel))
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(Color(uiColor: .secondaryLabel))
            }
            .padding(16)
            .background(Color(uiColor: .secondarySystemBackground))
            .cornerRadius(16)
        }
        .buttonStyle(ScaleButtonStyle())
    }
    
    private var typeDescription: String {
        switch type {
        case .insuranceCard:
            return "Extract plan name, member ID, and coverage info"
        case .document:
            return "Analyze IEP, Regional Center letters, medical reports"
        case .general:
            return "Ask any question about the image"
        }
    }
}

// MARK: - Attachment Type Sheet (Step 1: Choose what to analyze)

struct AttachmentTypeSheet: View {
    let onTypeSelected: (ImageAnalysisType) -> Void
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text("What would you like to analyze?")
                    .font(.title3.weight(.semibold))
                    .foregroundColor(Color(uiColor: .label))
                    .padding(.top, 8)
                
                VStack(spacing: 12) {
                    ForEach(ImageAnalysisType.allCases, id: \.rawValue) { type in
                        AttachmentTypeButton(type: type) {
                            dismiss()
                            onTypeSelected(type)
                        }
                    }
                }
                .padding(.horizontal)
                
                Spacer()
            }
            .padding(.top, 20)
            .navigationTitle("Add Attachment")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .foregroundColor(Color(uiColor: .label))
                }
            }
        }
        .presentationDetents([.medium])
    }
}

struct AttachmentTypeButton: View {
    let type: ImageAnalysisType
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: type.icon)
                    .font(.system(size: 24))
                    .foregroundColor(Color(hex: "6366F1"))
                    .frame(width: 50, height: 50)
                    .background(Color(hex: "6366F1").opacity(0.15))
                    .cornerRadius(12)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(type.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color(uiColor: .label))
                    
                    Text(typeDescription)
                        .font(.caption)
                        .foregroundColor(Color(uiColor: .secondaryLabel))
                        .lineLimit(2)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(Color(uiColor: .secondaryLabel))
            }
            .padding(16)
            .background(Color(uiColor: .secondarySystemBackground))
            .cornerRadius(16)
        }
        .buttonStyle(ScaleButtonStyle())
    }
    
    private var typeDescription: String {
        switch type {
        case .insuranceCard:
            return "Extract plan name, member ID, coverage"
        case .document:
            return "IEP, RC letters, medical reports"
        case .general:
            return "Ask any question about an image"
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
                    .foregroundColor(Color.primary)

                Text("I can help you find developmental\nservices in Los Angeles County")
                    .font(.subheadline)
                    .foregroundColor(Color.secondary)
                    .multilineTextAlignment(.center)
            }

            // Suggestions
            VStack(alignment: .leading, spacing: 12) {
                Text("Try asking:")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(Color.secondary)
                    .padding(.leading, 4)

                ForEach(suggestions, id: \.self) { suggestion in
                    SuggestionChip(text: suggestion) {
                        onSuggestionTap(suggestion)
                    }
                }
            }
        }
        .padding(24)
        .background(Color(uiColor: .secondarySystemBackground))
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
                    .foregroundColor(Color(uiColor: .label))
                    .multilineTextAlignment(.leading)

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color(uiColor: .systemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(uiColor: .separator), lineWidth: 0.5)
            )
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
                                    .foregroundColor(Color(uiColor: .tertiaryLabel))
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
                            : AnyShapeStyle(Color(uiColor: .tertiarySystemBackground))
                    )
                    .cornerRadius(18)
                }

                Text(message.timestamp, style: .time)
                    .font(.caption2)
                    .foregroundColor(Color(uiColor: .tertiaryLabel))
            }

            if message.role == .user {
                // User Avatar
                ZStack {
                    Circle()
                        .fill(Color(uiColor: .separator))
                        .frame(width: 36, height: 36)

                    Image(systemName: "person.fill")
                        .font(.system(size: 16))
                        .foregroundColor(Color.secondary)
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
                    .foregroundColor(feedback == .liked ? Color(hex: "22C55E") : Color(uiColor: .secondaryLabel))
            }

            // Dislike
            Button {
                onDislike?()
            } label: {
                Image(systemName: feedback == .disliked ? "hand.thumbsdown.fill" : "hand.thumbsdown")
                    .font(.system(size: 14))
                    .foregroundColor(feedback == .disliked ? Color(hex: "EF4444") : Color(uiColor: .secondaryLabel))
            }

            Divider()
                .frame(height: 16)

            // Copy
            Button {
                onCopy?()
            } label: {
                Image(systemName: "doc.on.doc")
                    .font(.system(size: 14))
                    .foregroundColor(Color(uiColor: .secondaryLabel))
            }

            // Share
            Button {
                onShare?()
            } label: {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 14))
                    .foregroundColor(Color(uiColor: .secondaryLabel))
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
                    .fill(Color(uiColor: .tertiaryLabel))
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
        .background(Color(uiColor: .tertiarySystemBackground))
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
    var onAttachment: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            Divider()

            HStack(spacing: 8) {
                // Attachment button - opens type selection first
                if let onAttachment = onAttachment {
                    Button(action: onAttachment) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 28))
                            .foregroundColor(isLoading ? Color(uiColor: .secondaryLabel) : Color(hex: "6366F1"))
                    }
                    .disabled(isLoading || isStreaming)
                }
                
                // Text field
                HStack {
                    TextField("Ask about services...", text: $text, axis: .vertical)
                        .focused(isFocused)
                        .lineLimit(1...5)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .foregroundColor(Color(uiColor: .label))
                        .disabled(isStreaming)
                }
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(24)

                // Send/Cancel button
                Button(action: isStreaming ? onCancel : onSend) {
                    ZStack {
                        Circle()
                            .fill(
                                isStreaming
                                    ? AnyShapeStyle(Color(hex: "EF4444"))
                                    : (text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading
                                        ? AnyShapeStyle(Color(uiColor: .quaternaryLabel))
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
                                        ? Color(uiColor: .secondaryLabel)
                                        : .white
                                )
                        }
                    }
                }
                .disabled(!isStreaming && (text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(uiColor: .systemBackground))
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
            Color(uiColor: .secondarySystemBackground)
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
                    .strokeBorder(Color(uiColor: .separator), lineWidth: 1)
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
        Text(parseMarkdown())
            .font(.body)
            .foregroundColor(isUserMessage ? .white : Color(uiColor: .label))
            .tint(isUserMessage ? .white.opacity(0.9) : Color(hex: "6366F1"))
            .textSelection(.enabled)
            .fixedSize(horizontal: false, vertical: true)
    }
    
    private func parseMarkdown() -> AttributedString {
        // Convert asterisk-based markdown to proper formatting
        let processed = processMarkdownSyntax(content)
        
        // Try to parse as markdown first
        if let attributed = try? AttributedString(markdown: processed, options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace)) {
            var result = attributed
            // Add link styling and phone detection
            addLinkStyling(&result)
            return result
        }
        
        // Fallback to basic text with link detection
        var result = AttributedString(content)
        addLinkStyling(&result)
        return result
    }
    
    private func processMarkdownSyntax(_ text: String) -> String {
        var result = text
        
        // Convert **text** to proper bold (already markdown)
        // Convert *text* to proper italic (already markdown)
        // Convert bullet points: "• " or "- " at line start
        
        // Replace "• " with "- " for consistent markdown bullets
        result = result.replacingOccurrences(of: "• ", with: "- ")
        
        return result
    }
    
    private func addLinkStyling(_ attributed: inout AttributedString) {
        let plainString = String(attributed.characters)
        
        // Detect URLs
        let urlPattern = #"https?://[^\s\)\]\>\"\']+"#
        if let regex = try? NSRegularExpression(pattern: urlPattern, options: []) {
            let range = NSRange(plainString.startIndex..., in: plainString)
            let matches = regex.matches(in: plainString, options: [], range: range)

            for match in matches.reversed() {
                if let stringRange = Range(match.range, in: plainString),
                   let attributedRange = Range(stringRange, in: attributed),
                   let url = URL(string: String(plainString[stringRange])) {
                    attributed[attributedRange].link = url
                    attributed[attributedRange].underlineStyle = .single
                }
            }
        }

        // Detect phone numbers
        let phonePattern = #"\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}"#
        if let regex = try? NSRegularExpression(pattern: phonePattern, options: []) {
            let range = NSRange(plainString.startIndex..., in: plainString)
            let matches = regex.matches(in: plainString, options: [], range: range)

            for match in matches.reversed() {
                if let stringRange = Range(match.range, in: plainString),
                   let attributedRange = Range(stringRange, in: attributed) {
                    let phoneNumber = String(plainString[stringRange]).replacingOccurrences(of: "[^0-9]", with: "", options: .regularExpression)
                    if let url = URL(string: "tel:\(phoneNumber)") {
                        attributed[attributedRange].link = url
                        attributed[attributedRange].underlineStyle = .single
                        attributed[attributedRange].foregroundColor = isUserMessage ? .white : UIColor(Color(hex: "22C55E"))
                    }
                }
            }
        }
    }
}

// MARK: - Document Picker

import UniformTypeIdentifiers

struct DocumentPicker: UIViewControllerRepresentable {
    let onDocumentPicked: (Data, String) -> Void  // (data, fileExtension)
    
    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        // Support images, PDFs, and common document types
        var supportedTypes: [UTType] = [
            .image, .pdf, .jpeg, .png, .heic, .gif, .webP,
            .plainText, .rtf
        ]
        // Add Word document types if available
        if let docx = UTType("org.openxmlformats.wordprocessingml.document") {
            supportedTypes.append(docx)
        }
        if let doc = UTType("com.microsoft.word.doc") {
            supportedTypes.append(doc)
        }
        
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: supportedTypes)
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(onDocumentPicked: onDocumentPicked)
    }
    
    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onDocumentPicked: (Data, String) -> Void
        
        init(onDocumentPicked: @escaping (Data, String) -> Void) {
            self.onDocumentPicked = onDocumentPicked
        }
        
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else { return }
            
            // Access the security-scoped resource
            guard url.startAccessingSecurityScopedResource() else { return }
            defer { url.stopAccessingSecurityScopedResource() }
            
            do {
                let data = try Data(contentsOf: url)
                let fileExtension = url.pathExtension.lowercased()
                onDocumentPicked(data, fileExtension)
            } catch {
                print("Error reading document: \(error)")
            }
            // Note: UIDocumentPickerViewController auto-dismisses after picking/cancelling
        }
        
        // Note: documentPickerWasCancelled is called after auto-dismiss, no action needed
    }
}

// Note: ShareSheet is defined in ContentView.swift

#Preview {
    ChatView()
        .environmentObject(AppState())
}
