//
//  ProviderListView.swift
//  NDD Resources
//
//  List view of resources matching web app's ProviderList
//

import SwiftUI
import CoreLocation

struct ProviderListView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var providerStore = ProviderStore()
    @StateObject private var locationService = LocationService()

    @State private var searchText = ""
    @State private var showFilters = false
    @State private var sortOption: SortOption = .distance
    @State private var searchScope: SearchScope = .all
    @State private var searchSuggestions: [String] = []

    enum SortOption: String, CaseIterable {
        case distance = "Distance"
        case name = "Name"
    }

    var filteredProviders: [Provider] {
        var providers = providerStore.sortedProviders

        // Filter by search text
        if !searchText.isEmpty {
            providers = providers.filter { provider in
                provider.name.localizedCaseInsensitiveContains(searchText) ||
                (provider.type?.localizedCaseInsensitiveContains(searchText) ?? false) ||
                provider.address.localizedCaseInsensitiveContains(searchText) ||
                (provider.therapyTypes?.contains { $0.localizedCaseInsensitiveContains(searchText) } ?? false)
            }
        }

        // Filter by search scope (therapy type)
        if let therapyType = searchScope.therapyType {
            providers = providers.filter { provider in
                provider.therapyTypes?.contains { $0.localizedCaseInsensitiveContains(therapyType) } ?? false
            }
        }

        // Sort
        switch sortOption {
        case .distance:
            providers.sort { ($0.distance ?? 999) < ($1.distance ?? 999) }
        case .name:
            providers.sort { $0.name < $1.name }
        }

        return providers
    }

    private var currentSuggestions: [String] {
        guard !searchText.isEmpty else { return [] }

        // Generate suggestions from provider names
        let providerSuggestions = providerStore.providers
            .map { $0.name }
            .filter { $0.localizedCaseInsensitiveContains(searchText) }
            .prefix(5)

        return Array(providerSuggestions)
    }

    var body: some View {
        NavigationStack {
            Group {
                if providerStore.isLoading {
                    loadingView
                } else if providerStore.providers.isEmpty {
                    emptyView
                } else {
                    providerList
                }
            }
            .navigationTitle("Resources")
            .searchable(
                text: $searchText,
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: "Search providers, services, or ZIP code"
            ) {
                // Search suggestions
                if !currentSuggestions.isEmpty {
                    ForEach(currentSuggestions, id: \.self) { suggestion in
                        Text(suggestion)
                            .searchCompletion(suggestion)
                    }
                }

                // Popular searches when empty
                if searchText.isEmpty {
                    Section("Popular Searches") {
                        ForEach(SearchStateManager.popularSearches.prefix(4), id: \.self) { search in
                            Label(search, systemImage: "magnifyingglass")
                                .searchCompletion(search)
                        }
                    }
                }
            }
            .searchScopes($searchScope, activation: .onSearchPresentation) {
                ForEach(SearchScope.allCases) { scope in
                    Text(scope.rawValue).tag(scope)
                }
            }
            .onSubmit(of: .search) {
                Task {
                    await searchWithCurrentFilters()
                }
            }
            .onChange(of: searchScope) { _, _ in
                // Refresh results when scope changes
                Task {
                    await searchWithCurrentFilters()
                }
            }
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Picker("Sort by", selection: $sortOption) {
                            ForEach(SortOption.allCases, id: \.self) { option in
                                Text(option.rawValue).tag(option)
                            }
                        }
                    } label: {
                        Image(systemName: "arrow.up.arrow.down")
                    }
                }

                ToolbarItem(placement: .primaryAction) {
                    Button {
                        showFilters = true
                    } label: {
                        ZStack(alignment: .topTrailing) {
                            Image(systemName: "slider.horizontal.3")

                            if activeFilterCount > 0 {
                                Circle()
                                    .fill(Color.accentBlue)
                                    .frame(width: 8, height: 8)
                                    .offset(x: 2, y: -2)
                            }
                        }
                    }
                }
            }
            .sheet(isPresented: $showFilters) {
                FilterSheetView(filters: $appState.searchFilters) {
                    Task { await refreshProviders() }
                }
                .presentationDetents([.medium, .large])
            }
            .refreshable {
                await refreshProviders()
            }
            .task {
                // Always load on first appearance
                if providerStore.providers.isEmpty {
                    await loadInitialProviders()
                }
            }
            // Listen for tab menu actions
            .onReceive(NotificationCenter.default.publisher(for: .refreshList)) { _ in
                Task { await refreshProviders() }
            }
            .onReceive(NotificationCenter.default.publisher(for: .sortList)) { _ in
                // Toggle sort option
                withAnimation {
                    sortOption = sortOption == .distance ? .name : .distance
                }
            }
        }
    }

    private var activeFilterCount: Int {
        var count = 0
        if appState.searchFilters.ageGroup != nil { count += 1 }
        if appState.searchFilters.diagnosis != nil { count += 1 }
        if appState.searchFilters.insurance != nil { count += 1 }
        count += appState.searchFilters.therapyTypes.count
        return count
    }

    private func searchWithCurrentFilters() async {
        var filters = appState.searchFilters

        // Apply scope as therapy filter
        if let therapyType = searchScope.therapyType {
            if !filters.therapyTypes.contains(therapyType) {
                filters.therapyTypes = [therapyType]
            }
        }

        let coordinate = locationService.coordinate ?? CLLocationCoordinate2D(latitude: 34.0522, longitude: -118.2437)

        await providerStore.search(
            query: searchText.isEmpty ? nil : searchText,
            location: coordinate,
            filters: filters
        )
    }

    // MARK: - Subviews

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
            Text("Loading resources...")
                .foregroundStyle(.secondary)
        }
    }

    private var emptyView: some View {
        ContentUnavailableView {
            Label("No Resources Found", systemImage: "building.2")
        } description: {
            Text("Try adjusting your search filters or expanding your search radius.")
        } actions: {
            Button("Refresh") {
                Task { await refreshProviders() }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.accentBlue)
        }
    }

    private var providerList: some View {
        VStack(spacing: 0) {
            // Results header
            resultsHeader

            List(filteredProviders) { provider in
                NavigationLink {
                    ProviderDetailView(provider: provider)
                } label: {
                    ProviderCardView(provider: provider)
                }
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                .listRowSeparator(.hidden)
            }
            .listStyle(.plain)
        }
    }

    private var resultsHeader: some View {
        HStack {
            Text("\(filteredProviders.count) resource\(filteredProviders.count == 1 ? "" : "s") found")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Spacer()

            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Text("Clear search")
                        .font(.caption)
                        .foregroundColor(.accentBlue)
                }
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
        .background(Color(.systemGroupedBackground))
    }

    // MARK: - Methods

    private func loadInitialProviders() async {
        if let coordinate = locationService.coordinate {
            await providerStore.search(
                location: coordinate,
                filters: appState.searchFilters
            )
        } else {
            // Default to LA center
            await providerStore.searchNearby(
                latitude: 34.0522,
                longitude: -118.2437,
                radiusMiles: appState.searchFilters.radiusMiles
            )
        }
    }

    private func refreshProviders() async {
        if let coordinate = locationService.coordinate {
            await providerStore.search(
                location: coordinate,
                filters: appState.searchFilters
            )
        } else if let lastLocation = providerStore.lastSearchLocation {
            await providerStore.search(
                location: lastLocation,
                filters: appState.searchFilters
            )
        } else {
            await loadInitialProviders()
        }
    }
}

