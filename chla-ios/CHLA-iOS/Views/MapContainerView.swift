//
//  MapContainerView.swift
//  NDD Resources
//
//  Main map view with resource pins
//

import SwiftUI
import MapKit

struct MapContainerView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var providerStore = ProviderStore()
    @StateObject private var locationService = LocationService()
    @StateObject private var searchState = SearchStateManager()
    @ObservedObject var visibilityManager = UIVisibilityManager.shared

    @State private var cameraPosition: MapCameraPosition = .automatic
    @State private var selectedProvider: Provider?
    @State private var showFilters = false
    @State private var showResultsSheet = false

    // LA County center as default
    private let defaultRegion = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 34.0522, longitude: -118.2437),
        span: MKCoordinateSpan(latitudeDelta: 0.5, longitudeDelta: 0.5)
    )

    var body: some View {
        ZStack {
            mapView

            // Top search bar with modern design
            VStack(spacing: 0) {
                searchOverlay

                // Search suggestions when active
                if searchState.showSuggestions && searchState.isSearchActive {
                    SearchSuggestionsView(searchState: searchState) { suggestion in
                        // Dismiss keyboard immediately
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)

                        // Close suggestions and search UI
                        searchState.showSuggestions = false
                        searchState.isSearchActive = false

                        // Update search text (for display)
                        searchState.searchText = suggestion
                        searchState.addToRecentSearches(suggestion)

                        // Perform search
                        Task {
                            await performSearchWithQuery(suggestion)
                        }
                    }
                    .frame(maxHeight: 350)
                    .background(.ultraThinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .padding(.horizontal)
                    .shadow(color: .black.opacity(0.2), radius: 20, y: 10)
                    .transition(.move(edge: .top).combined(with: .opacity))
                }

                Spacer()
            }
            .zIndex(10) // Ensure search is above map
            .animation(.spring(response: 0.35, dampingFraction: 0.8), value: searchState.showSuggestions)

            // Right side floating controls (iOS 26 style)
            HStack {
                Spacer()
                VStack(spacing: 0) {
                    Spacer()
                        .frame(height: 140)
                    GlassMapControls(
                        onLocationTap: { centerOnUserLocation() },
                        onFilterTap: { showFilters = true },
                        onRefreshTap: { Task { await performSearch() } },
                        activeFilterCount: activeFilterCount
                    )
                    Spacer()
                }
                .padding(.trailing, 16)
                .offset(x: visibilityManager.isHeaderVisible ? 0 : 80)
                .opacity(visibilityManager.isHeaderVisible ? 1 : 0)
                .animation(.spring(response: 0.4, dampingFraction: 0.8), value: visibilityManager.isHeaderVisible)
            }

            // Loading overlay
            loadingOverlay

            // Bottom info and results preview
            bottomOverlay
        }
        .sheet(isPresented: $showFilters) {
            filterSheet
        }
        .sheet(isPresented: $showResultsSheet) {
            SearchResultsSheet(
                providers: providerStore.sortedProviders,
                isLoading: providerStore.isLoading,
                selectedProvider: $selectedProvider,
                searchText: searchState.searchText
            )
            .presentationDetents([.fraction(0.25), .medium, .large])
            .presentationDragIndicator(.visible)
            .presentationBackgroundInteraction(.enabled)
            .interactiveDismissDisabled(false)
        }
        .sheet(item: $selectedProvider) { provider in
            NavigationStack {
                ProviderDetailView(provider: provider)
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .presentationBackground(.ultraThinMaterial)
            .environmentObject(appState)
        }
        .onAppear {
            setupInitialLocation()
            setupSearchCallback()
        }
        .onChange(of: locationService.currentLocation) { _, location in
            if let location = location {
                Task {
                    await searchNearLocation(location.coordinate)
                }
            }
        }
        .onChange(of: providerStore.providers) { _, providers in
            // Show results sheet when we have results and search is active
            if !providers.isEmpty && (searchState.isSearchActive || !searchState.searchText.isEmpty) {
                showResultsSheet = true
            }
        }
        // Listen for tab menu actions
        .onReceive(NotificationCenter.default.publisher(for: .useMyLocation)) { _ in
            centerOnUserLocation()
        }
        .onReceive(NotificationCenter.default.publisher(for: .showFilters)) { _ in
            showFilters = true
        }
    }

    private func setupSearchCallback() {
        searchState.onSearch = { [self] query, scope in
            Task {
                await performSearchWithQuery(query, scope: scope)
            }
        }
    }

    private func performSearchWithQuery(_ query: String, scope: SearchScope = .all) async {
        // For text searches, use minimal filters to get broad results
        var filters = SearchFilters()
        filters.radiusMiles = 100  // Large radius for text search

        // Only apply therapy filter if a specific scope is selected
        if let therapyType = scope.therapyType {
            filters.therapyTypes = [therapyType]
        }

        let coordinate = locationService.coordinate ?? defaultRegion.center

        print("🔍 Searching for: '\(query)' with radius: \(filters.radiusMiles) miles")

        await providerStore.search(
            query: query,
            location: coordinate,
            filters: filters
        )

        print("✅ Search returned \(providerStore.providers.count) results")
    }

    private func centerOnUserLocation() {
        if let coordinate = locationService.coordinate {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.8)) {
                cameraPosition = .region(MKCoordinateRegion(
                    center: coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1)
                ))
            }
            Task {
                await searchNearLocation(coordinate)
            }
        } else {
            locationService.requestLocation()
        }
    }

    // MARK: - Subviews

    @ViewBuilder
    private var mapView: some View {
        Map(position: $cameraPosition, selection: $selectedProvider) {
            UserAnnotation()

            ForEach(providerStore.providers) { provider in
                Marker(provider.name, coordinate: provider.coordinate)
                    .tint(Color.accentBlue)
                    .tag(provider)
            }
        }
        .mapStyle(.standard(elevation: .realistic))
        .mapControls {
            MapUserLocationButton()
            MapCompass()
            MapScaleView()
        }
        .ignoresSafeArea(edges: .top)
    }

    @ViewBuilder
    private var searchOverlay: some View {
        let shouldShow = visibilityManager.isHeaderVisible || searchState.isSearchActive
        return VStack(spacing: 0) {
            // Safe area spacer for status bar area
            Color.clear
                .frame(height: 50)
                .background(.ultraThinMaterial)

            ModernSearchBar(
                searchState: searchState,
                onFilterTap: { showFilters = true },
                activeFilterCount: activeFilterCount
            )
            .padding(.horizontal, 16)
            .padding(.top, 4)
            .padding(.bottom, 12)
            .background(.ultraThinMaterial)

            // Active filter chips
            if hasActiveFilters && !searchState.isSearchActive {
                ActiveFiltersBar(filters: appState.searchFilters, onClearAll: clearFilters, onRemove: removeFilter)
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .background(.ultraThinMaterial)
            }
        }
        .offset(y: shouldShow ? 0 : -180)
        .opacity(shouldShow ? 1 : 0)
        .animation(.spring(response: 0.4, dampingFraction: 0.8), value: shouldShow)
    }

    @ViewBuilder
    private var loadingOverlay: some View {
        if providerStore.isLoading {
            VStack {
                Spacer()
                VStack(spacing: 12) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Searching...")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(.secondary)
                }
                .padding(24)
                .background {
                    RoundedRectangle(cornerRadius: 20)
                        .fill(.ultraThinMaterial)
                    RoundedRectangle(cornerRadius: 20)
                        .fill(
                            LinearGradient(
                                colors: [.white.opacity(0.2), .clear],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(
                            LinearGradient(
                                colors: [.white.opacity(0.5), .white.opacity(0.1)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.5
                        )
                }
                .shadow(color: .black.opacity(0.15), radius: 20, y: 10)
                Spacer()
            }
        }
    }

    @ViewBuilder
    private var bottomOverlay: some View {
        VStack {
            Spacer()

            HStack {
                // Regional Center badge (if detected)
                if let rc = detectedRegionalCenter {
                    RegionalCenterBadge(name: rc)
                }

                Spacer()

                // Tappable resource count - opens results sheet
                if !providerStore.providers.isEmpty {
                    Button {
                        showResultsSheet = true
                    } label: {
                        ProviderCountBadge(count: providerStore.providerCount)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding()
        }
    }

    @ViewBuilder
    private var filterSheet: some View {
        FilterSheetView(filters: $appState.searchFilters) {
            Task {
                await performSearch()
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Computed Properties

    private var activeFilterCount: Int {
        var count = 0
        if appState.searchFilters.ageGroup != nil { count += 1 }
        if appState.searchFilters.diagnosis != nil { count += 1 }
        if appState.searchFilters.insurance != nil { count += 1 }
        count += appState.searchFilters.therapyTypes.count
        return count
    }

    private var hasActiveFilters: Bool {
        activeFilterCount > 0
    }

    private var detectedRegionalCenter: String? {
        // Try to find RC from user location
        if let coordinate = locationService.coordinate {
            return RegionalCenterMatcher.shared.findRegionalCenter(for: coordinate)?.shortName
        }
        return nil
    }

    // MARK: - Methods

    private func setupInitialLocation() {
        if locationService.hasLocationPermission {
            locationService.requestLocation()
        } else if locationService.shouldRequestPermission {
            locationService.requestPermission()
        } else {
            // Use default LA region
            cameraPosition = .region(defaultRegion)
            Task {
                await providerStore.searchNearby(
                    latitude: defaultRegion.center.latitude,
                    longitude: defaultRegion.center.longitude,
                    radiusMiles: appState.searchFilters.radiusMiles
                )
            }
        }
    }

    private func searchNearLocation(_ coordinate: CLLocationCoordinate2D) async {
        await providerStore.search(
            location: coordinate,
            filters: appState.searchFilters
        )

        // Center map on search location
        withAnimation {
            cameraPosition = .region(MKCoordinateRegion(
                center: coordinate,
                span: MKCoordinateSpan(latitudeDelta: 0.2, longitudeDelta: 0.2)
            ))
        }
    }

    private func performSearch() async {
        if let coordinate = locationService.coordinate {
            await searchNearLocation(coordinate)
        } else {
            await providerStore.search(
                location: defaultRegion.center,
                filters: appState.searchFilters
            )
        }
    }

    private func clearFilters() {
        appState.searchFilters.ageGroup = nil
        appState.searchFilters.diagnosis = nil
        appState.searchFilters.insurance = nil
        appState.searchFilters.therapyTypes = []
        Task {
            await performSearch()
        }
    }

    private func removeFilter(_ type: FilterType) {
        switch type {
        case .ageGroup:
            appState.searchFilters.ageGroup = nil
        case .diagnosis:
            appState.searchFilters.diagnosis = nil
        case .insurance:
            appState.searchFilters.insurance = nil
        case .therapy(let therapy):
            appState.searchFilters.therapyTypes.removeAll { $0 == therapy }
        }
        Task {
            await performSearch()
        }
    }
}

// MARK: - Filter Type Enum

enum FilterType: Hashable {
    case ageGroup
    case diagnosis
    case insurance
    case therapy(String)
}

// MARK: - Search Bar View (Liquid Glass Style)

struct SearchBarView: View {
    let onSearch: () async -> Void
    let onFilterTap: () -> Void
    let activeFilterCount: Int

    @State private var searchText = ""

    var body: some View {
        HStack(spacing: 12) {
            searchField
            filterButton
        }
    }

    @ViewBuilder
    private var searchField: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
                .fontWeight(.medium)

            TextField("Search resources...", text: $searchText)
                .submitLabel(.search)
                .onSubmit {
                    Task { await onSearch() }
                }

            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background {
            ZStack {
                // Glass base
                RoundedRectangle(cornerRadius: 16)
                    .fill(.ultraThinMaterial)
                // Subtle gradient overlay for depth
                RoundedRectangle(cornerRadius: 16)
                    .fill(
                        LinearGradient(
                            colors: [.white.opacity(0.15), .clear],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                // Glass edge highlight
                RoundedRectangle(cornerRadius: 16)
                    .stroke(
                        LinearGradient(
                            colors: [.white.opacity(0.5), .white.opacity(0.1)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 0.5
                    )
            }
        }
        .shadow(color: .black.opacity(0.1), radius: 8, y: 4)
        .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
    }

    @ViewBuilder
    private var filterButton: some View {
        Button(action: onFilterTap) {
            ZStack(alignment: .topTrailing) {
                Image(systemName: "slider.horizontal.3")
                    .font(.title3)
                    .fontWeight(.medium)
                    .foregroundStyle(.primary)
                    .padding(12)
                    .background {
                        ZStack {
                            Circle()
                                .fill(.ultraThinMaterial)
                            Circle()
                                .fill(
                                    LinearGradient(
                                        colors: [.white.opacity(0.2), .clear],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                            Circle()
                                .stroke(
                                    LinearGradient(
                                        colors: [.white.opacity(0.5), .white.opacity(0.1)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ),
                                    lineWidth: 0.5
                                )
                        }
                    }
                    .shadow(color: .black.opacity(0.1), radius: 8, y: 4)

                // Badge for active filters
                if activeFilterCount > 0 {
                    Text("\(activeFilterCount)")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(width: 20, height: 20)
                        .background(
                            Circle()
                                .fill(Color.accentBlue)
                                .shadow(color: Color.accentBlue.opacity(0.5), radius: 4)
                        )
                        .offset(x: 4, y: -4)
                }
            }
        }
    }
}

// MARK: - Active Filters Bar

struct ActiveFiltersBar: View {
    let filters: SearchFilters
    let onClearAll: () -> Void
    let onRemove: (FilterType) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // Clear all button
                Button {
                    onClearAll()
                } label: {
                    Text("Clear all")
                        .font(.caption)
                        .foregroundColor(.accentBlue)
                }
                .padding(.trailing, 4)

                // Age group chip
                if let age = filters.ageGroup {
                    FilterChip(label: age, onRemove: { onRemove(.ageGroup) })
                }

                // Diagnosis chip
                if let diagnosis = filters.diagnosis {
                    FilterChip(label: shortDiagnosis(diagnosis), onRemove: { onRemove(.diagnosis) })
                }

                // Insurance chip
                if let insurance = filters.insurance {
                    FilterChip(label: insurance, onRemove: { onRemove(.insurance) })
                }

                // Therapy chips
                ForEach(filters.therapyTypes, id: \.self) { therapy in
                    FilterChip(label: shortTherapy(therapy), onRemove: { onRemove(.therapy(therapy)) })
                }
            }
            .padding(.vertical, 4)
        }
    }

    private func shortDiagnosis(_ diagnosis: String) -> String {
        let short: [String: String] = [
            "Autism Spectrum Disorder": "Autism",
            "Global Development Delay": "Dev Delay",
            "Intellectual Disability": "Intellectual",
            "Speech and Language Disorder": "Speech"
        ]
        return short[diagnosis] ?? diagnosis
    }

    private func shortTherapy(_ therapy: String) -> String {
        return therapy.replacingOccurrences(of: " therapy", with: "")
            .replacingOccurrences(of: "Parent child interaction therapy/parent training behavior management", with: "Parent Training")
    }
}

// MARK: - Filter Chip (Glass Style)

struct FilterChip: View {
    let label: String
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 6) {
            Text(label)
                .font(.caption)
                .fontWeight(.semibold)

            Button(action: onRemove) {
                Image(systemName: "xmark")
                    .font(.system(size: 9, weight: .bold))
            }
        }
        .foregroundColor(.accentBlue)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background {
            Capsule()
                .fill(.ultraThinMaterial)
            Capsule()
                .fill(Color.accentBlue.opacity(0.1))
            Capsule()
                .stroke(Color.accentBlue.opacity(0.3), lineWidth: 0.5)
        }
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
}

// MARK: - Resource Count Badge (Glass Style)

struct ProviderCountBadge: View {
    let count: Int

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "building.2.fill")
                .font(.caption2)
                .foregroundStyle(.secondary)
        Text("\(count) resource\(count == 1 ? "" : "s")")
            .font(.caption)
            .fontWeight(.semibold)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background {
            Capsule()
                .fill(.ultraThinMaterial)
            Capsule()
                .fill(
                    LinearGradient(
                        colors: [.white.opacity(0.2), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            Capsule()
                .stroke(
                    LinearGradient(
                        colors: [.white.opacity(0.5), .white.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 0.5
                )
        }
        .shadow(color: .black.opacity(0.12), radius: 8, y: 4)
    }
}

// MARK: - Regional Center Badge (Glass Style)

struct RegionalCenterBadge: View {
    let name: String

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "building.2.fill")
                .font(.caption2)

            Text(name)
                .font(.caption)
                .fontWeight(.semibold)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .foregroundColor(.purple)
        .background {
            Capsule()
                .fill(.ultraThinMaterial)
            Capsule()
                .fill(Color.purple.opacity(0.15))
            Capsule()
                .stroke(
                    LinearGradient(
                        colors: [Color.purple.opacity(0.5), Color.purple.opacity(0.2)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 0.5
                )
        }
        .shadow(color: Color.purple.opacity(0.2), radius: 8, y: 4)
    }
}

// MARK: - Glass Map Controls (iOS 26 Style)

struct GlassMapControls: View {
    let onLocationTap: () -> Void
    let onFilterTap: () -> Void
    let onRefreshTap: () -> Void
    var activeFilterCount: Int = 0

    var body: some View {
        VStack(spacing: 0) {
            // Filter button with badge
            GlassControlButton(
                icon: "slider.horizontal.3",
                isActive: activeFilterCount > 0,
                badge: activeFilterCount > 0 ? activeFilterCount : nil,
                action: onFilterTap
            )

            GlassControlDivider()

            // Location button
            GlassControlButton(
                icon: "location.fill",
                action: onLocationTap
            )

            GlassControlDivider()

            // Refresh button
            GlassControlButton(
                icon: "arrow.clockwise",
                action: onRefreshTap
            )
        }
        .background {
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThinMaterial)
            RoundedRectangle(cornerRadius: 16)
                .fill(
                    LinearGradient(
                        colors: [.white.opacity(0.25), .white.opacity(0.05)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            RoundedRectangle(cornerRadius: 16)
                .stroke(
                    LinearGradient(
                        colors: [.white.opacity(0.6), .white.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 0.5
                )
        }
        .shadow(color: .black.opacity(0.15), radius: 12, y: 6)
        .shadow(color: .black.opacity(0.08), radius: 3, y: 1)
    }
}

// MARK: - Glass Control Button
struct GlassControlButton: View {
    let icon: String
    var isActive: Bool = false
    var badge: Int?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack(alignment: .topTrailing) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(isActive ? Color.accentBlue : .primary)
                    .frame(width: 44, height: 44)

                // Badge
                if let badge = badge {
                    Text("\(badge)")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 16, height: 16)
                        .background(
                            Circle()
                                .fill(Color.accentBlue)
                                .shadow(color: Color.accentBlue.opacity(0.5), radius: 4)
                        )
                        .offset(x: 4, y: -2)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Glass Control Divider
struct GlassControlDivider: View {
    var body: some View {
        Rectangle()
            .fill(
                LinearGradient(
                    colors: [.clear, .primary.opacity(0.15), .clear],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .frame(width: 32, height: 0.5)
    }
}

// MARK: - Filter Sheet View

struct FilterSheetView: View {
    @Binding var filters: SearchFilters
    let onApply: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                radiusSection
                ageGroupSection
                diagnosisSection
                insuranceSection
                therapySection
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Reset") {
                        resetFilters()
                    }
                    .foregroundColor(.red)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Apply") {
                        onApply()
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }

    @ViewBuilder
    private var radiusSection: some View {
        Section {
            Picker("Search Radius", selection: $filters.radiusMiles) {
                Text("5 mi").tag(5.0)
                Text("10 mi").tag(10.0)
                Text("15 mi").tag(15.0)
                Text("25 mi").tag(25.0)
                Text("50 mi").tag(50.0)
            }
            .pickerStyle(.segmented)
        } header: {
            Label("Search Radius", systemImage: "location.circle")
        }
    }

    @ViewBuilder
    private var ageGroupSection: some View {
        Section {
            Picker("Age Group", selection: $filters.ageGroup) {
                Text("Any Age").tag(String?.none)
                ForEach(SearchFilters.ageGroups, id: \.self) { age in
                    Text(age).tag(Optional(age))
                }
            }
        } header: {
            Label("Age Group", systemImage: "person.fill")
        }
    }

    @ViewBuilder
    private var diagnosisSection: some View {
        Section {
            Picker("Diagnosis", selection: $filters.diagnosis) {
                Text("Any Diagnosis").tag(String?.none)
                ForEach(SearchFilters.diagnoses, id: \.self) { diagnosis in
                    Text(diagnosis).tag(Optional(diagnosis))
                }
            }
        } header: {
            Label("Diagnosis", systemImage: "heart.fill")
        }
    }

    @ViewBuilder
    private var insuranceSection: some View {
        Section {
            Picker("Insurance", selection: $filters.insurance) {
                Text("Any Insurance").tag(String?.none)
                ForEach(SearchFilters.insuranceOptions, id: \.self) { insurance in
                    Text(insurance).tag(Optional(insurance))
                }
            }
        } header: {
            Label("Insurance", systemImage: "creditcard.fill")
        }
    }

    @ViewBuilder
    private var therapySection: some View {
        Section {
            ForEach(SearchFilters.therapyTypes, id: \.self) { therapy in
                Toggle(isOn: Binding(
                    get: { filters.therapyTypes.contains(therapy) },
                    set: { isOn in
                        if isOn {
                            if !filters.therapyTypes.contains(therapy) {
                                filters.therapyTypes.append(therapy)
                            }
                        } else {
                            filters.therapyTypes.removeAll { $0 == therapy }
                        }
                    }
                )) {
                    Text(therapy)
                        .font(.subheadline)
                }
                .tint(Color.accentBlue)
            }
        } header: {
            Label("Therapy Types", systemImage: "stethoscope")
        }
    }

    private func resetFilters() {
        filters.ageGroup = nil
        filters.diagnosis = nil
        filters.insurance = nil
        filters.therapyTypes = []
        filters.radiusMiles = 15.0
    }
}

#Preview {
    MapContainerView()
        .environmentObject(AppState())
}
