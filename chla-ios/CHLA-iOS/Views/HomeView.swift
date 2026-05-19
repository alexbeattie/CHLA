//
// HomeView.swift
// NDD Resources
//
// Beautiful, clean homepage with clear navigation
//

import SwiftUI
import CoreLocation

struct HomeView: View {
    @EnvironmentObject var appState: AppState
    @ObservedObject var visibilityManager = UIVisibilityManager.shared
    @ObservedObject private var languageManager = LanguageManager.shared
    @StateObject private var locationService = LocationService()
    @StateObject private var regionalCentersMapModel = RegionalCenterMapViewModel()
    @StateObject private var conversationHistory = ConversationHistory()
    @StateObject private var userMemory = UserMemory()

    @State private var showChatSheet = false
    @State private var showResetConfirmation = false
    @State private var showAboutSheet = false
    @State private var showFAQSheet = false
    @State private var userRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo?

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                // Hero Section with gradient background
                heroSection

                // Main Content
                VStack(spacing: 22) {
                    // AI Assistant Card
                    askKiNDDCard

                    // Setup shortcut
                    restartSetupCard

                    // Quick Actions
                    quickActionsSection

                    // Regional Centers Map Preview
                    regionalCentersPreview

                    // Service Categories
                    serviceCategoriesSection

                    // Your Regional Center (if detected)
                    if let center = userRegionalCenter {
                        yourRegionalCenterSection(center)
                    }

                    // Info Footer
                    infoFooter
                }
                .padding(.horizontal, 18)
                .padding(.top, 18)
                .padding(.bottom, 128)
            }
        }
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .onAppear {
            detectUserLocation()
            if regionalCentersMapModel.serviceAreas.isEmpty {
                Task {
                    await regionalCentersMapModel.fetchServiceAreas()
                }
            }
        }
        .overlay(alignment: .bottomTrailing) {
            floatingChatButton
        }
        .sheet(isPresented: $showChatSheet) {
            ChatView()
                .environmentObject(appState)
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

    // MARK: - Hero Section

    private var heroSection: some View {
        ZStack {
            // Gradient background
            LinearGradient(
                colors: [
                    Color(hex: "6366F1").opacity(0.15),
                    Color(hex: "8B5CF6").opacity(0.08),
                    Color(.systemGroupedBackground)
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(spacing: 14) {
                // Top bar with settings
                HStack {
                    Text("Los Angeles County")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(Color(hex: "6366F1"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color.white.opacity(0.65))
                        .clipShape(Capsule())

                    Spacer()

                    Menu {
                        // Language submenu
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
                .padding(.horizontal, 20)
                .padding(.top, 56)

                // Logo and title
                VStack(spacing: 12) {
                    // KiNDD Logo
                    Image("KiNDDLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(height: 72)
                        .shadow(color: Color(hex: "6366F1").opacity(0.2), radius: 12, y: 6)

                    Text("Resource Navigator")
                        .font(.headline)
                        .foregroundColor(.primary)

                    Text("Find services, understand regional center coverage, and get guided help faster.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
                .padding(.bottom, 8)
            }
        }
        .frame(height: 228)
    }

    // MARK: - Ask KiNDD Card

    private var askKiNDDCard: some View {
        Button {
            showChatSheet = true
        } label: {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 50, height: 50)

                        Image(systemName: "message.fill")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundColor(.white)
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Ask KiNDD")
                            .font(.title3.weight(.semibold))
                            .foregroundColor(.primary)

                        Text("Get personalized help finding services, understanding your next steps, and knowing which regional center to contact.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.leading)
                    }

                    Spacer()

                    Image(systemName: "chevron.right.circle.fill")
                        .font(.system(size: 26))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                }

                // Example prompts
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        PromptPill(text: "Find ABA near me")
                        PromptPill(text: "Early intervention")
                        PromptPill(text: "Which regional center serves me?")
                    }
                }
            }
            .padding(18)
            .background {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Color(.systemBackground))
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color(hex: "8B5CF6").opacity(0.12), lineWidth: 1)
            }
            .shadow(color: Color(hex: "8B5CF6").opacity(0.08), radius: 20, y: 10)
        }
        .buttonStyle(.plain)
    }

    private var restartSetupCard: some View {
        Button {
            showResetConfirmation = true
        } label: {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color(hex: "6366F1").opacity(0.12))
                        .frame(width: 48, height: 48)

                    Image(systemName: "person.text.rectangle")
                        .font(.system(size: 21, weight: .semibold))
                        .foregroundColor(Color(hex: "6366F1"))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text("Restart Welcome Setup")
                        .font(.headline)
                        .foregroundColor(.primary)

                    Text("Update ZIP code, care context, and preferences.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Image(systemName: "arrow.clockwise.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(Color(hex: "6366F1"))
            }
            .padding(16)
            .background {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color(.systemBackground))
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color(hex: "6366F1").opacity(0.14), lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }

    // MARK: - Quick Actions

    private var quickActionsSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Quick Actions")
                    .font(.headline)
                    .foregroundColor(.primary)

                Text("Jump straight into the map, your nearby providers, or regional center coverage.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding(.leading, 4)

            HStack(spacing: 12) {
                // Find Providers
                QuickActionTile(
                    icon: "location.fill",
                    title: "Near Me",
                    color: Color(hex: "3B82F6")
                ) {
                    NotificationCenter.default.post(name: .useMyLocation, object: nil)
                    appState.selectedTab = 1
                }

                // Regional Centers
                QuickActionTile(
                    icon: "map.fill",
                    title: "Regions",
                    color: Color(hex: "F59E0B")
                ) {
                    appState.selectedTab = 2
                }

                // Browse All
                QuickActionTile(
                    icon: "list.bullet",
                    title: "Browse",
                    color: Color(hex: "10B981")
                ) {
                    appState.selectedTab = 3
                }

                // Map
                QuickActionTile(
                    icon: "mappin.and.ellipse",
                    title: "Map",
                    color: Color(hex: "6366F1")
                ) {
                    appState.selectedTab = 1
                }
            }
        }
    }

    // MARK: - Service Categories

    private var serviceCategoriesSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Find by Service Type")
                    .font(.headline)
                    .foregroundColor(.primary)

                Text("Browse common care paths parents search for most often.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding(.leading, 4)

            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 10),
                GridItem(.flexible(), spacing: 10)
            ], spacing: 10) {
                ServiceCategoryCard(
                    icon: "brain.head.profile",
                    title: "ABA Therapy",
                    subtitle: "Behavior analysis",
                    color: Color(hex: "6366F1")
                ) {
                    appState.searchFilters.therapyTypes = ["ABA therapy"]
                    appState.selectedTab = 3
                }

                ServiceCategoryCard(
                    icon: "waveform.and.person.filled",
                    title: "Speech",
                    subtitle: "Language therapy",
                    color: Color(hex: "EC4899")
                ) {
                    appState.searchFilters.therapyTypes = ["Speech therapy"]
                    appState.selectedTab = 3
                }

                ServiceCategoryCard(
                    icon: "hand.raised.fill",
                    title: "Occupational",
                    subtitle: "Daily skills",
                    color: Color(hex: "10B981")
                ) {
                    appState.searchFilters.therapyTypes = ["Occupational therapy"]
                    appState.selectedTab = 3
                }

                ServiceCategoryCard(
                    icon: "figure.walk",
                    title: "Physical",
                    subtitle: "Motor skills",
                    color: Color(hex: "F59E0B")
                ) {
                    appState.searchFilters.therapyTypes = ["Physical therapy"]
                    appState.selectedTab = 3
                }
            }
        }
    }

    // MARK: - Regional Centers Preview

    private var regionalCentersPreview: some View {
        RegionalCentersMiniMapCard(
            serviceAreas: regionalCentersMapModel.serviceAreas,
            highlightedCenterShortName: userRegionalCenter?.shortName,
            isLoading: regionalCentersMapModel.isLoading
        ) {
            appState.selectedTab = 2
        }
    }

    // MARK: - Your Regional Center

    private func yourRegionalCenterSection(_ center: RegionalCenterMatcher.RegionalCenterInfo) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Your Regional Center")
                    .font(.headline)
                    .foregroundColor(.primary)

                Spacer()

                HStack(spacing: 4) {
                    Circle()
                        .fill(Color(hex: "10B981"))
                        .frame(width: 8, height: 8)
                    Text("Detected")
                        .font(.caption.weight(.medium))
                        .foregroundColor(Color(hex: "10B981"))
                }
            }
            .padding(.leading, 4)

            Button {
                appState.selectedTab = 2
            } label: {
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(center.uiColor.opacity(0.12))
                            .frame(width: 50, height: 50)

                        Text(center.shortName)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(center.uiColor)
                    }

                    VStack(alignment: .leading, spacing: 3) {
                        Text(center.name)
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.primary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)

                        Text(center.phone)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.tertiary)
                }
                .padding(16)
                .background {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color(.systemBackground))
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(center.uiColor.opacity(0.12), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.03), radius: 10, y: 4)
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Info Footer

    private var infoFooter: some View {
        VStack(spacing: 12) {
            Divider()
                .padding(.vertical, 4)

            // Quick links to About & FAQ
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

    // MARK: - Floating Chat Button

    private var floatingChatButton: some View {
        Button {
            showChatSheet = true
        } label: {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 58, height: 58)
                    .shadow(color: Color(hex: "8B5CF6").opacity(0.4), radius: 12, y: 6)

                Image(systemName: "message.fill")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(.white)
            }
        }
        .padding(.trailing, 20)
        .padding(.bottom, 104)
        .opacity(visibilityManager.isTabBarVisible ? 1 : 0)
        .offset(y: visibilityManager.isTabBarVisible ? 0 : 100)
        .animation(.spring(response: 0.4, dampingFraction: 0.8), value: visibilityManager.isTabBarVisible)
    }

    // MARK: - Helpers

    private func detectUserLocation() {
        if locationService.hasLocationPermission {
            if let location = locationService.currentLocation {
                userRegionalCenter = RegionalCenterMatcher.shared.findRegionalCenter(for: location.coordinate)
            }
        }
    }
}

// MARK: - Prompt Pill

struct PromptPill: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption.weight(.medium))
            .foregroundColor(Color(hex: "6366F1"))
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background {
                Capsule()
                    .fill(Color(hex: "6366F1").opacity(0.1))
            }
    }
}

// MARK: - Quick Action Tile

struct QuickActionTile: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 10) {
                ZStack {
                    Circle()
                        .fill(color.opacity(0.12))
                        .frame(width: 48, height: 48)

                    Image(systemName: icon)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(color)
                }

                Text(title)
                    .font(.caption.weight(.medium))
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.systemBackground))
            }
            .shadow(color: .black.opacity(0.03), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Service Category Card

struct ServiceCategoryCard: View {
    let icon: String
    let title: String
    let subtitle: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(color.opacity(0.12))
                        .frame(width: 48, height: 48)

                    Image(systemName: icon)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(color)
                }

                VStack(spacing: 2) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 110)
            .background {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.systemBackground))
            }
            .shadow(color: .black.opacity(0.03), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Info Badge

struct InfoBadge: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title2.weight(.bold))
                .foregroundColor(Color(hex: "6366F1"))

            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
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
