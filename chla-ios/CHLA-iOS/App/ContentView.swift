//
//  ContentView.swift
//  NDD Resources
//
//  Main content view with tab navigation
//

import SwiftUI

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
    @Namespace private var tabAnimation

    // Sheet states
    @State private var showFilters = false
    @State private var showFAQ = false
    @State private var showSettings = false
    @State private var showShareSheet = false

    private var tabInfo: (title: String, subtitle: String, icon: String) {
        switch appState.selectedTab {
        case 0: return ("Resources", "Find ABA & developmental services", "mappin.and.ellipse")
        case 1: return ("Regional Centers", "LA County service areas", "map.fill")
        case 2: return ("Browse", "Search all resources", "list.bullet")
        case 3: return ("About", "Learn about NDD Resources", "info.circle")
        case 4: return ("More", "Settings & information", "ellipsis.circle")
        default: return ("NDD Resources", "Developmental disability resources", "heart.fill")
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            // Tab Content
            Group {
                switch appState.selectedTab {
                case 0: MapContainerView()
                case 1: RegionalCentersTabView()
                case 2: ProviderListView()
                case 3: AboutView()
                case 4: MoreView()
                default: MapContainerView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // Floating Glass Tab Bar
            LiquidGlassTabBar(
                selectedTab: $appState.selectedTab,
                namespace: tabAnimation,
                onMenuAction: handleMenuAction
            )
            .padding(.horizontal, 20)
            .padding(.bottom, 8)
        }
        .ignoresSafeArea(.keyboard)
        .sheet(isPresented: $showFAQ) {
            NavigationStack {
                FAQView()
            }
        }
        .sheet(isPresented: $showSettings) {
            NavigationStack {
                SettingsView()
            }
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
        case .searchArea:
            appState.selectedTab = 0  // Go to map
        case .showFilters:
            NotificationCenter.default.post(name: .showFilters, object: nil)

        // Regions actions
        case .viewAsList:
            NotificationCenter.default.post(name: .regionsViewList, object: nil)
        case .viewAsMap:
            NotificationCenter.default.post(name: .regionsViewMap, object: nil)
        case .findMyCenter:
            appState.selectedTab = 1  // Go to regions

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
            if let url = URL(string: "https://apps.apple.com/app/id123456789?action=write-review") {
                openURL(url)
            }

        // More actions
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
    case showSettings
    case openWebsite
    case shareApp
}

// MARK: - Liquid Glass Tab Bar (iOS 26 Style)
struct LiquidGlassTabBar: View {
    @Binding var selectedTab: Int
    var namespace: Namespace.ID
    var onMenuAction: (TabMenuAction) -> Void

    struct TabInfo {
        let icon: String
        let label: String
        let menuItems: [(icon: String, title: String, action: TabMenuAction)]
    }

    private let tabs: [TabInfo] = [
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
        TabInfo(icon: "info.circle.fill", label: "About", menuItems: [
            ("questionmark.circle", "FAQ", .showFAQ),
            ("envelope", "Contact Us", .contactUs),
            ("star", "Rate App", .rateApp)
        ]),
        TabInfo(icon: "ellipsis.circle.fill", label: "More", menuItems: [
            ("gear", "Settings", .showSettings),
            ("globe", "Visit Website", .openWebsite),
            ("square.and.arrow.up", "Share App", .shareApp)
        ])
    ]

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<tabs.count, id: \.self) { index in
                GlassTabItem(
                    icon: tabs[index].icon,
                    label: tabs[index].label,
                    isSelected: selectedTab == index,
                    menuItems: tabs[index].menuItems,
                    namespace: namespace,
                    onMenuAction: onMenuAction,
                    onTap: {
                        withAnimation(.spring(response: 0.4, dampingFraction: 0.75)) {
                            selectedTab = index
                        }
                    }
                )
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
}

// MARK: - Glass Tab Item (Expandable with Context Menu)
struct GlassTabItem: View {
    let icon: String
    let label: String
    let isSelected: Bool
    let menuItems: [(icon: String, title: String, action: TabMenuAction)]
    var namespace: Namespace.ID
    var onMenuAction: (TabMenuAction) -> Void
    var onTap: () -> Void

    var body: some View {
        // Tab button content
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 18, weight: isSelected ? .semibold : .regular))
                .foregroundStyle(isSelected ? Color.accentBlue : .secondary)

            if isSelected {
                Text(label)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.accentBlue)
                    .transition(.asymmetric(
                        insertion: .scale(scale: 0.5).combined(with: .opacity),
                        removal: .scale(scale: 0.8).combined(with: .opacity)
                    ))
            }
        }
        .padding(.horizontal, isSelected ? 16 : 12)
        .padding(.vertical, 10)
        .background {
            if isSelected {
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [Color.accentBlue.opacity(0.18), Color.accentBlue.opacity(0.08)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .overlay(
                        Capsule()
                            .stroke(Color.accentBlue.opacity(0.25), lineWidth: 0.5)
                    )
                    .matchedGeometryEffect(id: "selectedTab", in: namespace)
            }
        }
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
    @State private var selectedView: Int = 0  // 0 = List, 1 = Map

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Segmented Picker
                Picker("View", selection: $selectedView) {
                    Label("List", systemImage: "list.bullet").tag(0)
                    Label("Map", systemImage: "map").tag(1)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
                .padding(.vertical, 12)
                .background(Color(.systemBackground))

                Divider()

                // Content
                if selectedView == 0 {
                    RegionalCentersListContent()
                } else {
                    RegionalCenterMapView()
                }
            }
            .navigationTitle("Regional Centers")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Regional Centers List Content
struct RegionalCentersListContent: View {
    @State private var searchText = ""
    @StateObject private var locationManager = RCLocationManager()

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
        let centersToFilter = userRegionalCenter != nil ? otherCenters : centers
        if searchText.isEmpty { return centersToFilter }
        return centersToFilter.filter { center in
            center.name.localizedCaseInsensitiveContains(searchText) ||
            center.shortName.localizedCaseInsensitiveContains(searchText)
        }
    }

    private var showUserCenter: Bool {
        guard let userCenter = userRegionalCenter else { return false }
        if searchText.isEmpty { return true }
        return userCenter.name.localizedCaseInsensitiveContains(searchText) ||
               userCenter.shortName.localizedCaseInsensitiveContains(searchText)
    }

    var body: some View {
        List {
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
                    NavigationLink {
                        RegionalCenterDetailSheet(center: userCenter)
                    } label: {
                        UserRCRow(center: userCenter)
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
                    NavigationLink {
                        RegionalCenterDetailSheet(center: center)
                    } label: {
                        RCListRow(center: center)
                    }
                }
            } header: {
                Text(userRegionalCenter != nil ? "Other Centers" : "All Centers")
            }
        }
        .listStyle(.insetGrouped)
        .searchable(text: $searchText, prompt: "Search centers")
        .onAppear {
            locationManager.requestLocation()
        }
    }
}

// MARK: - More View
struct MoreView: View {
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
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
