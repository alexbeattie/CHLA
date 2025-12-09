//
//  OnboardingView.swift
//  CHLA-iOS
//
//  Onboarding flow for new users
//

import SwiftUI
import CoreLocation

struct OnboardingView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var locationService = LocationService()

    @State private var currentStep = 0
    @State private var zipCode = ""
    @State private var selectedAgeGroup: String?
    @State private var selectedDiagnosis: String?
    @State private var selectedTherapies: Set<String> = []
    @State private var selectedInsurance: String?
    @State private var userRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo?

    private let totalSteps = 6 // Added regional center step

    var body: some View {
        VStack(spacing: 0) {
            // Progress indicator
            ProgressView(value: Double(currentStep + 1), total: Double(totalSteps))
                .tint(Color.accentBlue)
                .padding(.horizontal)
                .padding(.top)

            // Step content
            TabView(selection: $currentStep) {
                welcomeStep.tag(0)
                locationStep.tag(1)
                regionalCenterStep.tag(2) // New step
                ageGroupStep.tag(3)
                diagnosisStep.tag(4)
                therapyStep.tag(5)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(.easeInOut, value: currentStep)

            // Navigation buttons (Glass Style)
            HStack(spacing: 16) {
                if currentStep > 0 {
                    Button {
                        withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                            currentStep -= 1
                        }
                    } label: {
                        Text("Back")
                            .font(.body.weight(.medium))
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background {
                                Capsule()
                                    .fill(.ultraThinMaterial)
                                Capsule()
                                    .stroke(Color.primary.opacity(0.1), lineWidth: 0.5)
                            }
                    }
                    .buttonStyle(.plain)
                }

                Spacer()

                // Primary action button (Glass style)
                Button {
                    if currentStep == totalSteps - 1 {
                        completeOnboarding()
                    } else {
                        withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                            currentStep += 1
                        }
                    }
                } label: {
                    HStack(spacing: 8) {
                        Text(currentStep == totalSteps - 1 ? "Get Started" : "Next")
                            .font(.body.weight(.semibold))

                        if currentStep == totalSteps - 1 {
                            Image(systemName: "arrow.right")
                                .font(.body.weight(.semibold))
                        }
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, currentStep == totalSteps - 1 ? 28 : 24)
                    .padding(.vertical, 14)
                    .background {
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: canProceed
                                        ? [Color.accentBlue, Color.accentBlue.opacity(0.8)]
                                        : [.gray, .gray.opacity(0.8)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [.white.opacity(0.25), .clear],
                                    startPoint: .top,
                                    endPoint: .center
                                )
                            )
                        Capsule()
                            .stroke(
                                LinearGradient(
                                    colors: [.white.opacity(0.4), .white.opacity(0.1)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 0.5
                            )
                    }
                    .shadow(color: canProceed ? Color.accentBlue.opacity(0.4) : .clear, radius: 12, y: 6)
                    .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                }
                .buttonStyle(.plain)
                .disabled(!canProceed)
                .scaleEffect(canProceed ? 1 : 0.98)
                .animation(.spring(response: 0.3), value: canProceed)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
        }
        .background(Color.backgroundPrimary)
    }

    // MARK: - Step Views

    private var welcomeStep: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "heart.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(.linearGradient(
                    colors: [.accentBlue, .purple],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ))

            Text("Welcome to NDD Resources")
                .font(.largeTitle)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)

            Text("Find developmental disability resources in Los Angeles County")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Spacer()
            Spacer()
        }
        .padding()
    }

    private var locationStep: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "location.circle.fill")
                .font(.system(size: 60))
                .foregroundColor(.accentBlue)

            Text("Where are you located?")
                .font(.title)
                .fontWeight(.bold)

            Text("Enter your ZIP code to find resources and your Regional Center")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            TextField("ZIP Code", text: $zipCode)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
                .frame(maxWidth: 200)
                .multilineTextAlignment(.center)
                .font(.title2)
                .onChange(of: zipCode) { _, newZip in
                    if newZip.count == 5 {
                        userRegionalCenter = RegionalCenterMatcher.shared.findRegionalCenter(forZipCode: newZip)
                    }
                }

            if locationService.shouldRequestPermission {
                Button {
                    locationService.requestPermission()
                } label: {
                    Label("Use My Location", systemImage: "location.fill")
                }
                .buttonStyle(.bordered)
            } else if locationService.hasLocationPermission {
                if locationService.isLoading {
                    ProgressView()
                } else if locationService.currentLocation != nil {
                    Label("Location detected", systemImage: "checkmark.circle.fill")
                        .foregroundColor(.green)
                }
            }

            Spacer()
            Spacer()
        }
        .padding()
        .onChange(of: locationService.currentLocation) { _, location in
            if let location = location {
                Task {
                    if let zip = try? await locationService.getZipCode(for: location.coordinate) {
                        zipCode = zip
                        userRegionalCenter = RegionalCenterMatcher.shared.findRegionalCenter(forZipCode: zip)
                    }
                }
                // Also try coordinate-based matching
                userRegionalCenter = RegionalCenterMatcher.shared.findRegionalCenter(for: location.coordinate)
            }
        }
    }

    private var regionalCenterStep: some View {
        VStack(spacing: 24) {
            Spacer()

            if let rc = userRegionalCenter {
                // Found regional center
                Image(systemName: "building.2.crop.circle.fill")
                    .font(.system(size: 60))
                    .foregroundStyle(.linearGradient(
                        colors: [rcColor(rc.color), rcColor(rc.color).opacity(0.6)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ))

                Text("Your Regional Center")
                    .font(.title)
                    .fontWeight(.bold)

                // Regional Center Card
                VStack(spacing: 16) {
                    // Badge and name
                    HStack {
                        Text(rc.shortName)
                            .font(.caption.bold())
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(rcColor(rc.color))
                            .cornerRadius(8)

                        Spacer()

                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                            .font(.title3)
                    }

                    Text(rc.name)
                        .font(.headline)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Divider()

                    // Contact info
                    HStack {
                        Label(rc.phone, systemImage: "phone.fill")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)

                        Spacer()
                    }

                    HStack {
                        Label(rc.website, systemImage: "globe")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)

                        Spacer()
                    }
                }
                .padding()
                .background(rcColor(rc.color).opacity(0.08))
                .cornerRadius(16)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(rcColor(rc.color).opacity(0.3), lineWidth: 1)
                )
                .padding(.horizontal, 24)

                Text("Based on ZIP code \(zipCode)")
                    .font(.caption)
                    .foregroundStyle(.secondary)

            } else {
                // No regional center found
                Image(systemName: "building.2.crop.circle")
                    .font(.system(size: 60))
                    .foregroundColor(.secondary)

                Text("Regional Center")
                    .font(.title)
                    .fontWeight(.bold)

                Text("We couldn't determine your Regional Center. You can still browse all resources and centers in the app.")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Spacer()
            Spacer()
        }
        .padding()
    }

    private func rcColor(_ colorName: String) -> Color {
        switch colorName {
        case "orange": return .orange
        case "blue": return .blue
        case "purple": return .purple
        case "green": return .green
        case "teal": return .teal
        case "red": return .red
        case "indigo": return .indigo
        default: return .accentBlue
        }
    }

    private var ageGroupStep: some View {
        VStack(spacing: 24) {
            Text("Age Group")
                .font(.title)
                .fontWeight(.bold)

            Text("Select the age group you're looking for services for")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            VStack(spacing: 12) {
                ForEach(SearchFilters.ageGroups, id: \.self) { age in
                    SelectionButton(
                        title: ageGroupDisplayName(age),
                        isSelected: selectedAgeGroup == age
                    ) {
                        selectedAgeGroup = age
                    }
                }
            }
            .padding(.horizontal)

            Spacer()
        }
        .padding()
    }

    private var diagnosisStep: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Diagnosis")
                    .font(.title)
                    .fontWeight(.bold)

                Text("Select a diagnosis (optional)")
                    .font(.body)
                    .foregroundStyle(.secondary)

                VStack(spacing: 12) {
                    ForEach(SearchFilters.diagnoses, id: \.self) { diagnosis in
                        SelectionButton(
                            title: diagnosis,
                            isSelected: selectedDiagnosis == diagnosis
                        ) {
                            if selectedDiagnosis == diagnosis {
                                selectedDiagnosis = nil
                            } else {
                                selectedDiagnosis = diagnosis
                            }
                        }
                    }
                }
                .padding(.horizontal)

                Button("Skip") {
                    selectedDiagnosis = nil
                    currentStep += 1
                }
                .foregroundColor(.secondary)
            }
            .padding()
        }
    }

    private var therapyStep: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Therapy Types")
                    .font(.title)
                    .fontWeight(.bold)

                Text("Select the types of therapy you're looking for (optional)")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                VStack(spacing: 12) {
                    ForEach(SearchFilters.therapyTypes, id: \.self) { therapy in
                        SelectionButton(
                            title: therapyDisplayName(therapy),
                            isSelected: selectedTherapies.contains(therapy)
                        ) {
                            if selectedTherapies.contains(therapy) {
                                selectedTherapies.remove(therapy)
                            } else {
                                selectedTherapies.insert(therapy)
                            }
                        }
                    }
                }
                .padding(.horizontal)
            }
            .padding()
        }
    }

    // MARK: - Helpers

    private var canProceed: Bool {
        switch currentStep {
        case 0:
            return true
        case 1:
            return zipCode.count == 5 && zipCode.allSatisfy { $0.isNumber }
        default:
            return true
        }
    }

    private func completeOnboarding() {
        // Save filters to app state
        appState.searchFilters.ageGroup = selectedAgeGroup
        appState.searchFilters.diagnosis = selectedDiagnosis
        appState.searchFilters.therapyTypes = Array(selectedTherapies)
        appState.searchFilters.insurance = selectedInsurance

        // Haptic feedback
        let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
        impactFeedback.impactOccurred()

        // Complete onboarding with slight delay for button animation
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
                appState.completeOnboarding()
            }
        }
    }

    private func ageGroupDisplayName(_ age: String) -> String {
        switch age {
        case "0-5": return "0-5 years (Early Intervention)"
        case "6-12": return "6-12 years (School Age)"
        case "13-18": return "13-18 years (Adolescent)"
        case "19+": return "19+ years (Adult)"
        case "All Ages": return "All Ages"
        default: return age
        }
    }

    private func therapyDisplayName(_ therapy: String) -> String {
        switch therapy {
        case "ABA therapy": return "ABA Therapy"
        case "Speech therapy": return "Speech Therapy"
        case "Occupational therapy": return "Occupational Therapy"
        case "Physical therapy": return "Physical Therapy"
        case "Feeding therapy": return "Feeding Therapy"
        default: return therapy
        }
    }
}

// MARK: - Selection Button
struct SelectionButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title)
                    .font(.body)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.accentBlue)
                }
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isSelected ? Color.accentBlue.opacity(0.1) : Color.backgroundSecondary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.accentBlue : Color.clear, lineWidth: 2)
            )
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    OnboardingView()
        .environmentObject(AppState())
}
