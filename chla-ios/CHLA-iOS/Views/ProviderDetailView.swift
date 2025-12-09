//
//  ProviderDetailView.swift
//  CHLA-iOS
//
//  Detailed view of a single provider
//

import SwiftUI
import MapKit

struct ProviderDetailView: View {
    let provider: Provider
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @ObservedObject var visibilityManager = UIVisibilityManager.shared
    @State private var showFullMap = false
    @State private var showDirections = false
    @State private var lastDragValue: CGFloat = 0

    // Cache regional center lookup to avoid repeated calculations
    private var cachedRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo? {
        provider.regionalCenter
    }

    private var rcColor: Color {
        guard let rc = cachedRegionalCenter else { return .accentBlue }
        switch rc.color {
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
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Header
                headerSection

                Divider()

                // Quick actions
                quickActionsSection

                Divider()

                // Map Section
                mapSection

                Divider()

                // Contact Info
                contactSection

                // Regional Center
                if cachedRegionalCenter != nil {
                    Divider()
                    regionalCenterSection
                }

                if let description = provider.description, !description.isEmpty {
                    Divider()
                    aboutSection(description)
                }

                if let therapies = provider.therapyTypes, !therapies.isEmpty {
                    Divider()
                    servicesSection(therapies)
                }

                if let diagnoses = provider.diagnosesTreated, !diagnoses.isEmpty {
                    Divider()
                    diagnosesSection(diagnoses)
                }

                if let ageGroups = provider.ageGroups, !ageGroups.isEmpty {
                    Divider()
                    ageGroupsSection(ageGroups)
                }

                if let insurance = provider.insuranceAccepted, !insurance.isEmpty {
                    Divider()
                    insuranceSection(insurance)
                }
            }
            .padding()
        }
        .background(Color(.systemBackground))
        .navigationTitle("Resource Details")
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
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                ShareLink(
                    item: shareText,
                    subject: Text(provider.name),
                    message: Text("Check out this resource I found on NDD Resources")
                ) {
                    Image(systemName: "square.and.arrow.up")
                }
            }
        }
        .sheet(isPresented: $showFullMap) {
            FullMapView(
                title: provider.name,
                coordinate: provider.coordinate,
                address: provider.formattedAddress
            )
            .interactiveDismissDisabled(false)
        }
        .sheet(isPresented: $showDirections) {
            DirectionsMapView(
                destinationName: provider.name,
                destinationCoordinate: provider.coordinate,
                destinationAddress: provider.formattedAddress
            )
        }
    }

    // MARK: - Sections

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Regional center badge at top
            if let rc = cachedRegionalCenter {
                HStack {
                    Circle()
                        .fill(rcColor)
                        .frame(width: 12, height: 12)

                    Text(rc.shortName)
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(rcColor)
                }
            }

            Text(provider.name)
                .font(.title2)
                .fontWeight(.bold)

            if let type = provider.type, !type.isEmpty {
                Text(type)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            if provider.distance != nil {
                HStack(spacing: 4) {
                    Image(systemName: "location.fill")
                        .font(.caption)
                    Text(provider.distanceFormatted)
                        .font(.subheadline)
                }
                .foregroundColor(.accentBlue)
            }
        }
    }

    private var quickActionsSection: some View {
        HStack(spacing: 16) {
            if let phoneURL = provider.phoneURL {
                ActionButton(
                    icon: "phone.fill",
                    title: "Call",
                    color: .green
                ) {
                    openURL(phoneURL)
                }
            }

            if let emailURL = provider.emailURL {
                ActionButton(
                    icon: "envelope.fill",
                    title: "Email",
                    color: .blue
                ) {
                    openURL(emailURL)
                }
            }

            if let websiteURL = provider.websiteURL {
                ActionButton(
                    icon: "globe",
                    title: "Website",
                    color: .purple
                ) {
                    openURL(websiteURL)
                }
            }

            // Directions - opens in-app map
            ActionButton(
                icon: "arrow.triangle.turn.up.right.diamond.fill",
                title: "Directions",
                color: .orange
            ) {
                showDirections = true
            }
        }
    }

    private var mapSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Location", systemImage: "mappin.circle.fill")
                .font(.headline)

            // Map preview - tap to open full map
            Button {
                showFullMap = true
            } label: {
                Map(initialPosition: .region(MKCoordinateRegion(
                    center: provider.coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
                ))) {
                    Marker(provider.name, coordinate: provider.coordinate)
                        .tint(Color.accentBlue)
                }
                .frame(height: 180)
                .cornerRadius(12)
                .allowsHitTesting(false)
                .overlay(alignment: .topTrailing) {
                    Image(systemName: "arrow.up.left.and.arrow.down.right")
                        .font(.caption)
                        .padding(8)
                        .background(.ultraThinMaterial)
                        .cornerRadius(8)
                        .padding(8)
                }
            }
            .buttonStyle(.plain)
        }
    }

    private var contactSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Contact Information", systemImage: "info.circle.fill")
                .font(.headline)

            // Address
            Button {
                showFullMap = true
            } label: {
                HStack {
                    Image(systemName: "mappin.circle.fill")
                        .foregroundColor(.accentBlue)
                        .frame(width: 24)

                    Text(provider.formattedAddress)
                        .font(.body)
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.leading)

                    Spacer()

                    Image(systemName: "chevron.right")
                        .foregroundStyle(.secondary)
                        .font(.caption)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
            }
            .buttonStyle(.plain)

            // Phone
            if let phone = provider.formattedPhone {
                Button {
                    if let url = provider.phoneURL {
                        openURL(url)
                    }
                } label: {
                    HStack {
                        Image(systemName: "phone.fill")
                            .foregroundColor(.green)
                            .frame(width: 24)

                        Text(phone)
                            .font(.body)
                            .foregroundColor(.primary)

                        Spacer()

                        Text("Call")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.green)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.green.opacity(0.1))
                            .cornerRadius(8)
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }

            // Email
            if let email = provider.email, !email.isEmpty {
                Button {
                    if let url = provider.emailURL {
                        openURL(url)
                    }
                } label: {
                    HStack {
                        Image(systemName: "envelope.fill")
                            .foregroundColor(.blue)
                            .frame(width: 24)

                        Text(email)
                            .font(.body)
                            .foregroundColor(.primary)
                            .lineLimit(1)

                        Spacer()

                        Text("Email")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.blue)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(8)
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }

            // Website
            if let displayWebsite = provider.displayWebsite {
                Button {
                    if let url = provider.websiteURL {
                        openURL(url)
                    }
                } label: {
                    HStack {
                        Image(systemName: "globe")
                            .foregroundColor(.purple)
                            .frame(width: 24)

                        Text(displayWebsite)
                            .font(.body)
                            .foregroundColor(.primary)
                            .lineLimit(1)

                        Spacer()

                        Text("Visit")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.purple)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.purple.opacity(0.1))
                            .cornerRadius(8)
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var regionalCenterSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Regional Center", systemImage: "building.2.fill")
                .font(.headline)

            if let rc = cachedRegionalCenter {
                VStack(alignment: .leading, spacing: 12) {
                    // RC Badge and Name
                    HStack {
                        Text(rc.shortName)
                            .font(.caption.bold())
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(rcColor)
                            .cornerRadius(8)

                        Text(rc.name)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .lineLimit(2)
                    }

                    Text("This resource is in the service area of \(rc.shortName).")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    // Contact buttons
                    HStack(spacing: 12) {
                        Button {
                            if let url = URL(string: "tel://\(rc.phone.filter { $0.isNumber })") {
                                openURL(url)
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "phone.fill")
                                    .font(.caption2)
                                Text(rc.phone)
                                    .font(.caption)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color.green.opacity(0.1))
                            .foregroundColor(.green)
                            .cornerRadius(8)
                        }
                        .buttonStyle(.plain)

                        Button {
                            if let url = URL(string: "https://\(rc.website)") {
                                openURL(url)
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "globe")
                                    .font(.caption2)
                                Text(rc.website)
                                    .font(.caption)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(rcColor.opacity(0.1))
                            .foregroundColor(rcColor)
                            .cornerRadius(8)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
                .background(rcColor.opacity(0.08))
                .cornerRadius(12)
            }
        }
    }

    private func aboutSection(_ description: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("About", systemImage: "info.circle.fill")
                .font(.headline)

            Text(description)
                .font(.body)
                .foregroundStyle(.secondary)
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemGray6))
                .cornerRadius(12)
        }
    }

    private func servicesSection(_ therapies: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Services", systemImage: "heart.circle.fill")
                .font(.headline)

            FlowLayout(spacing: 8) {
                ForEach(therapies, id: \.self) { therapy in
                    TagView(text: therapy, color: .accentBlue)
                }
            }
        }
    }

    private func diagnosesSection(_ diagnoses: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Diagnoses Treated", systemImage: "stethoscope")
                .font(.headline)

            FlowLayout(spacing: 8) {
                ForEach(diagnoses, id: \.self) { diagnosis in
                    TagView(text: diagnosis, color: .purple)
                }
            }
        }
    }

    private func ageGroupsSection(_ ageGroups: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Age Groups", systemImage: "person.2.fill")
                .font(.headline)

            FlowLayout(spacing: 8) {
                ForEach(ageGroups, id: \.self) { age in
                    TagView(text: age, color: .orange)
                }
            }
        }
    }

    private func insuranceSection(_ insurance: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Insurance Accepted", systemImage: "creditcard.fill")
                .font(.headline)

            let insuranceList = parseInsurance(insurance)
            if !insuranceList.isEmpty {
                FlowLayout(spacing: 8) {
                    ForEach(insuranceList, id: \.self) { item in
                        TagView(text: item, color: .green)
                    }
                }
            }
        }
    }

    /// Parse insurance string which could be comma-separated, pipe-separated, or JSON
    private func parseInsurance(_ insurance: String) -> [String] {
        // Try to detect JSON array format
        if insurance.hasPrefix("[") {
            // It's likely JSON - try to parse it
            if let data = insurance.data(using: .utf8),
               let array = try? JSONSerialization.jsonObject(with: data) as? [String] {
                return array.map { $0.trimmingCharacters(in: .whitespaces) }
                    .filter { !$0.isEmpty }
            }
        }

        // Try pipe separator first (more specific)
        if insurance.contains("|") {
            return insurance.split(separator: "|")
                .map { String($0).trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
        }

        // Fall back to comma separator
        return insurance.split(separator: ",")
            .map { String($0).trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
    }

    // MARK: - Helpers

    private var shareText: String {
        var text = "\(provider.name)\n\(provider.address)"
        if let phone = provider.formattedPhone {
            text += "\nPhone: \(phone)"
        }
        if let website = provider.displayWebsite {
            text += "\nWebsite: \(website)"
        }
        return text
    }
}

#Preview {
    NavigationStack {
        ProviderDetailView(provider: .mock)
    }
}
