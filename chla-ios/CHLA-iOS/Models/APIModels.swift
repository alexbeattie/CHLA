//
//  APIModels.swift
//  CHLA-iOS
//
//  API response models and supporting types
//

import Foundation

// MARK: - Health Check Response
struct HealthCheckResponse: Codable {
    let status: String
    let database: String?
    let providers: Int?
    let regionalCenters: Int?
    let version: String?
    let error: String?

    enum CodingKeys: String, CodingKey {
        case status, database, providers, version, error
        case regionalCenters = "regional_centers"
    }

    var isHealthy: Bool {
        status == "healthy"
    }
}

// MARK: - GeoJSON Types
struct GeoJSONFeatureCollection: Codable {
    let type: String
    let features: [GeoJSONFeature]
}

struct GeoJSONFeature: Codable, Identifiable {
    let type: String
    let properties: RegionalCenterProperties
    let geometry: GeoJSONGeometry?

    var id: Int { properties.centerId }
}

struct RegionalCenterProperties: Codable {
    let name: String
    let phone: String?
    let address: String?
    let website: String?
    let serviceAreas: [String]?
    let zipCodes: [String]?
    let centerId: Int
    let officeType: String?
    let countyServed: String?
    let latitude: Double?
    let longitude: Double?
    let suite: String?
    let city: String?
    let state: String?
    let zipCode: String?
    let addressStreet: String?

    enum CodingKeys: String, CodingKey {
        case name, phone, address, website
        case serviceAreas = "service_areas"
        case zipCodes = "zip_codes"
        case centerId = "center_id"
        case officeType = "office_type"
        case countyServed = "county_served"
        case latitude, longitude, suite, city, state
        case zipCode = "zip_code"
        case addressStreet = "address_street"
        // Alternative keys from /service_areas/ endpoint
        case regionalCenter = "regional_center"
        case id
        case telephone
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        // Handle name from either 'name' or 'regional_center'
        if let name = try? container.decode(String.self, forKey: .name) {
            self.name = name
        } else if let rcName = try? container.decode(String.self, forKey: .regionalCenter) {
            self.name = rcName
        } else {
            self.name = "Unknown"
        }

        // Handle ID from either 'center_id' or 'id'
        if let centerId = try? container.decode(Int.self, forKey: .centerId) {
            self.centerId = centerId
        } else if let id = try? container.decode(Int.self, forKey: .id) {
            self.centerId = id
        } else {
            self.centerId = 0
        }

        // Handle phone from either 'phone' or 'telephone'
        if let phone = try? container.decodeIfPresent(String.self, forKey: .phone) {
            self.phone = phone
        } else if let telephone = try? container.decodeIfPresent(String.self, forKey: .telephone) {
            self.phone = telephone
        } else {
            self.phone = nil
        }

        address = try container.decodeIfPresent(String.self, forKey: .address)
        website = try container.decodeIfPresent(String.self, forKey: .website)
        officeType = try container.decodeIfPresent(String.self, forKey: .officeType)
        countyServed = try container.decodeIfPresent(String.self, forKey: .countyServed)
        latitude = try container.decodeIfPresent(Double.self, forKey: .latitude)
        longitude = try container.decodeIfPresent(Double.self, forKey: .longitude)
        suite = try container.decodeIfPresent(String.self, forKey: .suite)
        city = try container.decodeIfPresent(String.self, forKey: .city)
        state = try container.decodeIfPresent(String.self, forKey: .state)
        zipCode = try container.decodeIfPresent(String.self, forKey: .zipCode)
        addressStreet = try container.decodeIfPresent(String.self, forKey: .addressStreet)

        // Handle arrays that might be strings
        if let areas = try? container.decodeIfPresent([String].self, forKey: .serviceAreas) {
            serviceAreas = areas
        } else {
            serviceAreas = nil
        }

        if let zips = try? container.decodeIfPresent([String].self, forKey: .zipCodes) {
            zipCodes = zips
        } else {
            zipCodes = nil
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(name, forKey: .name)
        try container.encodeIfPresent(phone, forKey: .phone)
        try container.encodeIfPresent(address, forKey: .address)
        try container.encodeIfPresent(website, forKey: .website)
        try container.encodeIfPresent(serviceAreas, forKey: .serviceAreas)
        try container.encodeIfPresent(zipCodes, forKey: .zipCodes)
        try container.encode(centerId, forKey: .centerId)
        try container.encodeIfPresent(officeType, forKey: .officeType)
        try container.encodeIfPresent(countyServed, forKey: .countyServed)
        try container.encodeIfPresent(latitude, forKey: .latitude)
        try container.encodeIfPresent(longitude, forKey: .longitude)
        try container.encodeIfPresent(suite, forKey: .suite)
        try container.encodeIfPresent(city, forKey: .city)
        try container.encodeIfPresent(state, forKey: .state)
        try container.encodeIfPresent(zipCode, forKey: .zipCode)
        try container.encodeIfPresent(addressStreet, forKey: .addressStreet)
    }
}

struct GeoJSONGeometry: Codable {
    let type: String
    let coordinates: GeoJSONCoordinates?

