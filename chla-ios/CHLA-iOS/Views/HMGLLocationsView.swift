//
//  HMGLLocationsView.swift
//  CHLA-iOS
//
//  View for browsing Help Me Grow LA locations
//

import SwiftUI
import CoreLocation
import MapKit

struct HMGLLocationsView: View {
    @StateObject private var store = HMGLLocationStore()
    @StateObject private var locationManager = HMGLLocationManager()
    @State private var selectedLocation: HMGLLocation?
    @State private var viewMode: ViewMode = .list
    @State private var sortOption: SortOption = .distance

    enum ViewMode: String, CaseIterable {
        case list = "List"
        case map = "Map"
    }

    enum SortOption: String, CaseIterable {
        case distance = "Distance"
        case name = "Name"
        case city = "City"
    }

    private var sortedLocations: [HMGLLocation] {
        let locations = store.nearbyLocations.isEmpty ? store.filteredLocations : store.nearbyLocations
        switch sortOption {
        case .distance:
            return locations.sorted { ($0.distance ?? 999) < ($1.distance ?? 999) }
        case .name:
            return locations.sorted { ($0.displayName) < ($1.displayName) }
        case .city:
            return locations.sorted { ($0.city ?? "ZZZ") < ($1.city ?? "ZZZ") }
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                // Background
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Stats Header (collapsed when in map mode)
                    if viewMode == .list, let stats = store.stats {
                        StatsHeaderView(stats: stats)
                            .padding(.horizontal)
                            .padding(.top, 8)
                    }

                    // Search Bar
                    HMGLSearchBar(text: $store.searchQuery)
                        .padding(.horizontal)
                        .padding(.vertical, 8)

                    // View Mode & Sort Picker
                    HStack {
                        // View Mode Toggle
                        Picker("View", selection: $viewMode) {
                            ForEach(ViewMode.allCases, id: \.self) { mode in
                                Label(mode.rawValue, systemImage: mode == .list ? "list.bullet" : "map")
                                    .tag(mode)
                            }
                        }
                        .pickerStyle(.segmented)

                        // Sort Menu
                        Menu {
                            ForEach(SortOption.allCases, id: \.self) { option in
                                Button {
                                    sortOption = option
                                } label: {
                                    HStack {
                                        Text(option.rawValue)
                                        if sortOption == option {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                }
                            }
                        } label: {
                            Image(systemName: "arrow.up.arrow.down.circle")
                                .font(.title2)
                                .foregroundStyle(Color.accentBlue)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)

                    // Content
                    if store.isLoading {
                        Spacer()
                        ProgressView("Loading locations...")
                            .progressViewStyle(.circular)
                        Spacer()
                    } else if let error = store.error {
                        Spacer()
                        ErrorView(message: error) {
                            Task {
                                await loadNearby()
                            }
                        }
                        Spacer()
                    } else {
                        if viewMode == .list {
                            LocationListView(
                                locations: sortedLocations,
                                selectedLocation: $selectedLocation
                            )
                        } else {
                            HMGLMapView(
                                locations: sortedLocations,
                                selectedLocation: $selectedLocation,
                                userLocation: locationManager.location
                            )
                        }
                    }
                }
            }
            .navigationTitle("Help Me Grow LA")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task {
                            await loadNearby()
                        }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .sheet(item: $selectedLocation) { location in
                HMGLLocationDetailView(location: location)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
            }
            .task {
                await store.loadStats()
                await loadNearby()
            }
        }
    }

    private func loadNearby() async {
        if let location = locationManager.location {
            await store.loadNearbyLocations(
                latitude: location.coordinate.latitude,
                longitude: location.coordinate.longitude
            )
        } else {
            // Default to downtown LA
            await store.loadNearbyLocations(latitude: 34.0522, longitude: -118.2437)
        }
    }
}

// MARK: - Stats Header
struct StatsHeaderView: View {
    let stats: HMGLStatsResponse

    var body: some View {
        HStack(spacing: 16) {
            StatBadge(
                icon: "building.2.fill",
                value: "\(stats.totalLocations.formatted())",
                label: "Total"
            )

            StatBadge(
                icon: "mappin.circle.fill",
                value: "\(stats.withCoordinates.formatted())",
                label: "Mapped"
            )

            StatBadge(
                icon: "building.columns.fill",
                value: "\(stats.countyLocations)",
                label: "County"
            )
        }
        .padding()
        .background {
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThinMaterial)
        }
    }
}

struct StatBadge: View {
    let icon: String
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(Color.accentBlue)

            Text(value)
                .font(.headline)
                .fontWeight(.bold)

            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Search Bar
struct HMGLSearchBar: View {
    @Binding var text: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)

            TextField("Search locations...", text: $text)
                .textFieldStyle(.plain)

            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(12)
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
        }
    }
}

