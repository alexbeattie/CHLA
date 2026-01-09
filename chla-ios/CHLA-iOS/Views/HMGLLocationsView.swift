//
//  HMGLLocationsView.swift
//  CHLA-iOS
//
//  View for browsing Help Me Grow LA locations
//

import SwiftUI
import CoreLocation

struct HMGLLocationsView: View {
    @StateObject private var store = HMGLLocationStore()
    @StateObject private var locationManager = HMGLLocationManager()
    @State private var selectedLocation: HMGLLocation?
    @State private var showingNearby = true
    
    var body: some View {
        NavigationStack {
            ZStack {
                // Background
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Stats Header
                    if let stats = store.stats {
                        StatsHeaderView(stats: stats)
                            .padding(.horizontal)
                            .padding(.top, 8)
                    }
                    
                    // Search Bar
                    SearchBarView(text: $store.searchQuery)
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                    
                    // Segment Picker
                    Picker("View", selection: $showingNearby) {
                        Text("Nearby").tag(true)
                        Text("All Locations").tag(false)
                    }
                    .pickerStyle(.segmented)
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
                                if showingNearby {
                                    await loadNearby()
                                } else {
                                    await store.loadLocations()
                                }
                            }
                        }
                        Spacer()
                    } else {
                        LocationListView(
                            locations: showingNearby ? store.nearbyLocations : store.filteredLocations,
                            selectedLocation: $selectedLocation
                        )
                    }
                }
            }
            .navigationTitle("Help Me Grow LA")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task {
                            await store.loadStats()
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
                if showingNearby {
                    await loadNearby()
                } else {
                    await store.loadLocations()
                }
            }
            .onChange(of: showingNearby) { _, newValue in
                Task {
                    if newValue {
                        await loadNearby()
                    } else {
                        await store.loadLocations()
                    }
                }
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
                .foregroundStyle(.accentBlue)
            
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
struct SearchBarView: View {
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
                    .foregroundStyle(.accentBlue)
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
                            .foregroundStyle(.accentBlue)
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
    @Environment(\.openURL) private var openURL
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
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
                    
                    Divider()
                    
                    // Contact Info
                    VStack(alignment: .leading, spacing: 12) {
                        if let address = location.fullAddress, !address.isEmpty {
                            DetailRow(icon: "mappin.circle.fill", title: "Address", value: address)
                        }
                        
                        if let phone = location.primaryPhone ?? location.phones, !phone.isEmpty {
                            DetailRow(icon: "phone.fill", title: "Phone", value: phone, isPhone: true)
                        }
                        
                        if let email = location.email, !email.isEmpty {
                            DetailRow(icon: "envelope.fill", title: "Email", value: email)
                        }
                        
                        if let url = location.url, !url.isEmpty {
                            DetailRow(icon: "globe", title: "Website", value: url, isLink: true)
                        }
                        
                        if let hours = location.hours, !hours.isEmpty {
                            DetailRow(icon: "clock.fill", title: "Hours", value: hours)
                        }
                    }
                    
                    // Tags
                    if let tags = location.tags, !tags.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Tags")
                                .font(.headline)
                            
                            FlowLayout(spacing: 8) {
                                ForEach(tags, id: \.self) { tag in
                                    Text(tag)
                                        .font(.caption)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 6)
                                        .background(Color.accentBlue.opacity(0.15))
                                        .foregroundStyle(.accentBlue)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                    
                    // Programs
                    if let programs = location.programs, !programs.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Programs")
                                .font(.headline)
                            
                            ForEach(programs, id: \.self) { program in
                                HStack {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundStyle(.green)
                                    Text(program)
                                        .font(.subheadline)
                                }
                            }
                        }
                    }
                    
                    Spacer(minLength: 60)
                }
                .padding()
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
}

// MARK: - Detail Row
struct DetailRow: View {
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
                .foregroundStyle(.accentBlue)
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
                            .foregroundStyle(.accentBlue)
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
                            .foregroundStyle(.accentBlue)
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
struct FlowLayout: Layout {
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
