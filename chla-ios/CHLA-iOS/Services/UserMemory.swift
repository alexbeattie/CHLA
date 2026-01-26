//
//  UserMemory.swift
//  CHLA-iOS
//
//  Persistent memory of user context for personalized LLM responses
//

import Foundation

struct UserMemoryContext: Codable {
    var childAge: String?
    var childName: String?
    var diagnoses: [String]
    var therapyInterests: [String]
    var regionalCenter: String?
    var zipCode: String?
    var preferredLanguage: String?
    var insuranceType: String?
    var notes: [String]  // Freeform notes extracted from conversations
    var lastUpdated: Date
    
    init() {
        self.diagnoses = []
        self.therapyInterests = []
        self.notes = []
        self.lastUpdated = Date()
    }
    
    // Check if memory has meaningful content
    var hasContent: Bool {
        childAge != nil ||
        childName != nil ||
        !diagnoses.isEmpty ||
        !therapyInterests.isEmpty ||
        regionalCenter != nil ||
        insuranceType != nil ||
        !notes.isEmpty
    }
    
    // Generate a summary for LLM context injection
    var contextSummary: String {
        var parts: [String] = []
        
        if let name = childName {
            parts.append("Child's name: \(name)")
        }
        if let age = childAge {
            parts.append("Child's age: \(age)")
        }
        if !diagnoses.isEmpty {
            parts.append("Diagnoses/conditions: \(diagnoses.joined(separator: ", "))")
        }
        if !therapyInterests.isEmpty {
            parts.append("Interested in: \(therapyInterests.joined(separator: ", "))")
        }
        if let rc = regionalCenter {
            parts.append("Regional Center: \(rc)")
        }
        if let insurance = insuranceType {
            parts.append("Insurance: \(insurance)")
        }
        if let zip = zipCode {
            parts.append("Location: ZIP \(zip)")
        }
        
        return parts.joined(separator: ". ")
    }
}

@MainActor
class UserMemory: ObservableObject {
    @Published var context: UserMemoryContext
    
    private let storageKey = "user_memory_context"
    
    init() {
        self.context = UserMemoryContext()
        loadContext()
    }
    
    // MARK: - Persistence
    
    private func loadContext() {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let decoded = try? JSONDecoder().decode(UserMemoryContext.self, from: data) else {
            return
        }
        context = decoded
    }
    
    private func saveContext() {
        context.lastUpdated = Date()
        guard let encoded = try? JSONEncoder().encode(context) else { return }
        UserDefaults.standard.set(encoded, forKey: storageKey)
    }
    
    // MARK: - Update Methods
    
    func setChildInfo(name: String?, age: String?) {
        if let name = name, !name.isEmpty {
            context.childName = name
        }
        if let age = age, !age.isEmpty {
            context.childAge = age
        }
        saveContext()
    }
    
    func addDiagnosis(_ diagnosis: String) {
        let cleaned = diagnosis.trimmingCharacters(in: .whitespacesAndNewlines)
        if !cleaned.isEmpty && !context.diagnoses.contains(cleaned) {
            context.diagnoses.append(cleaned)
            saveContext()
        }
    }
    
    func addTherapyInterest(_ therapy: String) {
        let cleaned = therapy.trimmingCharacters(in: .whitespacesAndNewlines)
        if !cleaned.isEmpty && !context.therapyInterests.contains(cleaned) {
            context.therapyInterests.append(cleaned)
            saveContext()
        }
    }
    
    func setRegionalCenter(_ rc: String) {
        context.regionalCenter = rc
        saveContext()
    }
    
    func setZipCode(_ zip: String) {
        context.zipCode = zip
        saveContext()
    }
    
    func setInsurance(_ insurance: String) {
        context.insuranceType = insurance
        saveContext()
    }
    
    func addNote(_ note: String) {
        let cleaned = note.trimmingCharacters(in: .whitespacesAndNewlines)
        if !cleaned.isEmpty && !context.notes.contains(cleaned) {
            context.notes.append(cleaned)
            // Keep only last 10 notes
            if context.notes.count > 10 {
                context.notes = Array(context.notes.suffix(10))
            }
            saveContext()
        }
    }
    
    func clearAll() {
        context = UserMemoryContext()
        saveContext()
    }
    
    // MARK: - Context Extraction
    
    /// Analyze a message for extractable context
    func extractContextFromMessage(_ message: String) {
        let lowered = message.lowercased()
        
        // Extract therapy interests
        let therapies = [
            ("aba", "ABA therapy"),
            ("applied behavior", "ABA therapy"),
            ("speech", "Speech therapy"),
            ("occupational", "Occupational therapy"),
            ("physical therapy", "Physical therapy"),
            ("feeding", "Feeding therapy"),
            ("play therapy", "Play therapy")
        ]
        
        for (keyword, therapy) in therapies {
            if lowered.contains(keyword) {
                addTherapyInterest(therapy)
            }
        }
        
        // Extract diagnoses
        let diagnoses = [
            ("autism", "Autism Spectrum Disorder"),
            ("asd", "Autism Spectrum Disorder"),
            ("adhd", "ADHD"),
            ("developmental delay", "Developmental Delay"),
            ("speech delay", "Speech Delay"),
            ("down syndrome", "Down Syndrome"),
            ("cerebral palsy", "Cerebral Palsy")
        ]
        
        for (keyword, diagnosis) in diagnoses {
            if lowered.contains(keyword) {
                addDiagnosis(diagnosis)
            }
        }
        
        // Extract age patterns like "2 year old", "18 months", "my 3-year-old"
        let agePatterns = [
            "\\b(\\d+)\\s*(year|yr)s?\\s*old\\b",
            "\\b(\\d+)\\s*months?\\s*old\\b",
            "my\\s*(\\d+)\\s*(year|yr|month)"
        ]
        
        for pattern in agePatterns {
            if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
               let match = regex.firstMatch(in: message, range: NSRange(message.startIndex..., in: message)),
               let range = Range(match.range, in: message) {
                let ageText = String(message[range])
                context.childAge = ageText
                saveContext()
                break
            }
        }
        
        // Extract ZIP code
        let zipPattern = "\\b(9\\d{4})\\b"  // CA ZIP codes start with 9
        if let regex = try? NSRegularExpression(pattern: zipPattern, options: []),
           let match = regex.firstMatch(in: message, range: NSRange(message.startIndex..., in: message)),
           let range = Range(match.range, in: message) {
            let zip = String(message[range])
            setZipCode(zip)
        }
        
        // Extract insurance types
        let insuranceKeywords = [
            ("medi-cal", "Medi-Cal"),
            ("medicaid", "Medi-Cal"),
            ("regional center", "Regional Center Funded"),
            ("private insurance", "Private Insurance"),
            ("kaiser", "Kaiser"),
            ("blue cross", "Blue Cross"),
            ("blue shield", "Blue Shield"),
            ("united", "United Healthcare"),
            ("anthem", "Anthem")
        ]
        
        for (keyword, insurance) in insuranceKeywords {
            if lowered.contains(keyword) {
                setInsurance(insurance)
                break
            }
        }
    }
    
    /// Get context to inject into LLM prompt
    var llmContextInjection: String? {
        guard context.hasContent else { return nil }
        
        return """
        [User Context: \(context.contextSummary)]
        """
    }
}
