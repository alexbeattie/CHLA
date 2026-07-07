//
//  ContentView.swift
//  NDD Resources
//
//  Main content view with tab navigation
//

import SwiftUI
#if canImport(AppKit)
import AppKit
#endif
#if canImport(UIKit)
import UIKit
#endif

// MARK: - UI Visibility Manager
@MainActor
class UIVisibilityManager: ObservableObject {
    static let shared = UIVisibilityManager()

    @Published var isTabBarVisible = true
    @Published var isHeaderVisible = true
    @Published var lastScrollOffset: CGFloat = 0

    private var autoShowTimer: Timer?
    private let autoShowDelay: TimeInterval = 5.0  // Auto-show after 5 seconds

    func handleScroll(offset: CGFloat) {
        let delta = offset - lastScrollOffset
        let threshold: CGFloat = 12

        // Scrolling down significantly - hide UI
        if delta > threshold && offset > 80 {
            hideUI()
        }
        // Scrolling up - show UI
        else if delta < -threshold {
            showUI()
        }

        lastScrollOffset = offset
    }

    func hideUI() {
        withAnimation(.easeOut(duration: 0.25)) {
            isTabBarVisible = false
            isHeaderVisible = false
        }

        // Start auto-show timer
        startAutoShowTimer()
    }

    func showUI() {
        // Cancel any pending auto-show
        cancelAutoShowTimer()

        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            isTabBarVisible = true
            isHeaderVisible = true
        }
    }

    func toggleUI() {
        if isTabBarVisible {
            hideUI()
        } else {
            showUI()
        }
    }

    private func startAutoShowTimer() {
        cancelAutoShowTimer()
        autoShowTimer = Timer.scheduledTimer(withTimeInterval: autoShowDelay, repeats: false) { [weak self] _ in
            Task { @MainActor in
                self?.showUI()
            }
        }
    }

    private func cancelAutoShowTimer() {
        autoShowTimer?.invalidate()
        autoShowTimer = nil
    }
}

// MARK: - Scroll Offset Preference Key
struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

// MARK: - Scrollable Content Wrapper
struct ScrollTrackingView<Content: View>: View {
    @ObservedObject var visibilityManager = UIVisibilityManager.shared
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ScrollView {
            content
                .background(
                    GeometryReader { geo in
                        Color.clear.preference(
                            key: ScrollOffsetPreferenceKey.self,
                            value: -geo.frame(in: .named("scroll")).origin.y
                        )
                    }
                )
        }
        .coordinateSpace(name: "scroll")
        .onPreferenceChange(ScrollOffsetPreferenceKey.self) { offset in
            visibilityManager.handleScroll(offset: offset)
        }
    }
}

struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @State private var showMainView = false

    var body: some View {
        ZStack {
            // Main app view (always rendered but hidden during onboarding)
            MainTabView()
                .opacity(showMainView ? 1 : 0)
                .scaleEffect(showMainView ? 1 : 0.92)
                .blur(radius: showMainView ? 0 : 10)

            // Onboarding overlay
            if appState.isOnboarding {
                OnboardingView()
                    .transition(.asymmetric(
                        insertion: .opacity,
                        removal: .opacity.combined(with: .scale(scale: 1.05)).combined(with: .blur)
                    ))
                    .zIndex(1)
            }
        }
        .onChange(of: appState.isOnboarding) { _, isOnboarding in
            if !isOnboarding {
                // Elegant reveal animation
                withAnimation(.spring(response: 0.7, dampingFraction: 0.8)) {
                    showMainView = true
                }
            }
        }
        .onAppear {
            showMainView = !appState.isOnboarding
        }
    }
}

// MARK: - Blur Transition
extension AnyTransition {
    static var blur: AnyTransition {
        .modifier(
            active: BlurModifier(radius: 20),
            identity: BlurModifier(radius: 0)
        )
    }
}

struct BlurModifier: ViewModifier {
    let radius: CGFloat

    func body(content: Content) -> some View {
        content.blur(radius: radius)
    }
}

