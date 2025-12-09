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
