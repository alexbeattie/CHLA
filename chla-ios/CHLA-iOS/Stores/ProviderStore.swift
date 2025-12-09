//
//  ProviderStore.swift
//  CHLA-iOS
//
//  State management for providers
//

import Foundation
import CoreLocation

/// Store for managing provider data and search state
@MainActor
class ProviderStore: ObservableObject {
    // MARK: - Published State

    @Published var providers: [Provider] = []
    @Published var selectedProvider: Provider?
    @Published var isLoading = false
    @Published var error: Error?
    @Published var searchQuery = ""
    @Published var lastSearchLocation: CLLocationCoordinate2D?

    // MARK: - Computed Properties

    var hasProviders: Bool {
        !providers.isEmpty
    }

    var providerCount: Int {
        providers.count
    }

    var sortedProviders: [Provider] {
        providers.sorted { ($0.distance ?? 999) < ($1.distance ?? 999) }
    }

    // MARK: - API Methods

    /// Fetch all providers
    func fetchProviders() async {
        isLoading = true
        error = nil

        do {
            providers = try await APIService.shared.getProviders()
        } catch {
            self.error = error
            print("❌ Error fetching providers: \(error)")
        }

        isLoading = false
    }

    /// Search providers near a location
    func searchNearby(
        latitude: Double,
        longitude: Double,
        radiusMiles: Double = 15
    ) async {
        isLoading = true
        error = nil
        lastSearchLocation = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)

        do {
            providers = try await APIService.shared.getProvidersNearby(
                latitude: latitude,
                longitude: longitude,
                radiusMiles: radiusMiles
            )
        } catch {
            self.error = error
            print("❌ Error searching nearby: \(error)")
        }

        isLoading = false
    }

    /// Comprehensive search with filters
    func search(
        query: String? = nil,
        location: CLLocationCoordinate2D? = nil,
        filters: SearchFilters
    ) async {
        isLoading = true
        error = nil

        if let location = location {
            lastSearchLocation = location
        }

        do {
            providers = try await APIService.shared.searchProviders(
                query: query,
                latitude: location?.latitude,
                longitude: location?.longitude,
                radiusMiles: filters.radiusMiles,
                insurance: filters.insurance,
                ageGroup: filters.ageGroup,
                diagnosis: filters.diagnosis,
                therapyTypes: filters.therapyTypes
            )
        } catch {
            self.error = error
            print("❌ Error searching: \(error)")
        }

        isLoading = false
    }

    /// Search by regional center ZIP code
    func searchByRegionalCenter(zipCode: String, filters: SearchFilters) async {
        isLoading = true
        error = nil

        do {
            let response = try await APIService.shared.getProvidersByRegionalCenter(
                zipCode: zipCode,
                insurance: filters.insurance,
                ageGroup: filters.ageGroup,
                diagnosis: filters.diagnosis,
                therapy: filters.therapyTypes.first
            )
            providers = response.results
        } catch {
            self.error = error
            print("❌ Error searching by regional center: \(error)")
        }

        isLoading = false
    }

    /// Clear current search results
    func clearResults() {
        providers = []
        selectedProvider = nil
        error = nil
        lastSearchLocation = nil
    }

    /// Select a provider
    func select(_ provider: Provider) {
        selectedProvider = provider
    }

    /// Deselect current provider
    func deselect() {
        selectedProvider = nil
    }
}