// MARK: - Main Tab View (Liquid Glass Style)
struct MainTabView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.openURL) private var openURL
    @ObservedObject var visibilityManager = UIVisibilityManager.shared

    // Sheet states
    @State private var showFilters = false
    @State private var showFAQ = false
    @State private var showSettings = false
    @State private var showShareSheet = false

    private var tabInfo: (title: String, subtitle: String, icon: String) {
        switch appState.selectedTab {
        case 0: return ("Home", "Welcome to NDD Resources", "house.fill")
        case 1: return ("Map", "Find ABA & developmental services", "mappin.and.ellipse")
        case 2: return ("Regions", "LA County service areas", "map.fill")
        case 3: return ("Browse", "Search all resources", "list.bullet")
        case 4: return ("More", "Settings & information", "ellipsis.circle")
        default: return ("NDD Resources", "Developmental disability resources", "heart.fill")
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            // Tab Content - Full Screen
            Group {
                switch appState.selectedTab {
                case 0:
                    HomeView()
                case 1:
                    MapContainerView()
                        .simultaneousGesture(
                            TapGesture()
                                .onEnded { _ in visibilityManager.toggleUI() }
                        )
                case 2:
                    RegionalCentersTabView()
                        .simultaneousGesture(
                            TapGesture()
                                .onEnded { _ in visibilityManager.toggleUI() }
                        )
                case 3: ProviderListView()
                case 4: MoreView()
                default: HomeView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .ignoresSafeArea()
            .onChange(of: appState.selectedTab) { _, newTab in
                // Refresh List when tab is selected
                if newTab == 2 {
                    NotificationCenter.default.post(name: .refreshList, object: nil)
                }
                // Show UI when switching tabs
                visibilityManager.showUI()
            }

            // Floating Glass Tab Bar
            LiquidGlassTabBar(
                selectedTab: $appState.selectedTab,
                onMenuAction: handleMenuAction,
                onChatTap: { appState.openChat() }
            )
            .padding(.horizontal, 20)
            .padding(.bottom, 8)
            .offset(y: visibilityManager.isTabBarVisible ? 0 : 120)
            .opacity(visibilityManager.isTabBarVisible ? 1 : 0)
            .animation(.spring(response: 0.4, dampingFraction: 0.8), value: visibilityManager.isTabBarVisible)
        }
        .ignoresSafeArea(.keyboard)
        .statusBarHidden(!visibilityManager.isHeaderVisible)
        .sheet(isPresented: $appState.showChat, onDismiss: { appState.pendingChatPrompt = nil }) {
            ChatView(initialPrompt: appState.pendingChatPrompt)
                .environmentObject(appState)
                .kinddSheet()
        }
        .sheet(isPresented: $showFAQ) {
            NavigationStack {
                FAQView()
            }
            .kinddSheet()
        }
        .sheet(isPresented: $showSettings) {
            NavigationStack {
                SettingsView()
            }
            .kinddSheet()
        }
        .sheet(isPresented: $showShareSheet) {
            ShareSheet(items: [
                "Check out NDD Resources - Find developmental disability services in LA County!",
                URL(string: "https://kinddhelp.com")!
            ])
        }
    }

    private func handleMenuAction(_ action: TabMenuAction) {
        // Haptic feedback
        let impact = UIImpactFeedbackGenerator(style: .light)
        impact.impactOccurred()

        switch action {
        // Map actions
        case .useMyLocation:
            NotificationCenter.default.post(name: .useMyLocation, object: nil)
            appState.selectedTab = 1  // Go to map
        case .searchArea:
            appState.selectedTab = 1  // Go to map
        case .showFilters:
            NotificationCenter.default.post(name: .showFilters, object: nil)

        // Regions actions
        case .viewAsList:
            NotificationCenter.default.post(name: .regionsViewList, object: nil)
        case .viewAsMap:
            NotificationCenter.default.post(name: .regionsViewMap, object: nil)
        case .findMyCenter:
            appState.selectedTab = 2  // Go to regions

        // List actions
        case .sortList:
            NotificationCenter.default.post(name: .sortList, object: nil)
        case .filterList:
            NotificationCenter.default.post(name: .showFilters, object: nil)
        case .refreshList:
            NotificationCenter.default.post(name: .refreshList, object: nil)

        // About actions
        case .showFAQ:
            showFAQ = true
        case .contactUs:
            if let url = URL(string: "mailto:support@kinddhelp.com") {
                openURL(url)
            }
        case .rateApp:
            if let url = URL(string: "https://apps.apple.com/app/id6756593861?action=write-review") {
                openURL(url)
            }

        // More actions
        case .editOnboardingProfile:
            appState.resetOnboarding()
        case .showSettings:
            showSettings = true
        case .openWebsite:
            if let url = URL(string: "https://kinddhelp.com") {
                openURL(url)
            }
        case .shareApp:
            showShareSheet = true
        }
    }
}

// MARK: - Regional Center Identity
// MARK: - Sheet Presentation

extension View {
    /// Shared treatment for every bottom sheet: visible grabber, soft corner
    /// radius matching the card language, and a consistent canvas color
    func kinddSheet() -> some View {
        self
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(28)
            .presentationBackground(Color(.systemGroupedBackground))
    }
}

// MARK: - Notification Names for Tab Actions
extension Notification.Name {
    static let useMyLocation = Notification.Name("useMyLocation")
    static let showFilters = Notification.Name("showFilters")
    static let regionsViewList = Notification.Name("regionsViewList")
    static let regionsViewMap = Notification.Name("regionsViewMap")
    static let sortList = Notification.Name("sortList")
    static let refreshList = Notification.Name("refreshList")
}

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Tab Menu Actions
enum TabMenuAction {
    case useMyLocation
    case searchArea
    case showFilters
    case viewAsList
    case viewAsMap
    case findMyCenter
    case sortList
    case filterList
    case refreshList
    case showFAQ
    case contactUs
    case rateApp
    case editOnboardingProfile
    case showSettings
    case openWebsite
    case shareApp
}

// MARK: - Liquid Glass Tab Bar (iOS 26 Style)
struct LiquidGlassTabBar: View {
    @Binding var selectedTab: Int
    var onMenuAction: (TabMenuAction) -> Void
    var onChatTap: () -> Void

    struct TabInfo {
        let icon: String
        let label: String
        let menuItems: [(icon: String, title: String, action: TabMenuAction)]
    }

    private let tabs: [TabInfo] = [
        TabInfo(icon: "house.fill", label: "Home", menuItems: [
            ("magnifyingglass", "Search", .searchArea),
            ("location.fill", "Near Me", .useMyLocation),
            ("questionmark.circle", "FAQ", .showFAQ)
        ]),
        TabInfo(icon: "mappin.and.ellipse", label: "Map", menuItems: [
            ("location.fill", "Use My Location", .useMyLocation),
            ("magnifyingglass", "Search Area", .searchArea),
            ("slider.horizontal.3", "Filters", .showFilters)
        ]),
        TabInfo(icon: "map.fill", label: "Regions", menuItems: [
            ("list.bullet", "View as List", .viewAsList),
            ("map", "View as Map", .viewAsMap),
            ("location.circle", "Find My Center", .findMyCenter)
        ]),
        TabInfo(icon: "list.bullet", label: "List", menuItems: [
            ("arrow.up.arrow.down", "Sort", .sortList),
            ("line.3.horizontal.decrease.circle", "Filter", .filterList),
            ("arrow.clockwise", "Refresh", .refreshList)
        ]),
        TabInfo(icon: "ellipsis.circle.fill", label: "More", menuItems: [
            ("person.text.rectangle", "Edit Profile & Onboarding", .editOnboardingProfile),
            ("gear", "Settings", .showSettings),
            ("globe", "Visit Website", .openWebsite),
            ("square.and.arrow.up", "Share App", .shareApp)
        ])
    ]

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<2, id: \.self) { index in
                tabItem(index)
            }

            chatCenterButton

            ForEach(2..<tabs.count, id: \.self) { index in
                tabItem(index)
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 6)
        .background {
            Capsule()
                .fill(.ultraThinMaterial)
            Capsule()
                .fill(
                    LinearGradient(
                        colors: [.white.opacity(0.3), .white.opacity(0.05)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            Capsule()
                .stroke(
                    LinearGradient(
                        colors: [.white.opacity(0.7), .white.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 0.5
                )
        }
        .shadow(color: .black.opacity(0.2), radius: 24, y: 12)
        .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
    }

    private func tabItem(_ index: Int) -> some View {
        GlassTabItem(
            icon: tabs[index].icon,
            label: tabs[index].label,
            isSelected: selectedTab == index,
            menuItems: tabs[index].menuItems,
            onMenuAction: onMenuAction,
            onTap: {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.75)) {
                    selectedTab = index
                }
            }
        )
    }

    private var chatCenterButton: some View {
        Button(action: onChatTap) {
            Image(systemName: "sparkles")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Ask KiNDD")
    }
}

// MARK: - Glass Tab Item (Expandable with Context Menu)
struct GlassTabItem: View {
    let icon: String
    let label: String
    let isSelected: Bool
    let menuItems: [(icon: String, title: String, action: TabMenuAction)]
    var onMenuAction: (TabMenuAction) -> Void
    var onTap: () -> Void

    var body: some View {
        // Tab button content: tint-only selection so six items fit at 375pt
        Image(systemName: icon)
            .font(.system(size: 18, weight: isSelected ? .semibold : .regular))
            .foregroundStyle(isSelected ? Color.accentBlue : .secondary)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .accessibilityLabel(label)
            .contentShape(Capsule())
        .onTapGesture {
            onTap()
        }
        .contextMenu {
            // Long-press context menu
            ForEach(menuItems, id: \.title) { item in
                Button {
                    onMenuAction(item.action)
                } label: {
                    Label(item.title, systemImage: item.icon)
                }
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.7), value: isSelected)
    }
}

// MARK: - Floating Glass Search Button
struct FloatingSearchButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(.primary)
                .frame(width: 52, height: 52)
                .background {
                    Circle()
                        .fill(.ultraThinMaterial)
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [.white.opacity(0.3), .white.opacity(0.05)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [.white.opacity(0.6), .white.opacity(0.15)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.5
                        )
                }
                .shadow(color: .black.opacity(0.15), radius: 12, y: 6)
        }
    }
}

// MARK: - App Header (Liquid Glass Style)
struct AppHeader: View {
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("NDD Resources")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(.secondary)

                    Text(title)
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                }

                Spacer()

                // App icon/badge (glass style)
                ZStack {
                    Circle()
                        .fill(.ultraThinMaterial)
                        .frame(width: 44, height: 44)
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color.accentBlue.opacity(0.2), Color.accentBlue.opacity(0.05)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 44, height: 44)
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [.white.opacity(0.5), Color.accentBlue.opacity(0.2)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.5
                        )
                        .frame(width: 44, height: 44)
                    Image(systemName: "heart.fill")
                        .font(.title3)
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.accentBlue, .accentBlue.opacity(0.7)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                }
                .shadow(color: Color.accentBlue.opacity(0.3), radius: 8, y: 4)
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 12)

            // Subtle subtitle
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 14)
            }

            // Glass divider
            Rectangle()
                .fill(
                    LinearGradient(
                        colors: [.clear, Color.primary.opacity(0.1), .clear],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .frame(height: 1)
        }
        .background {
            Rectangle()
                .fill(Color(.systemBackground))
            // Subtle bottom gradient for depth
            VStack {
                Spacer()
                LinearGradient(
                    colors: [.clear, .black.opacity(0.02)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 20)
            }
        }
    }
}