// MARK: - Provider Card View (Liquid Glass Style)

struct ProviderCardView: View {
    let provider: Provider

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header: Name + Distance
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(provider.name)
                        .font(.headline)
                        .foregroundColor(.primary)
                        .lineLimit(2)

                    if let type = provider.type, !type.isEmpty {
                        Text(type)
                            .font(.caption)
                            .fontWeight(.medium)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background {
                                Capsule()
                                    .fill(Color.accentBlue.opacity(0.12))
                                Capsule()
                                    .stroke(Color.accentBlue.opacity(0.2), lineWidth: 0.5)
                            }
                            .foregroundColor(.accentBlue)
                    }
                }

                Spacer()

                // Distance badge (glass style)
                if provider.distance != nil {
                    VStack(spacing: 4) {
                        Image(systemName: "location.fill")
                            .font(.caption)
                            .foregroundColor(.accentBlue)

                        Text(provider.distanceFormatted)
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.accentBlue)
                    }
                    .padding(10)
                    .background {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(.ultraThinMaterial)
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color.accentBlue.opacity(0.08))
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.accentBlue.opacity(0.2), lineWidth: 0.5)
                    }
                }
            }

            // Address
            HStack(spacing: 6) {
                Image(systemName: "mappin.circle.fill")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Text(provider.formattedAddress)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }

            // Services tags
            if let therapyTypes = provider.therapyTypes, !therapyTypes.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(Array(therapyTypes.prefix(3)), id: \.self) { therapy in
                            ServiceTag(text: shortTherapyName(therapy))
                        }
                        if therapyTypes.count > 3 {
                            ServiceTag(text: "+\(therapyTypes.count - 3) more", secondary: true)
                        }
                    }
                }
            }

            // Bottom row: Phone + Regional Center
            HStack {
                if let phone = provider.formattedPhone {
                    Label(phone, systemImage: "phone.fill")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                if let rcName = provider.regionalCenterShortName {
                    Text(rcName)
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(
                            Capsule()
                                .fill(
                                    LinearGradient(
                                        colors: [.purple, .purple.opacity(0.8)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .shadow(color: .purple.opacity(0.4), radius: 4, y: 2)
                        )
                }
            }
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(.systemBackground))
            // Subtle inner glow
            RoundedRectangle(cornerRadius: 16)
                .fill(
                    LinearGradient(
                        colors: [.white.opacity(0.05), .clear],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            // Glass edge
            RoundedRectangle(cornerRadius: 16)
                .stroke(
                    LinearGradient(
                        colors: [.white.opacity(0.3), .black.opacity(0.05)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 0.5
                )
        }
        .shadow(color: .black.opacity(0.08), radius: 12, y: 6)
        .shadow(color: .black.opacity(0.04), radius: 2, y: 1)
    }

    private func shortTherapyName(_ therapy: String) -> String {
        therapy
            .replacingOccurrences(of: " therapy", with: "")
            .replacingOccurrences(of: "Parent child interaction therapy/parent training behavior management", with: "Parent Training")
    }
}

// MARK: - Service Tag (Glass Style)

struct ServiceTag: View {
    let text: String
    var secondary: Bool = false

    var body: some View {
        Text(text)
            .font(.caption2)
            .fontWeight(.semibold)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background {
                Capsule()
                    .fill(secondary ? Color(.systemGray5) : Color.green.opacity(0.12))
                Capsule()
                    .stroke(secondary ? Color.gray.opacity(0.2) : Color.green.opacity(0.25), lineWidth: 0.5)
            }
            .foregroundColor(secondary ? .secondary : .green)
    }
}

// MARK: - Provider Row View (simplified for sections)

struct ProviderRowView: View {
    let provider: Provider

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(provider.name)
                        .font(.headline)
                        .lineLimit(2)

                    if let type = provider.type, !type.isEmpty {
                        Text(type)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    if provider.distance != nil {
                        Text(provider.distanceFormatted)
                            .font(.caption)
                            .fontWeight(.medium)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.accentBlue.opacity(0.1))
                            .foregroundColor(.accentBlue)
                            .cornerRadius(8)
                    }

                    if let rcShortName = provider.regionalCenterShortName {
                        Text(rcShortName)
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .foregroundColor(.purple)
                    }
                }
            }

            Text(provider.formattedAddress)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)

            if !provider.servicesFormatted.isEmpty {
                Text(provider.servicesFormatted)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    ProviderListView()
        .environmentObject(AppState())
}
