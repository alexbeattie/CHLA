//
//  ModernSearchView.swift
//  NDD Resources
//
//  Modern search experience with suggestions, scopes, and live results
//

import SwiftUI
import Combine

// MARK: - Search Scope
enum SearchScope: String, CaseIterable, Identifiable {
    case all = "All"
    case abaTherapy = "ABA"
    case speechTherapy = "Speech"
    case occupationalTherapy = "OT"
    case physicalTherapy = "PT"

    var id: String { rawValue }

    var therapyType: String? {
        switch self {
        case .all: return nil
        case .abaTherapy: return "ABA therapy"
        case .speechTherapy: return "Speech therapy"
        case .occupationalTherapy: return "Occupational therapy"
        case .physicalTherapy: return "Physical therapy"
        }
    }

    var icon: String {
        switch self {
        case .all: return "magnifyingglass"
        case .abaTherapy: return "brain.head.profile"
        case .speechTherapy: return "waveform"
        case .occupationalTherapy: return "hand.raised.fill"
        case .physicalTherapy: return "figure.walk"
        }
    }
}

// MARK: - Search State Manager
@MainActor
class SearchStateManager: ObservableObject {
    @Published var searchText = ""
    @Published var isSearchActive = false
    @Published var selectedScope: SearchScope = .all
    @Published var showSuggestions = false
    @Published var recentSearches: [String] = []

    private var cancellables = Set<AnyCancellable>()
    private let debounceDelay: TimeInterval = 0.4

    // Callback for when search should execute
    var onSearch: ((String, SearchScope) -> Void)?

    init() {
        loadRecentSearches()
        setupDebounce()
    }

    private func setupDebounce() {
        $searchText
            .debounce(for: .seconds(debounceDelay), scheduler: DispatchQueue.main)
            .removeDuplicates()
            .sink { [weak self] text in
                guard let self = self else { return }
                if !text.isEmpty && self.isSearchActive {
                    self.onSearch?(text, self.selectedScope)
                }
            }
            .store(in: &cancellables)
    }

    func addToRecentSearches(_ query: String) {
        guard !query.isEmpty else { return }
        // Remove if exists, add to front
        recentSearches.removeAll { $0.lowercased() == query.lowercased() }
        recentSearches.insert(query, at: 0)
        // Keep only last 10
        if recentSearches.count > 10 {
            recentSearches = Array(recentSearches.prefix(10))
        }
        saveRecentSearches()
    }

    func clearRecentSearches() {
        recentSearches = []
        saveRecentSearches()
    }

    private func loadRecentSearches() {
        recentSearches = UserDefaults.standard.stringArray(forKey: "recentSearches") ?? []
    }

    private func saveRecentSearches() {
        UserDefaults.standard.set(recentSearches, forKey: "recentSearches")
    }

    static let popularSearches = [
        "ABA therapy",
        "Speech therapy near me",
        "Autism services",
        "Early intervention",
        "Occupational therapy",
        "Regional Center"
    ]
}

// MARK: - Modern Search Bar
struct ModernSearchBar: View {
    @ObservedObject var searchState: SearchStateManager
    @FocusState private var isTextFieldFocused: Bool
    let onFilterTap: () -> Void
    let activeFilterCount: Int

    var body: some View {
        VStack(spacing: 0) {
            // Main search bar
            HStack(spacing: 12) {
                searchField
                filterButton
            }

            // Search scopes (visible when search is active)
            if searchState.isSearchActive || !searchState.searchText.isEmpty {
                searchScopes
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.8), value: searchState.isSearchActive)
    }

    @ViewBuilder
    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(searchState.isSearchActive ? Color.accentBlue : .secondary)
                .fontWeight(.medium)
                .animation(.easeInOut(duration: 0.2), value: searchState.isSearchActive)

            TextField("Search providers, ZIP code...", text: $searchState.searchText)
                .focused($isTextFieldFocused)
                .submitLabel(.search)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .onSubmit {
                    searchState.addToRecentSearches(searchState.searchText)
                    searchState.onSearch?(searchState.searchText, searchState.selectedScope)
                }
                .onChange(of: isTextFieldFocused) { _, focused in
                    withAnimation(.spring(response: 0.3)) {
                        searchState.isSearchActive = focused
                        // Show suggestions when search is active
                        searchState.showSuggestions = focused
                    }
                }

            if !searchState.searchText.isEmpty {
                Button {
                    withAnimation(.spring(response: 0.25)) {
                        searchState.searchText = ""
                        searchState.showSuggestions = true
                    }
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                        .font(.system(size: 18))
                }
                .transition(.scale.combined(with: .opacity))
            }

            // Cancel button when active
            if searchState.isSearchActive {
                Button("Cancel") {
                    withAnimation(.spring(response: 0.3)) {
                        searchState.searchText = ""
                        searchState.isSearchActive = false
                        searchState.showSuggestions = false
                        isTextFieldFocused = false
                    }
                }
                .font(.subheadline)
                .foregroundColor(.accentBlue)
                .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background {
            glassBackground(cornerRadius: 16)
        }
        .shadow(color: .black.opacity(0.1), radius: 8, y: 4)
    }

