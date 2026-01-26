//
//  LLMService.swift
//  CHLA-iOS
//
//  Created for KiNDD Resource Navigator
//  Integrates with AWS Bedrock via Django backend
//  Supports streaming responses (SSE)
//

import Foundation
import SwiftUI

// MARK: - Models

struct ChatMessage: Identifiable, Equatable {
    let id: UUID
    let role: MessageRole
    var content: String
    let timestamp: Date
    var isLoading: Bool = false
    var isStreaming: Bool = false
    var feedback: MessageFeedback?
    var providersReferenced: [String]?
    var regionalCenter: String?

    init(
        id: UUID = UUID(),
        role: MessageRole,
        content: String,
        timestamp: Date = Date(),
        isLoading: Bool = false,
        isStreaming: Bool = false,
        feedback: MessageFeedback? = nil,
        providersReferenced: [String]? = nil,
        regionalCenter: String? = nil
    ) {
        self.id = id
        self.role = role
        self.content = content
        self.timestamp = timestamp
        self.isLoading = isLoading
        self.isStreaming = isStreaming
        self.feedback = feedback
        self.providersReferenced = providersReferenced
        self.regionalCenter = regionalCenter
    }

    enum MessageRole: String {
        case user
        case assistant
        case system
    }

    enum MessageFeedback: String {
        case liked
        case disliked
    }

    static func == (lhs: ChatMessage, rhs: ChatMessage) -> Bool {
        lhs.id == rhs.id
    }
}

struct LLMResponse: Codable {
    let query: String
    let answer: String
    let providersReferenced: [String]?
    let regionalCenter: String?

    enum CodingKeys: String, CodingKey {
        case query
        case answer
        case providersReferenced = "providers_referenced"
        case regionalCenter = "regional_center"
    }
}

struct SSEChunk: Codable {
    let type: String  // "chunk", "done", "error"
    let content: String?
    let providersReferenced: [String]?
    let regionalCenter: String?
    let message: String?  // For errors

    enum CodingKeys: String, CodingKey {
        case type
        case content
        case providersReferenced = "providers_referenced"
        case regionalCenter = "regional_center"
        case message
    }
}

struct UserContext: Codable {
    var zipCode: String?
    var childAge: Int?
    var diagnosis: String?
    var insurance: String?
    var currentServices: [String]?

    enum CodingKeys: String, CodingKey {
        case zipCode = "zip_code"
        case childAge = "child_age"
        case diagnosis
        case insurance
        case currentServices = "current_services"
    }
}

struct ImageAnalysisResponse: Codable {
    let analysis: String
    let type: String
}

struct DocumentAnalysisResponse: Codable {
    let analysis: String
    let fileType: String
    let textExtracted: Bool
    let textLength: Int?
    
    enum CodingKeys: String, CodingKey {
        case analysis
        case fileType = "file_type"
        case textExtracted = "text_extracted"
        case textLength = "text_length"
    }
}

enum ImageAnalysisType: String, CaseIterable {
    case insuranceCard = "insurance_card"
    case document = "document"
    case general = "general"
    
    var title: String {
        switch self {
        case .insuranceCard: return "Insurance Card"
        case .document: return "Document (IEP, Letter)"
        case .general: return "General Photo"
        }
    }
    
    var icon: String {
        switch self {
        case .insuranceCard: return "creditcard"
        case .document: return "doc.text"
        case .general: return "photo"
        }
    }
}

// MARK: - LLM Service

@MainActor
class LLMService: ObservableObject {
    static let shared = LLMService()

    @Published var messages: [ChatMessage] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var useStreaming = true  // Toggle streaming mode

    private let baseURL: String
    private var streamingTask: Task<Void, Never>?
    private var pendingText = ""
    private var updateDebounceTask: Task<Void, Never>?

    init() {
        #if DEBUG
        #if targetEnvironment(simulator)
        self.baseURL = "http://127.0.0.1:8000/api/llm"
        #else
        // Physical device - use production API
        self.baseURL = "https://api.kinddhelp.com/api/llm"
        #endif
        #else
        self.baseURL = "https://api.kinddhelp.com/api/llm"
        #endif
    }

    // MARK: - Public Methods

    func ask(_ query: String, context: UserContext? = nil) async {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        // Cancel any existing streaming
        streamingTask?.cancel()

        // Add user message
        let userMessage = ChatMessage(role: .user, content: query)
        messages.append(userMessage)

        isLoading = true
        error = nil

        if useStreaming {
            await askStreaming(query, context: context)
        } else {
            await askNonStreaming(query, context: context)
        }

        isLoading = false
    }

