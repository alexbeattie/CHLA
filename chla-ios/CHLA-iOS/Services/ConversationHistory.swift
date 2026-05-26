//
//  ConversationHistory.swift
//  CHLA-iOS
//
//  Manages conversation history persistence
//

import Foundation

// Saved conversation model
struct SavedConversation: Codable, Identifiable {
    let id: UUID
    let title: String
    let preview: String
    let messages: [SavedMessage]
    let createdAt: Date
    let updatedAt: Date
    
    struct SavedMessage: Codable {
        let role: String  // "user", "assistant", "system"
        let content: String
        let timestamp: Date
    }
    
    // Generate title from first user message
    static func generateTitle(from messages: [SavedMessage]) -> String {
        guard let firstUserMessage = messages.first(where: { $0.role == "user" }) else {
            return "New Conversation"
        }
        
        // Truncate to first 50 chars
        let content = firstUserMessage.content
        if content.count <= 50 {
            return content
        }
        return String(content.prefix(47)) + "..."
    }
    
    // Generate preview from last assistant message
    static func generatePreview(from messages: [SavedMessage]) -> String {
        guard let lastAssistantMessage = messages.last(where: { $0.role == "assistant" }) else {
            return "No response yet"
        }
        
        let content = lastAssistantMessage.content
        if content.count <= 80 {
            return content
        }
        return String(content.prefix(77)) + "..."
    }
}

@MainActor
class ConversationHistory: ObservableObject {
    @Published var conversations: [SavedConversation] = []
    
    private let storageKey = "saved_conversations"
    private let maxConversations = 50  // Limit stored conversations
    
    init() {
        loadConversations()
    }
    
    // Save current conversation
    func saveConversation(messages: [(role: String, content: String, timestamp: Date)], existingId: UUID? = nil) {
        let savedMessages = messages.map { SavedMessage(role: $0.role, content: $0.content, timestamp: $0.timestamp) }
        
        guard !savedMessages.isEmpty else { return }
        
        // Filter out system messages for title/preview
        let userMessages = savedMessages.filter { $0.role != "system" }
        guard !userMessages.isEmpty else { return }
        
        let title = SavedConversation.generateTitle(from: savedMessages)
        let preview = SavedConversation.generatePreview(from: savedMessages)
        
        if let existingId = existingId, let index = conversations.firstIndex(where: { $0.id == existingId }) {
            // Update existing conversation
            let updated = SavedConversation(
                id: existingId,
                title: title,
                preview: preview,
                messages: savedMessages,
                createdAt: conversations[index].createdAt,
                updatedAt: Date()
            )
            conversations[index] = updated
        } else {
            // Create new conversation
            let conversation = SavedConversation(
                id: UUID(),
                title: title,
                preview: preview,
                messages: savedMessages,
                createdAt: Date(),
                updatedAt: Date()
            )
            conversations.insert(conversation, at: 0)
        }
        
        // Limit total conversations
        if conversations.count > maxConversations {
            conversations = Array(conversations.prefix(maxConversations))
        }
        
        persistConversations()
    }
    
    // Delete a conversation
    func deleteConversation(id: UUID) {
        conversations.removeAll { $0.id == id }
        persistConversations()
    }
    
    // Delete all conversations
    func clearAll() {
        conversations.removeAll()
        persistConversations()
    }
    
    // Load conversations from storage
    private func loadConversations() {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let decoded = try? JSONDecoder().decode([SavedConversation].self, from: data) else {
            return
        }
        conversations = decoded.sorted { $0.updatedAt > $1.updatedAt }
    }
    
    // Persist to UserDefaults
    private func persistConversations() {
        guard let encoded = try? JSONEncoder().encode(conversations) else { return }
        UserDefaults.standard.set(encoded, forKey: storageKey)
    }
    
    // Format date for display
    static func formatDate(_ date: Date) -> String {
        let calendar = Calendar.current
        let now = Date()
        
        if calendar.isDateInToday(date) {
            let formatter = DateFormatter()
            formatter.timeStyle = .short
            return formatter.string(from: date)
        } else if calendar.isDateInYesterday(date) {
            return "Yesterday"
        } else if calendar.isDate(date, equalTo: now, toGranularity: .weekOfYear) {
            let formatter = DateFormatter()
            formatter.dateFormat = "EEEE"  // Day name
            return formatter.string(from: date)
        } else {
            let formatter = DateFormatter()
            formatter.dateStyle = .short
            return formatter.string(from: date)
        }
    }
}

private typealias SavedMessage = SavedConversation.SavedMessage
