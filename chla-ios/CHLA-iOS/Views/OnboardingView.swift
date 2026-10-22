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

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var currentStep = 0
    @State private var zipCode = ""
    @State private var selectedAgeGroups: Set<String> = []
    @State private var hasMultipleChildren = false
    @State private var selectedStage: JourneyStage?
    @State private var selectedDiagnoses: [String] = []
    @State private var selectedAudienceType = "family"
    @State private var userRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo?

    private let totalSteps = 7

    /// Step-change animation; settles to a short ease-out under Reduce Motion
    private var stepAnimation: Animation {
        reduceMotion ? .easeOut(duration: 0.2) : .spring(response: 0.4, dampingFraction: 0.8)
    }

    /// Steps fill the page so their spacers keep the centered layout, and
    /// scroll once the content is taller than the page (small phones, large
    /// type) so text never truncates.
    private func stepContainer<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        let stepContent = content()
        return GeometryReader { proxy in
            ScrollView {
                stepContent
                    .frame(maxWidth: .infinity, minHeight: proxy.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
            .scrollIndicatorsFlash(onAppear: true)
        }
        .clipped()
    }

    var body: some View {
        steps
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

    private var steps: some View {
        VStack(spacing: 0) {
            progressDots
                .padding(.top, 18)

            TabView(selection: $currentStep) {
                welcomeStep.tag(0)
                locationStep.tag(1)
                matchedStep.tag(2)
                howToStep.tag(3)
                journeyStep.tag(4)
                diagnosisStep.tag(5)
                ageGroupStep.tag(6)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(.easeInOut, value: currentStep)

            navigationButtons
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
        }
    }

    // MARK: - Progress

    private var progressDots: some View {
        HStack(spacing: 8) {
            ForEach(0..<totalSteps, id: \.self) { step in
                Capsule()
                    .fill(step == currentStep ? Theme.indigo : Theme.indigo.opacity(0.18))
                    .frame(width: step == currentStep ? 22 : 7, height: 7)
                    .animation(stepAnimation, value: currentStep)
            }
        }
    }

    // MARK: - Navigation

    private var navigationButtons: some View {
        HStack(spacing: 16) {
            if currentStep > 0 {
                Button {
                    withAnimation(stepAnimation) {
                        currentStep -= 1
                    }
                } label: {
                    Text(L10n.Onboarding.back)
                        .font(.system(.body, design: .rounded).weight(.medium))
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 13)
                        .background(.ultraThinMaterial, in: Capsule())
                }
                .buttonStyle(.pressable)
            }

            Spacer()

            Button {
                if currentStep == totalSteps - 1 {
                    completeOnboarding()
                } else {
                    withAnimation(stepAnimation) {
                        currentStep += 1
                    }
                }
            } label: {
                HStack(spacing: 8) {
                    Text(currentStep == totalSteps - 1 ? L10n.Onboarding.getStarted : L10n.Common.continueText)
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
            .buttonStyle(.pressable)
            .disabled(!canProceed)
            .animation(.easeOut(duration: 0.2), value: canProceed)
        }
    }

    // MARK: - Step 0: Welcome

    private var welcomeStep: some View {
        stepContainer {
        VStack(spacing: 18) {
            Spacer()

            Image("KiNDDLogo")
                .resizable()
                .scaledToFit()
                .frame(height: 52)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background {
                    if colorScheme == .dark {
                        RoundedRectangle(cornerRadius: 16, style: .continuous).fill(.white)
                    }
                }

            Text(L10n.Intro.title)
                .font(.system(.title, design: .rounded).weight(.bold))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
                .padding(.horizontal, 24)

            Text(L10n.Intro.lead)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 28)

            VStack(alignment: .leading, spacing: 12) {
                OnboardingInfoRow(
                    icon: "questionmark.circle",
                    title: L10n.Onboarding.howItWorksTitle,
                    detail: L10n.Onboarding.howItWorksBody
                )
                OnboardingInfoRow(
                    icon: "list.bullet.rectangle",
                    title: L10n.Onboarding.servicesTitle,
                    detail: L10n.Onboarding.servicesBody
                )
                OnboardingInfoRow(
                    icon: "person.2",
                    title: L10n.Onboarding.audienceTitle,
                    detail: L10n.Onboarding.audienceBody
                )
            }
            .onboardingCard()
            .padding(.horizontal, 8)

            VStack(alignment: .leading, spacing: 10) {
                Text(L10n.Onboarding.roleLabel)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)

                Picker("Audience", selection: $selectedAudienceType) {
                    Label(L10n.Onboarding.roleFamily, systemImage: "person.2.fill").tag("family")
                    Label(L10n.Onboarding.roleClinician, systemImage: "stethoscope").tag("clinician")
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
    }

    // MARK: - Step 1: ZIP

    private var locationStep: some View {
        stepContainer {
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
                .buttonStyle(.pressable)
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
    }

    // MARK: - Step 2: The matched moment

    @ViewBuilder
    private var matchedStep: some View {
        stepContainer {
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

    // MARK: - Step 3: How to use KiNDD

    private var howToStep: some View {
        stepContainer {
            HowToGuideContent()
        }
    }

    // MARK: - Step 4: Journey stage

    private var journeyStep: some View {
        stepContainer {
        VStack(spacing: 22) {
            Spacer(minLength: 8)

            stepIcon("point.topleft.down.curvedto.point.bottomright.up.fill", tint: Theme.violet)

            Text("Where are you in the journey?")
                .font(.system(.title, design: .rounded).weight(.bold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Text("Optional - KiNDD uses this to suggest a next step. Nothing is locked in.")
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
    }

    // MARK: - Step 5: Diagnoses

    private var diagnosisStep: some View {
        stepContainer {
        VStack(spacing: 22) {
            Spacer(minLength: 8)

            stepIcon("heart.text.square", tint: Theme.purple)

            Text("Any diagnoses you're navigating?")
                .font(.system(.title, design: .rounded).weight(.bold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Text("Optional - pick any that apply. We'll highlight providers who treat them.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            VStack(spacing: 10) {
                ForEach(SearchFilters.diagnoses, id: \.self) { diagnosis in
                    SelectionButton(
                        title: diagnosis,
                        isSelected: selectedDiagnoses.contains(diagnosis)
                    ) {
                        if let index = selectedDiagnoses.firstIndex(of: diagnosis) {
                            selectedDiagnoses.remove(at: index)
                        } else {
                            selectedDiagnoses.append(diagnosis)
                        }
                    }
                }
            }
            .padding(.horizontal, 24)

            Spacer()
        }
        .padding()
        }
    }

    // MARK: - Step 6: Age group

    private var ageGroupStep: some View {
        stepContainer {
        VStack(spacing: 22) {
            Spacer(minLength: 8)

            stepIcon("figure.and.child.holdinghands", tint: Theme.pink)

            Text(hasMultipleChildren || selectedAgeGroups.count > 1
                 ? "How old are your children?"
                 : "How old is your child?")
                .font(.system(.title, design: .rounded).weight(.bold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Text("Optional - pick every age that applies. You can switch between kids later in Filters.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            VStack(spacing: 10) {
                ForEach(SearchFilters.onboardingAgeGroups, id: \.self) { age in
                    SelectionButton(
                        title: ageGroupDisplayName(age),
                        isSelected: selectedAgeGroups.contains(age)
                    ) {
                        toggleAge(age)
                    }
                }

                SelectionButton(
                    title: "I have more than 1 child",
                    icon: "person.2.fill",
                    isSelected: hasMultipleChildren || selectedAgeGroups.count > 1
                ) {
                    hasMultipleChildren.toggle()
                    if !hasMultipleChildren && selectedAgeGroups.count > 1 {
                        if let first = selectedAgeGroups.sorted().first {
                            selectedAgeGroups = [first]
                        }
                    }
                }
            }
            .padding(.horizontal, 24)

            Spacer()
        }
        .padding()
        }
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
        default:
            return true
        }
    }

    private func toggleAge(_ age: String) {
        if selectedAgeGroups.contains(age) {
            selectedAgeGroups.remove(age)
        } else {
            selectedAgeGroups.insert(age)
        }
        if selectedAgeGroups.count > 1 {
            hasMultipleChildren = true
        }
    }

    private func completeOnboarding() {
        let ages = SearchFilters.onboardingAgeGroups.filter { selectedAgeGroups.contains($0) }
        appState.saveChildAges(ages, hasMultiple: hasMultipleChildren)
        // Filter pipeline is single-valued (?diagnosis= on the API); first pick
        // is the primary. All picks land in UserMemory for chat context.
        appState.searchFilters.diagnosis = selectedDiagnoses.first(where: { $0 != "Other" })
        if !selectedDiagnoses.isEmpty {
            let memory = UserMemory()
            for diagnosis in selectedDiagnoses where diagnosis != "Other" {
                memory.addDiagnosis(diagnosis)
            }
        }
        appState.saveUserContext(
            zipCode: zipCode,
            audienceType: selectedAudienceType,
            regionalCenterName: userRegionalCenter?.name,
            regionalCenterShortName: userRegionalCenter?.shortName
        )
        if let stage = selectedStage {
            appState.saveJourneyStage(stage)
        }

        Haptics.action()

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation(stepAnimation) {
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

// MARK: - How-to guide (onboarding step + Settings)

struct HowToGuideContent: View {
    @State private var showIntro = false

    var body: some View {
        VStack(spacing: 18) {
            Spacer(minLength: 8)

            ZStack {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Theme.indigo.opacity(0.13))
                    .frame(width: 64, height: 64)

                Image(systemName: "slider.horizontal.3")
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundColor(Theme.indigo)
            }

            Text(L10n.Onboarding.howToTitle)
                .font(.system(.title, design: .rounded).weight(.bold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Text(L10n.Onboarding.howToLead)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            VStack(alignment: .leading, spacing: 12) {
                OnboardingInfoRow(
                    icon: "slider.horizontal.3",
                    title: L10n.Onboarding.filtersTitle,
                    detail: L10n.Onboarding.filtersBody
                )
                OnboardingInfoRow(
                    icon: "person.2.fill",
                    title: L10n.Onboarding.kidsTitle,
                    detail: L10n.Onboarding.kidsBody
                )
                OnboardingInfoRow(
                    icon: "lock.shield",
                    title: L10n.Onboarding.privacyTitle,
                    detail: L10n.Onboarding.privacyBody
                )
            }
            .onboardingCard()
            .padding(.horizontal, 8)

            goodToKnowCard
                .padding(.horizontal, 8)

            Spacer()
        }
        .padding()
        .tint(Theme.indigoDeep)
        .introSheet(isPresented: $showIntro)
    }

    private var goodToKnowCard: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "info.circle")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Theme.indigo)
                .frame(width: 22)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 8) {
                Text(L10n.Onboarding.goodToKnowTitle)
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .accessibilityAddTraits(.isHeader)

                Text(L10n.Onboarding.goodToKnowBody)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)

                Button {
                    showIntro = true
                } label: {
                    HStack(spacing: 4) {
                        Text(L10n.Onboarding.readFullIntro)
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .accessibilityHidden(true)
                    }
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundColor(Theme.indigoDeep)
                }
                .buttonStyle(.pressable)

                Text(IntroView.linkified(L10n.Onboarding.supportLine))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Theme.indigo.opacity(0.07))
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Theme.indigo.opacity(0.18), lineWidth: 1)
        }
    }
}

/// Icon, bold title, and one short line. The detail links the support
/// address and "Privacy Policy" phrase when present.
struct OnboardingInfoRow: View {
    let icon: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Theme.indigo)
                .frame(width: 22)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                Text(IntroView.linkified(detail))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
    }
}

private extension View {
    func onboardingCard() -> some View {
        padding(16)
            .background {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Theme.cardSurface)
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.primary.opacity(0.06), lineWidth: 1)
            }
    }
}

struct HowToGuideView: View {
    var body: some View {
        ScrollView {
            HowToGuideContent()
        }
        .background(Theme.canvas.ignoresSafeArea())
        .navigationTitle("How to use KiNDD")
        .navigationBarTitleDisplayMode(.inline)
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
        .buttonStyle(.pressableCard)
    }
}

#Preview {
    OnboardingView()
        .environmentObject(AppState())
}