// MARK: - Regional Centers Tab View
struct RegionalCentersTabView: View {
    @State private var selectedView: Int = 1  // 0 = List, 1 = Map (default to Map)
    @ObservedObject var visibilityManager = UIVisibilityManager.shared

    var body: some View {
        ZStack(alignment: .top) {
            // Content
            if selectedView == 0 {
                // List content - using RegionalCentersView from separate file
                RegionalCentersView()
                    .safeAreaInset(edge: .top, spacing: 0) {
                        // Invisible spacer to push content below picker
                        Color.clear.frame(height: 44)
                    }
            } else {
                // Full screen map
                RegionalCenterMapView()
            }

            // Floating picker overlay - always visible
            VStack(spacing: 0) {
                Picker("View", selection: $selectedView) {
                    Text("List").tag(0)
                    Text("Map").tag(1)
                }
                .pickerStyle(.segmented)
                .frame(width: 160)
                .padding(.vertical, 8)
                .background(
                    Capsule()
                        .fill(.ultraThinMaterial)
                        .shadow(color: .black.opacity(0.1), radius: 8, y: 2)
                )
                .padding(.top, 54)

                Spacer()
            }
        }
        .ignoresSafeArea(edges: .top)
    }
}

// MARK: - Regional Centers List Content
struct RegionalCentersListContent: View {
    @State private var selectedCenter: RegionalCenterMatcher.RegionalCenterInfo?
    @StateObject private var locationManager = RCLocationManager()
    @ObservedObject var visibilityManager = UIVisibilityManager.shared
    @State private var lastScrollOffset: CGFloat = 0

