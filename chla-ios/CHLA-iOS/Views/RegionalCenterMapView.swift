//
//  RegionalCenterMapView.swift
//  CHLA-iOS
//
//  Map view showing regional center service areas with GeoJSON polygons
//

import SwiftUI
import MapKit

struct RegionalCenterMapView: View {
    @StateObject private var viewModel = RegionalCenterMapViewModel()

    // Centered on LA County with appropriate zoom
    @State private var cameraPosition: MapCameraPosition = .region(MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 34.05, longitude: -118.25),
        span: MKCoordinateSpan(latitudeDelta: 0.7, longitudeDelta: 0.7)
    ))
    @State private var selectedCenter: ServiceAreaFeature?
    @State private var showCenterDetail = false

    var body: some View {
        ZStack {
            mapContent

            if viewModel.isLoading {
                loadingOverlay
            }

            // Floating controls on right side
            VStack {
                HStack {
                    Spacer()
                    VStack(spacing: 0) {
                        // Location button
                        Button {
                            withAnimation {
                                cameraPosition = .region(MKCoordinateRegion(
                                    center: CLLocationCoordinate2D(latitude: 34.05, longitude: -118.25),
                                    span: MKCoordinateSpan(latitudeDelta: 0.7, longitudeDelta: 0.7)
                                ))
                            }
                        } label: {
                            Image(systemName: "location.fill")
                                .font(.system(size: 18))
                                .foregroundColor(.accentBlue)
                                .frame(width: 44, height: 44)
                        }

                        Divider()
                            .frame(width: 30)

                        // Refresh button
                        Button {
                            Task { await viewModel.fetchServiceAreas() }
                        } label: {
                            Image(systemName: "arrow.clockwise")
                                .font(.system(size: 18))
                                .foregroundColor(.accentBlue)
                                .frame(width: 44, height: 44)
                        }
                    }
                    .background(.ultraThinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                }
                .padding(.trailing, 16)
                .padding(.top, 8)

                Spacer()

                // Legend at bottom
                legendView
                    .padding()
            }
        }
        .ignoresSafeArea(edges: .all)
        .sheet(item: $selectedCenter) { center in
            RegionalCenterInfoSheet(feature: center)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
        .onAppear {
            Task { await viewModel.fetchServiceAreas() }
        }
    }

    // MARK: - Map Content

    @ViewBuilder
    private var mapContent: some View {
        Map(position: $cameraPosition, selection: $selectedCenter) {
            // Service area polygons - render all polygons for each feature
            ForEach(viewModel.serviceAreas) { feature in
                ForEach(Array(feature.allMapPolygons.enumerated()), id: \.offset) { _, polygon in
                    MapPolygon(polygon)
                        .foregroundStyle(feature.fillColor.opacity(0.25))
                        .stroke(feature.strokeColor, lineWidth: 2.5)
                }
            }

            // Center markers (on top of polygons) - using default Marker
            ForEach(viewModel.serviceAreas) { feature in
                if let coordinate = feature.centerCoordinate {
                    Marker(feature.shortName, coordinate: coordinate)
                        .tint(feature.strokeColor)
                        .tag(feature)
                }
            }

            // User location
            UserAnnotation()
        }
        .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
        .mapControls {
            MapUserLocationButton()
            MapCompass()
            MapScaleView()
        }
    }

    // MARK: - Loading Overlay

    @ViewBuilder
    private var loadingOverlay: some View {
        VStack {
            Spacer()
            HStack {
                Spacer()
                VStack(spacing: 12) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Loading service areas...")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(20)
                .background(.ultraThinMaterial)
                .cornerRadius(16)
                Spacer()
            }
            Spacer()
        }
    }

    // MARK: - Legend

    @ViewBuilder
    private var legendView: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("LA County Regional Centers")
                .font(.caption)
                .fontWeight(.semibold)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(viewModel.serviceAreas.prefix(6)) { feature in
                        Button {
                            selectedCenter = feature
                            if let coord = feature.centerCoordinate {
                                withAnimation {
                                    cameraPosition = .region(MKCoordinateRegion(
                                        center: coord,
                                        span: MKCoordinateSpan(latitudeDelta: 0.15, longitudeDelta: 0.15)
                                    ))
                                }
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Circle()
                                    .fill(feature.strokeColor)
                                    .frame(width: 10, height: 10)
                                Text(feature.shortName)
                                    .font(.caption2)
                                    .foregroundColor(.primary)
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(selectedCenter?.id == feature.id ? feature.strokeColor.opacity(0.2) : Color.clear)
                            .cornerRadius(12)
                        }
                    }
                }
            }
        }
        .padding(12)
        .background(.ultraThinMaterial)
        .cornerRadius(12)
    }
}

