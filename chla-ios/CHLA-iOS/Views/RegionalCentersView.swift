//
//  RegionalCentersView.swift
//  CHLA-iOS
//
//  View for browsing regional centers with full detail pages
//
// swiftlint:disable file_length

import SwiftUI
import MapKit
import CoreLocation

struct RegionalCentersView: View {
    @State private var searchText = ""
    @State private var selectedCenter: RegionalCenterMatcher.RegionalCenterInfo?
    @StateObject private var locationManager = RCLocationManager()

    private let centers = RegionalCenterMatcher.shared.laRegionalCenters

    /// The user's regional center based on their location
    private var userRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo? {
        guard let location = locationManager.userLocation else { return nil }
        return RegionalCenterMatcher.shared.findRegionalCenter(for: location.coordinate)
    }

    /// Other centers (excluding user's center)
    private var otherCenters: [RegionalCenterMatcher.RegionalCenterInfo] {
        guard let userCenter = userRegionalCenter else { return centers }
        return centers.filter { $0.id != userCenter.id }
    }

    var filteredCenters: [RegionalCenterMatcher.RegionalCenterInfo] {
        let centersToFilter = userRegionalCenter != nil ? otherCenters : centers
        if searchText.isEmpty {
            return centersToFilter
        }
        return centersToFilter.filter { center in
            center.name.localizedCaseInsensitiveContains(searchText) ||
            center.shortName.localizedCaseInsensitiveContains(searchText)
        }
    }

    /// Check if user's center matches search
    private var showUserCenter: Bool {
        guard let userCenter = userRegionalCenter else { return false }
        if searchText.isEmpty { return true }
        return userCenter.name.localizedCaseInsensitiveContains(searchText) ||
               userCenter.shortName.localizedCaseInsensitiveContains(searchText)
    }

    var body: some View {
        List {
            // Header section
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

                    // swiftlint:disable:next line_length
                    Text("Regional Centers coordinate services for individuals with developmental disabilities. Each center serves specific areas of LA County.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 8)
            }

            // User's Regional Center (highlighted at top)
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

            // Other Centers list
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
        .navigationTitle("Regional Centers")
        .searchable(text: $searchText, prompt: "Search centers")
        .onAppear {
            locationManager.requestLocation()
        }
    }
}

// MARK: - Location Manager for RC View
class RCLocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published var userLocation: CLLocation?
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer // Don't need high accuracy
    }

    func requestLocation() {
        switch manager.authorizationStatus {
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            manager.requestLocation()
        default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        userLocation = locations.first
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location error: \(error.localizedDescription)")
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        if manager.authorizationStatus == .authorizedWhenInUse ||
           manager.authorizationStatus == .authorizedAlways {
            manager.requestLocation()
        }
    }
}

// MARK: - User's RC Row (Highlighted)
struct UserRCRow: View {
    let center: RegionalCenterMatcher.RegionalCenterInfo

    private var centerColor: Color {
        switch center.color {
        case "orange": return .orange
        case "blue": return .blue
        case "purple": return .purple
        case "green": return .green
        case "teal": return .teal
        case "red": return .red
        case "indigo": return .indigo
        default: return .accentBlue
        }
    }

    var body: some View {
        HStack(spacing: 14) {
            // Highlighted icon
            ZStack {
                Circle()
                    .fill(centerColor.opacity(0.15))
                    .frame(width: 44, height: 44)

                Image(systemName: "building.2.fill")
                    .font(.title3)
                    .foregroundColor(centerColor)
            }

            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Text(center.shortName)
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(centerColor)
                        .cornerRadius(6)

                    Image(systemName: "checkmark.circle.fill")
                        .font(.caption)
                        .foregroundColor(.green)
                }

                Text(center.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(2)

                Text(center.phone)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()
        }
        .padding(.vertical, 8)
    }
}

// MARK: - RC List Row
struct RCListRow: View {
    let center: RegionalCenterMatcher.RegionalCenterInfo

    private var centerColor: Color {
        switch center.color {
        case "orange": return .orange
        case "blue": return .blue
        case "purple": return .purple
        case "green": return .green
        case "teal": return .teal
        case "red": return .red
        case "indigo": return .indigo
        default: return .accentBlue
        }
    }