    private let centers = RegionalCenterMatcher.shared.laRegionalCenters

    private var userRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo? {
        guard let location = locationManager.userLocation else { return nil }
        return RegionalCenterMatcher.shared.findRegionalCenter(for: location.coordinate)
    }

    private var otherCenters: [RegionalCenterMatcher.RegionalCenterInfo] {
        guard let userCenter = userRegionalCenter else { return centers }
        return centers.filter { $0.id != userCenter.id }
    }

    var filteredCenters: [RegionalCenterMatcher.RegionalCenterInfo] {
        userRegionalCenter != nil ? otherCenters : centers
    }

    private var showUserCenter: Bool {
        userRegionalCenter != nil
    }

    var body: some View {
        List {
            // Scroll detection
            Color.clear
                .frame(height: 1)
                .background(
                    GeometryReader { geo in
                        Color.clear
                            .preference(key: ScrollOffsetPreferenceKey.self,
                                      value: geo.frame(in: .named("scroll")).minY)
                    }
                )
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)

            // Info Header
            Section {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 12) {
                        Image(systemName: "building.2.crop.circle.fill")
                            .font(.largeTitle)
                            .foregroundStyle(
                                LinearGradient(
                                    colors: [.accentBlue, .purple],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Los Angeles County")
                                .font(.headline)
                            Text("\(centers.count) Regional Centers")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }

                    Text("Tap any center to view contact info, services, and coverage.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 8)
            }

            // User's Regional Center
            if let userCenter = userRegionalCenter, showUserCenter {
                Section {
                    UserRCRow(center: userCenter)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            selectedCenter = userCenter
                        }
                } header: {
                    HStack {
                        Image(systemName: "location.fill")
                            .foregroundColor(.accentBlue)
                        Text("Your Regional Center")
                    }
                } footer: {
                    Text("Based on your current location")
                }
            }

            // All/Other Centers
            Section {
                ForEach(filteredCenters, id: \.id) { center in
                    RCListRow(center: center)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            selectedCenter = center
                        }
                }
            } header: {
                Text(userRegionalCenter != nil ? "Other Centers" : "All Centers")
            }
        }
        .listStyle(.insetGrouped)
        .coordinateSpace(name: "scroll")
        .onPreferenceChange(ScrollOffsetPreferenceKey.self) { offset in
            let delta = offset - lastScrollOffset
            if delta < -20 {
                visibilityManager.hideUI()
            } else if delta > 20 {
                visibilityManager.showUI()
            }
            lastScrollOffset = offset
        }
        .sheet(item: $selectedCenter) { center in
            RegionalCenterDetailSheet(center: center)
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
        .onChange(of: selectedCenter) { _, newValue in
            // Show UI when sheet closes
            if newValue == nil {
                visibilityManager.showUI()
            }
        }
        .onAppear {
            locationManager.requestLocation()
        }
    }
}

