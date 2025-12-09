//
//  RegionalCenterStore.swift
//  CHLA-iOS
//
//  State management for regional centers
//

import Foundation
import CoreLocation

/// Store for managing regional center data
@MainActor
class RegionalCenterStore: ObservableObject {
    // MARK: - Published State

    @Published var regionalCenters: [RegionalCenter] = []
    @Published var serviceAreaBoundaries: GeoJSONFeatureCollection?
    @Published var selectedCenter: RegionalCenter?
    @Published var userRegionalCenter: RegionalCenter?
    @Published var isLoading = false
    @Published var error: Error?

    // MARK: - Computed Properties

    var hasCenters: Bool {
        !regionalCenters.isEmpty
    }

    var centerCount: Int {
        regionalCenters.count
    }

    var laCenters: [RegionalCenter] {
        regionalCenters.filter { $0.isLaRegionalCenter == true }
    }

    // MARK: - API Methods

    /// Fetch all regional centers
    func fetchRegionalCenters() async {
        isLoading = true
        error = nil

        do {
            regionalCenters = try await APIService.shared.getRegionalCenters()
        } catch {
            self.error = error
            print("❌ Error fetching regional centers: \(error)")
        }

        isLoading = false
    }

    /// Fetch service area boundaries (GeoJSON)
    func fetchServiceAreaBoundaries() async {
        isLoading = true
        error = nil

        do {
            serviceAreaBoundaries = try await APIService.shared.getServiceAreaBoundaries()
        } catch {
            self.error = error
            print("❌ Error fetching service areas: \(error)")
        }

        isLoading = false
    }

    /// Find regional center by ZIP code
    func findByZipCode(_ zipCode: String) async {
        isLoading = true
        error = nil

        do {
            userRegionalCenter = try await APIService.shared.getRegionalCenterByZip(zipCode: zipCode)
            selectedCenter = userRegionalCenter
        } catch {
            self.error = error
            print("❌ Error finding by ZIP: \(error)")
        }

        isLoading = false
    }

    /// Find nearby regional centers
    func findNearby(
        latitude: Double,
        longitude: Double,
        radiusMiles: Double = 25
    ) async {
        isLoading = true
        error = nil

        do {
            regionalCenters = try await APIService.shared.getRegionalCentersNearby(
                latitude: latitude,
                longitude: longitude,
                radiusMiles: radiusMiles
            )

            // Set the closest one as user's regional center
            if let closest = regionalCenters.first {
                userRegionalCenter = closest
            }
        } catch {
            self.error = error
            print("❌ Error finding nearby: \(error)")
        }

        isLoading = false
    }

    /// Select a regional center
    func select(_ center: RegionalCenter) {
        selectedCenter = center
    }

    /// Deselect current center
    func deselect() {
        selectedCenter = nil
    }

    /// Get regional center that serves a specific ZIP code from cached data
    func centerForZipCode(_ zipCode: String) -> RegionalCenter? {
        regionalCenters.first { center in
            center.zipCodes?.contains(zipCode) == true
        }
    }
}
