//
//  RegionalCenterMatcher.swift
//  CHLA-iOS
//
//  Matches providers to their serving Regional Center
//

import Foundation
import CoreLocation
import SwiftUI

// MARK: - Centralized Regional Center Colors
/// Use these colors throughout the app for consistent branding
extension Color {
    /// Returns the official color for a regional center by shortName/acronym
    static func regionalCenterColor(for shortName: String) -> Color {
        switch shortName.uppercased() {
        case "NLACRC":
            return Color(red: 0.85, green: 0.65, blue: 0.13)   // Gold/Yellow
        case "FDLRC":
            return Color(red: 0.60, green: 0.40, blue: 0.70)    // Purple
        case "HRC":
            return Color(red: 0.20, green: 0.60, blue: 0.86)    // Blue
        case "SCLARC":
            return Color(red: 0.95, green: 0.55, blue: 0.20)    // Orange
        case "ELARC":
            return Color(red: 0.30, green: 0.75, blue: 0.45)    // Green
        case "WRC":
            return Color(red: 0.90, green: 0.30, blue: 0.50)    // Pink/Magenta
        case "SGPRC", "SG/PRC":
            return Color(red: 0.20, green: 0.55, blue: 0.35)    // Dark Green/Teal
        default:
            return .accentBlue
        }
    }
}

/// Matches providers to LA County Regional Centers based on location
class RegionalCenterMatcher {
    static let shared = RegionalCenterMatcher()

    // MARK: - Regional Center Data

    struct RegionalCenterInfo {
        let id: Int
        let name: String
        let shortName: String
        let phone: String
        let website: String
        let coordinate: CLLocationCoordinate2D
        let color: String // Legacy - use Color.regionalCenterColor(for:) instead

        /// Get the SwiftUI Color for this regional center
        var uiColor: Color {
            Color.regionalCenterColor(for: shortName)
        }
    }

    /// LA County Regional Centers with their approximate service area centers
    let laRegionalCenters: [RegionalCenterInfo] = [
        RegionalCenterInfo(
            id: 1,
            name: "North Los Angeles County Regional Center",
            shortName: "NLACRC",
            phone: "(818) 778-1900",
            website: "nlacrc.org",
            coordinate: CLLocationCoordinate2D(latitude: 34.201126, longitude: -118.468492),
            color: "orange"
        ),
        RegionalCenterInfo(
            id: 2,
            name: "Westside Regional Center",
            shortName: "WRC",
            phone: "(310) 258-4000",
            website: "westsiderc.org",
            coordinate: CLLocationCoordinate2D(latitude: 34.020508, longitude: -118.389124),
            color: "blue"
        ),
        RegionalCenterInfo(
            id: 3,
            name: "South Central Los Angeles Regional Center",
            shortName: "SCLARC",
            phone: "(213) 744-7000",
            website: "sclarc.org",
            coordinate: CLLocationCoordinate2D(latitude: 33.985947, longitude: -118.274231),
            color: "purple"
        ),
        RegionalCenterInfo(
            id: 4,
            name: "Eastern Los Angeles Regional Center",
            shortName: "ELARC",
            phone: "(626) 299-4700",
            website: "elarc.org",
            coordinate: CLLocationCoordinate2D(latitude: 34.095554, longitude: -118.136558),
            color: "green"
        ),
        RegionalCenterInfo(
            id: 5,
            name: "Harbor Regional Center",
            shortName: "HRC",
            phone: "(310) 540-1711",
            website: "harborrc.org",
            coordinate: CLLocationCoordinate2D(latitude: 33.829963, longitude: -118.290833),
            color: "teal"
        ),
        RegionalCenterInfo(
            id: 6,
            name: "Frank D. Lanterman Regional Center",
            shortName: "FDLRC",
            phone: "(213) 383-1300",
            website: "lanterman.org",
            coordinate: CLLocationCoordinate2D(latitude: 34.065543, longitude: -118.288803),
            color: "red"
        ),
        RegionalCenterInfo(
            id: 7,
            name: "San Gabriel/Pomona Regional Center",
            shortName: "SGPRC",
            phone: "(909) 620-7722",
            website: "sgprc.org",
            coordinate: CLLocationCoordinate2D(latitude: 34.055287, longitude: -117.750012),
            color: "indigo"
        )
    ]

    // MARK: - Matching

    /// Find the closest regional center for a given coordinate
    func findRegionalCenter(for coordinate: CLLocationCoordinate2D) -> RegionalCenterInfo? {
        let location = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)

        var closestCenter: RegionalCenterInfo?
        var closestDistance: CLLocationDistance = .greatestFiniteMagnitude

