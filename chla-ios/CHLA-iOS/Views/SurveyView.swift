//
//  SurveyView.swift
//  CHLA-iOS
//
//  Anonymous feedback survey, reachable from Settings and About
//

import SwiftUI

// MARK: - View Model

@MainActor
final class SurveyViewModel: ObservableObject {
    enum Status: Equatable {
        case idle
        case submitting
        case success
        case empty
        case rateLimited
        case failed
    }

    @Published var answers = SurveyAnswers() {
        didSet {
            if status != .submitting && status != .idle && answers != oldValue {
                status = .idle
            }
        }
    }
    @Published private(set) var status: Status = .idle

    private let send: (SurveySubmission) async throws -> Void

    init(send: @escaping (SurveySubmission) async throws -> Void = { try await APIService.shared.submitSurvey($0) }) {
        self.send = send
    }

    func submit() async {
        guard status != .submitting else { return }
        guard !answers.isEmpty else {
            status = .empty
            return
        }

        status = .submitting
        let submission = SurveySubmission(
            answers: answers,
            languageCode: LocalizationHelper.currentLanguageCode,
            appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        )

        do {
            try await send(submission)
            answers = SurveyAnswers()
            status = .success
        } catch APIServiceError.httpError(statusCode: 429) {
            status = .rateLimited
        } catch {
            status = .failed
        }
    }
}

// MARK: - View

struct SurveyView: View {
    @StateObject private var model = SurveyViewModel()
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    private struct RatingSpec {
        let question: SurveyRatingQuestion
        let questionKey: String
        let scale: String
    }

    private let ratingSpecs: [RatingSpec] = [
        RatingSpec(question: .easeOfUse, questionKey: "easeOfUse", scale: "ease"),
        RatingSpec(question: .helpfulness, questionKey: "helpfulness", scale: "helpful"),
        RatingSpec(question: .difficultyBefore, questionKey: "difficultyBefore", scale: "ease"),
        RatingSpec(question: .confidenceAfter, questionKey: "confidenceAfter", scale: "confident"),
        RatingSpec(question: .needsAddressed, questionKey: "needsAddressed", scale: "extent")
    ]