    enum CodingKeys: String, CodingKey {
        case type, coordinates
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        type = try container.decode(String.self, forKey: .type)

        // GeoJSON coordinates can be deeply nested arrays
        // We'll decode them as a generic structure
        coordinates = try? container.decode(GeoJSONCoordinates.self, forKey: .coordinates)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        try container.encodeIfPresent(coordinates, forKey: .coordinates)
    }
}

// Flexible coordinate type that can handle various GeoJSON coordinate structures
enum GeoJSONCoordinates: Codable {
    case point([Double])
    case lineString([[Double]])
    case polygon([[[Double]]])
    case multiPolygon([[[[Double]]]])

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        // Try each type in order of complexity
        if let multiPolygon = try? container.decode([[[[Double]]]].self) {
            self = .multiPolygon(multiPolygon)
        } else if let polygon = try? container.decode([[[Double]]].self) {
            self = .polygon(polygon)
        } else if let lineString = try? container.decode([[Double]].self) {
            self = .lineString(lineString)
        } else if let point = try? container.decode([Double].self) {
            self = .point(point)
        } else {
            throw DecodingError.typeMismatch(
                GeoJSONCoordinates.self,
                DecodingError.Context(
                    codingPath: decoder.codingPath,
                    debugDescription: "Could not decode GeoJSON coordinates"
                )
            )
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .point(let coords):
            try container.encode(coords)
        case .lineString(let coords):
            try container.encode(coords)
        case .polygon(let coords):
            try container.encode(coords)
        case .multiPolygon(let coords):
            try container.encode(coords)
        }
    }
}

// MARK: - Regional Center Search Response
struct RegionalCenterSearchResponse: Codable {
    let count: Int
    let regionalCenter: RegionalCenterInfo
    let results: [Provider]

    enum CodingKeys: String, CodingKey {
        case count
        case regionalCenter = "regional_center"
        case results
    }
}

struct RegionalCenterInfo: Codable {
    let id: Int
    let name: String
    let zipCodes: [String]?

    enum CodingKeys: String, CodingKey {
        case id, name
        case zipCodes = "zip_codes"
    }
}

// MARK: - Insurance Carrier
struct InsuranceCarrier: Codable, Identifiable {
    let id: Int
    let name: String
    let description: String?
}

// MARK: - Funding Source
struct FundingSource: Codable, Identifiable {
    let id: Int
    let name: String
    let description: String?
}

// MARK: - Service Delivery Model
struct ServiceDeliveryModel: Codable, Identifiable {
    let id: Int
    let name: String
    let description: String?
}

// MARK: - Paginated Response
/// Generic wrapper for Django REST Framework paginated responses
struct PaginatedResponse<T: Codable>: Codable {
    let count: Int
    let next: String?
    let previous: String?
    let results: [T]
}

// MARK: - API Error
struct APIError: Codable {
    let error: String
    let detail: String?
    let message: String?
}

// MARK: - HMGL (Help Me Grow LA) Location
/// Represents a location from the Help Me Grow LA database
struct HMGLLocation: Codable, Identifiable {
    let locationId: Int
    let name: String?
    let organization: String?
    let city: String?
    let state: String?
    let zip: String?
    let latitude: Double?
    let longitude: Double?
    let fullAddress: String?
    let primaryPhone: String?
    let primaryPhoneClean: String?
    let tags: [HMGLTag]?
    let programs: [HMGLProgram]?

    // Additional fields from detail view
    let phones: String?
    let hours: String?
    let hoursClean: String?
    let email: String?
    let descriptionHtml: String?
    let descriptionClean: String?
    let url: String?
    let imgurl: String?
    let phoneList: [String]?
    let hourList: [String]?
    let customAttributes: [String: AnyCodable]?
    let distance: Double?

    var id: Int { locationId }