        for center in laRegionalCenters {
            let centerLocation = CLLocation(
                latitude: center.coordinate.latitude,
                longitude: center.coordinate.longitude
            )
            let distance = location.distance(from: centerLocation)

            if distance < closestDistance {
                closestDistance = distance
                closestCenter = center
            }
        }

        return closestCenter
    }

    /// Find regional center by extracting zip code from address
    func findRegionalCenter(forAddress address: String) -> RegionalCenterInfo? {
        // Extract zip code from address (5-digit pattern)
        let zipPattern = #"\b(\d{5})(?:-\d{4})?\b"#
        if let regex = try? NSRegularExpression(pattern: zipPattern),
           let match = regex.firstMatch(in: address, range: NSRange(address.startIndex..., in: address)),
           let range = Range(match.range(at: 1), in: address) {
            let zipCode = String(address[range])
            return findRegionalCenter(forZipCode: zipCode)
        }
        return nil
    }

    /// Find regional center for a specific zip code
    /// Based on LA County zip code distributions
    func findRegionalCenter(forZipCode zip: String) -> RegionalCenterInfo? {
        // North LA (San Fernando Valley area)
        let northLAZips = ["91301", "91302", "91303", "91304", "91306", "91307", "91311", "91316",
                          "91324", "91325", "91326", "91330", "91331", "91335", "91340", "91342",
                          "91343", "91344", "91345", "91350", "91351", "91352", "91354", "91355",
                          "91356", "91364", "91367", "91371", "91381", "91384", "91387", "91390",
                          "91401", "91402", "91403", "91405", "91406", "91411", "91423", "91436",
                          "91501", "91502", "91504", "91505", "91506", "91601", "91602", "91604",
                          "91605", "91606", "91607", "91608"]

        // Westside
        let westsideZips = ["90024", "90025", "90034", "90035", "90045", "90049", "90056", "90064",
                           "90066", "90067", "90077", "90094", "90230", "90232", "90245", "90254",
                           "90266", "90272", "90290", "90291", "90292", "90293", "90301", "90302",
                           "90303", "90304", "90305", "90401", "90402", "90403", "90404", "90405"]

        // South Central
        let southCentralZips = ["90001", "90002", "90003", "90007", "90008", "90011", "90016",
                               "90018", "90037", "90043", "90044", "90047", "90059", "90061",
                               "90062", "90089", "90220", "90221", "90222", "90262"]

        // Eastern LA
        let easternLAZips = ["90022", "90023", "90032", "90033", "90040", "90058", "90063",
                            "91731", "91732", "91733", "91754", "91755", "91770", "91776",
                            "91780", "91801", "91803"]

        // Harbor
        let harborZips = ["90247", "90248", "90249", "90260", "90274", "90275", "90277",
                         "90278", "90501", "90502", "90503", "90504", "90505", "90506",
                         "90507", "90508", "90509", "90510", "90710", "90717", "90731",
                         "90732", "90744", "90745", "90746", "90810", "90813"]

        // Lanterman (Central LA)
        let lantermanZips = ["90004", "90005", "90006", "90010", "90012", "90013", "90014",
                            "90015", "90017", "90019", "90020", "90026", "90027", "90028",
                            "90029", "90031", "90036", "90038", "90039", "90041", "90042",
                            "90046", "90048", "90057", "90065", "90068", "90069", "90071",
                            "90210", "90211", "90212", "91206"]

        // San Gabriel/Pomona
        let sgprcZips = ["91001", "91006", "91007", "91010", "91016", "91024", "91030",
                        "91101", "91103", "91104", "91105", "91106", "91107", "91108",
                        "91702", "91706", "91722", "91723", "91724", "91740", "91741",
                        "91744", "91745", "91746", "91748", "91750", "91765", "91766",
                        "91767", "91768", "91773", "91789", "91790", "91791", "91792"]

        // Match zip to regional center
        if northLAZips.contains(zip) {
            return laRegionalCenters.first { $0.shortName == "NLACRC" }
        } else if westsideZips.contains(zip) {
            return laRegionalCenters.first { $0.shortName == "WRC" }
        } else if southCentralZips.contains(zip) {
            return laRegionalCenters.first { $0.shortName == "SCLARC" }
        } else if easternLAZips.contains(zip) {
            return laRegionalCenters.first { $0.shortName == "ELARC" }
        } else if harborZips.contains(zip) {
            return laRegionalCenters.first { $0.shortName == "HRC" }
        } else if lantermanZips.contains(zip) {
            return laRegionalCenters.first { $0.shortName == "FDLRC" }
        } else if sgprcZips.contains(zip) {
            return laRegionalCenters.first { $0.shortName == "SGPRC" }
        }

        return nil
    }
}
