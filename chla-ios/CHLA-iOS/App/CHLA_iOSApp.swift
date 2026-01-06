//
//  CHLA_iOSApp.swift
//  CHLA-iOS
//
//  Healthcare Provider Map for LA County Regional Centers
//

import SwiftUI

@main
struct CHLA_iOSApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}

// MARK: - App State
/// Global application state managing user preferences and session data
@MainActor
class AppState: ObservableObject {
    @Published var isOnboarding: Bool
    @Published var userLocation: Location?
    @Published var selectedRegionalCenter: RegionalCenter?
    @Published var searchFilters: SearchFilters
    @Published var selectedTab: Int = 0

    init() {
        // Check if user has completed onboarding
        self.isOnboarding = !UserDefaults.standard.bool(forKey: "hasCompletedOnboarding")
        self.searchFilters = SearchFilters()
    }

    func completeOnboarding() {
        UserDefaults.standard.set(true, forKey: "hasCompletedOnboarding")
        isOnboarding = false
    }

    func resetOnboarding() {
        UserDefaults.standard.set(false, forKey: "hasCompletedOnboarding")
        isOnboarding = true
    }

    /// Navigate to a specific tab
    func navigateToTab(_ tab: Int) {
        selectedTab = tab
    }

    /// Navigate to home tab
    func navigateToHome() {
        selectedTab = 0
    }

    /// Navigate to map tab
    func navigateToMap() {
        selectedTab = 1
    }

    /// Navigate to regions tab
    func navigateToRegions() {
        selectedTab = 2
    }

    /// Navigate to browse/list tab
    func navigateToBrowse() {
        selectedTab = 3
    }
}

// MARK: - Location
/// Simple location struct for user position
struct Location: Codable, Equatable {
    let latitude: Double
    let longitude: Double
}

// MARK: - Search Filters
/// User's search filter preferences
struct SearchFilters: Codable, Equatable {
    var ageGroup: String?
    var diagnosis: String?
    var therapyTypes: [String] = []
    var insurance: String?
    var radiusMiles: Double = 15.0

    static let ageGroups = ["0-5", "6-12", "13-18", "19+", "All Ages"]

    static let diagnoses = [
        "Autism Spectrum Disorder",
        "Global Development Delay",
        "Intellectual Disability",
        "Speech and Language Disorder",
        "Other"
    ]

    static let therapyTypes = [
        "ABA therapy",
        "Speech therapy",
        "Occupational therapy",
        "Physical therapy",
        "Feeding therapy",
        "Parent child interaction therapy/parent training behavior management"
    ]

    static let insuranceOptions = [
        "Regional Center",
        "Private Pay",
        "Medi-Cal",
        "Medicare",
        "Blue Cross",
        "Blue Shield",
        "Anthem",
        "Aetna",
        "Cigna",
        "Kaiser Permanente",
        "United Healthcare",
        "Health Net",
        "Molina",
        "L.A. Care",
        "Covered California"
    ]
}