    func cancelStreaming() {
        streamingTask?.cancel()
        streamingTask = nil
        updateDebounceTask?.cancel()

        // Flush pending text and mark as complete
        if let index = messages.lastIndex(where: { $0.isStreaming }) {
            flushPendingText(id: messages[index].id)
            messages[index].isStreaming = false
        }
        pendingText = ""
        isLoading = false
    }

    func clearChat() {
        cancelStreaming()
        messages.removeAll()
        pendingText = ""
        error = nil
    }
    
    // Add a restored message from history
    func addRestoredMessage(role: ChatMessage.MessageRole, content: String, timestamp: Date) {
        let message = ChatMessage(
            role: role,
            content: content,
            timestamp: timestamp,
            isStreaming: false
        )
        messages.append(message)
    }

    // MARK: - Message Actions

    func setFeedback(_ messageId: UUID, feedback: ChatMessage.MessageFeedback?) {
        if let index = messages.firstIndex(where: { $0.id == messageId }) {
            messages[index].feedback = feedback
            // TODO: Send feedback to backend for analytics
        }
    }

    func copyMessage(_ messageId: UUID) -> String? {
        if let message = messages.first(where: { $0.id == messageId }) {
            return message.content
        }
        return nil
    }

    // MARK: - Image Analysis
    
    func analyzeImage(_ imageData: Data, type: ImageAnalysisType, prompt: String? = nil) async {
        isLoading = true
        error = nil
        
        // Add user message indicating image upload
        let userMessage = ChatMessage(
            role: .user,
            content: "📷 Analyzing \(type.title.lowercased())..."
        )
        messages.append(userMessage)
        
        // Add loading placeholder
        let loadingMessage = ChatMessage(role: .assistant, content: "", isLoading: true)
        messages.append(loadingMessage)
        
        do {
            let response = try await sendImageAnalysis(imageData, type: type, prompt: prompt)
            
            // Remove loading message and add real response
            messages.removeAll { $0.isLoading }
            let assistantMessage = ChatMessage(
                role: .assistant,
                content: response.analysis
            )
            messages.append(assistantMessage)
            
        } catch {
            messages.removeAll { $0.isLoading }
            addErrorMessage(error.localizedDescription)
        }
        
        isLoading = false
    }
    
    private func sendImageAnalysis(_ imageData: Data, type: ImageAnalysisType, prompt: String?) async throws -> ImageAnalysisResponse {
        let urlString = "\(baseURL)/analyze-image/"
        print("🟡 Image Analysis Request to: \(urlString)")
        
        guard let url = URL(string: urlString) else {
            throw LLMError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 120  // Longer timeout for image processing
        
        // Encode image as base64
        let base64Image = imageData.base64EncodedString()
        
        // Detect image type from data
        var mediaType = "image/jpeg"
        if imageData.count > 8 {
            let header = [UInt8](imageData.prefix(8))
            if header[0] == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47 {
                mediaType = "image/png"
            }
        }
        
        // Build request with data URL format
        let dataURL = "data:\(mediaType);base64,\(base64Image)"
        
        var body: [String: Any] = [
            "image": dataURL,
            "type": type.rawValue
        ]
        
        if let prompt = prompt {
            body["prompt"] = prompt
        }
        
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw LLMError.invalidResponse
        }
        
        print("🟢 Image Analysis Status: \(httpResponse.statusCode)")
        
        guard httpResponse.statusCode == 200 else {
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorMessage = errorJson["error"] as? String {
                throw LLMError.serverError(errorMessage)
            }
            throw LLMError.httpError(httpResponse.statusCode)
        }
        
        return try JSONDecoder().decode(ImageAnalysisResponse.self, from: data)
    }
    
    // MARK: - Document Analysis
    
    func analyzeDocument(_ documentData: Data, fileType: String, prompt: String? = nil) async {
        isLoading = true
        error = nil
        
        // Add user message indicating document upload
        let userMessage = ChatMessage(
            role: .user,
            content: "📄 Analyzing \(fileType.uppercased()) document..."
        )
        messages.append(userMessage)
        
        // Add loading placeholder
        let loadingMessage = ChatMessage(role: .assistant, content: "", isLoading: true)
        messages.append(loadingMessage)
        
        do {
            let response = try await sendDocumentAnalysis(documentData, fileType: fileType, prompt: prompt)
            
            // Remove loading message and add real response
            messages.removeAll { $0.isLoading }
            let assistantMessage = ChatMessage(
                role: .assistant,
                content: response.analysis
            )
            messages.append(assistantMessage)
            
        } catch {
            messages.removeAll { $0.isLoading }
            addErrorMessage(error.localizedDescription)
        }
        
        isLoading = false
    }
    