// MARK: - Location List
struct LocationListView: View {
    let locations: [HMGLLocation]
    @Binding var selectedLocation: HMGLLocation?

    var body: some View {
        if locations.isEmpty {
            ContentUnavailableView(
                "No Locations Found",
                systemImage: "mappin.slash",
                description: Text("Try searching in a different area")
            )
        } else {
            List(locations) { location in
                HMGLLocationRow(location: location)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        selectedLocation = location
                    }
            }
            .listStyle(.plain)
        }
    }
}

// MARK: - Location Row
struct HMGLLocationRow: View {
    let location: HMGLLocation

    var body: some View {
        HStack(spacing: 12) {
            // Icon
            ZStack {
                Circle()
                    .fill(Color.accentBlue.opacity(0.15))
                    .frame(width: 44, height: 44)

                Image(systemName: locationIcon)
                    .font(.system(size: 18))
                    .foregroundStyle(Color.accentBlue)
            }

            // Info
            VStack(alignment: .leading, spacing: 4) {
                Text(location.displayName)
                    .font(.headline)
                    .lineLimit(2)

                if let org = location.organization, !org.isEmpty, org != location.name {
                    Text(org)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }

                HStack(spacing: 4) {
                    if !location.locationString.isEmpty {
                        Text(location.locationString)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    if let distance = location.distance {
                        Text("•")
                            .foregroundStyle(.secondary)
                        Text(String(format: "%.1f mi", distance))
                            .font(.caption)
                            .foregroundStyle(Color.accentBlue)
                    }
                }
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 4)
    }

    private var locationIcon: String {
        if location.organization?.lowercased().contains("school") == true {
            return "building.columns"
        } else if location.organization?.lowercased().contains("hospital") == true {
            return "cross.circle"
        } else if location.organization?.lowercased().contains("police") == true {
            return "shield"
        } else if location.organization?.lowercased().contains("park") == true {
            return "leaf"
        } else {
            return "mappin.circle"
        }
    }
}

// MARK: - HMGL Map View
struct HMGLMapView: View {
    let locations: [HMGLLocation]
    @Binding var selectedLocation: HMGLLocation?
    let userLocation: CLLocation?

    @State private var region: MKCoordinateRegion
    @State private var selectedAnnotation: HMGLLocation?

    init(locations: [HMGLLocation], selectedLocation: Binding<HMGLLocation?>, userLocation: CLLocation?) {
        self.locations = locations
        self._selectedLocation = selectedLocation
        self.userLocation = userLocation

        // Default to LA if no user location
        let center = userLocation?.coordinate ?? CLLocationCoordinate2D(latitude: 34.0522, longitude: -118.2437)
        self._region = State(initialValue: MKCoordinateRegion(
            center: center,
            span: MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1)
        ))
    }

    var body: some View {
        Map(coordinateRegion: $region, annotationItems: mappableLocations) { location in
            MapAnnotation(coordinate: location.coordinate) {
                Button {
                    selectedLocation = location.hmglLocation
                } label: {
                    VStack(spacing: 2) {
                        Image(systemName: "mappin.circle.fill")
                            .font(.title)
                            .foregroundStyle(Color.accentBlue)
                            .background(Circle().fill(.white).padding(4))

                        Text(location.hmglLocation.displayName)
                            .font(.caption2)
                            .fontWeight(.medium)
                            .lineLimit(1)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 2)
                            .background(.ultraThinMaterial)
                            .clipShape(Capsule())
                    }
                }
            }
        }
        .ignoresSafeArea(edges: .bottom)
        .overlay(alignment: .bottomTrailing) {
            // Recenter button
            Button {
                if let loc = userLocation {
                    withAnimation {
                        region.center = loc.coordinate
                    }
                }
            } label: {
                Image(systemName: "location.fill")
                    .font(.title2)
                    .padding(12)
                    .background(.ultraThinMaterial)
                    .clipShape(Circle())
                    .shadow(radius: 4)
            }
            .padding()
        }
        .overlay(alignment: .top) {
            // Location count badge
            Text("\(locations.count) locations")
                .font(.caption)
                .fontWeight(.medium)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(.ultraThinMaterial)
                .clipShape(Capsule())
                .padding(.top, 8)
        }
    }

    private var mappableLocations: [MappableLocation] {
        locations.compactMap { location in
            guard let lat = location.latitude, let lng = location.longitude else { return nil }
            return MappableLocation(
                id: location.id,
                coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lng),
                hmglLocation: location
            )
        }
    }
}

struct MappableLocation: Identifiable {
    let id: Int
    let coordinate: CLLocationCoordinate2D
    let hmglLocation: HMGLLocation
}