    @ViewBuilder
    private var searchScopes: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(SearchScope.allCases) { scope in
                    ScopeButton(
                        scope: scope,
                        isSelected: searchState.selectedScope == scope
                    ) {
                        withAnimation(.spring(response: 0.3)) {
                            searchState.selectedScope = scope
                        }
                        // Trigger search with new scope
                        if !searchState.searchText.isEmpty {
                            searchState.onSearch?(searchState.searchText, scope)
                        }
                    }
                }
            }
            .padding(.horizontal, 4)
        }
        .padding(.top, 12)
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

    @ViewBuilder
    private func glassBackground(cornerRadius: CGFloat) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(.ultraThinMaterial)
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(
                    LinearGradient(
                        colors: [.white.opacity(0.15), .clear],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            RoundedRectangle(cornerRadius: cornerRadius)
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
}

// MARK: - Scope Button
struct ScopeButton: View {
    let scope: SearchScope
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: scope.icon)
                    .font(.system(size: 12, weight: .semibold))
                Text(scope.rawValue)
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background {
                if isSelected {
                    Capsule()
                        .fill(Color.accentBlue)
                        .shadow(color: Color.accentBlue.opacity(0.3), radius: 6, y: 3)
                } else {
                    Capsule()
                        .fill(.ultraThinMaterial)
                    Capsule()
                        .stroke(Color.primary.opacity(0.1), lineWidth: 0.5)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Search Suggestions Sheet
struct SearchSuggestionsView: View {
    @ObservedObject var searchState: SearchStateManager
    let onSelectSuggestion: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Recent searches
            if !searchState.recentSearches.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Label("Recent", systemImage: "clock.arrow.circlepath")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundStyle(.secondary)

                        Spacer()

                        Button("Clear") {
                            withAnimation {
                                searchState.clearRecentSearches()
                            }
                        }
                        .font(.caption)
                        .foregroundColor(.accentBlue)
                    }

                    ForEach(searchState.recentSearches.prefix(5), id: \.self) { search in
                        SuggestionRow(
                            icon: "clock",
                            text: search,
                            isRecent: true
                        ) {
                            onSelectSuggestion(search)
                        }
                    }
                }
            }

            // Popular searches
            VStack(alignment: .leading, spacing: 12) {
                Label("Popular", systemImage: "flame.fill")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(.secondary)

                ForEach(SearchStateManager.popularSearches, id: \.self) { search in
                    SuggestionRow(
                        icon: "magnifyingglass",
                        text: search,
                        isRecent: false
                    ) {
                        onSelectSuggestion(search)
                    }
                }
            }
        }
        .padding()
    }
}

// MARK: - Suggestion Row
struct SuggestionRow: View {
    let icon: String
    let text: String
    let isRecent: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 14))
                    .foregroundStyle(isRecent ? .secondary : Color.accentBlue)
                    .frame(width: 24)

                Text(text)
                    .font(.body)
                    .foregroundStyle(.primary)

                Spacer()

                Image(systemName: "arrow.up.left")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 12)
            .background {
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color.primary.opacity(0.03))
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Bottom Sheet Results
struct SearchResultsSheet: View {
    let providers: [Provider]
    let isLoading: Bool
    @Binding var selectedProvider: Provider?
    let searchText: String

    @State private var sheetDetent: PresentationDetent = .fraction(0.25)

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    loadingView
                } else if providers.isEmpty {
                    emptyView
                } else {
                    resultsList
                }
            }
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var navigationTitle: String {
        if isLoading {
            return "Searching..."
        } else if providers.isEmpty {
            return "No Results"
        } else {
            return "\(providers.count) Provider\(providers.count == 1 ? "" : "s")"
        }
    }

    @ViewBuilder
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Finding providers...")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private var emptyView: some View {
        ContentUnavailableView {
            Label("No Providers Found", systemImage: "building.2")
        } description: {
            if !searchText.isEmpty {
                Text("Try different search terms or expand your filters.")
            } else {
                Text("Search for providers by name, location, or service type.")
            }
        }
    }

    @ViewBuilder
    private var resultsList: some View {
        List {
            ForEach(providers) { provider in
                ProviderResultRow(provider: provider)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    .listRowSeparator(.hidden)
                    .onTapGesture {
                        selectedProvider = provider
                    }
            }
        }
        .listStyle(.plain)
    }
}

// MARK: - Provider Result Row
struct ProviderResultRow: View {
    let provider: Provider

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(provider.name)
                        .font(.headline)
                        .lineLimit(2)

                    if let type = provider.type {
                        Text(type)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                }

                Spacer()

                if let distance = provider.distance {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(String(format: "%.1f", distance))
                            .font(.system(.title3, design: .rounded))
                            .fontWeight(.bold)
                            .foregroundColor(.accentBlue)
                        Text("mi")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            // Address
            HStack(spacing: 6) {
                Image(systemName: "mappin.circle.fill")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(provider.formattedAddress)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            // Services tags
            if let therapies = provider.therapyTypes, !therapies.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(therapies.prefix(3), id: \.self) { therapy in
                            Text(shortTherapyName(therapy))
                                .font(.caption2)
                                .fontWeight(.medium)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.accentBlue.opacity(0.1))
                                .foregroundColor(.accentBlue)
                                .cornerRadius(6)
                        }
                    }
                }
            }
        }
        .padding()
        .background {
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThinMaterial)
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.primary.opacity(0.08), lineWidth: 0.5)
        }
    }

    private func shortTherapyName(_ therapy: String) -> String {
        therapy
            .replacingOccurrences(of: " therapy", with: "")
            .replacingOccurrences(of: "Parent child interaction therapy/parent training behavior management", with: "Parent Training")
    }
}

// MARK: - Preview
#Preview {
    VStack {
        ModernSearchBar(
            searchState: SearchStateManager(),
            onFilterTap: {},
            activeFilterCount: 2
        )
        .padding()

        Spacer()
    }
    .background(Color.gray.opacity(0.1))
}
