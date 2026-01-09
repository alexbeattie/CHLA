//
//  HMGLLocationStore.swift
//  CHLA-iOS
//
//  State management for Help Me Grow LA locations
//

import Foundation
import CoreLocation

@MainActor
class HMGLLocationStore: ObservableObject {
    // MARK: - Published Properties
    
    @Published var locations: [HMGLLocation] = []
    @Published var nearbyLocations: [HMGLLocation] = []
    @Published var stats: HMGLStatsResponse?
    @Published var isLoading = false
    @Published var error: String?
    @Published var searchQuery = ""
    @Published var selectedCity: String?
    
    // MARK: - Private Properties
    
    private let apiService = APIService.shared
    
    // MARK: - Computed Properties
    
    var filteredLocations: [HMGLLocation] {
        if searchQuery.isEmpty {
            return locations
        }
        return locations.filter { location in
            let query = searchQuery.lowercased()
            return (location.name?.lowercased().contains(query) ?? false) ||
                   (location.organization?.lowercased().contains(query) ?? false) ||
                   (location.city?.lowercased().contains(query) ?? false)
        }
    }
    
    // MARK: - Public Methods
    
    /// Load all HMGL locations
    func loadLocations() async {
        isLoading = true
        error = nil
        
        do {
            locations = try await apiService.getHMGLLocations()
            print("📍 Loaded \(locations.count) HMGL locations")
        } catch {
            self.error = error.localizedDescription
            print("❌ Failed to load HMGL locations: \(error)")
        }
        
        isLoading = false
    }
    
    /// Load nearby locations
    func loadNearbyLocations(latitude: Double, longitude: Double, radius: Double = 10) async {
        isLoading = true
        error = nil
        
        do {
            let response = try await apiService.getHMGLLocationsNearby(
                latitude: latitude,
                longitude: longitude,
                radiusMiles: radius
            )
            nearbyLocations = response.results
            print("📍 Found \(nearbyLocations.count) nearby HMGL locations")
        } catch {
            self.error = error.localizedDescription
            print("❌ Failed to load nearby HMGL locations: \(error)")
        }
        
        isLoading = false
    }
    
    /// Load locations by city
    func loadLocationsByCity(_ city: String) async {
        isLoading = true
        error = nil
        selectedCity = city
        
        do {
            let response = try await apiService.getHMGLLocationsByCity(city: city)
            locations = response.results
            print("📍 Loaded \(locations.count) HMGL locations in \(city)")
        } catch {
            self.error = error.localizedDescription
            print("❌ Failed to load HMGL locations for \(city): \(error)")
        }
        
        isLoading = false
    }
    
    /// Load statistics
    func loadStats() async {
        do {
            stats = try await apiService.getHMGLStats()
            print("📊 HMGL Stats: \(stats?.totalLocations ?? 0) total locations")
        } catch {
            print("❌ Failed to load HMGL stats: \(error)")
        }
    }
    
    /// Search by program
    func searchByProgram(_ program: String) async {
        isLoading = true
        error = nil
        
        do {
            let response = try await apiService.getHMGLLocationsByProgram(program: program)
            locations = response.results
            print("📍 Found \(locations.count) HMGL locations with program: \(program)")
        } catch {
            self.error = error.localizedDescription
            print("❌ Failed to search HMGL programs: \(error)")
        }
        
        isLoading = false
    }
    
    /// Search by tag
    func searchByTag(_ tag: String) async {
        isLoading = true
        error = nil
        
        do {
            let response = try await apiService.getHMGLLocationsByTag(tag: tag)
            locations = response.results
            print("📍 Found \(locations.count) HMGL locations with tag: \(tag)")
        } catch {
            self.error = error.localizedDescription
            print("❌ Failed to search HMGL tags: \(error)")
        }
        
        isLoading = false
    }
    
    /// Clear all data
    func clear() {
        locations = []
        nearbyLocations = []
        stats = nil
        error = nil
        searchQuery = ""
        selectedCity = nil
    }
}