// MARK: - Error View
struct ErrorView: View {
    let message: String
    let retryAction: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.orange)

            Text("Something went wrong")
                .font(.headline)

            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button("Try Again") {
                retryAction()
            }
            .buttonStyle(.bordered)
        }
    }
}

// MARK: - Location Detail View
struct HMGLLocationDetailView: View {
    let location: HMGLLocation
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                detailContent
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var detailContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            headerSection
            Divider()
            contactSection
            tagsSection
            programsSection
            Spacer(minLength: 60)
        }
        .padding()
    }

    @ViewBuilder
    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(location.displayName)
                .font(.title2)
                .fontWeight(.bold)

            if let org = location.organization, !org.isEmpty {
                Text(org)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }

    @ViewBuilder
    private var contactSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let address = location.fullAddress, !address.isEmpty {
                HMGLDetailRow(icon: "mappin.circle.fill", title: "Address", value: address)
            }

            if let phone = location.primaryPhone ?? location.phones, !phone.isEmpty {
                HMGLDetailRow(icon: "phone.fill", title: "Phone", value: phone, isPhone: true)
            }

            if let email = location.email, !email.isEmpty {
                HMGLDetailRow(icon: "envelope.fill", title: "Email", value: email)
            }

            if let url = location.url, !url.isEmpty {
                HMGLDetailRow(icon: "globe", title: "Website", value: url, isLink: true)
            }

            if let hours = location.displayHours, !hours.isEmpty {
                HMGLDetailRow(icon: "clock.fill", title: "Hours", value: hours)
            }
        }
    }

    @ViewBuilder
    private var tagsSection: some View {
        let tagNames = location.tagNames
        if !tagNames.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Tags")
                    .font(.headline)

                HMGLFlowLayout(spacing: 8) {
                    ForEach(tagNames, id: \.self) { tag in
                        HMGLTagView(tag: tag)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var programsSection: some View {
        let programNames = location.programNames
        if !programNames.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Programs")
                    .font(.headline)

                ForEach(programNames, id: \.self) { program in
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                        Text(program)
                            .font(.subheadline)
                    }
                }
            }
        }
    }
}

struct HMGLTagView: View {
    let tag: String

    var body: some View {
        Text(tag)
            .font(.caption)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color.accentBlue.opacity(0.15))
            .foregroundStyle(Color.accentBlue)
            .clipShape(Capsule())
    }
}

// MARK: - Detail Row
struct HMGLDetailRow: View {
    let icon: String
    let title: String
    let value: String
    var isPhone: Bool = false
    var isLink: Bool = false

    @Environment(\.openURL) private var openURL

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Color.accentBlue)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption)
                    .foregroundStyle(.secondary)

                if isPhone {
                    Button {
                        if let url = URL(string: "tel:\(value.replacingOccurrences(of: " ", with: ""))") {
                            openURL(url)
                        }
                    } label: {
                        Text(value)
                            .font(.subheadline)
                            .foregroundStyle(Color.accentBlue)
                    }
                } else if isLink {
                    Button {
                        var urlString = value
                        if !urlString.hasPrefix("http") {
                            urlString = "https://\(urlString)"
                        }
                        if let url = URL(string: urlString) {
                            openURL(url)
                        }
                    } label: {
                        Text(value)
                            .font(.subheadline)
                            .foregroundStyle(Color.accentBlue)
                            .lineLimit(1)
                    }
                } else {
                    Text(value)
                        .font(.subheadline)
                }
            }
        }
    }
}

// MARK: - Flow Layout for Tags
struct HMGLFlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(in: proposal.width ?? 0, subviews: subviews, spacing: spacing)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(in: bounds.width, subviews: subviews, spacing: spacing)
        for (index, subview) in subviews.enumerated() {
            subview.place(at: CGPoint(x: bounds.minX + result.positions[index].x,
                                      y: bounds.minY + result.positions[index].y),
                         proposal: .unspecified)
        }
    }

    struct FlowResult {
        var positions: [CGPoint] = []
        var size: CGSize = .zero

        init(in width: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var currentX: CGFloat = 0
            var currentY: CGFloat = 0
            var lineHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if currentX + size.width > width && currentX > 0 {
                    currentX = 0
                    currentY += lineHeight + spacing
                    lineHeight = 0
                }

                positions.append(CGPoint(x: currentX, y: currentY))
                lineHeight = max(lineHeight, size.height)
                currentX += size.width + spacing
                self.size.width = max(self.size.width, currentX)
            }

            self.size.height = currentY + lineHeight
        }
    }
}

// MARK: - Location Manager for HMGL
class HMGLLocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var location: CLLocation?

    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.requestWhenInUseAuthorization()
        manager.startUpdatingLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        location = locations.last
        manager.stopUpdatingLocation()
    }
}

#Preview {
    HMGLLocationsView()
}