    private func sendDocumentAnalysis(_ documentData: Data, fileType: String, prompt: String?) async throws -> DocumentAnalysisResponse {
        let urlString = "\(baseURL)/analyze-document/"
        print("🟡 Document Analysis Request to: \(urlString)")
        
        guard let url = URL(string: urlString) else {
            throw LLMError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 120  // Longer timeout for document processing
        
        // Encode document as base64
        let base64Document = documentData.base64EncodedString()
        
        var body: [String: Any] = [
            "document": base64Document,
            "file_type": fileType
        ]
        
        if let prompt = prompt {
            body["prompt"] = prompt
        }
        
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw LLMError.invalidResponse
        }
        
        print("🟢 Document Analysis Status: \(httpResponse.statusCode)")
        
        guard httpResponse.statusCode == 200 else {
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorMessage = errorJson["error"] as? String {
                throw LLMError.serverError(errorMessage)
            }
            throw LLMError.httpError(httpResponse.statusCode)
        }
        
        return try JSONDecoder().decode(DocumentAnalysisResponse.self, from: data)
    }
    
    func exportConversation(userZipCode: String? = nil, regionalCenter: String? = nil) -> String {
        var export = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
        export += "       KiNDD Chat Export\n"
        export += "       Neurodevelopmental Services Navigator\n"
        export += "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
        export += "📅 Date: \(Date().formatted(date: .long, time: .shortened))\n"

        if let zip = userZipCode {
            export += "📍 Location: \(zip)\n"
        }
        if let rc = regionalCenter {
            export += "🏢 Regional Center: \(rc)\n"
        }

        export += "\n" + String(repeating: "─", count: 40) + "\n\n"

        for message in messages {
            let role = message.role == .user ? "👤 You" : "✨ KiNDD"
            let time = message.timestamp.formatted(date: .omitted, time: .shortened)
            export += "[\(time)] \(role):\n\(message.content)\n\n"

            // Include referenced providers if available
            if let providers = message.providersReferenced, !providers.isEmpty {
                export += "📋 Providers referenced: \(providers.count)\n\n"
            }
        }

        export += String(repeating: "─", count: 40) + "\n"
        export += "Exported from KiNDD - kinddhelp.com\n"
        export += "LA County Developmental Services\n"

        return export
    }

    // MARK: - Private Methods - Streaming

    private func askStreaming(_ query: String, context: UserContext?) async {
        let urlString = "\(baseURL)/stream/"
        print("🟡 Streaming request to: \(urlString)")

        guard let url = URL(string: urlString) else {
            addErrorMessage("Invalid API URL")
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        // Don't set Accept header - DRF returns 406 for text/event-stream
        request.timeoutInterval = 120

        // Get device locale for AI response language
        let locale = Locale.current.language.languageCode?.identifier ?? "en"
        
        var body: [String: Any] = [
            "query": query,
            "locale": locale  // Pass locale to backend for AI language
        ]
        if let context = context {
            var contextDict: [String: Any] = [:]
            if let zip = context.zipCode { contextDict["zip_code"] = zip }
            if let age = context.childAge { contextDict["child_age"] = age }
            if let dx = context.diagnosis { contextDict["diagnosis"] = dx }
            if let ins = context.insurance { contextDict["insurance"] = ins }
            if let services = context.currentServices { contextDict["current_services"] = services }
            body["context"] = contextDict
        }

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        } catch {
            addErrorMessage("Failed to encode request")
            return
        }

        // Add placeholder streaming message
        let streamingMessageId = UUID()
        let streamingMessage = ChatMessage(
            id: streamingMessageId,
            role: .assistant,
            content: "",
            isStreaming: true
        )
        messages.append(streamingMessage)

        // Start streaming
        do {
            let (bytes, response) = try await URLSession.shared.bytes(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                updateStreamingMessage(id: streamingMessageId, content: "Invalid response", isComplete: true)
                return
            }

            print("🟢 HTTP Status: \(httpResponse.statusCode)")

            guard httpResponse.statusCode == 200 else {
                // Read error body
                var errorBody = ""
                for try await byte in bytes {
                    errorBody.append(Character(UnicodeScalar(byte)))
                    if errorBody.count > 500 { break }
                }
                print("🔴 Error body: \(errorBody)")
                updateStreamingMessage(id: streamingMessageId, content: "Server error (\(httpResponse.statusCode)): \(errorBody.prefix(200))", isComplete: true)
                return
            }

            var buffer = ""

            for try await byte in bytes {
                buffer.append(Character(UnicodeScalar(byte)))

                // Check for complete SSE event
                while let range = buffer.range(of: "\n\n") {
                    let eventData = String(buffer[..<range.lowerBound])
                    buffer.removeSubrange(..<range.upperBound)

                    // Parse SSE event
                    if eventData.hasPrefix("data: ") {
                        let jsonString = String(eventData.dropFirst(6))
                        if let data = jsonString.data(using: .utf8),
                           let chunk = try? JSONDecoder().decode(SSEChunk.self, from: data) {
                            await handleSSEChunk(chunk, messageId: streamingMessageId)
                        }
                    }
                }
            }

        } catch {
            if !Task.isCancelled {
                updateStreamingMessage(id: streamingMessageId, content: "Connection error: \(error.localizedDescription)", isComplete: true)
            }
        }
    }

