//
//  OnboardingView.swift
//  CHLA-iOS
//
//  First-run flow: welcome, ZIP, the matched-center moment, journey stage
//

import SwiftUI
import MapKit
import CoreLocation

struct OnboardingView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var locationService = LocationService()
    @StateObject private var mapModel = RegionalCenterMapViewModel()

    @State private var currentStep = 0
    @State private var zipCode = ""
    @State private var selectedAgeGroup: String?
    @State private var selectedStage: JourneyStage?
    @State private var selectedAudienceType = "family"
    @State private var userRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo?

    private let totalSteps = 5

    var body: some View {
        VStack(spacing: 0) {
            progressDots
                .padding(.top, 18)

            TabView(selection: $currentStep) {
                welcomeStep.tag(0)
                locationStep.tag(1)
                matchedStep.tag(2)
                journeyStep.tag(3)
                ageGroupStep.tag(4)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(.easeInOut, value: currentStep)

            navigationButtons
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
        }
        .background {
            ZStack(alignment: .top) {
                Theme.canvas

                Theme.topWash(dark: colorScheme == .dark)
                    .frame(height: 360)
                    .frame(maxHeight: .infinity, alignment: .top)
            }
            .ignoresSafeArea()
        }
        .onAppear {
            if mapModel.serviceAreas.isEmpty {
                Task { await mapModel.fetchServiceAreas() }
            }
        }
    }

    // MARK: - Progress

    private var progressDots: some View {
        HStack(spacing: 8) {
            ForEach(0..<totalSteps, id: \.self) { step in
                Capsule()
                    .fill(step == currentStep ? Theme.indigo : Theme.indigo.opacity(0.18))
                    .frame(width: step == currentStep ? 22 : 7, height: 7)
                    .animation(.spring(response: 0.35, dampingFraction: 0.8), value: currentStep)
            }
        }
    }

    // MARK: - Navigation

    private var navigationButtons: some View {
        HStack(spacing: 16) {
            if currentStep > 0 {
                Button {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                        currentStep -= 1
                    }
                } label: {
                    Text("Back")
                        .font(.system(.body, design: .rounded).weight(.medium))
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 13)
                        .background(.ultraThinMaterial, in: Capsule())
                }
                .buttonStyle(.plain)
            }

            Spacer()

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
                    Text(currentStep == totalSteps - 1 ? "Get Started" : "Continue")
                        .font(.system(.body, design: .rounded).weight(.semibold))

                    Image(systemName: "arrow.right")
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 26)
                .padding(.vertical, 14)
                .background {
                    Capsule().fill(canProceed ? AnyShapeStyle(Theme.accentGradient) : AnyShapeStyle(Color.gray.opacity(0.5)))
                }
                .shadow(color: canProceed ? Theme.indigo.opacity(0.35) : .clear, radius: 12, y: 6)
            }
            .buttonStyle(.plain)
            .disabled(!canProceed)
            .animation(.easeOut(duration: 0.2), value: canProceed)
        }
    }

    // MARK: - Step 0: Welcome

    private var welcomeStep: some View {
        VStack(spacing: 22) {
            Spacer()

            Image("KiNDDLogo")
                .resizable()
                .scaledToFit()
                .frame(height: 64)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background {
                    if colorScheme == .dark {
                        RoundedRectangle(cornerRadius: 16, style: .continuous).fill(.white)
                    }
                }

            Text("You found the right place.")
                .font(.system(.largeTitle, design: .rounded).weight(.bold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Text("KiNDD helps LA County families navigate developmental services - regional centers, therapy providers, and what to do next.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            VStack(alignment: .leading, spacing: 10) {
                Text("I'm here as a")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)

                Picker("Audience", selection: $selectedAudienceType) {
                    Label("Parent or family", systemImage: "person.2.fill").tag("family")
                    Label("Clinician", systemImage: "stethoscope").tag("clinician")
                }
                .pickerStyle(.segmented)
            }
            .padding(.horizontal, 36)
            .padding(.top, 8)

            Spacer()
            Spacer()
        }
        .padding()
    }

    // MARK: - Step 1: ZIP

    private var locationStep: some View {
        VStack(spacing: 22) {
            Spacer()

            stepIcon("mappin.and.ellipse", tint: Theme.indigo)

            Text("Where is home?")
                .font(.system(.title, design: .rounded).weight(.bold))

            Text("Every LA County family is assigned a regional center by ZIP code. Yours decides who to call.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            TextField("ZIP code", text: $zipCode)
                .keyboardType(.numberPad)
                .font(.system(.title2, design: .rounded).weight(.semibold))
                .multilineTextAlignment(.center)
                .padding(.vertical, 14)
                .frame(maxWidth: 220)
                .background {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Theme.cardSurface)
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Theme.indigo.opacity(zipCode.count == 5 ? 0.5 : 0.15), lineWidth: 1.5)
                }
                .onChange(of: zipCode) { _, newZip in
                    if newZip.count == 5 {
                        userRegionalCenter = RegionalCenterMatcher.shared.findRegionalCenter(forZipCode: newZip)
                    } else {
                        userRegionalCenter = nil
                    }
                }

            if locationService.shouldRequestPermission {
                Button {
                    locationService.requestPermission()
                } label: {
                    Label("Use my location instead", systemImage: "location.fill")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(Theme.indigo)
                }
                .buttonStyle(.plain)
            } else if locationService.hasLocationPermission && locationService.isLoading {
                ProgressView()
            }

            Spacer()
            Spacer()
        }
        .padding()
        .onChange(of: locationService.currentLocation) { _, location in
            guard let location else { return }
            Task {
                if let zip = try? await locationService.getZipCode(for: location.coordinate) {
                    zipCode = zip
                    userRegionalCenter = RegionalCenterMatcher.shared.findRegionalCenter(forZipCode: zip)
                }
            }
        }
    }

    // MARK: - Step 2: The matched moment

    @ViewBuilder
    private var matchedStep: some View {
        if let rc = userRegionalCenter {
            VStack(spacing: 0) {
                Spacer(minLength: 12)

                ZStack(alignment: .bottom) {
                    matchedMap(for: rc)
                        .frame(height: 380)

                    matchedCard(for: rc)
                        .padding(10)
                }
                .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 26, style: .continuous)
                        .stroke(Color.primary.opacity(0.06), lineWidth: 1)
                }
                .shadow(color: rc.uiColor.opacity(0.18), radius: 20, y: 10)
                .padding(.horizontal, 18)

                Spacer(minLength: 12)
            }
        } else {
            VStack(spacing: 22) {
                Spacer()

                stepIcon("building.2", tint: .secondary)

                Text("We'll figure it out together")
                    .font(.system(.title, design: .rounded).weight(.bold))
                    .multilineTextAlignment(.center)

                Text("We couldn't match that ZIP to a regional center. You can still browse everything, and KiNDD can help you find who serves your family.")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 36)

                Spacer()
                Spacer()
            }
            .padding()
        }
    }

    @ViewBuilder
    private func matchedMap(for rc: RegionalCenterMatcher.RegionalCenterInfo) -> some View {
        if mapModel.serviceAreas.isEmpty {
            ZStack {
                LinearGradient(
                    colors: [rc.uiColor.opacity(0.45), Theme.violet.opacity(0.25)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Image(systemName: "map.fill")
                    .font(.system(size: 44))
                    .foregroundColor(.white.opacity(0.7))
            }
        } else {
            Map(
                initialPosition: .region(MKCoordinateRegion(
                    center: rc.coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.6, longitudeDelta: 0.6)
                )),
                interactionModes: []
            ) {
                ForEach(mapModel.serviceAreas) { feature in
                    ForEach(Array(feature.allMapPolygons.enumerated()), id: \.offset) { _, polygon in
                        let mine = normalizedShortName(feature.shortName) == normalizedShortName(rc.shortName)
                        MapPolygon(polygon)
                            .foregroundStyle(feature.fillColor.opacity(mine ? 0.38 : 0.10))
                            .stroke(feature.strokeColor.opacity(mine ? 1 : 0.45), lineWidth: mine ? 3 : 1)
                    }
                }
            }
            .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
            .allowsHitTesting(false)
        }
    }

    private func matchedCard(for rc: RegionalCenterMatcher.RegionalCenterInfo) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Your Regional Center")
                    .font(.caption.weight(.semibold))
                    .textCase(.uppercase)
                    .kerning(0.8)
                    .foregroundColor(.secondary)

                Spacer()

                HStack(spacing: 4) {
                    Circle().fill(Theme.matched).frame(width: 8, height: 8)
                    Text("Matched")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(Theme.matched)
                }
            }

            Text(rc.name)
                .font(.system(.title3, design: .rounded).weight(.bold))
                .foregroundColor(.primary)

            Text("They coordinate evaluations, services, and funding for your family - and they're expecting calls like yours.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 6) {
                Image(systemName: "phone.fill")
                    .font(.caption)
                Text(rc.phone)
                    .font(.subheadline.weight(.semibold))
            }
            .foregroundColor(rc.uiColor)
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: Theme.cardRadius, style: .continuous)
                .fill(Theme.cardSurface)
            RoundedRectangle(cornerRadius: Theme.cardRadius, style: .continuous)
                .stroke(rc.uiColor.opacity(0.25), lineWidth: 1)
        }
    }

    // MARK: - Step 3: Journey stage

    private var journeyStep: some View {
        VStack(spacing: 22) {
            Spacer(minLength: 8)

            stepIcon("point.topleft.down.curvedto.point.bottomright.up.fill", tint: Theme.violet)

            Text("Where are you in the journey?")
                .font(.system(.title, design: .rounded).weight(.bold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Text("KiNDD uses this to suggest your next step - nothing is locked in.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            VStack(spacing: 10) {
                ForEach(JourneyStage.allCases) { stage in
                    SelectionButton(
                        title: stage.label,
                        icon: stage.icon,
                        isSelected: selectedStage == stage
                    ) {
                        selectedStage = stage
                    }
                }
            }
            .padding(.horizontal, 24)

            Spacer()
        }
        .padding()
    }

    // MARK: - Step 4: Age group

    private var ageGroupStep: some View {
        VStack(spacing: 22) {
            Spacer(minLength: 8)

            stepIcon("figure.and.child.holdinghands", tint: Theme.pink)

            Text("How old is your child?")
                .font(.system(.title, design: .rounded).weight(.bold))

            Text("Optional - it helps us show age-appropriate services first.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            VStack(spacing: 10) {
                ForEach(SearchFilters.ageGroups, id: \.self) { age in
                    SelectionButton(
                        title: ageGroupDisplayName(age),
                        isSelected: selectedAgeGroup == age
                    ) {
                        selectedAgeGroup = selectedAgeGroup == age ? nil : age
                    }
                }
            }
            .padding(.horizontal, 24)

            Spacer()
        }
        .padding()
    }

    // MARK: - Shared bits

    private func stepIcon(_ systemName: String, tint: Color) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(tint.opacity(0.13))
                .frame(width: 64, height: 64)

            Image(systemName: systemName)
                .font(.system(size: 26, weight: .semibold))
                .foregroundColor(tint)
        }
    }

    private func normalizedShortName(_ shortName: String) -> String {
        shortName.replacingOccurrences(of: "/", with: "").uppercased()
    }

    // MARK: - Flow logic

    private var canProceed: Bool {
        switch currentStep {
        case 1:
            return zipCode.count == 5 && zipCode.allSatisfy { $0.isNumber }
        case 3:
            return selectedStage != nil
        default:
            return true
        }
    }

    private func completeOnboarding() {
        appState.searchFilters.ageGroup = selectedAgeGroup
        appState.saveUserContext(
            zipCode: zipCode,
            audienceType: selectedAudienceType,
            regionalCenterName: userRegionalCenter?.name,
            regionalCenterShortName: userRegionalCenter?.shortName
        )
        if let stage = selectedStage {
            appState.saveJourneyStage(stage)
        }

        UIImpactFeedbackGenerator(style: .medium).impactOccurred()

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
}

// MARK: - Selection Button

struct SelectionButton: View {
    let title: String
    var icon: String? = nil
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(isSelected ? Theme.indigo : .secondary)
                        .frame(width: 26)
                }

                Text(title)
                    .font(.system(.body, design: .rounded).weight(.medium))
                    .foregroundColor(.primary)

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(Theme.indigo)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isSelected ? Theme.indigo.opacity(0.10) : Theme.cardSurface)
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(isSelected ? Theme.indigo.opacity(0.6) : Color.clear, lineWidth: 1.5)
            }
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    OnboardingView()
        .environmentObject(AppState())
}