    var body: some View {
        ScrollView {
            if model.status == .success {
                successCard
            } else {
                form
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background {
            ZStack(alignment: .top) {
                Theme.canvas
                Theme.topWash(dark: colorScheme == .dark)
                    .frame(height: 360)
                    .frame(maxHeight: .infinity, alignment: .top)
            }
            .ignoresSafeArea()
        }
        .tint(Theme.indigo)
        .navigationTitle(L10n.Survey.entryLabel)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: model.status) { _, status in
            if let message = statusMessage(for: status) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    // MARK: - Form

    private var form: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 8) {
                Text(L10n.Survey.title)
                    .font(.system(.title2, design: .rounded).weight(.bold))
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)

                Text(L10n.Survey.lead)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.bottom, 4)

            questionCard(L10n.Survey.diagnosis) {
                textField(L10n.Survey.diagnosis, text: $model.answers.diagnosis, lines: 1...3)
            }

            questionCard(L10n.Survey.diagnosisStatus) {
                VStack(spacing: 8) {
                    ForEach(SurveyDiagnosisStatus.allCases) { status in
                        option(
                            title: label(for: status),
                            isSelected: model.answers.diagnosisStatus == status
                        ) {
                            model.answers.toggleDiagnosisStatus(status)
                        }
                    }
                }
            }

            ForEach(ratingSpecs, id: \.question) { spec in
                questionCard(L10n.Survey.question(spec.questionKey)) {
                    VStack(spacing: 8) {
                        ForEach(1...5, id: \.self) { value in
                            option(
                                title: L10n.Survey.scaleOption(spec.scale, value),
                                isSelected: model.answers.ratings[spec.question] == value
                            ) {
                                model.answers.toggleRating(value, for: spec.question)
                            }
                        }
                    }
                }
            }

            questionCard(L10n.Survey.experienceBefore) {
                textField(L10n.Survey.experienceBefore, text: $model.answers.experienceBefore, lines: 3...8)
            }
            questionCard(L10n.Survey.unmetNeeds) {
                textField(L10n.Survey.unmetNeeds, text: $model.answers.unmetNeeds, lines: 3...8)
            }
            questionCard(L10n.Survey.improvements) {
                textField(L10n.Survey.improvements, text: $model.answers.improvements, lines: 3...8)
            }
            questionCard(L10n.Survey.additionalComments) {
                textField(L10n.Survey.additionalComments, text: $model.answers.additionalComments, lines: 3...8)
            }

            submitArea
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 20)
    }

    private func questionCard<Content: View>(_ question: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(question)
                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityHidden(true)

            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Theme.cardSurface)
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color.primary.opacity(0.06), lineWidth: 1)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(question)
    }

    private func textField(_ question: String, text: Binding<String>, lines: ClosedRange<Int>) -> some View {
        TextField("", text: text, axis: .vertical)
            .lineLimit(lines)
            .font(.body)
            .padding(12)
            .background {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Theme.canvas)
            }
            .accessibilityLabel(question)
    }

    private func option(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 20))
                    .foregroundColor(isSelected ? Theme.indigo : .secondary.opacity(0.6))
                    .accessibilityHidden(true)

                Text(title)
                    .font(.system(.body, design: .rounded).weight(.medium))
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(isSelected ? Theme.indigo.opacity(0.10) : Theme.canvas)
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(isSelected ? Theme.indigo.opacity(0.6) : Color.clear, lineWidth: 1.5)
            }
            .contentShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.pressableCard)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityHint(isSelected ? L10n.Survey.clearHint : "")
    }

    private func label(for status: SurveyDiagnosisStatus) -> String {
        switch status {
        case .underEvaluation: return L10n.Survey.underEvaluation
        case .notYetDiagnosed: return L10n.Survey.notYetDiagnosed
        }
    }

    // MARK: - Submit

    private var submitArea: some View {
        VStack(spacing: 12) {
            if let message = statusMessage(for: model.status) {
                Text(message)
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(model.status == .empty ? .orange : .red)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity)
            }

            Button {
                Task { await model.submit() }
            } label: {
                HStack(spacing: 8) {
                    if model.status == .submitting {
                        ProgressView()
                            .tint(.white)
                    }
                    Text(model.status == .submitting ? L10n.Survey.submitting : L10n.Survey.submit)
                        .font(.system(.body, design: .rounded).weight(.semibold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background {
                    Capsule().fill(model.status == .submitting
                                   ? AnyShapeStyle(Color.gray.opacity(0.5))
                                   : AnyShapeStyle(Theme.accentGradient))
                }
            }
            .buttonStyle(.pressable)
            .disabled(model.status == .submitting)
        }
        .padding(.top, 8)
    }

    private func statusMessage(for status: SurveyViewModel.Status) -> String? {
        switch status {
        case .empty: return L10n.Survey.empty
        case .rateLimited: return L10n.Survey.rateLimited
        case .failed: return L10n.Survey.error
        case .success: return L10n.Survey.success
        case .idle, .submitting: return nil
        }
    }

    // MARK: - Success

    private var successCard: some View {
        VStack(spacing: 18) {
            ZStack {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Theme.matched.opacity(0.13))
                    .frame(width: 64, height: 64)
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundColor(Theme.matched)
            }
            .accessibilityHidden(true)

            Text(L10n.Survey.success)
                .font(.system(.title3, design: .rounded).weight(.semibold))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)

            Button {
                dismiss()
            } label: {
                Text(L10n.Survey.done)
                    .font(.system(.body, design: .rounded).weight(.semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 14)
                    .background(Capsule().fill(Theme.accentGradient))
            }
            .buttonStyle(.pressable)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .padding(.top, 40)
    }
}

#Preview {
    NavigationStack {
        SurveyView()
    }
}