    enum CodingKeys: String, CodingKey {
        case locationId = "location_id"
        case name, organization, city, state, zip
        case latitude, longitude
        case fullAddress = "full_address"
        case primaryPhone = "primary_phone"
        case primaryPhoneClean = "primary_phone_clean"
        case tags, programs
        case phones, hours, email
        case hoursClean = "hours_clean"
        case descriptionHtml = "description_html"
        case descriptionClean = "description_clean"
        case url, imgurl
        case phoneList = "phone_list"
        case hourList = "hour_list"
        case customAttributes = "custom_attributes"
        case distance
    }

    /// Clean hours display
    var displayHours: String? {
        hoursClean ?? hours
    }

    /// Clean description display
    var displayDescription: String? {
        descriptionClean ?? descriptionHtml
    }

    /// Clean phone display
    var displayPhone: String? {
        primaryPhoneClean ?? primaryPhone
    }

    /// Display name with fallback
    var displayName: String {
        name ?? organization ?? "Unknown Location"
    }

    /// Formatted location string
    var locationString: String {
        [city, state].compactMap { $0 }.joined(separator: ", ")
    }

    /// Get tag names as strings
    var tagNames: [String] {
        tags?.compactMap { $0.tag1 } ?? []
    }

    /// Get program names as strings
    var programNames: [String] {
        programs?.compactMap { $0.name } ?? []
    }
}

// MARK: - HMGL Tag
struct HMGLTag: Codable {
    let tag1: String?
    let tagID: Int?
    let imageURL: String?
    let tagTypeID: Int?
}

// MARK: - HMGL Program
struct HMGLProgram: Codable {
    let name: String?
    let programID: Int?
    let description: String?

    // Flexible decoding for various program formats
    init(from decoder: Decoder) throws {
        // Try object first
        if let container = try? decoder.container(keyedBy: CodingKeys.self) {
            name = try container.decodeIfPresent(String.self, forKey: .name)
            programID = try container.decodeIfPresent(Int.self, forKey: .programID)
            description = try container.decodeIfPresent(String.self, forKey: .description)
        } else if let stringValue = try? decoder.singleValueContainer().decode(String.self) {
            // It's just a string
            name = stringValue
            programID = nil
            description = nil
        } else {
            name = nil
            programID = nil
            description = nil
        }
    }

    enum CodingKeys: String, CodingKey {
        case name
        case programID = "program_id"
        case description
    }
}

// MARK: - HMGL Location Response
struct HMGLLocationResponse: Codable {
    let count: Int
    let searchParams: HMGLSearchParams?
    let results: [HMGLLocation]

    enum CodingKeys: String, CodingKey {
        case count
        case searchParams = "search_params"
        case results
    }
}

struct HMGLSearchParams: Codable {
    let lat: Double?
    let lng: Double?
    let radiusMiles: Double?

    enum CodingKeys: String, CodingKey {
        case lat, lng
        case radiusMiles = "radius_miles"
    }
}

// MARK: - HMGL Stats Response
struct HMGLStatsResponse: Codable {
    let totalLocations: Int
    let withCoordinates: Int
    let countyLocations: Int
    let topCities: [HMGLCityCount]
    let topOrganizations: [HMGLOrgCount]

    enum CodingKeys: String, CodingKey {
        case totalLocations = "total_locations"
        case withCoordinates = "with_coordinates"
        case countyLocations = "county_locations"
        case topCities = "top_cities"
        case topOrganizations = "top_organizations"
    }
}

struct HMGLCityCount: Codable, Identifiable {
    let city: String
    let count: Int

    var id: String { city }
}

struct HMGLOrgCount: Codable, Identifiable {
    let organization: String
    let count: Int

    var id: String { organization }
}

// MARK: - AnyCodable for flexible JSON
struct AnyCodable: Codable {
    let value: Any

    init(_ value: Any) {
        self.value = value
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let string = try? container.decode(String.self) {
            value = string
        } else if let int = try? container.decode(Int.self) {
            value = int
        } else if let double = try? container.decode(Double.self) {
            value = double
        } else if let bool = try? container.decode(Bool.self) {
            value = bool
        } else if let array = try? container.decode([AnyCodable].self) {
            value = array.map { $0.value }
        } else if let dict = try? container.decode([String: AnyCodable].self) {
            value = dict.mapValues { $0.value }
        } else {
            value = NSNull()
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        if let string = value as? String {
            try container.encode(string)
        } else if let int = value as? Int {
            try container.encode(int)
        } else if let double = value as? Double {
            try container.encode(double)
        } else if let bool = value as? Bool {
            try container.encode(bool)
        } else {
            try container.encodeNil()
        }
    }
}
