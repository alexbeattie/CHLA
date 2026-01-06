//
//  LLMService.swift
//  CHLA-iOS
//
//  Created for KiNDD Resource Navigator
//  Integrates with AWS Bedrock via Django backend
//

import Foundation
import SwiftUI

// MARK: - Models

struct ChatMessage: Identifiable, Equatable {
    let id = UUID()
    let role: MessageRole
    let content: String
    let timestamp = Date()
    var isLoading: Bool = false

    enum MessageRole {
        case user
        case assistant
        case system
    }

    static func == (lhs: ChatMessage, rhs: ChatMessage) -> Bool {
        lhs.id == rhs.id
    }
}

struct LLMResponse: Codable {
    let query: String
    let answer: String
    let providersReferenced: [Int]?
    let regionalCenter: String?

    enum CodingKeys: String, CodingKey {
        case query
        case answer
        case providersReferenced = "providers_referenced"
        case regionalCenter = "regional_center"
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

// MARK: - LLM Service

@MainActor
class LLMService: ObservableObject {
    static let shared = LLMService()

    @Published var messages: [ChatMessage] = []
    @Published var isLoading = false
    @Published var error: String?

    private let baseURL: String

    init() {
        #if DEBUG
        // For iOS Simulator: use localhost
        // For physical iPhone: use your Mac's IP (e.g., 192.168.1.194)
        #if targetEnvironment(simulator)
        self.baseURL = "http://127.0.0.1:8000/api/llm"
        #else
        // Physical device - use Mac's local IP
        self.baseURL = "http://192.168.1.194:8000/api/llm"
        #endif
        #else
        self.baseURL = "https://api.kinddhelp.com/api/llm"
        #endif
    }

    // MARK: - Public Methods

    func ask(_ query: String, context: UserContext? = nil) async {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        // Add user message
        let userMessage = ChatMessage(role: .user, content: query)
        messages.append(userMessage)

        // Add loading placeholder
        let loadingMessage = ChatMessage(role: .assistant, content: "", isLoading: true)
        messages.append(loadingMessage)

        isLoading = true
        error = nil

        do {
            let response = try await sendQuery(query, context: context)

            // Remove loading message and add real response
            messages.removeAll { $0.isLoading }
            let assistantMessage = ChatMessage(role: .assistant, content: response.answer)
            messages.append(assistantMessage)

        } catch {
            // Remove loading message and show error
            messages.removeAll { $0.isLoading }
            self.error = error.localizedDescription

            let errorMessage = ChatMessage(
                role: .system,
                content: "Sorry, I couldn't process your request. Please try again."
            )
            messages.append(errorMessage)
        }

        isLoading = false
    }

    func clearChat() {
        messages.removeAll()
        error = nil
    }

    // MARK: - Private Methods

    private func sendQuery(_ query: String, context: UserContext?) async throws -> LLMResponse {
        guard let url = URL(string: "\(baseURL)/ask/") else {
            throw LLMError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 60 // LLM can take time

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
