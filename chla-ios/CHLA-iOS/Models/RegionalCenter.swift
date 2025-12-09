//
//  RegionalCenter.swift
//  CHLA-iOS
//
//  Regional Center model matching Django RegionalCenter
//

import Foundation
import CoreLocation

/// LA County Regional Center
/// Matches the Django RegionalCenter model structure
struct RegionalCenter: Codable, Identifiable, Equatable, Hashable {
    let id: Int
    let regionalCenter: String
    let officeType: String?
    let address: String
    let suite: String?
    let city: String
    let state: String
    let zipCode: String
    let telephone: String?
    let website: String?
    let countyServed: String?
    let losAngelesHealthDistrict: String?
    let latitude: Double?
    let longitude: Double?
    let locationName: String?

    // Service area data
    let zipCodes: [String]?
    let serviceAreas: [String]?
    let isLaRegionalCenter: Bool?

    // Computed distance (set by API when using location-based search)
    var distance: Double?

    // MARK: - Computed Properties

    var coordinate: CLLocationCoordinate2D? {
        guard let lat = latitude, let lng = longitude else { return nil }
        return CLLocationCoordinate2D(latitude: lat, longitude: lng)
    }

    var fullAddress: String {
        var parts = [address]
        if let suite = suite, !suite.isEmpty {
            parts.append("Suite \(suite)")
        }
        parts.append("\(city), \(state) \(zipCode)")
        return parts.joined(separator: ", ")
    }

    var formattedPhone: String? {
        guard let phone = telephone, !phone.isEmpty else { return nil }
        return phone
    }

    var phoneURL: URL? {
        guard let phone = telephone else { return nil }
        let digits = phone.filter { $0.isNumber }
        return URL(string: "tel://\(digits)")
    }

    var websiteURL: URL? {
        guard let website = website, !website.isEmpty else { return nil }
        if website.hasPrefix("http") {
            return URL(string: website)
        }
        return URL(string: "https://\(website)")
    }

    var mapsURL: URL? {
        let encodedAddress = fullAddress.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        return URL(string: "http://maps.apple.com/?address=\(encodedAddress)")
    }

    var directionsURL: URL? {
        guard let lat = latitude, let lng = longitude else { return nil }
        return URL(string: "http://maps.apple.com/?daddr=\(lat),\(lng)")
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

    var zipCodeCount: Int {
        zipCodes?.count ?? 0
    }

    // MARK: - Coding Keys

    enum CodingKeys: String, CodingKey {
        case id
        case regionalCenter = "regional_center"
        case officeType = "office_type"
        case address, suite, city, state
        case zipCode = "zip_code"
        case telephone, website
        case countyServed = "county_served"
        case losAngelesHealthDistrict = "los_angeles_health_district"
        case latitude, longitude
        case locationName = "location_name"
        case zipCodes = "zip_codes"
        case serviceAreas = "service_areas"
        case isLaRegionalCenter = "is_la_regional_center"
        case distance
    }

    // MARK: - Custom Decoder (handles zip_codes as String or Array)

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(Int.self, forKey: .id)
        regionalCenter = try container.decode(String.self, forKey: .regionalCenter)
        officeType = try container.decodeIfPresent(String.self, forKey: .officeType)
        address = try container.decodeIfPresent(String.self, forKey: .address) ?? ""
        suite = try container.decodeIfPresent(String.self, forKey: .suite)
        city = try container.decodeIfPresent(String.self, forKey: .city) ?? ""
        state = try container.decodeIfPresent(String.self, forKey: .state) ?? "CA"
        zipCode = try container.decodeIfPresent(String.self, forKey: .zipCode) ?? ""
        telephone = try container.decodeIfPresent(String.self, forKey: .telephone)
        website = try container.decodeIfPresent(String.self, forKey: .website)
        countyServed = try container.decodeIfPresent(String.self, forKey: .countyServed)
        losAngelesHealthDistrict = try container.decodeIfPresent(String.self, forKey: .losAngelesHealthDistrict)
        latitude = try container.decodeIfPresent(Double.self, forKey: .latitude)
        longitude = try container.decodeIfPresent(Double.self, forKey: .longitude)
        locationName = try container.decodeIfPresent(String.self, forKey: .locationName)

        // Handle zipCodes as either [String] or String
        if let zipArray = try? container.decodeIfPresent([String].self, forKey: .zipCodes) {
            zipCodes = zipArray
        } else if let zipString = try? container.decodeIfPresent(String.self, forKey: .zipCodes),
                  !zipString.isEmpty {
            zipCodes = zipString.contains(",")
                ? zipString.components(separatedBy: ",").map { $0.trimmingCharacters(in: .whitespaces) }
                : [zipString]
        } else {
            zipCodes = nil
        }

        // Handle serviceAreas as either [String] or String  
        if let areasArray = try? container.decodeIfPresent([String].self, forKey: .serviceAreas) {
            serviceAreas = areasArray
        } else if let areasString = try? container.decodeIfPresent(String.self, forKey: .serviceAreas),
                  !areasString.isEmpty {
            serviceAreas = areasString.contains(",")
                ? areasString.components(separatedBy: ",").map { $0.trimmingCharacters(in: .whitespaces) }
                : [areasString]
        } else {
            serviceAreas = nil
        }

        isLaRegionalCenter = try container.decodeIfPresent(Bool.self, forKey: .isLaRegionalCenter)
        distance = try container.decodeIfPresent(Double.self, forKey: .distance)
    }