// MARK: - More View
struct MoreView: View {
    @EnvironmentObject var appState: AppState
    @ObservedObject var visibilityManager = UIVisibilityManager.shared
    @State private var lastDragValue: CGFloat = 0

    var body: some View {
        NavigationStack {
            List {
                Section {
                    NavigationLink {
                        FAQView()
                    } label: {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("FAQ")
                                Text("Common questions answered")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: "questionmark.circle.fill")
                                .foregroundColor(.orange)
                        }
                    }
                } header: {
                    Text("Help")
                }

                Section {
                    Link(destination: URL(string: "https://kinddhelp.com")!) {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("NDD Resources Website")
                                Text("kinddhelp.com")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: "globe")
                                .foregroundColor(.green)
                        }
                    }
                } header: {
                    Text("Links")
                }

                Section {
                    Button {
                        appState.resetOnboarding()
                    } label: {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Restart Welcome Setup")
                                Text("Update ZIP code, care context, and preferences")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: "person.text.rectangle")
                                .foregroundColor(.indigo)
                        }
                    }
                } header: {
                    Text("Profile")
                }

                Section {
                    NavigationLink {
                        ClinicianResourcesView()
                    } label: {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Clinicians")
                                Text("Referral and Regional Center tools")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: "clipboard.fill")
                                .foregroundColor(.teal)
                        }
                    }

                    NavigationLink {
                        SettingsView()
                    } label: {
                        Label("Settings", systemImage: "gear")
                    }
                }

                Section {
                    HStack {
                        Label("Version", systemImage: "app.badge")
                        Spacer()
                        Text("1.0.0")
                            .foregroundStyle(.secondary)
                    }

                    HStack {
                        Label("Resources", systemImage: "building.2")
                        Spacer()
                        Text("370+")
                            .foregroundStyle(.secondary)
                    }
                } header: {
                    Text("App Info")
                } footer: {
                    Text("NDD Resources\n© 2025 All rights reserved.")
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 8)
                }
            }
            .navigationTitle("More")
            .simultaneousGesture(
                DragGesture()
                    .onChanged { value in
                        let delta = value.translation.height - lastDragValue
                        if delta < -10 {
                            visibilityManager.hideUI()
                        } else if delta > 10 {
                            visibilityManager.showUI()
                        }
                        lastDragValue = value.translation.height
                    }
                    .onEnded { _ in
                        lastDragValue = 0
                    }
            )
        }
    }
}

