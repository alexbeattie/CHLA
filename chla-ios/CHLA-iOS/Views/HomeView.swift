//
// HomeView.swift
// NDD Resources
//
// Home: regional center orientation, guided chat entry, service browsing
//

import SwiftUI
import MapKit

struct HomeView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.openURL) private var openURL
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var languageManager = LanguageManager.shared
    @StateObject private var regionalCentersMapModel = RegionalCenterMapViewModel()

    @State private var showResetConfirmation = false
    @State private var showAboutSheet = false
    @State private var showFAQSheet = false
    @State private var userRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo?
    @State private var zipInput = ""
    @State private var zipLookupFailed = false

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 22) {
                compactHeader

                mapHero

                serviceTypeRow

                nextStepSlot

                questionRowsSection

                infoFooter
            }
            .padding(.horizontal, 18)
            .padding(.top, 56)
            .padding(.bottom, 24)
        }
        .background {
            ZStack(alignment: .top) {
                Color(.systemGroupedBackground)

                LinearGradient(
                    colors: [Color(hex: "6366F1").opacity(colorScheme == .dark ? 0.22 : 0.10), .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 360)
                .frame(maxHeight: .infinity, alignment: .top)
            }
            .ignoresSafeArea()
        }
        .safeAreaInset(edge: .bottom) {
            chatCapsule
                // The parent tab container ignores safe areas, so this measures from
                // the screen edge: pill spans 42-92pt, capsule sits 10pt above it
                .padding(.bottom, 102)
        }
        .onAppear {
            resolveRegionalCenter()
            if regionalCentersMapModel.serviceAreas.isEmpty {
                Task {
                    await regionalCentersMapModel.fetchServiceAreas()
                }
            }
        }
        .sheet(isPresented: $showAboutSheet) {
            NavigationStack {
                AboutView()
                    .environmentObject(appState)
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(L10n.Common.close) {
                                showAboutSheet = false
                            }
                        }
                    }
            }
            .kinddSheet()
        }
        .sheet(isPresented: $showFAQSheet) {
            NavigationStack {
                FAQView()
                    .environmentObject(appState)
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(L10n.Common.close) {
                                showFAQSheet = false
                            }
                        }
                    }
            }
            .kinddSheet()
        }
        .confirmationDialog(
            L10n.Home.changePreferences,
            isPresented: $showResetConfirmation,
            titleVisibility: .visible
        ) {
            Button(L10n.Common.continueText) {
                appState.resetOnboarding()
            }
            Button(L10n.Common.cancel, role: .cancel) {}
        } message: {
            Text(L10n.Home.changePreferencesMessage)
        }
    }

    // MARK: - Chat Capsule

    private var chatCapsule: some View {
        Button {
            appState.openChat()
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "sparkles")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )

                Text("Ask KiNDD anything...")
                    .font(.system(.subheadline, design: .rounded).weight(.medium))
                    .foregroundColor(.secondary)

                Spacer()

                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 34, height: 34)

                    Image(systemName: "arrow.up")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                }
            }
            .padding(.leading, 16)
            .padding(.trailing, 7)
            .frame(height: 48)
            .background {
                Capsule()
                    .fill(.ultraThinMaterial)
                Capsule()
                    .fill(Color(.secondarySystemGroupedBackground).opacity(colorScheme == .dark ? 0.6 : 0.75))
                Capsule()
                    .stroke(Color(hex: "8B5CF6").opacity(0.25), lineWidth: 1)
            }
            .shadow(color: Color(hex: "8B5CF6").opacity(0.18), radius: 12, y: 5)
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 18)
        .accessibilityLabel("Ask KiNDD anything")
    }

    // MARK: - Compact Header

    private var compactHeader: some View {
        HStack(spacing: 12) {
            // Logo asset is dark-on-transparent; needs a light chip until a dark variant exists
            Image("KiNDDLogo")
                .resizable()
                .scaledToFit()
                .frame(height: colorScheme == .dark ? 28 : 36)
                .padding(.horizontal, colorScheme == .dark ? 10 : 0)
                .padding(.vertical, colorScheme == .dark ? 6 : 0)
                .background {
                    if colorScheme == .dark {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(.white)
                    }
                }

            Spacer()

            Text("Los Angeles County")
                .font(.caption.weight(.semibold))
                .foregroundColor(Color(hex: colorScheme == .dark ? "A5B4FC" : "6366F1"))
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(hex: "6366F1").opacity(colorScheme == .dark ? 0.22 : 0.1))
                .clipShape(Capsule())

            Menu {
                Menu {
                    ForEach(AppLanguage.allCases) { language in
                        Button {
                            languageManager.currentLanguage = language
                        } label: {
                            HStack {
                                Text(language.displayName)
                                if languageManager.currentLanguage == language {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    Label("Language", systemImage: "globe")
                }

                Divider()

                Button {
                    showResetConfirmation = true
                } label: {
                    Label("Change Preferences", systemImage: "slider.horizontal.3")
                }

                Button {
                    appState.selectedTab = 4
                } label: {
                    Label("Settings", systemImage: "gear")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.system(size: 22))
                    .foregroundColor(Color(hex: "6366F1"))
            }
        }
    }

    // MARK: - Map Hero

    private var mapHero: some View {
        ZStack(alignment: .bottom) {
            heroMapSurface
                .frame(height: 340)

            Group {
                if let center = userRegionalCenter {
                    knownCenterCard(center)
                } else {
                    zipEntryCard
                }
            }
            .padding(10)
        }
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .stroke(Color.primary.opacity(0.06), lineWidth: 1)
        }
        .shadow(color: Color(hex: "6366F1").opacity(0.10), radius: 18, y: 8)
    }

    @ViewBuilder
    private var heroMapSurface: some View {
        if regionalCentersMapModel.serviceAreas.isEmpty {
            ZStack {
                LinearGradient(
                    colors: [Color(hex: "6366F1").opacity(0.30), Color(hex: "EC4899").opacity(0.18)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                ProgressView()
                    .tint(.white)
            }
        } else {
            Map(initialPosition: .region(heroRegion), interactionModes: []) {
                ForEach(regionalCentersMapModel.serviceAreas) { feature in
                    ForEach(Array(feature.allMapPolygons.enumerated()), id: \.offset) { _, polygon in
                        let highlighted = isHeroHighlighted(feature)
                        MapPolygon(polygon)
                            .foregroundStyle(feature.fillColor.opacity(highlighted ? 0.34 : 0.15))
                            .stroke(feature.strokeColor.opacity(highlighted ? 1 : 0.7), lineWidth: highlighted ? 3 : 1.5)
                    }
                }
            }
            .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
            .allowsHitTesting(false)
            .overlay(alignment: .topTrailing) {
                Button {
                    appState.selectedTab = 2
                } label: {
                    HStack(spacing: 5) {
                        Image(systemName: "map.fill")
                            .font(.caption2)
                        Text("Explore")
                            .font(.caption.weight(.semibold))
                    }
                    .foregroundColor(.primary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(.ultraThinMaterial, in: Capsule())
                }
                .buttonStyle(.plain)
                .padding(10)
            }
        }
    }

    private var heroRegion: MKCoordinateRegion {
        // Frame the urban core with the "Los Angeles" label near the top edge so
        // the colored service areas fill the view; the user's own area is
        // emphasized by the highlighted polygon instead
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 33.87, longitude: -118.26),
            span: MKCoordinateSpan(latitudeDelta: 0.55, longitudeDelta: 0.6)
        )
    }

    private func isHeroHighlighted(_ feature: ServiceAreaFeature) -> Bool {
        guard let short = userRegionalCenter?.shortName else { return false }
        let normalize = { (s: String) in s.replacingOccurrences(of: "/", with: "").uppercased() }
        return normalize(feature.shortName) == normalize(short)
    }

    private func knownCenterCard(_ center: RegionalCenterMatcher.RegionalCenterInfo) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Your Regional Center")
                    .font(.caption.weight(.semibold))
                    .textCase(.uppercase)
                    .kerning(0.8)
                    .foregroundColor(.secondary)

                Spacer()

                HStack(spacing: 4) {
                    Circle()
                        .fill(Color(hex: "10B981"))
                        .frame(width: 8, height: 8)
                    Text("Matched")
                        .font(.caption.weight(.medium))
                        .foregroundColor(Color(hex: "10B981"))
                }
            }

            Text(center.name)
                .font(.system(.title2, design: .rounded).weight(.bold))
                .foregroundColor(.primary)
                .multilineTextAlignment(.leading)

            Text("Regional centers coordinate evaluations, services, and funding for your family.")
                .font(.subheadline)
                .foregroundColor(.secondary)

            HStack(spacing: 10) {
                Button {
                    let digits = center.phone.filter(\.isNumber)
                    if let url = URL(string: "tel://\(digits)") {
                        openURL(url)
                    }
                } label: {
                    Label(center.phone, systemImage: "phone.fill")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(center.uiColor))
                }

                Button {
                    appState.selectedTab = 2
                } label: {
                    Text("Details")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(center.uiColor)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Capsule().stroke(center.uiColor.opacity(0.4), lineWidth: 1))
                }

                Spacer()
            }
        }
        .padding(18)
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground))
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [center.uiColor.opacity(0.08), .clear],
                        startPoint: .topLeading,
                        endPoint: .bottom
                    )
                )
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(center.uiColor.opacity(0.18), lineWidth: 1)
        }
        .shadow(color: center.uiColor.opacity(0.08), radius: 16, y: 8)
    }

    private var zipEntryCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Who serves your family?")
                .font(.system(.title2, design: .rounded).weight(.bold))
                .foregroundColor(.primary)

            Text("Every family in LA County is assigned a regional center by ZIP code. Enter yours to see who to contact.")
                .font(.subheadline)
                .foregroundColor(.secondary)

            HStack(spacing: 10) {
                TextField("ZIP code", text: $zipInput)
                    .keyboardType(.numberPad)
                    .font(.body.monospacedDigit())
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(Color(.tertiarySystemFill))
                    }
                    .onChange(of: zipInput) { _, _ in
                        zipLookupFailed = false
                    }

                Button {
                    lookupZip()
                } label: {
                    Text("Find")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(Color(hex: "6366F1")))
                }
                .disabled(!isValidZip)
                .opacity(isValidZip ? 1 : 0.5)
            }

            if zipLookupFailed {
                Text("That ZIP code doesn't match an LA County regional center. Double-check it, or ask KiNDD for help.")
                    .font(.caption)
                    .foregroundColor(.red)
            }
        }
        .padding(18)
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground))
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: "6366F1").opacity(0.14), lineWidth: 1)
        }
        .shadow(color: Color(hex: "6366F1").opacity(0.06), radius: 16, y: 8)
    }

    // MARK: - Next Step

    private struct NextStep {
        let title: String
        let detail: String
        let chatLabel: String
        let chatPrompt: String
        let showsCall: Bool
    }

    @ViewBuilder
    private var nextStepSlot: some View {
        if let stage = appState.journeyStage, let step = nextStep(for: stage) {
            nextStepCard(step)
        }
    }

    private func nextStep(for stage: JourneyStage) -> NextStep? {
        let centerName = userRegionalCenter?.name ?? "your regional center"

        switch stage {
        case .justDiagnosed:
            return NextStep(
                title: "Request an intake evaluation",
                detail: "One call to \(centerName) starts everything - eligibility, evaluations, and services. No referral needed.",
                chatLabel: "What do I say?",
                chatPrompt: "We just got a diagnosis. What do I say when I call my regional center to request an intake evaluation for my child?",
                showsCall: true
            )
        case .waitingIntake:
            return NextStep(
                title: "Get ready for the intake",
                detail: "Gathering records now - evaluations, medical notes, your own observations - makes the appointment count.",
                chatLabel: "Help me prepare",
                chatPrompt: "How do we prepare for our regional center intake appointment? What documents and information should we bring?",
                showsCall: false
            )
        case .receivingServices:
            return NextStep(
                title: "Get more from your IPP",
                detail: "Your Individual Program Plan is renegotiable. Many families don't know what they can ask for.",
                chatLabel: "What can I ask for?",
                chatPrompt: "My child already receives regional center services. How do I prepare for an IPP meeting, and what services can I ask for?",
                showsCall: false
            )
        case .exploring:
            return nil
        }
    }

    private func nextStepCard(_ step: NextStep) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "arrow.turn.down.right")
                    .font(.caption.weight(.bold))
                Text("Your Next Step")
                    .font(.caption.weight(.semibold))
                    .textCase(.uppercase)
                    .kerning(0.8)
            }
            .foregroundColor(Theme.violet)

            Text(step.title)
                .font(.system(.title3, design: .rounded).weight(.bold))
                .foregroundColor(.primary)

            Text(step.detail)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 10) {
                if step.showsCall, let center = userRegionalCenter {
                    Button {
                        let digits = center.phone.filter(\.isNumber)
                        if let url = URL(string: "tel://\(digits)") {
                            openURL(url)
                        }
                    } label: {
                        Label("Call now", systemImage: "phone.fill")
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(Capsule().fill(Theme.accentGradient))
                    }
                    .buttonStyle(.plain)
                }

                Button {
                    appState.openChat(prompt: step.chatPrompt)
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "sparkles")
                            .font(.caption.weight(.semibold))
                        Text(step.chatLabel)
                            .font(.subheadline.weight(.semibold))
                    }
                    .foregroundColor(Theme.violet)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Capsule().stroke(Theme.violet.opacity(0.4), lineWidth: 1))
                }
                .buttonStyle(.plain)

                Spacer()
            }
            .padding(.top, 2)
        }
        .padding(18)
        .background {
            RoundedRectangle(cornerRadius: Theme.cardRadius, style: .continuous)
                .fill(Theme.cardSurface)
            RoundedRectangle(cornerRadius: Theme.cardRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Theme.violet.opacity(0.07), .clear],
                        startPoint: .topLeading,
                        endPoint: .bottom
                    )
                )
            RoundedRectangle(cornerRadius: Theme.cardRadius, style: .continuous)
                .stroke(Theme.violet.opacity(0.18), lineWidth: 1)
        }
        .shadow(color: Theme.violet.opacity(0.08), radius: 14, y: 6)
    }

    // MARK: - Question Rows

    private var questionRowsSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text("How can we help?")
                    .font(.system(.title3, design: .rounded).weight(.semibold))
                    .foregroundColor(.primary)

                Text("Tap a question, or ask your own anytime below.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding(.leading, 4)

            VStack(spacing: 10) {
                questionRow(
                    icon: "sparkles",
                    tint: Color(hex: "8B5CF6"),
                    text: "We just got a diagnosis. What do we do first?"
                )

                questionRow(
                    icon: "mappin.and.ellipse",
                    tint: Color(hex: "EC4899"),
                    text: "Find ABA therapy near me"
                )

                if let center = userRegionalCenter {
                    questionRow(
                        icon: "building.2",
                        tint: Color(hex: "6366F1"),
                        text: "What services can \(center.shortName) help fund?"
                    )
                } else {
                    questionRow(
                        icon: "building.2",
                        tint: Color(hex: "6366F1"),
                        text: "Which regional center serves my ZIP?"
                    )
                }
            }
        }
    }

    private func questionRow(icon: String, tint: Color, text: String) -> some View {
        Button {
            appState.openChat(prompt: text)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(tint)
                    .frame(width: 36, height: 36)
                    .background {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(tint.opacity(0.12))
                    }

                Text(text)
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.leading)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.tertiary)
            }
            .padding(14)
            .background {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
            }
            .shadow(color: .black.opacity(0.03), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Service Types

    private var serviceTypeRow: some View {
        HStack(spacing: 10) {
            serviceChip(
                icon: "brain.head.profile",
                label: "ABA Therapy",
                color: Color(hex: "6366F1"),
                therapy: "ABA therapy"
            )
            serviceChip(
                icon: "waveform.and.person.filled",
                label: "Speech",
                color: Color(hex: "EC4899"),
                therapy: "Speech therapy"
            )
            serviceChip(
                icon: "hand.raised.fill",
                label: "Occupational",
                color: Color(hex: "8B5CF6"),
                therapy: "Occupational therapy"
            )
            serviceChip(
                icon: "figure.walk",
                label: "Physical",
                color: Color(hex: "A855F7"),
                therapy: "Physical therapy"
            )
        }
    }

    private func serviceChip(icon: String, label: String, color: Color, therapy: String) -> some View {
        Button {
            appState.searchFilters.therapyTypes = [therapy]
            appState.selectedTab = 3
        } label: {
            VStack(spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(color.opacity(0.13))
                        .frame(width: 42, height: 42)

                    Image(systemName: icon)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(color)
                }

                Text(label)
                    .font(.caption2.weight(.medium))
                    .foregroundColor(.primary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
            }
            .shadow(color: .black.opacity(0.03), radius: 6, y: 3)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Info Footer

    private var infoFooter: some View {
        VStack(spacing: 12) {
            Divider()
                .padding(.vertical, 4)

            HStack(spacing: 16) {
                Button {
                    showAboutSheet = true
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "info.circle")
                            .font(.caption)
                        Text(L10n.About.title)
                            .font(.caption.weight(.medium))
                    }
                    .foregroundColor(Color(hex: "6366F1"))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background {
                        Capsule()
                            .stroke(Color(hex: "6366F1").opacity(0.3), lineWidth: 1)
                    }
                }

                Button {
                    showFAQSheet = true
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "questionmark.circle")
                            .font(.caption)
                        Text(L10n.FAQ.title)
                            .font(.caption.weight(.medium))
                    }
                    .foregroundColor(Color(hex: "6366F1"))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background {
                        Capsule()
                            .stroke(Color(hex: "6366F1").opacity(0.3), lineWidth: 1)
                    }
                }
            }
        }
    }

    // MARK: - Helpers

    private var isValidZip: Bool {
        zipInput.count == 5 && zipInput.allSatisfy(\.isNumber)
    }

    private func lookupZip() {
        guard let match = RegionalCenterMatcher.shared.findRegionalCenter(forZipCode: zipInput) else {
            zipLookupFailed = true
            return
        }

        userRegionalCenter = match
        appState.saveUserContext(
            zipCode: zipInput,
            regionalCenterName: match.name,
            regionalCenterShortName: match.shortName
        )
    }

    private func resolveRegionalCenter() {
        // Only the persisted ZIP-derived match is authoritative; coordinate
        // proximity can assign the wrong center, so it is not used here
        if let shortName = appState.userRegionalCenterShortName,
           let match = RegionalCenterMatcher.shared.laRegionalCenters.first(where: { $0.shortName == shortName }) {
            userRegionalCenter = match
        }
    }
}

// MARK: - Notification Names

extension Notification.Name {
    static let searchByZip = Notification.Name("searchByZip")
}

#Preview {
    HomeView()
        .environmentObject(AppState())
}