    // MARK: - Manual Initializer (for mock data)

    init(id: Int, regionalCenter: String, officeType: String?, address: String, suite: String?,
         city: String, state: String, zipCode: String, telephone: String?, website: String?,
         countyServed: String?, losAngelesHealthDistrict: String?, latitude: Double?, longitude: Double?,
         locationName: String?, zipCodes: [String]?, serviceAreas: [String]?, isLaRegionalCenter: Bool?,
         distance: Double?) {
        self.id = id
        self.regionalCenter = regionalCenter
        self.officeType = officeType
        self.address = address
        self.suite = suite
        self.city = city
        self.state = state
        self.zipCode = zipCode
        self.telephone = telephone
        self.website = website
        self.countyServed = countyServed
        self.losAngelesHealthDistrict = losAngelesHealthDistrict
        self.latitude = latitude
        self.longitude = longitude
        self.locationName = locationName
        self.zipCodes = zipCodes
        self.serviceAreas = serviceAreas
        self.isLaRegionalCenter = isLaRegionalCenter
        self.distance = distance
    }
}

// MARK: - Mock Data (All 7 LA Regional Centers)
extension RegionalCenter {
    static let mock = RegionalCenter(
        id: 1,
        regionalCenter: "North Los Angeles County Regional Center",
        officeType: "Main Office",
        address: "15400 Sherman Way",
        suite: "170",
        city: "Van Nuys",
        state: "CA",
        zipCode: "91406",
        telephone: "(818) 778-1900",
        website: "https://nlacrc.org",
        countyServed: "Los Angeles",
        losAngelesHealthDistrict: "San Fernando Valley",
        latitude: 34.201126,
        longitude: -118.468492,
        locationName: "Van Nuys Main",
        zipCodes: ["91301", "91302", "91303", "91304", "91306", "91307", "91311", "91316", "91324", "91325", "91326", "91330", "91331", "91335", "91340", "91342", "91343", "91344", "91345", "91350", "91351", "91352", "91354", "91355", "91356", "91364", "91367", "91401", "91402", "91403", "91405", "91406", "91411", "91423", "91436"],
        serviceAreas: ["Van Nuys", "Sherman Oaks", "Encino", "Tarzana", "Northridge", "Granada Hills", "Sylmar", "Pacoima", "Sun Valley", "North Hollywood", "Burbank", "Glendale"],
        isLaRegionalCenter: true,
        distance: nil
    )

