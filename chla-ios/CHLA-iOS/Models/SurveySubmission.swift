//
//  SurveySubmission.swift
//  CHLA-iOS
//
//  Feedback survey answers and the POST /api/surveys/responses/ body
//

import Foundation

enum SurveyDiagnosisStatus: String, CaseIterable, Identifiable, Sendable {
    case underEvaluation = "under_evaluation"
    case notYetDiagnosed = "not_yet_diagnosed"

    var id: String { rawValue }
}

/// The five 1-5 rating questions, in display order. Raw values are the API keys.
enum SurveyRatingQuestion: String, CaseIterable, Identifiable, Sendable {
    case easeOfUse = "ease_of_use"
    case helpfulness = "helpfulness"
    case difficultyBefore = "difficulty_before"
    case confidenceAfter = "confidence_after"
    case needsAddressed = "needs_addressed"

    var id: String { rawValue }
}

/// What the user has entered on the survey screen. Every field is optional.
struct SurveyAnswers: Equatable, Sendable {
    var diagnosis = ""
    var diagnosisStatus: SurveyDiagnosisStatus?
    var ratings: [SurveyRatingQuestion: Int] = [:]
    var experienceBefore = ""
    var unmetNeeds = ""
    var improvements = ""
    var additionalComments = ""

    /// The server rejects a submission with nothing answered, so the client
    /// checks first and never sends one.
    var isEmpty: Bool {
        let texts = [diagnosis, experienceBefore, unmetNeeds, improvements, additionalComments]
        return diagnosisStatus == nil
            && ratings.isEmpty
            && texts.allSatisfy { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    }

    mutating func toggleRating(_ value: Int, for question: SurveyRatingQuestion) {
        ratings[question] = ratings[question] == value ? nil : value
    }

    mutating func toggleDiagnosisStatus(_ status: SurveyDiagnosisStatus) {
        diagnosisStatus = diagnosisStatus == status ? nil : status
    }
}

/// Request body. Only keys the server knows are encoded (unknown keys are a 400),
/// and no ZIP, location, profile, or device identifiers are ever included.
/// Unanswered ratings and a missing app version are omitted from the JSON.
struct SurveySubmission: Encodable, Equatable, Sendable {
    static let diagnosisMaxLength = 1000
    static let longTextMaxLength = 4000

    let diagnosis: String
    let diagnosisStatus: String
    let easeOfUse: Int?
    let helpfulness: Int?
    let difficultyBefore: Int?
    let confidenceAfter: Int?
    let needsAddressed: Int?
    let experienceBefore: String
    let unmetNeeds: String
    let improvements: String
    let additionalComments: String
    let locale: String
    let platform: String
    let appVersion: String?

    init(answers: SurveyAnswers, languageCode: String, appVersion: String?) {
        func clean(_ text: String, max: Int) -> String {
            String(text.trimmingCharacters(in: .whitespacesAndNewlines).prefix(max))
        }
        func rating(_ question: SurveyRatingQuestion) -> Int? {
            answers.ratings[question].flatMap { (1...5).contains($0) ? $0 : nil }
        }

        diagnosis = clean(answers.diagnosis, max: Self.diagnosisMaxLength)
        diagnosisStatus = answers.diagnosisStatus?.rawValue ?? ""
        easeOfUse = rating(.easeOfUse)
        helpfulness = rating(.helpfulness)
        difficultyBefore = rating(.difficultyBefore)
        confidenceAfter = rating(.confidenceAfter)
        needsAddressed = rating(.needsAddressed)
        experienceBefore = clean(answers.experienceBefore, max: Self.longTextMaxLength)
        unmetNeeds = clean(answers.unmetNeeds, max: Self.longTextMaxLength)
        improvements = clean(answers.improvements, max: Self.longTextMaxLength)
        additionalComments = clean(answers.additionalComments, max: Self.longTextMaxLength)
        locale = languageCode == "es" ? "es" : "en"
        platform = "ios"
        self.appVersion = appVersion
    }

    enum CodingKeys: String, CodingKey {
        case diagnosis
        case diagnosisStatus = "diagnosis_status"
        case easeOfUse = "ease_of_use"
        case helpfulness
        case difficultyBefore = "difficulty_before"
        case confidenceAfter = "confidence_after"
        case needsAddressed = "needs_addressed"
        case experienceBefore = "experience_before"
        case unmetNeeds = "unmet_needs"
        case improvements
        case additionalComments = "additional_comments"
        case locale
        case platform
        case appVersion = "app_version"
    }
}