    var body: some View {
        HStack(spacing: 14) {
            // Color indicator
            Circle()
                .fill(centerColor)
                .frame(width: 12, height: 12)

            VStack(alignment: .leading, spacing: 4) {
                Text(center.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)

                HStack(spacing: 8) {
                    Text(center.shortName)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(centerColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(centerColor.opacity(0.12))
                        .cornerRadius(6)

                    Text(center.phone)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()
        }
        .padding(.vertical, 6)
    }
}

// MARK: - Regional Center Detail Sheet (Full Implementation)
// swiftlint:disable:next type_body_length
struct RegionalCenterDetailSheet: View {
    let center: RegionalCenterMatcher.RegionalCenterInfo
    @Environment(\.openURL) private var openURL
    @ObservedObject var visibilityManager = UIVisibilityManager.shared
    @State private var showFullMap = false
    @State private var showDirections = false
    @State private var zipSearch = ""
    @State private var zipSearchResult: ZipSearchResult?
    @State private var isCheckingZip = false
    @State private var lastDragValue: CGFloat = 0

    private var centerColor: Color {
        switch center.color {
        case "orange": return .orange
        case "blue": return .blue
        case "purple": return .purple
        case "green": return .green
        case "teal": return .teal
        case "red": return .red
        case "indigo": return .indigo
        default: return .accentBlue
        }
    }

    private var centerData: RCDetailData {
        RCDetailData.getData(for: center.shortName.lowercased())
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Hero Header
                heroSection

                VStack(alignment: .leading, spacing: 24) {
                    // Stats Row
                    statsSection

                    // ZIP Code Search
                    zipSearchSection

                    Divider()

                    // About Section
                    aboutSection

                    Divider()

                    // Services Section
                    servicesSection

                    Divider()

                    // Quick Actions
                    quickActionsSection

                    Divider()

                    // Contact Card
                    contactSection

                    Divider()

                    // Cities Served
                    citiesSection

                    Divider()

                    // How to Access Services
                    howToAccessSection

                    Divider()

                    // Other Regional Centers
                    otherCentersSection
                }
                .padding()
            }
        }
        .navigationTitle(center.shortName)
        .navigationBarTitleDisplayMode(.inline)
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
        .sheet(isPresented: $showFullMap) {
            FullMapView(
                title: center.name,
                coordinate: center.coordinate,
                address: centerData.address
            )
        }
        .sheet(isPresented: $showDirections) {
            DirectionsMapView(
                destinationName: center.name,
                destinationCoordinate: center.coordinate,
                destinationAddress: centerData.address
            )
        }
    }

    // MARK: - Hero Section
    private var heroSection: some View {
        ZStack(alignment: .bottomLeading) {
            // Gradient background
            LinearGradient(
                colors: [centerColor, centerColor.opacity(0.7)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .frame(height: 200)

            // Content
            VStack(alignment: .leading, spacing: 12) {
                // Badge
                Text(center.shortName)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(centerColor)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.white)
                    .cornerRadius(20)

                Text(center.name)
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)

                Text(centerData.tagline)
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.9))
            }
            .padding()
            .padding(.bottom, 8)
        }
    }

    // MARK: - Stats Section
    private var statsSection: some View {
        HStack(spacing: 12) {
            StatBubble(
                value: "\(centerData.providerCount)+",
                label: "Resources",
                icon: "person.2.fill",
                color: centerColor
            )
            StatBubble(
                value: "\(centerData.citiesServed.count)",
                label: "Cities",
                icon: "building.2.fill",
                color: .orange
            )
            StatBubble(
                value: centerData.residentsServed,
                label: "Residents",
                icon: "heart.fill",
                color: .pink
            )
        }
    }

    // MARK: - ZIP Search Section
    private var zipSearchSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Check Your Coverage")
                .font(.headline)

            HStack(spacing: 12) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)

                    TextField("Enter ZIP code", text: $zipSearch)
                        .keyboardType(.numberPad)
                        .textContentType(.postalCode)
                        .onChange(of: zipSearch) { _, newValue in
                            // Filter to digits only and limit to 5
                            let filtered = newValue.filter { $0.isNumber }
                            if filtered != newValue || filtered.count > 5 {
                                zipSearch = String(filtered.prefix(5))
                            }
                            // Clear result when user types
                            if zipSearchResult != nil && newValue.count < 5 {
                                zipSearchResult = nil
                            }
                        }
                        .onSubmit { checkZipCode() }

                    // Clear button
                    if !zipSearch.isEmpty {
                        Button {
                            zipSearch = ""
                            zipSearchResult = nil
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)

                // Check button
                Button {
                    checkZipCode()
                    hideKeyboard()
                } label: {
                    Group {
                        if isCheckingZip {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("Check")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                        }
                    }
                    .foregroundColor(.white)
                    .frame(width: 70)
                    .padding(.vertical, 14)
                    .background(zipSearch.count == 5 && !isCheckingZip ? centerColor : Color.gray)
                    .cornerRadius(12)
                }
                .disabled(zipSearch.count != 5 || isCheckingZip)
            }

            // Search Result
            if let result = zipSearchResult {
                // If it's another RC, make it tappable
                if case .otherRC(let rcShortName) = result.type,
                   let targetRC = findRCByShortName(rcShortName) {
                    NavigationLink {
                        RegionalCenterDetailSheet(center: targetRC)
                    } label: {
                        zipResultRow(result: result)
                    }
                    .buttonStyle(.plain)
                } else {
                    zipResultRow(result: result)
                }
            }
        }
    }

    private func checkZipCode() {
        let zip = zipSearch.trimmingCharacters(in: .whitespaces)

        // Validate input
        guard !zip.isEmpty else {
            zipSearchResult = ZipSearchResult(
                type: .invalidInput,
                message: "Please enter a ZIP code"
            )
            return
        }

        guard zip.count == 5 else {
            zipSearchResult = ZipSearchResult(
                type: .invalidInput,
                message: "ZIP code must be 5 digits"
            )
            return
        }

        guard zip.allSatisfy({ $0.isNumber }) else {
            zipSearchResult = ZipSearchResult(
                type: .invalidInput,
                message: "ZIP code must contain only numbers"
            )
            return
        }

        // Call API to check ZIP code
        isCheckingZip = true
        Task {
            do {
                let rcFromAPI = try await APIService.shared.getRegionalCenterByZip(zipCode: zip)

                await MainActor.run {
                    isCheckingZip = false

                    // Check if this RC matches the current one we're viewing
                    let apiRCName = rcFromAPI.regionalCenter
                    let currentRCName = center.name

                    if apiRCName.lowercased().contains(center.shortName.lowercased()) ||
                       currentRCName.lowercased().contains(apiRCName.lowercased()) ||
                       apiRCName == currentRCName {
                        // ZIP is in this RC
                        zipSearchResult = ZipSearchResult(
                            type: .served,
                            message: "✓ ZIP \(zip) is served by \(center.shortName)!"
                        )
                    } else {
                        // ZIP is in a different RC
                        let shortName = extractShortName(from: apiRCName)
                        zipSearchResult = ZipSearchResult(
                            type: .otherRC(shortName),
                            message: "ZIP \(zip) is served by \(shortName)"
                        )
                    }
                }
            } catch {
                await MainActor.run {
                    isCheckingZip = false
                    // API returned 404 or error - ZIP not found
                    zipSearchResult = ZipSearchResult(
                        type: .notFound,
                        message: "ZIP \(zip) not found in LA County"
                    )
                }
            }
        }
    }

    private func extractShortName(from fullName: String) -> String {
        // Extract short name from full RC name
        let mappings: [String: String] = [
            "San Gabriel": "SGPRC",
            "Pomona": "SGPRC",
            "Eastern Los Angeles": "ELARC",
            "Eastern LA": "ELARC",
            "North Los Angeles": "NLACRC",
            "North LA": "NLACRC",
            "South Central": "SCLARC",
            "Harbor": "HRC",
            "Westside": "WRC",
            "Lanterman": "FDLRC",
            "Frank D. Lanterman": "FDLRC"
        ]

        for (keyword, shortName) in mappings {
            if fullName.contains(keyword) {
                return shortName
            }
        }
        return fullName
    }

    private func hideKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil, from: nil, for: nil
        )
    }

    @ViewBuilder
    private func zipResultRow(result: ZipSearchResult) -> some View {
        HStack(spacing: 10) {
            Image(systemName: result.icon)
                .font(.title3)
                .foregroundColor(result.color)

            VStack(alignment: .leading, spacing: 2) {
                Text(result.message)
                    .font(.subheadline)
                    .foregroundColor(.primary)

                // Show helpful next step
                if case .otherRC(let rcName) = result.type {
                    Text("View \(rcName) for more information")
                        .font(.caption)
                        .foregroundColor(.secondary)
                } else if case .notFound = result.type {
                    Text("This ZIP may be outside LA County")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            // Chevron for other RC (tappable)
            if case .otherRC = result.type {
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(result.color.opacity(0.1))
        .cornerRadius(12)
        .transition(.opacity.combined(with: .move(edge: .top)))
        .animation(.easeInOut(duration: 0.2), value: zipSearchResult?.message)
    }

    private func findRCByShortName(_ shortName: String) -> RegionalCenterMatcher.RegionalCenterInfo? {
        let allCenters = RegionalCenterMatcher.shared.laRegionalCenters
        return allCenters.first { center in
            center.shortName.uppercased() == shortName.uppercased()
        }
    }

    // MARK: - About Section
    private var aboutSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("About", systemImage: "info.circle.fill")
                .font(.headline)

            Text(centerData.about)
                .font(.body)
                .foregroundColor(.secondary)
                .lineSpacing(4)

            // Service highlights
            FlowLayout(spacing: 8) {
                ForEach(centerData.serviceHighlights, id: \.self) { highlight in
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption2)
                            .foregroundColor(.green)
                        Text(highlight)
                            .font(.caption)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.green.opacity(0.1))
                    .cornerRadius(16)
                }
            }
        }
    }

    // MARK: - Services Section
    private var servicesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Services Offered", systemImage: "list.bullet.clipboard.fill")
                .font(.headline)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(centerData.services, id: \.self) { service in
                    HStack(spacing: 8) {
                        Image(systemName: serviceIcon(for: service))
                            .font(.body)
                            .foregroundColor(centerColor)
                            .frame(width: 24)

                        Text(service)
                            .font(.caption)
                            .lineLimit(2)
                            .minimumScaleFactor(0.8)

                        Spacer()
                    }
                    .padding(10)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                }
            }
        }
    }

    private func serviceIcon(for service: String) -> String {
        let lowercased = service.lowercased()
        if lowercased.contains("early") { return "figure.and.child.holdinghands" }
        if lowercased.contains("employ") { return "briefcase.fill" }
        if lowercased.contains("living") { return "house.fill" }
        if lowercased.contains("respite") { return "person.2.fill" }
        if lowercased.contains("therapy") || lowercased.contains("behavioral") { return "brain.head.profile" }
        if lowercased.contains("transition") { return "arrow.right.circle.fill" }
        if lowercased.contains("day") { return "sun.max.fill" }
        if lowercased.contains("transport") { return "car.fill" }
        if lowercased.contains("crisis") { return "exclamationmark.triangle.fill" }
        return "checkmark.circle.fill"
    }

    // MARK: - Quick Actions
    private var quickActionsSection: some View {
        HStack(spacing: 16) {
            ActionButton(icon: "phone.fill", title: "Call", color: .green) {
                let digits = center.phone.replacingOccurrences(
                    of: "[^0-9]", with: "", options: .regularExpression
                )
                if let url = URL(string: "tel:\(digits)") { openURL(url) }
            }

            ActionButton(icon: "globe", title: "Website", color: centerColor) {
                var urlString = center.website
                if !urlString.hasPrefix("http") { urlString = "https://\(urlString)" }
                if let url = URL(string: urlString) { openURL(url) }
            }

            ActionButton(icon: "map.fill", title: "Map", color: .orange) {
                showFullMap = true
            }

            ActionButton(icon: "arrow.triangle.turn.up.right.diamond.fill", title: "Directions", color: .blue) {
                showDirections = true
            }
        }
    }

    // MARK: - Contact Section
    private var contactSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Contact Information", systemImage: "person.crop.circle.fill")
                .font(.headline)

            VStack(spacing: 0) {
                ContactRow(
                    icon: "phone.fill", iconColor: .green,
                    label: "Phone", value: center.phone
                ) {
                    let digits = center.phone.replacingOccurrences(
                        of: "[^0-9]", with: "", options: .regularExpression
                    )
                    if let url = URL(string: "tel:\(digits)") { openURL(url) }
                }
                Divider().padding(.leading, 44)

                ContactRow(
                    icon: "globe", iconColor: centerColor,
                    label: "Website", value: center.website
                ) {
                    var urlString = center.website
                    if !urlString.hasPrefix("http") { urlString = "https://\(urlString)" }
                    if let url = URL(string: urlString) { openURL(url) }
                }
                Divider().padding(.leading, 44)

                ContactRow(
                    icon: "mappin.circle.fill", iconColor: .red,
                    label: "Address", value: centerData.address
                ) {
                    showFullMap = true
                }
            }
            .background(Color(.systemGray6))
            .cornerRadius(12)
        }
    }

    // MARK: - Cities Section
    private var citiesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("Cities & Communities", systemImage: "building.2.crop.circle.fill")
                    .font(.headline)
                Spacer()
                Text("\(centerData.citiesServed.count) areas")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            FlowLayout(spacing: 8) {
                ForEach(centerData.citiesServed, id: \.self) { city in
                    Text(city)
                        .font(.caption)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(centerColor.opacity(0.1))
                        .foregroundColor(centerColor)
                        .cornerRadius(16)
                }
            }
        }
    }

    // MARK: - How to Access Section
    private var howToAccessSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("How to Access Services", systemImage: "questionmark.circle.fill")
                .font(.headline)

            VStack(alignment: .leading, spacing: 16) {
                AccessStep(
                    number: 1,
                    title: "Contact Your Regional Center",
                    description: "Call \(center.shortName) at \(center.phone) to request an intake."
                )
                AccessStep(
                    number: 2,
                    title: "Complete Intake Process",
                    description: "Provide diagnosis, medical records, and identification."
                )
                AccessStep(
                    number: 3,
                    title: "Eligibility Determination",
                    description: "A coordinator will assess eligibility based on DD criteria."
                )
                AccessStep(
                    number: 4,
                    title: "Develop Individual Plan",
                    description: "Work with your coordinator to create an IPP."
                )
            }
        }
    }

    // MARK: - Other Centers Section
    private var otherCentersSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Other Regional Centers", systemImage: "building.2.fill")
                .font(.headline)

            let otherCenters = RegionalCenterMatcher.shared.laRegionalCenters.filter { $0.id != center.id }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(otherCenters, id: \.id) { otherCenter in
                        NavigationLink {
                            RegionalCenterDetailSheet(center: otherCenter)
                        } label: {
                            OtherCenterCard(center: otherCenter)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

// MARK: - ZIP Search Result
struct ZipSearchResult {
    enum ResultType {
        case served           // ZIP is in this RC
        case otherRC(String)  // ZIP is in a different RC (with RC name)
        case notFound         // ZIP not found in any LA County RC
        case invalidInput     // Invalid ZIP format
    }

    let type: ResultType
    let message: String

    var isServed: Bool {
        if case .served = type { return true }
        return false
    }

    var icon: String {
        switch type {
        case .served: return "checkmark.circle.fill"
        case .otherRC: return "arrow.right.circle.fill"
        case .notFound: return "questionmark.circle.fill"
        case .invalidInput: return "exclamationmark.circle.fill"
        }
    }

    var color: Color {
        switch type {
        case .served: return .green
        case .otherRC: return .orange
        case .notFound: return .gray
        case .invalidInput: return .red
        }
    }
}

// MARK: - Stat Bubble
struct StatBubble: View {
    let value: String
    let label: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(color.opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - Contact Row
struct ContactRow: View {
    let icon: String
    let iconColor: Color
    let label: String
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .foregroundColor(iconColor)
                    .frame(width: 24)

                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(value)
                        .font(.subheadline)
                        .foregroundColor(.primary)
                        .lineLimit(2)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Access Step
struct AccessStep: View {
    let number: Int
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color.accentBlue)
                    .frame(width: 28, height: 28)
                Text("\(number)")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineSpacing(2)
            }
        }
    }
}

// MARK: - Other Center Card
struct OtherCenterCard: View {
    let center: RegionalCenterMatcher.RegionalCenterInfo

    private var cardColor: Color {
        switch center.color {
        case "orange": return .orange
        case "blue": return .blue
        case "purple": return .purple
        case "green": return .green
        case "teal": return .teal
        case "red": return .red
        case "indigo": return .indigo
        default: return .accentBlue
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(center.shortName)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(cardColor)
                .cornerRadius(6)

            Text(center.name)
                .font(.caption)
                .fontWeight(.medium)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            Text(center.phone)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(width: 140, alignment: .leading)
        .padding(12)
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

// MARK: - Regional Center Detail Data
// swiftlint:disable:next type_body_length
struct RCDetailData {
    let tagline: String
    let about: String
    let address: String
    let providerCount: Int
    let residentsServed: String
    let services: [String]
    let serviceHighlights: [String]
    let citiesServed: [String]
    let zipCodes: [String]

    static func getData(for centerId: String) -> RCDetailData {
        // Common services offered by all regional centers
        let commonServices = [
            "Early Intervention", "Day Programs", "Employment Services",
            "Independent Living", "Respite Care", "Behavioral Services",
            "Transportation", "Family Support"
        ]

        switch centerId {
        case "sgprc":
            return sgprcData()
        case "elarc":
            return elarcData()
        case "nlacrc":
            return nlacrcData()
        case "sclarc":
            return sclarcData()
        case "fdlrc":
            return fdlrcData()
        case "hrc":
            return hrcData()
        case "wrc":
            return wrcData()
        default:
            return RCDetailData(
                tagline: "Serving individuals with developmental disabilities",
                about: """
                    This Regional Center provides comprehensive services to \
                    individuals with developmental disabilities and their \
                    families.
                    """,
                address: "Contact regional center for address",
                providerCount: 200,
                residentsServed: "30,000+",
                services: commonServices,
                serviceHighlights: [
                    "Early Start Program", "Employment Support",
                    "Community Living", "Family Resources"
                ],
                citiesServed: ["Contact regional center for service areas"],
                zipCodes: []
            )
        }
    }

    private static func sgprcData() -> RCDetailData {
        RCDetailData(
            tagline: "Serving the San Gabriel Valley and Pomona communities",
            about: """
                San Gabriel/Pomona Regional Center provides lifelong services \
                and support to individuals with developmental disabilities \
                and their families in the eastern San Gabriel Valley and \
                Pomona areas. We coordinate services including early \
                intervention, day programs, employment support, and more.
                """,
            address: "75 Rancho Camino Drive, Pomona, CA 91766",
            providerCount: 245,
            residentsServed: "38,000+",
            services: [
                "Early Intervention", "Day Programs", "Employment Services",
                "Independent Living", "Respite Care", "Behavioral Services",
                "Transportation", "Crisis Support"
            ],
            serviceHighlights: [
                "Early Start Program", "Supported Employment",
                "Independent Living", "Family Support"
            ],
            citiesServed: [
                "Pomona", "Claremont", "La Verne", "San Dimas", "Glendora",
                "Azusa", "Covina", "West Covina", "Diamond Bar", "Walnut",
                "Rowland Heights", "Hacienda Heights", "Industry",
                "Baldwin Park", "Irwindale", "Duarte", "Monrovia", "Arcadia",
                "Temple City", "El Monte", "South El Monte"
            ],
            zipCodes: [
                "91766", "91767", "91768", "91711", "91750", "91773", "91740",
                "91702", "91722", "91723", "91724", "91765", "91789", "91748",
                "91745", "91744", "91746", "91706", "91010", "91016", "91017",
                "91007", "91006", "91775", "91776", "91731", "91732", "91733"
            ]
        )
    }

    private static func elarcData() -> RCDetailData {
        RCDetailData(
            tagline: "Supporting individuals and families in Eastern LA",
            about: """
                Eastern Los Angeles Regional Center serves individuals with \
                developmental disabilities in the eastern portion of Los \
                Angeles County. We provide comprehensive services to support \
                independence, inclusion, and quality of life.
                """,
            address: "1000 S. Fremont Ave., Alhambra, CA 91803",
            providerCount: 312,
            residentsServed: "42,000+",
            services: [
                "Early Intervention", "Day Programs", "Employment Services",
                "Residential Services", "Respite Care", "Behavioral Health",
                "Transition Services", "Family Support"
            ],
            serviceHighlights: [
                "Early Start Program", "Self-Determination",
                "Employment First", "Community Integration"
            ],
            citiesServed: [
                "Alhambra", "Monterey Park", "Rosemead", "San Gabriel",
                "South Pasadena", "Pasadena", "Altadena", "Sierra Madre",
                "Montebello", "Commerce", "Bell Gardens", "Pico Rivera",
                "Whittier", "La Mirada", "Santa Fe Springs", "Norwalk"
            ],
            zipCodes: [
                "91801", "91803", "91754", "91755", "91770", "91775", "91776",
                "91030", "91101", "91103", "91104", "91106", "91001", "91024",
                "91640", "90640", "90201", "90660", "90601", "90602", "90603",
                "90604", "90638", "90650"
            ]
        )
    }

    private static func nlacrcData() -> RCDetailData {
        RCDetailData(
            tagline: "Empowering lives in North Los Angeles County",
            about: """
                North Los Angeles County Regional Center serves individuals \
                with developmental disabilities in northern LA County, \
                including the San Fernando, Antelope, and Santa Clarita \
                Valleys. We support individuals to live, work, and \
                participate fully in their communities.
                """,
            address: "15400 Sherman Way, Suite 170, Van Nuys, CA 91406",
            providerCount: 428,
            residentsServed: "52,000+",
            services: [
                "Early Intervention", "Day Programs", "Employment Services",
                "Independent Living", "Respite Care", "Behavioral Services",
                "Transportation", "Transition Services"
            ],
            serviceHighlights: [
                "Early Start", "Supported Living",
                "Competitive Employment", "Person-Centered Planning"
            ],
            citiesServed: [
                "Van Nuys", "North Hollywood", "Sherman Oaks", "Encino",
                "Tarzana", "Woodland Hills", "Canoga Park", "Chatsworth",
                "Northridge", "Granada Hills", "Sylmar", "San Fernando",
                "Pacoima", "Sunland", "Tujunga", "Lancaster", "Palmdale",
                "Santa Clarita", "Valencia", "Newhall"
            ],
            zipCodes: [
                "91401", "91405", "91406", "91411", "91601", "91602", "91604",
                "91605", "91606", "91423", "91316", "91356", "91367", "91364",
                "91303", "91304", "91311", "91324", "91325", "91326", "91342",
                "91340", "91331", "91040", "91042", "93534", "93535", "93536",
                "91350", "91354", "91355", "91321"
            ]
        )
    }

    private static func sclarcData() -> RCDetailData {
        RCDetailData(
            tagline: "Serving South Central Los Angeles communities",
            about: """
                South Central Los Angeles Regional Center provides services \
                to individuals with developmental disabilities in South \
                Central LA. We ensure people with disabilities have the \
                opportunity to live fulfilling lives as valued community \
                members.
                """,
            address: "2500 S. Western Ave., Los Angeles, CA 90018",
            providerCount: 198,
            residentsServed: "28,000+",
            services: [
                "Early Intervention", "Day Programs", "Employment Services",
                "Residential Services", "Respite Care", "Behavioral Health",
                "Crisis Services", "Family Support"
            ],
            serviceHighlights: [
                "Early Start", "Community Living",
                "Employment Support", "Cultural Competency"
            ],
            citiesServed: [
                "South Los Angeles", "Inglewood", "Hawthorne", "Gardena",
                "Compton", "Lynwood", "South Gate", "Huntington Park",
                "Florence-Firestone", "Willowbrook", "Athens", "Westmont"
            ],
            zipCodes: [
                // South Central LA
                "90001", "90002", "90003", "90007", "90008", "90011", "90016",
                "90018", "90037", "90043", "90044", "90047", "90059", "90061",
                "90062", "90089",
                // Inglewood / Hawthorne / Gardena (some overlap with HRC)
                "90250", "90301", "90302", "90303", "90304", "90305",
                // Compton / Lynwood
                "90220", "90221", "90222", "90247", "90248", "90262",
                // South Gate / Huntington Park
                "90255", "90280", "90201", "90270"
            ]
        )
    }

    private static func fdlrcData() -> RCDetailData {
        RCDetailData(
            tagline: "Supporting individuals in LA and surrounding areas",
            about: """
                Frank D. Lanterman Regional Center serves individuals with \
                developmental disabilities in Los Angeles and surrounding \
                areas. We are dedicated to supporting individuals and \
                families through quality services that promote independence \
                and community participation.
                """,
            address: "3303 Wilshire Blvd., Suite 700, Los Angeles, CA 90010",
            providerCount: 356,
            residentsServed: "35,000+",
            services: [
                "Early Intervention", "Day Programs", "Employment Services",
                "Independent Living", "Respite Care", "Behavioral Services",
                "Residential Services", "Transition Services"
            ],
            serviceHighlights: [
                "Early Start", "Supported Employment",
                "Self-Advocacy", "Family Resource Center"
            ],
            citiesServed: [
                "Los Angeles", "Glendale", "Burbank", "La Cañada",
                "Eagle Rock", "Highland Park", "Mt. Washington",
                "Atwater Village", "Silver Lake", "Echo Park",
                "Los Feliz", "Griffith Park"
            ],
            zipCodes: [
                // Central LA / Koreatown / Wilshire
                "90004", "90005", "90006", "90010", "90017", "90019", "90020",
                "90026", "90027", "90028", "90029", "90036", "90038", "90057",
                // Glendale / Burbank / La Cañada
                "91201", "91202", "91203", "91204", "91205", "91206", "91207",
                "91208", "91501", "91502", "91504", "91505", "91506", "91011",
                // Eagle Rock / Highland Park / Northeast LA
                "90039", "90041", "90042", "90065", "90031", "90032", "90033",
                // Pasadena area (shared with SGPRC)
                "91101", "91103", "91104", "91105", "91106", "91107", "91214"
            ]
        )
    }

    private static func hrcData() -> RCDetailData {
        RCDetailData(
            tagline: "Serving the Harbor area and South Bay communities",
            about: """
                Harbor Regional Center provides services and supports to \
                individuals with developmental disabilities living in the \
                Harbor and South Bay areas. Our mission is to empower \
                individuals to achieve their full potential through \
                person-centered services.
                """,
            address: "21231 Hawthorne Blvd., Torrance, CA 90503",
            providerCount: 267,
            residentsServed: "31,000+",
            services: [
                "Early Intervention", "Day Programs", "Employment Services",
                "Independent Living", "Respite Care", "Behavioral Health",
                "Transportation", "Crisis Support"
            ],
            serviceHighlights: [
                "Early Start", "Competitive Employment",
                "Community Living", "Transition Services"
            ],
            citiesServed: [
                "Torrance", "Redondo Beach", "Hermosa Beach", "Manhattan Beach",
                "El Segundo", "Carson", "Lomita", "Palos Verdes",
                "Rolling Hills", "San Pedro", "Wilmington", "Harbor City",
                "Long Beach (West)"
            ],
            zipCodes: [
                "90503", "90504", "90505", "90277", "90278", "90254", "90266",
                "90245", "90745", "90746", "90747", "90710", "90717", "90274",
                "90275", "90731", "90732", "90744", "90810"
            ]
        )
    }

    private static func wrcData() -> RCDetailData {
        RCDetailData(
            tagline: "Serving the Westside and Southern LA County",
            about: """
                Westside Regional Center provides lifelong services to \
                individuals with developmental disabilities on the Westside \
                and in Southern Los Angeles County. We support individuals \
                and families to achieve their personal goals and live \
                meaningful lives in their communities.
                """,
            address: "5901 Green Valley Circle, Suite 320, Culver City, CA 90230",
            providerCount: 289,
            residentsServed: "33,000+",
            services: [
                "Early Intervention", "Day Programs", "Employment Services",
                "Supported Living", "Respite Care", "Behavioral Services",
                "Self-Determination", "Family Support"
            ],
            serviceHighlights: [
                "Early Start", "Supported Living",
                "Self-Determination", "Community Integration"
            ],
            citiesServed: [
                "Culver City", "Santa Monica", "Venice", "Mar Vista",
                "Playa Vista", "Marina del Rey", "Westchester", "Playa del Rey",
                "El Segundo", "Inglewood", "Lennox", "LAX Area",
                "West Los Angeles", "Brentwood", "Pacific Palisades", "Malibu"
            ],
            zipCodes: [
                "90230", "90232", "90401", "90402", "90403", "90404", "90405",
                "90291", "90292", "90066", "90094", "90293", "90045", "90301",
                "90304", "90025", "90049", "90272", "90265"
            ]
        )
    }
}

// MARK: - Section Header (Renamed to avoid conflict)
struct SectionHeader: View {
    let title: String
    let icon: String

    var body: some View {
        Label(title, systemImage: icon)
            .font(.headline)
    }
}

// MARK: - Flow Layout
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) -> CGSize {
        let result = FlowResult(
            in: proposal.replacingUnspecifiedDimensions().width,
            subviews: subviews,
            spacing: spacing
        )
        return result.size
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )
        for (index, subview) in subviews.enumerated() {
            let position = result.positions[index]
            subview.place(
                at: CGPoint(
                    x: bounds.minX + position.x,
                    y: bounds.minY + position.y
                ),
                proposal: .unspecified
            )
        }
    }

    struct FlowResult {
        var positions: [CGPoint] = []
        var size: CGSize = .zero

        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var xPos: CGFloat = 0
            var yPos: CGFloat = 0
            var rowHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if xPos + size.width > maxWidth && xPos > 0 {
                    xPos = 0
                    yPos += rowHeight + spacing
                    rowHeight = 0
                }

                positions.append(CGPoint(x: xPos, y: yPos))
                rowHeight = max(rowHeight, size.height)
                xPos += size.width + spacing
            }

            self.size = CGSize(width: maxWidth, height: yPos + rowHeight)
        }
    }
}

// MARK: - Tag View
struct TagView: View {
    let text: String
    var color: Color = .accentBlue

    var body: some View {
        Text(text)
            .font(.caption)
            .fontWeight(.semibold)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color(.systemBackground).opacity(0.95))
            .foregroundColor(color)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(color.opacity(0.4), lineWidth: 1)
            )
            .cornerRadius(8)
            .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
    }
}

// MARK: - Action Button
struct ActionButton: View {
    let icon: String
    let title: String
    var color: Color = .accentBlue
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                Text(title)
                    .font(.caption)
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(color.opacity(0.1))
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack {
        RegionalCentersView()
    }
}