    private func handleSSEChunk(_ chunk: SSEChunk, messageId: UUID) async {
        switch chunk.type {
        case "chunk":
            if let content = chunk.content {
                appendToStreamingMessage(id: messageId, text: content)
            }
        case "done":
            finalizeStreamingMessage(
                id: messageId,
                providersReferenced: chunk.providersReferenced,
                regionalCenter: chunk.regionalCenter
            )
        case "error":
            updateStreamingMessage(id: messageId, content: chunk.message ?? "Unknown error", isComplete: true)
        default:
            break
        }
    }

    private func appendToStreamingMessage(id: UUID, text: String) {
        // Batch text updates for smoother animation
        pendingText += text

        // Debounce updates - flush every 50ms for smooth streaming
        updateDebounceTask?.cancel()
        updateDebounceTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 50_000_000) // 50ms
            guard !Task.isCancelled else { return }

            if let index = messages.firstIndex(where: { $0.id == id }) {
                withAnimation(.easeOut(duration: 0.1)) {
                    messages[index].content += pendingText
                }
                pendingText = ""
            }
        }
    }

    private func flushPendingText(id: UUID) {
        updateDebounceTask?.cancel()
        if !pendingText.isEmpty, let index = messages.firstIndex(where: { $0.id == id }) {
            messages[index].content += pendingText
            pendingText = ""
        }
    }

    private func updateStreamingMessage(id: UUID, content: String, isComplete: Bool) {
        if let index = messages.firstIndex(where: { $0.id == id }) {
            messages[index].content = content
            messages[index].isStreaming = !isComplete
        }
    }

    private func finalizeStreamingMessage(id: UUID, providersReferenced: [String]?, regionalCenter: String?) {
        // Flush any remaining pending text
        flushPendingText(id: id)

        if let index = messages.firstIndex(where: { $0.id == id }) {
            withAnimation(.easeOut(duration: 0.3)) {
                messages[index].isStreaming = false
                messages[index].providersReferenced = providersReferenced
                messages[index].regionalCenter = regionalCenter
            }
        }
    }

    // MARK: - Private Methods - Non-Streaming (fallback)

    private func askNonStreaming(_ query: String, context: UserContext?) async {
        // Add loading placeholder
        let loadingMessage = ChatMessage(role: .assistant, content: "", isLoading: true)
        messages.append(loadingMessage)

        do {
            let response = try await sendQuery(query, context: context)

            // Remove loading message and add real response
            messages.removeAll { $0.isLoading }
            let assistantMessage = ChatMessage(
                role: .assistant,
                content: response.answer,
                providersReferenced: response.providersReferenced,
                regionalCenter: response.regionalCenter
            )
            messages.append(assistantMessage)

        } catch {
            messages.removeAll { $0.isLoading }
            addErrorMessage(error.localizedDescription)
        }
    }

    private func addErrorMessage(_ text: String) {
        self.error = text
        print("🔴 LLM Error: \(text)")
        print("🔴 URL: \(baseURL)")

        let errorMessage = ChatMessage(
            role: .system,
            content: "Error: \(text)"
        )
        messages.append(errorMessage)
    }

    private func sendQuery(_ query: String, context: UserContext?) async throws -> LLMResponse {
        let urlString = "\(baseURL)/ask/"
        print("🟡 LLM Request to: \(urlString)")

        guard let url = URL(string: urlString) else {
            throw LLMError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 60

        var body: [String: Any] = ["query": query]

        if let context = context {
            var contextDict: [String: Any] = [:]
            if let zip = context.zipCode { contextDict["zip_code"] = zip }
            if let age = context.childAge { contextDict["child_age"] = age }
            if let dx = context.diagnosis { contextDict["diagnosis"] = dx }
            if let ins = context.insurance { contextDict["insurance"] = ins }
            if let services = context.currentServices { contextDict["current_services"] = services }
            body["context"] = contextDict
        }

        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw LLMError.invalidResponse
        }

        guard httpResponse.statusCode == 200 else {
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorMessage = errorJson["error"] as? String {
                throw LLMError.serverError(errorMessage)
            }
            throw LLMError.httpError(httpResponse.statusCode)
        }

        return try JSONDecoder().decode(LLMResponse.self, from: data)
    }
}

// MARK: - Errors

enum LLMError: LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(Int)
    case serverError(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid API URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .httpError(let code):
            return "Server error (code: \(code))"
        case .serverError(let message):
            return message
        }
    }
}