    static let mockList: [RegionalCenter] = [
        .mock,
        RegionalCenter(
            id: 2,
            regionalCenter: "Westside Regional Center",
            officeType: "Main Office",
            address: "5901 Green Valley Circle",
            suite: "200",
            city: "Culver City",
            state: "CA",
            zipCode: "90230",
            telephone: "(310) 258-4000",
            website: "https://westsiderc.org",
            countyServed: "Los Angeles",
            losAngelesHealthDistrict: "West",
            latitude: 33.983957,
            longitude: -118.392,
            locationName: "Culver City Main",
            zipCodes: ["90024", "90025", "90034", "90035", "90045", "90049", "90056", "90064", "90066", "90067", "90077", "90094", "90230", "90232", "90245", "90254", "90266", "90272", "90290", "90291", "90292", "90293", "90301", "90302", "90303", "90304", "90305", "90401", "90402", "90403", "90404", "90405"],
            serviceAreas: ["Santa Monica", "Venice", "Culver City", "Westwood", "Brentwood", "Pacific Palisades", "Marina del Rey", "Playa del Rey", "Inglewood", "El Segundo", "Manhattan Beach", "Hermosa Beach", "Redondo Beach"],
            isLaRegionalCenter: true,
            distance: nil
        ),
        RegionalCenter(
            id: 3,
            regionalCenter: "Eastern Los Angeles Regional Center",
            officeType: "Main Office",
            address: "1000 South Fremont Avenue",
            suite: "A-10",
            city: "Alhambra",
            state: "CA",
            zipCode: "91803",
            telephone: "(626) 299-4700",
            website: "https://elarc.org",
            countyServed: "Los Angeles",
            losAngelesHealthDistrict: "East",
            latitude: 34.080352,
            longitude: -118.151849,
            locationName: "Alhambra Main",
            zipCodes: ["90022", "90023", "90032", "90033", "90040", "90058", "90063", "91731", "91732", "91733", "91754", "91755", "91770", "91776", "91780", "91801", "91803"],
            serviceAreas: ["East LA", "Alhambra", "Montebello", "Monterey Park", "El Monte", "South El Monte", "Rosemead", "San Gabriel", "Temple City"],
            isLaRegionalCenter: true,
            distance: nil
        ),
        RegionalCenter(
            id: 4,
            regionalCenter: "South Central Los Angeles Regional Center",
            officeType: "Main Office",
            address: "2500 S. Western Avenue",
            suite: nil,
            city: "Los Angeles",
            state: "CA",
            zipCode: "90018",
            telephone: "(213) 744-7000",
            website: "https://sclarc.org",
            countyServed: "Los Angeles",
            losAngelesHealthDistrict: "South",
            latitude: 34.032682,
            longitude: -118.308972,
            locationName: "South Central Main",
            zipCodes: ["90001", "90002", "90003", "90007", "90008", "90011", "90016", "90018", "90037", "90043", "90044", "90047", "90059", "90061", "90062", "90089", "90220", "90221", "90222", "90262"],
            serviceAreas: ["South Central LA", "Watts", "Compton", "Willowbrook", "Florence", "Vermont Square", "Hyde Park", "Leimert Park", "Baldwin Hills"],
            isLaRegionalCenter: true,
            distance: nil
        ),
        RegionalCenter(
            id: 5,
            regionalCenter: "Harbor Regional Center",
            officeType: "Main Office",
            address: "21231 Hawthorne Boulevard",
            suite: nil,
            city: "Torrance",
            state: "CA",
            zipCode: "90503",
            telephone: "(310) 540-1711",
            website: "https://harborrc.org",
            countyServed: "Los Angeles",
            losAngelesHealthDistrict: "Harbor",
            latitude: 33.836732,
            longitude: -118.353594,
            locationName: "Torrance Main",
            zipCodes: ["90247", "90248", "90249", "90260", "90274", "90275", "90277", "90278", "90501", "90502", "90503", "90504", "90505", "90506", "90507", "90508", "90509", "90510", "90710", "90717", "90731", "90732", "90744", "90745", "90746", "90810", "90813"],
            serviceAreas: ["Torrance", "San Pedro", "Wilmington", "Carson", "Lomita", "Palos Verdes", "Rolling Hills", "Harbor City", "Long Beach (part)"],
            isLaRegionalCenter: true,
            distance: nil
        ),
        RegionalCenter(
            id: 6,
            regionalCenter: "Frank D. Lanterman Regional Center",
            officeType: "Main Office",
            address: "3303 Wilshire Boulevard",
            suite: "700",
            city: "Los Angeles",
            state: "CA",
            zipCode: "90010",
            telephone: "(213) 383-1300",
            website: "https://lanterman.org",
            countyServed: "Los Angeles",
            losAngelesHealthDistrict: "Central",
            latitude: 34.061772,
            longitude: -118.294495,
            locationName: "Koreatown Main",
            zipCodes: ["90004", "90005", "90006", "90010", "90012", "90013", "90014", "90015", "90017", "90019", "90020", "90026", "90027", "90028", "90029", "90031", "90036", "90038", "90039", "90041", "90042", "90046", "90048", "90057", "90065", "90068", "90069", "90071", "90210", "90211", "90212", "91206"],
            serviceAreas: ["Hollywood", "Los Feliz", "Silver Lake", "Echo Park", "Downtown LA", "Koreatown", "Mid-Wilshire", "Beverly Hills", "West Hollywood", "Eagle Rock", "Highland Park", "Glendale (part)"],
            isLaRegionalCenter: true,
            distance: nil
        ),
        RegionalCenter(
            id: 7,
            regionalCenter: "San Gabriel/Pomona Regional Center",
            officeType: "Main Office",
            address: "75 Rancho Camino Drive",
            suite: nil,
            city: "Pomona",
            state: "CA",
            zipCode: "91766",
            telephone: "(909) 620-7722",
            website: "https://sgprc.org",
            countyServed: "Los Angeles",
            losAngelesHealthDistrict: "San Gabriel Valley",
            latitude: 34.027688,
            longitude: -117.756777,
            locationName: "Pomona Main",
            zipCodes: ["91001", "91006", "91007", "91010", "91016", "91024", "91030", "91101", "91103", "91104", "91105", "91106", "91107", "91108", "91702", "91706", "91722", "91723", "91724", "91740", "91741", "91744", "91745", "91746", "91748", "91750", "91765", "91766", "91767", "91768", "91773", "91789", "91790", "91791", "91792"],
            serviceAreas: ["Pomona", "Claremont", "La Verne", "San Dimas", "Glendora", "Azusa", "Duarte", "Monrovia", "Arcadia", "Pasadena", "Altadena", "Sierra Madre", "Covina", "West Covina", "Diamond Bar", "Walnut", "Rowland Heights"],
            isLaRegionalCenter: true,
            distance: nil
        )
    ]
}
