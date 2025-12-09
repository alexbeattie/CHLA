//
//  Provider.swift
//  CHLA-iOS
//
//  Provider model matching Django ProviderV2
//

import Foundation
import CoreLocation

/// Healthcare provider model
/// Matches the Django ProviderV2 model structure
struct Provider: Codable, Identifiable, Equatable, Hashable {
    let id: UUID
    let name: String
    let type: String?
    let phone: String?
    let email: String?
    let website: String?
    let description: String?

    // Location
    let latitude: Double
    let longitude: Double
    let address: String

    // Service details
    let insuranceAccepted: String?
    let ageGroups: [String]?
    let diagnosesTreated: [String]?
    let therapyTypes: [String]?

    // Computed distance (set by API when using location-based search)
    var distance: Double?

    // Timestamps
    let createdAt: Date?
    let updatedAt: Date?

    // MARK: - Computed Properties

    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }

    var formattedPhone: String? {
        guard let phone = phone, !phone.isEmpty else { return nil }
        // Basic US phone formatting
        let digits = phone.filter { $0.isNumber }
        if digits.count == 10 {
            let areaCode = digits.prefix(3)
            let prefix = digits.dropFirst(3).prefix(3)
            let line = digits.suffix(4)
            return "(\(areaCode)) \(prefix)-\(line)"
        }
        return phone
    }

    var phoneURL: URL? {
        guard let phone = phone, !phone.isEmpty else { return nil }
        let digits = phone.filter { $0.isNumber }
        guard !digits.isEmpty else { return nil }
        return URL(string: "tel://\(digits)")
    }

    var websiteURL: URL? {
        guard let website = website, !website.isEmpty else { return nil }
        if website.hasPrefix("http") {
            return URL(string: website)
        }
        return URL(string: "https://\(website)")
    }

    /// Clean website display (no http://, https://, or trailing /)
    var displayWebsite: String? {
        guard let website = website, !website.isEmpty else { return nil }
        var clean = website
        clean = clean.replacingOccurrences(of: "https://", with: "")
        clean = clean.replacingOccurrences(of: "http://", with: "")
        clean = clean.replacingOccurrences(of: "www.", with: "")
        if clean.hasSuffix("/") {
            clean = String(clean.dropLast())
        }
        return clean
    }

    var emailURL: URL? {
        guard let email = email, !email.isEmpty else { return nil }
        return URL(string: "mailto:\(email)")
    }

    var mapsURL: URL? {
        let encodedAddress = address.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        return URL(string: "http://maps.apple.com/?address=\(encodedAddress)")
    }

    var directionsURL: URL? {
        URL(string: "http://maps.apple.com/?daddr=\(latitude),\(longitude)")
    }

    var distanceFormatted: String {
        guard let distance = distance else { return "" }
        if distance < 0.1 {
            return "< 0.1 mi"
        } else if distance < 10 {
            return String(format: "%.1f mi", distance)
        } else {
            return String(format: "%.0f mi", distance)
        }
    }

    var servicesFormatted: String {
        var services: [String] = []
        if let types = therapyTypes, !types.isEmpty {
            services.append(contentsOf: types)
        }
        if let diagnosis = diagnosesTreated, !diagnosis.isEmpty {
            services.append(contentsOf: diagnosis)
        }
        return services.prefix(3).joined(separator: " • ")
    }

    /// The Regional Center serving this provider's location
    var regionalCenter: RegionalCenterMatcher.RegionalCenterInfo? {
        // First try to match by zip code from address
        if let center = RegionalCenterMatcher.shared.findRegionalCenter(forAddress: address) {
            return center
        }
        // Fall back to closest by location
        return RegionalCenterMatcher.shared.findRegionalCenter(for: coordinate)
    }

    /// Short name of the serving regional center (e.g., "NLACRC")
    var regionalCenterShortName: String? {
        regionalCenter?.shortName
    }

    /// Full name of the serving regional center
    var regionalCenterName: String? {
        regionalCenter?.name
    }

    /// Formatted address - parses JSON if address is stored as JSON string
    var formattedAddress: String {
        // Check if address looks like JSON
        if address.hasPrefix("{") && address.hasSuffix("}") {
            // Try to parse as JSON
            if let data = address.data(using: .utf8),
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                let street = json["street"] as? String ?? ""
                let city = json["city"] as? String ?? ""
                let state = json["state"] as? String ?? ""
                let zip = json["zip"] as? String ?? ""

                var parts: [String] = []
                if !street.isEmpty { parts.append(street) }
                if !city.isEmpty {
                    var cityStateZip = city
                    if !state.isEmpty { cityStateZip += ", \(state)" }
                    if !zip.isEmpty { cityStateZip += " \(zip)" }
                    parts.append(cityStateZip)
                }
                return parts.joined(separator: "\n")
            }
        }
        // Return as-is if not JSON
        return address
    }

    // MARK: - Coding Keys

    enum CodingKeys: String, CodingKey {
        case id, name, type, phone, email, website, description
        case latitude, longitude, address
        case insuranceAccepted = "insurance_accepted"
        case ageGroups = "age_groups"
        case diagnosesTreated = "diagnoses_treated"
        case therapyTypes = "therapy_types"
        case distance
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }

    // MARK: - Custom Decoder (handles string or number for lat/lng)

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        type = try container.decodeIfPresent(String.self, forKey: .type)
        phone = try container.decodeIfPresent(String.self, forKey: .phone)
        email = try container.decodeIfPresent(String.self, forKey: .email)
        website = try container.decodeIfPresent(String.self, forKey: .website)
        description = try container.decodeIfPresent(String.self, forKey: .description)
        address = try container.decodeIfPresent(String.self, forKey: .address) ?? ""

        // Handle latitude as either Double or String
        if let latDouble = try? container.decode(Double.self, forKey: .latitude) {
            latitude = latDouble
        } else if let latString = try? container.decode(String.self, forKey: .latitude),
                  let latDouble = Double(latString) {
            latitude = latDouble
        } else {
            latitude = 0.0
        }

        // Handle longitude as either Double or String
        if let lngDouble = try? container.decode(Double.self, forKey: .longitude) {
            longitude = lngDouble
        } else if let lngString = try? container.decode(String.self, forKey: .longitude),
                  let lngDouble = Double(lngString) {
            longitude = lngDouble
        } else {
            longitude = 0.0
        }

        insuranceAccepted = try container.decodeIfPresent(String.self, forKey: .insuranceAccepted)
        ageGroups = try container.decodeIfPresent([String].self, forKey: .ageGroups)
        diagnosesTreated = try container.decodeIfPresent([String].self, forKey: .diagnosesTreated)
        therapyTypes = try container.decodeIfPresent([String].self, forKey: .therapyTypes)
        distance = try container.decodeIfPresent(Double.self, forKey: .distance)

        // Skip date parsing - API returns non-standard format
        createdAt = nil
        updatedAt = nil
    }

    // MARK: - Manual Initializer (for mock data)

    init(
        id: UUID,
        name: String,
        type: String?,
        phone: String?,
        email: String?,
        website: String?,
        description: String?,
        latitude: Double,
        longitude: Double,
        address: String,
        insuranceAccepted: String?,
        ageGroups: [String]?,
        diagnosesTreated: [String]?,
        therapyTypes: [String]?,
        distance: Double?,
        createdAt: Date?,
        updatedAt: Date?
    ) {
        self.id = id
        self.name = name
        self.type = type
        self.phone = phone
        self.email = email
        self.website = website
        self.description = description
        self.latitude = latitude
        self.longitude = longitude
        self.address = address
        self.insuranceAccepted = insuranceAccepted
        self.ageGroups = ageGroups
        self.diagnosesTreated = diagnosesTreated
        self.therapyTypes = therapyTypes
        self.distance = distance
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

// MARK: - Mock Data
extension Provider {
    static let mock = Provider(
        id: UUID(),
        name: "ABC Therapy Center",
        type: "ABA Therapy Resource",
        phone: "3105551234",
        email: "info@abctherapy.com",
        website: "https://abctherapy.com",
        description: "Comprehensive ABA therapy services for children with autism spectrum disorder.",
        latitude: 34.0522,
        longitude: -118.2437,
        address: "123 Main St, Los Angeles, CA 90001",
        insuranceAccepted: "Regional Center, Medi-Cal, Blue Cross",
        ageGroups: ["0-5", "6-12"],
        diagnosesTreated: ["Autism Spectrum Disorder"],
        therapyTypes: ["ABA therapy", "Speech therapy"],
        distance: 2.5,
        createdAt: Date(),
        updatedAt: Date()
    )

    static let mockList: [Provider] = [
        .mock,
        Provider(
            id: UUID(),
            name: "Speech Works LA",
            type: "Speech Therapy Resource",
            phone: "3235559876",
            email: "contact@speechworksla.com",
            website: "speechworksla.com",
            description: "Specialized speech and language therapy for all ages.",
            latitude: 34.0622,
            longitude: -118.2537,
            address: "456 Oak Ave, Los Angeles, CA 90012",
            insuranceAccepted: "Medi-Cal, Kaiser",
            ageGroups: ["All Ages"],
            diagnosesTreated: ["Speech and Language Disorder"],
            therapyTypes: ["Speech therapy"],
            distance: 3.2,
            createdAt: Date(),
            updatedAt: Date()
        ),
        Provider(
            id: UUID(),
            name: "Occupational Therapy Associates",
            type: "Occupational Therapy",
            phone: "8185557890",
            email: nil,
            website: nil,
            description: "OT services for sensory processing and motor skills development.",
            latitude: 34.1722,
            longitude: -118.3937,
            address: "789 Valley Blvd, Van Nuys, CA 91405",
            insuranceAccepted: "Private Pay, Regional Center",
            ageGroups: ["0-5", "6-12", "13-18"],
            diagnosesTreated: ["Sensory Processing Disorder", "Cerebral Palsy"],
            therapyTypes: ["Occupational therapy", "Physical therapy"],
            distance: 5.8,
            createdAt: Date(),
            updatedAt: Date()
        )
    ]
}