// MARK: - Clinician Resources
struct ClinicianResourcesView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) private var dismiss
    @State private var zipCode = ""
    @State private var matchedCenter: RegionalCenterMatcher.RegionalCenterInfo?
    @State private var selectedPreset = ClinicianServicePreset.aba

    var body: some View {
        List {
            heroSection

            regionalCenterLookupSection

            servicePresetSection

            handoffSummarySection

            referralFlowSection
        }
        .navigationTitle("Clinicians")
        .onAppear {
            zipCode = appState.userZipCode ?? ""
            updateMatchedCenter()
        }
    }

    private var heroSection: some View {
        Section {
            VStack(alignment: .leading, spacing: 18) {
                HStack(alignment: .top, spacing: 14) {
                    Image(systemName: "stethoscope.circle.fill")
                        .font(.system(size: 44))
                        .foregroundStyle(Color.accentBlue)

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Referral Prep")
                            .font(.largeTitle.weight(.bold))
                        Text("Build a family-ready handoff with Regional Center context, service filters, and next steps.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }

                HStack(spacing: 12) {
                    ClinicianStatusPill(
                        title: zipCode.count == 5 ? zipCode : "ZIP needed",
                        icon: "mappin.and.ellipse",
                        color: .blue
                    )
                    ClinicianStatusPill(
                        title: matchedCenter?.shortName ?? "No RC yet",
                        icon: "building.2.fill",
                        color: matchedCenter?.uiColor ?? .secondary
                    )
                }

                Button {
                    saveClinicianContext()
                    applyPresetAndNavigate(selectedPreset, destination: .browse)
                } label: {
                    Label("Start Referral Search", systemImage: "magnifyingglass.circle.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(matchedCenter == nil)
            }
            .padding(.vertical, 10)
        }
    }

    private var regionalCenterLookupSection: some View {
        Section {
            TextField("Family ZIP code", text: $zipCode)
                .keyboardType(.numberPad)
                .textContentType(.postalCode)
                .onChange(of: zipCode) { _, newValue in
                    zipCode = String(newValue.filter(\.isNumber).prefix(5))
                    updateMatchedCenter()
                }

            if let matchedCenter {
                regionalCenterCard(matchedCenter)

                Button {
                    saveClinicianContext()
                } label: {
                    Label("Use This Clinician Context", systemImage: "checkmark.circle.fill")
                }

                HStack {
                    if let phoneURL = phoneURL(for: matchedCenter.phone) {
                        Link(destination: phoneURL) {
                            Label("Call", systemImage: "phone.fill")
                        }
                    }

                    Spacer()

                    if let websiteURL = websiteURL(for: matchedCenter.website) {
                        Link(destination: websiteURL) {
                            Label("Website", systemImage: "safari.fill")
                        }
                    }

                    Spacer()

                    Button {
                        appState.navigateToRegions()
                        dismiss()
                    } label: {
                        Label("Regions", systemImage: "map.fill")
                    }
                }
                .font(.subheadline.weight(.semibold))
            } else if zipCode.count == 5 {
                ContentUnavailableView(
                    "No Regional Center Match",
                    systemImage: "exclamationmark.magnifyingglass",
                    description: Text("Try confirming the ZIP code or use the full Regional Centers list.")
                )
            }
        } header: {
            Text("Family Location")
        } footer: {
            Text("ZIP matching is a starting point for triage. Confirm eligibility and service area details with the Regional Center before handoff.")
        }
    }

    private var servicePresetSection: some View {
        Section {
            ForEach(ClinicianServicePreset.allCases) { preset in
                Button {
                    selectedPreset = preset
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: preset.icon)
                            .font(.headline)
                            .foregroundStyle(preset.color)
                            .frame(width: 34, height: 34)
                            .background(preset.color.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))

                        VStack(alignment: .leading, spacing: 3) {
                            Text(preset.title)
                                .font(.headline)
                                .foregroundStyle(.primary)
                            Text(preset.subtitle)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }

                        Spacer()

                        Image(systemName: selectedPreset == preset ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(selectedPreset == preset ? Color.accentBlue : Color.secondary.opacity(0.45))
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }

            HStack {
                Button {
                    saveClinicianContext()
                    applyPresetAndNavigate(selectedPreset, destination: .browse)
                } label: {
                    Label("Browse Matches", systemImage: "list.bullet.rectangle")
                }
                .buttonStyle(.borderedProminent)

                Button {
                    saveClinicianContext()
                    applyPresetAndNavigate(selectedPreset, destination: .map)
                } label: {
                    Label("Map", systemImage: "map")
                }
                .buttonStyle(.bordered)
            }
        } header: {
            Text("Service Starters")
        } footer: {
            Text("Selecting a starter updates the app filters so Browse and Map open with a referral-focused search.")
        }
    }

    private var handoffSummarySection: some View {
        Section {
            VStack(alignment: .leading, spacing: 12) {
                Text(handoffSummary)
                    .font(.callout.monospaced())
                    .textSelection(.enabled)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))

                HStack {
                    ShareLink(item: handoffSummary) {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                    .buttonStyle(.borderedProminent)

                    Button {
                        copyHandoffSummary()
                    } label: {
                        Label("Copy", systemImage: "doc.on.doc")
                    }
                    .buttonStyle(.bordered)
                }
            }
        } header: {
            Text("Family Handoff")
        }
    }

    private var referralFlowSection: some View {
        Section("Referral Flow") {
            ClinicianStepCard(
                icon: "person.text.rectangle.fill",
                title: "1. Intake",
                detail: "Confirm family ZIP, child age, diagnosis, payer, and current services before searching."
            )

            ClinicianStepCard(
                icon: "slider.horizontal.3",
                title: "2. Match",
                detail: "Use the selected service starter to open a filtered provider list, then narrow by distance and funding source."
            )

            ClinicianStepCard(
                icon: "paperplane.fill",
                title: "3. Handoff",
                detail: "Share the Regional Center contact and suggested next steps so the family leaves with a clear action plan."
            )
        }
    }

    private func regionalCenterCard(_ center: RegionalCenterMatcher.RegionalCenterInfo) -> some View {
        HStack(spacing: 12) {
            Text(center.shortName)
                .font(.caption.weight(.black))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(center.uiColor, in: Circle())
                .overlay(Circle().stroke(.white, lineWidth: 3))
                .shadow(color: center.uiColor.opacity(0.25), radius: 8, y: 4)

            VStack(alignment: .leading, spacing: 4) {
                Text(center.name)
                    .font(.headline)
                Text(center.phone)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(center.website)
                    .font(.caption)
                    .foregroundStyle(center.uiColor)
            }
        }
        .padding(.vertical, 6)
    }

    private var handoffSummary: String {
        var lines = [
            "Clinician referral prep",
            "Family ZIP: \(zipCode.isEmpty ? "Not entered" : zipCode)",
            "Service focus: \(selectedPreset.title)"
        ]

        if let matchedCenter {
            lines.append("Regional Center: \(matchedCenter.name) (\(matchedCenter.shortName))")
            lines.append("Phone: \(matchedCenter.phone)")
            lines.append("Website: \(matchedCenter.website)")
        } else {
            lines.append("Regional Center: Not matched yet")
        }

        lines.append("Next steps: confirm eligibility, review funding source, and share provider options.")
        return lines.joined(separator: "\n")
    }

    private func copyHandoffSummary() {
        #if canImport(UIKit)
        UIPasteboard.general.string = handoffSummary
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        #elseif canImport(AppKit)
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(handoffSummary, forType: .string)
        #endif
    }

    private func saveClinicianContext() {
        appState.saveUserContext(
            zipCode: zipCode.count == 5 ? zipCode : nil,
            audienceType: "clinician",
            regionalCenterName: matchedCenter?.name,
            regionalCenterShortName: matchedCenter?.shortName
        )
    }

    private func applyPresetAndNavigate(_ preset: ClinicianServicePreset, destination: ClinicianDestination) {
        appState.searchFilters.therapyTypes = preset.therapyTypes
        appState.searchFilters.insurance = preset.insurance
        appState.searchFilters.radiusMiles = 15.0

        switch destination {
        case .browse:
            appState.navigateToBrowse()
        case .map:
            appState.navigateToMap()
        }

        dismiss()
    }

    private func updateMatchedCenter() {
        guard zipCode.count == 5 else {
            matchedCenter = nil
            return
        }

        matchedCenter = RegionalCenterMatcher.shared.findRegionalCenter(forZipCode: zipCode)
    }

    private func phoneURL(for phone: String) -> URL? {
        let digits = phone.filter(\.isNumber)
        guard !digits.isEmpty else { return nil }
        return URL(string: "tel://\(digits)")
    }

    private func websiteURL(for website: String) -> URL? {
        if website.hasPrefix("http") {
            return URL(string: website)
        }
        return URL(string: "https://\(website)")
    }
}

private enum ClinicianDestination {
    case browse
    case map
}

private enum ClinicianServicePreset: String, CaseIterable, Identifiable {
    case aba
    case speech
    case occupational
    case physical
    case earlyIntervention

    var id: String { rawValue }

    var title: String {
        switch self {
        case .aba:
            return "ABA Therapy"
        case .speech:
            return "Speech Therapy"
        case .occupational:
            return "Occupational Therapy"
        case .physical:
            return "Physical Therapy"
        case .earlyIntervention:
            return "Early Intervention"
        }
    }

    var subtitle: String {
        switch self {
        case .aba:
            return "Behavior support and parent training"
        case .speech:
            return "Communication and language services"
        case .occupational:
            return "Sensory, feeding, and daily living skills"
        case .physical:
            return "Motor development and mobility support"
        case .earlyIntervention:
            return "Regional Center funded starting point"
        }
    }

    var icon: String {
        switch self {
        case .aba:
            return "brain.head.profile"
        case .speech:
            return "waveform.and.person.filled"
        case .occupational:
            return "hand.raised.fill"
        case .physical:
            return "figure.walk"
        case .earlyIntervention:
            return "figure.and.child.holdinghands"
        }
    }

    var color: Color {
        switch self {
        case .aba:
            return Color(hex: "6366F1")
        case .speech:
            return Color(hex: "EC4899")
        case .occupational:
            return Color(hex: "10B981")
        case .physical:
            return Color(hex: "F59E0B")
        case .earlyIntervention:
            return Color(hex: "14B8A6")
        }
    }

    var therapyTypes: [String] {
        switch self {
        case .aba:
            return ["ABA therapy"]
        case .speech:
            return ["Speech therapy"]
        case .occupational:
            return ["Occupational therapy"]
        case .physical:
            return ["Physical therapy"]
        case .earlyIntervention:
            return []
        }
    }

    var insurance: String? {
        switch self {
        case .earlyIntervention:
            return "Regional Center"
        default:
            return nil
        }
    }
}

private struct ClinicianStatusPill: View {
    let title: String
    let icon: String
    let color: Color

    var body: some View {
        Label(title, systemImage: icon)
            .font(.caption.weight(.semibold))
            .foregroundStyle(color)
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(color.opacity(0.12), in: Capsule())
    }
}

private struct ClinicianStepCard: View {
    let icon: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.headline)
                .foregroundStyle(Color.accentBlue)
                .frame(width: 34, height: 34)
                .background(Color.accentBlue.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
