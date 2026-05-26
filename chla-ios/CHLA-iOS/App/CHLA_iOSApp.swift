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
struct ProviderMapTarget: Identifiable, Equatable {
    let id = UUID()
    let query: String
    let providers: [Provider]

    init(query: String, providers: [Provider] = []) {
        self.query = query
        self.providers = providers
    }
}

/// Global application state managing user preferences and session data
@MainActor
class AppState: ObservableObject {
    @Published var isOnboarding: Bool
    @Published var userLocation: Location?
    @Published var selectedRegionalCenter: RegionalCenter?
    @Published var searchFilters: SearchFilters
    @Published var selectedTab: Int = 0
    @Published var userZipCode: String?
    @Published var userChildAge: Int?
    @Published var userDiagnosis: String?
    @Published var userInsurance: String?
    @Published var userAudienceType: String
    @Published var userRegionalCenterName: String?
    @Published var userRegionalCenterShortName: String?
    @Published var pendingMapProviderTarget: ProviderMapTarget?

    var userRegionalCenterColor: Color {
        guard let shortName = userRegionalCenterShortName else {
            return .accentBlue
        }

        return .regionalCenterColor(for: shortName)
    }

    init() {
        // Check if user has completed onboarding
        self.isOnboarding = !UserDefaults.standard.bool(forKey: "hasCompletedOnboarding")
        self.searchFilters = SearchFilters()

        // Load saved user context
        self.userZipCode = UserDefaults.standard.string(forKey: "userZipCode")
        self.userChildAge = UserDefaults.standard.object(forKey: "userChildAge") as? Int
        self.userDiagnosis = UserDefaults.standard.string(forKey: "userDiagnosis")
        self.userInsurance = UserDefaults.standard.string(forKey: "userInsurance")
        self.userAudienceType = UserDefaults.standard.string(forKey: "userAudienceType") ?? "family"
        self.userRegionalCenterName = UserDefaults.standard.string(forKey: "userRegionalCenterName")
        self.userRegionalCenterShortName = UserDefaults.standard.string(forKey: "userRegionalCenterShortName")
    }

    func saveUserContext(
        zipCode: String? = nil,
        childAge: Int? = nil,
        diagnosis: String? = nil,
        insurance: String? = nil,
        audienceType: String? = nil,
        regionalCenterName: String? = nil,
        regionalCenterShortName: String? = nil
    ) {
        if let zip = zipCode {
            userZipCode = zip
            UserDefaults.standard.set(zip, forKey: "userZipCode")
        }
        if let age = childAge {
            userChildAge = age
            UserDefaults.standard.set(age, forKey: "userChildAge")
        }
        if let dx = diagnosis {
            userDiagnosis = dx
            UserDefaults.standard.set(dx, forKey: "userDiagnosis")
        }
        if let ins = insurance {
            userInsurance = ins
            UserDefaults.standard.set(ins, forKey: "userInsurance")
        }
        if let audience = audienceType {
            userAudienceType = audience
            UserDefaults.standard.set(audience, forKey: "userAudienceType")
        }
        if let rcName = regionalCenterName {
            userRegionalCenterName = rcName
            UserDefaults.standard.set(rcName, forKey: "userRegionalCenterName")
        }
        if let rcShortName = regionalCenterShortName {
            userRegionalCenterShortName = rcShortName
            UserDefaults.standard.set(rcShortName, forKey: "userRegionalCenterShortName")
        }
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

    /// Navigate to the map and ask it to focus a provider by name.
    func showProviderOnMap(named providerName: String) {
        pendingMapProviderTarget = ProviderMapTarget(query: providerName)
        selectedTab = 1
    }

    /// Navigate to the map and show a known set of providers as pins.
    func showProvidersOnMap(_ providers: [Provider]) {
        pendingMapProviderTarget = ProviderMapTarget(
            query: providers.map(\.name).joined(separator: " "),
            providers: providers
        )
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