// MARK: - Regional Center Marker
// RegionalCenterMapMarker is now in MapMarkers.swift with enhanced animations

// MARK: - Info Sheet
struct RegionalCenterInfoSheet: View {
    let feature: ServiceAreaFeature
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var showDirections = false

    // Get full details from matcher
    private var rcInfo: RegionalCenterMatcher.RegionalCenterInfo? {
        RegionalCenterMatcher.shared.laRegionalCenters.first { $0.shortName == feature.shortName }
    }

    // Full data from mock list (has more details) - improved matching
    private var rcData: RegionalCenter? {
        // First try exact acronym match
        if let match = RegionalCenter.mockList.first(where: { rc in
            let rcAcronym = extractAcronym(from: rc.regionalCenter)
            return rcAcronym == feature.shortName || feature.acronym == rcAcronym
        }) {
            return match
        }
        // Fallback: fuzzy name match
        return RegionalCenter.mockList.first { rc in
            let rcNameLower = rc.regionalCenter.lowercased()
            let featureNameLower = feature.name.lowercased()
            return rcNameLower.contains(featureNameLower.prefix(8)) ||
                   featureNameLower.contains(rcNameLower.prefix(8))
        }
    }

    private func extractAcronym(from name: String) -> String {
        let lower = name.lowercased()
        if lower.contains("north") { return "NLACRC" }
        if lower.contains("south central") { return "SCLARC" }
        if lower.contains("eastern") { return "ELARC" }
        if lower.contains("westside") { return "WRC" }
        if lower.contains("harbor") { return "HRC" }
        if lower.contains("san gabriel") || lower.contains("pomona") { return "SG/PRC" }
        if lower.contains("lanterman") { return "FDLRC" }
        return ""
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    headerSection

                    // Quick Actions
                    quickActionsSection

                    Divider()

                    // Contact Info
                    contactSection

                    Divider()

                    // Service Area
                    serviceAreaSection

                    // Communities Served
                    if let areas = rcData?.serviceAreas, !areas.isEmpty {
                        Divider()
                        communitiesSection(areas)
                    }

                    // Zip Codes
                    if let zips = rcData?.zipCodes, !zips.isEmpty {
                        Divider()
                        zipCodesSection(zips)
                    }

                    Divider()

                    // Map Preview
                    mapPreviewSection
                }
                .padding()
            }
            .navigationTitle("Regional Center")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(isPresented: $showDirections) {
                if let coord = feature.centerCoordinate {
                    DirectionsMapView(
                        destinationName: feature.name,
                        destinationCoordinate: coord,
                        destinationAddress: rcData?.fullAddress ?? ""
                    )
                }
            }
        }
    }

    // MARK: - Header

    private var headerSection: some View {
        HStack(alignment: .top, spacing: 12) {
            // Color badge
            RoundedRectangle(cornerRadius: 6)
                .fill(feature.strokeColor)
                .frame(width: 8)

            VStack(alignment: .leading, spacing: 8) {
                Text(feature.name)
                    .font(.title2)
                    .fontWeight(.bold)

                HStack(spacing: 8) {
                    Text(feature.shortName)
                        .font(.subheadline.bold())
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(feature.strokeColor)
                        .cornerRadius(6)

                    if let office = rcData?.officeType, !office.isEmpty {
                        Text(office)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                if let district = rcData?.losAngelesHealthDistrict, !district.isEmpty {
                    Label(district + " District", systemImage: "map")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    // MARK: - Quick Actions

    private var quickActionsSection: some View {
        HStack(spacing: 12) {
            // Call
            if let info = rcInfo {
                Button {
                    if let url = URL(string: "tel://\(info.phone.filter { $0.isNumber })") {
                        openURL(url)
                    }
                } label: {
                    VStack(spacing: 6) {
                        Image(systemName: "phone.fill")
                            .font(.title2)
                        Text("Call")
                            .font(.caption)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.green.opacity(0.1))
                    .foregroundColor(.green)
                    .cornerRadius(12)
                }
            }

            // Website
            if let info = rcInfo {
                Button {
                    if let url = URL(string: "https://\(info.website)") {
                        openURL(url)
                    }
                } label: {
                    VStack(spacing: 6) {
                        Image(systemName: "globe")
                            .font(.title2)
                        Text("Website")
                            .font(.caption)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.blue.opacity(0.1))
                    .foregroundColor(.blue)
                    .cornerRadius(12)
                }
            }

            // Directions
            Button {
                showDirections = true
            } label: {
                VStack(spacing: 6) {
                    Image(systemName: "arrow.triangle.turn.up.right.diamond.fill")
                        .font(.title2)
                    Text("Directions")
                        .font(.caption)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color.orange.opacity(0.1))
                .foregroundColor(.orange)
                .cornerRadius(12)
            }
        }
    }

    // MARK: - Contact Section

    private var contactSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            RCInfoSectionHeader(title: "Contact Information", icon: "person.crop.circle.fill")

            VStack(spacing: 10) {
                // Address
                if let rc = rcData {
                    HStack(alignment: .top) {
                        Image(systemName: "mappin.circle.fill")
                            .foregroundColor(feature.strokeColor)
                            .frame(width: 24)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(rc.address)
                            if let suite = rc.suite, !suite.isEmpty {
                                Text("Suite \(suite)")
                            }
                            Text("\(rc.city), \(rc.state) \(rc.zipCode)")
                        }
                        .font(.subheadline)
                        Spacer()
                    }
                    .padding()
                    .background(Color.backgroundSecondary)
                    .cornerRadius(10)
                }

                // Phone
                if let info = rcInfo {
                    Button {
                        if let url = URL(string: "tel://\(info.phone.filter { $0.isNumber })") {
                            openURL(url)
                        }
                    } label: {
                        HStack {
                            Image(systemName: "phone.fill")
                                .foregroundColor(.green)
                                .frame(width: 24)
                            Text(info.phone)
                                .font(.subheadline)
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(.secondary)
                        }
                        .padding()
                        .background(Color.backgroundSecondary)
                        .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                }

                // Website
                if let info = rcInfo {
                    Button {
                        if let url = URL(string: "https://\(info.website)") {
                            openURL(url)
                        }
                    } label: {
                        HStack {
                            Image(systemName: "globe")
                                .foregroundColor(.blue)
                                .frame(width: 24)
                            Text(info.website)
                                .font(.subheadline)
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(.secondary)
                        }
                        .padding()
                        .background(Color.backgroundSecondary)
                        .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Service Area

    private var serviceAreaSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            RCInfoSectionHeader(title: "Service Area", icon: "map.fill")

            Text(feature.description)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(feature.strokeColor.opacity(0.08))
                .cornerRadius(10)
        }
    }

    // MARK: - Communities

    private func communitiesSection(_ areas: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            RCInfoSectionHeader(title: "Communities Served", icon: "building.2.fill")

            FlowLayout(spacing: 8) {
                ForEach(areas, id: \.self) { area in
                    Text(area)
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(feature.strokeColor.opacity(0.15))
                        .foregroundColor(feature.strokeColor)
                        .cornerRadius(8)
                }
            }
        }
    }

    // MARK: - Zip Codes

    private func zipCodesSection(_ zips: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                RCInfoSectionHeader(title: "Zip Codes", icon: "number")
                Spacer()
                Text("\(zips.count) areas")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(zips.prefix(20), id: \.self) { zip in
                        Text(zip)
                            .font(.caption.monospaced())
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.backgroundSecondary)
                            .cornerRadius(6)
                    }
                    if zips.count > 20 {
                        Text("+\(zips.count - 20) more")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 8)
                    }
                }
            }
        }
    }

    // MARK: - Map Preview

    private var mapPreviewSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            RCInfoSectionHeader(title: "Location", icon: "mappin.and.ellipse")

            if let coord = feature.centerCoordinate {
                Button {
                    showDirections = true
                } label: {
                    ZStack(alignment: .topTrailing) {
                        Map(initialPosition: .region(MKCoordinateRegion(
                            center: coord,
                            span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
                        ))) {
                            Marker(feature.shortName, coordinate: coord)
                                .tint(feature.strokeColor)
                        }
                        .frame(height: 180)
                        .cornerRadius(12)
                        .disabled(true)

                        Image(systemName: "arrow.up.left.and.arrow.down.right")
                            .font(.caption)
                            .padding(6)
                            .background(.ultraThinMaterial)
                            .cornerRadius(6)
                            .padding(8)
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - Section Header for RC Info
struct RCInfoSectionHeader: View {
    let title: String
    let icon: String

    var body: some View {
        Label(title, systemImage: icon)
            .font(.headline)
            .foregroundColor(.primary)
    }
}

// MARK: - View Model
@MainActor
class RegionalCenterMapViewModel: ObservableObject {
    @Published var serviceAreas: [ServiceAreaFeature] = []
    @Published var isLoading = false
    @Published var error: Error?

    // Colors using centralized Color.regionalCenterColor(for:)
    private func colorForAcronym(_ acronym: String) -> Color {
        Color.regionalCenterColor(for: acronym)
    }

    // Center coordinates for each regional center (centered in service area)
    private let officeCoordinates: [String: CLLocationCoordinate2D] = [
        "NLACRC": CLLocationCoordinate2D(latitude: 34.42, longitude: -118.50),      // North LA - Antelope Valley area
        "FDLRC": CLLocationCoordinate2D(latitude: 34.15, longitude: -118.25),       // Lanterman - Glendale/Pasadena area
        "HRC": CLLocationCoordinate2D(latitude: 33.85, longitude: -118.32),         // Harbor - South Bay
        "SCLARC": CLLocationCoordinate2D(latitude: 33.96, longitude: -118.28),      // South Central - centered in orange area
        "ELARC": CLLocationCoordinate2D(latitude: 34.02, longitude: -118.08),       // Eastern LA - East of downtown
        "WRC": CLLocationCoordinate2D(latitude: 34.02, longitude: -118.45),         // Westside - West LA area
        "SG/PRC": CLLocationCoordinate2D(latitude: 34.08, longitude: -117.85)       // San Gabriel - Pomona area
    ]

    func fetchServiceAreas() async {
        isLoading = true
        error = nil

        // Load from bundled GeoJSON file (has real polygon boundaries)
        guard let url = Bundle.main.url(forResource: "la_regional_centers", withExtension: "geojson") else {
            print("❌ Could not find la_regional_centers.geojson in bundle")
            isLoading = false
            return
        }

        do {
            let data = try Data(contentsOf: url)
            let geojson = try JSONDecoder().decode(LocalGeoJSONFeatureCollection.self, from: data)

            var features: [ServiceAreaFeature] = []
            for feature in geojson.features {
                let acronym = feature.properties.acronym
                let color = colorForAcronym(acronym)
                let officeCoord = officeCoordinates[acronym]

                let serviceArea = ServiceAreaFeature(
                    localFeature: feature,
                    color: color,
                    officeCoordinate: officeCoord
                )
                features.append(serviceArea)
            }

            serviceAreas = features
            print("✅ Loaded \(features.count) LA regional centers from local GeoJSON")
        } catch {
            self.error = error
            print("❌ Error loading local GeoJSON: \(error)")
        }

        isLoading = false
    }
}

// MARK: - Local GeoJSON Models (for bundled file)
struct LocalGeoJSONFeatureCollection: Codable {
    let type: String
    let features: [LocalGeoJSONFeature]
}

struct LocalGeoJSONFeature: Codable {
    let type: String
    let properties: LocalRegionalCenterProperties
    let geometry: LocalGeoJSONGeometry
}

struct LocalRegionalCenterProperties: Codable {
    let objectId: Int
    let regionalCenter: String
    let catchmentAreaDesc: String
    let acronym: String

    enum CodingKeys: String, CodingKey {
        case objectId = "OBJECTID"
        case regionalCenter = "REGIONALCENTER"
        case catchmentAreaDesc = "CATCHMENT_AREA_DESC"
        case acronym = "ACRONYM"
    }
}

struct LocalGeoJSONGeometry: Codable {
    let type: String
    let coordinates: LocalGeoJSONCoordinates

    enum CodingKeys: String, CodingKey {
        case type, coordinates
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        type = try container.decode(String.self, forKey: .type)

        // Parse coordinates based on geometry type
        if type == "MultiPolygon" {
            let coords = try container.decode([[[[Double]]]].self, forKey: .coordinates)
            coordinates = .multiPolygon(coords)
        } else if type == "Polygon" {
            let coords = try container.decode([[[Double]]].self, forKey: .coordinates)
            coordinates = .polygon(coords)
        } else {
            coordinates = .polygon([])
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        switch coordinates {
        case .polygon(let coords):
            try container.encode(coords, forKey: .coordinates)
        case .multiPolygon(let coords):
            try container.encode(coords, forKey: .coordinates)
        }
    }
}

enum LocalGeoJSONCoordinates: Codable {
    case polygon([[[Double]]])
    case multiPolygon([[[[Double]]]])
}

// MARK: - Service Area Feature Model
struct ServiceAreaFeature: Identifiable, Hashable {
    let id: Int
    let name: String
    let acronym: String
    let description: String
    let allPolygons: [[[[Double]]]]  // All polygons from MultiPolygon
    let color: Color
    let officeCoordinate: CLLocationCoordinate2D?  // Office location

    // Initialize from local GeoJSON file (has real polygon boundaries)
    init(localFeature: LocalGeoJSONFeature, color: Color, officeCoordinate: CLLocationCoordinate2D? = nil) {
        self.id = localFeature.properties.objectId
        self.name = localFeature.properties.regionalCenter
        self.acronym = localFeature.properties.acronym
        self.description = localFeature.properties.catchmentAreaDesc
        self.color = color
        self.officeCoordinate = officeCoordinate

        // Extract coordinates from geometry
        switch localFeature.geometry.coordinates {
        case .polygon(let polygonCoords):
            self.allPolygons = [polygonCoords]
        case .multiPolygon(let multiCoords):
            self.allPolygons = multiCoords
        }
    }

    var shortName: String {
        // Use acronym from data, or derive from name
        if !acronym.isEmpty {
            return acronym
        }
        let nameLower = name.lowercased()
        if nameLower.contains("north") { return "NLACRC" }
        if nameLower.contains("south central") { return "SCLARC" }
        if nameLower.contains("eastern") { return "ELARC" }
        if nameLower.contains("westside") { return "WRC" }
        if nameLower.contains("harbor") { return "HRC" }
        if nameLower.contains("san gabriel") { return "SGPRC" }
        if nameLower.contains("lanterman") { return "FDLRC" }
        return String(name.prefix(4)).uppercased()
    }

    var fillColor: Color { color }
    var strokeColor: Color { color }

    var centerCoordinate: CLLocationCoordinate2D? {
        // First priority: use explicit office coordinate
        if let coord = officeCoordinate {
            return coord
        }

        // Calculate centroid from polygon
        guard let firstPolygon = allPolygons.first,
              let firstRing = firstPolygon.first,
              !firstRing.isEmpty else { return nil }

        var sumLat = 0.0
        var sumLng = 0.0
        for point in firstRing {
            sumLng += point[0]
            sumLat += point[1]
        }
        return CLLocationCoordinate2D(
            latitude: sumLat / Double(firstRing.count),
            longitude: sumLng / Double(firstRing.count)
        )
    }

    /// Returns the first/main polygon for simple display
    var mapPolygon: MKPolygon? {
        guard let firstPolygon = allPolygons.first,
              let ring = firstPolygon.first,
              ring.count >= 3 else { return nil }

        let coordinates = ring.map { point -> CLLocationCoordinate2D in
            CLLocationCoordinate2D(latitude: point[1], longitude: point[0])
        }

        return MKPolygon(coordinates: coordinates, count: coordinates.count)
    }

    /// Returns all polygons for complete coverage
    var allMapPolygons: [MKPolygon] {
        var polygons: [MKPolygon] = []

        for polygon in allPolygons {
            guard let ring = polygon.first, ring.count >= 3 else { continue }

            let coordinates = ring.map { point -> CLLocationCoordinate2D in
                CLLocationCoordinate2D(latitude: point[1], longitude: point[0])
            }

            polygons.append(MKPolygon(coordinates: coordinates, count: coordinates.count))
        }

        return polygons
    }

    // Hashable
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    static func == (lhs: ServiceAreaFeature, rhs: ServiceAreaFeature) -> Bool {
        lhs.id == rhs.id
    }
}

#Preview {
    RegionalCenterMapView()
        .environmentObject(AppState())
}
