//
//  String+Localization.swift
//  CHLA-iOS
//
//  Localization helper for SwiftUI
//  Using hardcoded strings for now - can add .strings file support later
//

import Foundation

extension String {
    /// Returns the localized version of this string key
    var localized: String {
        NSLocalizedString(self, comment: "")
    }
    
    /// Returns the localized version with format arguments
    func localized(with arguments: CVarArg...) -> String {
        String(format: self.localized, arguments: arguments)
    }
}

// MARK: - Localization Keys (Hardcoded English)

/// Centralized localization - hardcoded for reliability
enum L10n {
    // MARK: - Common
    enum Common {
        static let search = "Search"
        static let cancel = "Cancel"
        static let close = "Close"
        static let save = "Save"
        static let loading = "Loading..."
        static let error = "Error"
        static let refresh = "Refresh"
        static let clearAll = "Clear All"
        static let seeAll = "See All"
        static let continueText = "Continue"
        static let settings = "Settings"
    }
    
    // MARK: - Navigation
    enum Nav {
        static let home = "Home"
        static let map = "Map"
        static let regions = "Regions"
        static let browse = "Browse"
        static let more = "More"
    }
    
    // MARK: - Home
    enum Home {
        static let resourceNavigator = "Resource Navigator"
        static let tagline = "Find developmental disability services\nin Los Angeles County"
        static let enterZip = "Enter ZIP code"
        static let quickActions = "Quick Actions"
        static let nearMe = "Near Me"
        static let useLocation = "Use location"
        static let map = "Map"
        static let explore = "Explore"
        static let browse = "Browse"
        static let allResources = "All resources"
        static let therapyTypes = "Therapy Types"
        static let yourRegionalCenter = "Your Regional Center"
        static let laCountyRegionalCenters = "LA County Regional Centers"
        static let sevenRegionalCenters = "7 Regional Centers"
        static let findYourCenter = "Find your center by location"
        static let atAGlance = "At a Glance"
        static let resources = "Resources"
        static let centers = "Centers"
        static let free = "Free"
        static let always = "Always"
        static let changePreferences = "Change My Preferences"
        static let changePreferencesMessage = "This will guide you through the setup to update your location, age group, and therapy preferences."
    }
    
    // MARK: - Resources
    enum Resources {
        static let title = "Resources"
        static let searchPrompt = "Search resources, services, or ZIP code"
        static let loading = "Loading resources..."
        static let noResults = "No Resources Found"
        static let noResultsDescription = "Try adjusting your search filters or expanding your search radius."
        static func found(_ count: Int) -> String {
            count == 1 ? "\(count) resource found" : "\(count) resources found"
        }
        static let clearSearch = "Clear search"
        static let popularSearches = "Popular Searches"
        static let sortBy = "Sort by"
        static let distance = "Distance"
        static let name = "Name"
    }
    
    // MARK: - Provider
    enum Provider {
        static let away = "away"
        static let directions = "Directions"
        static let website = "Website"
        static let services = "Services"
        static let ages = "Ages"
        static let accepts = "Accepts"
        static let more = "more"
        static let call = "Call"
        static let getDirections = "Get Directions"
    }
    
    // MARK: - Regional Centers
    enum RC {
        static let title = "Regional Centers"
        static let laCounty = "Los Angeles County"
        static let findByZip = "Find by ZIP Code"
        static let viewOnMap = "View on Map"
        static let contact = "Contact"
        static let website = "Website"
        static let servesAreas = "Serves areas"
    }
    
    // MARK: - Filters
    enum Filters {
        static let title = "Filters"
        static let therapyType = "Therapy Type"
        static let ageGroup = "Age Group"
        static let insurance = "Insurance"
        static let diagnosis = "Diagnosis"
        static let apply = "Apply Filters"
        static let reset = "Reset"
    }
    
    // MARK: - About
    enum About {
        static let title = "About KINDD"
        static let mission = "Our Mission"
        static let missionText = "We create hope and build healthier futures by connecting families with the autism and developmental disability services they need."
        static let version = "Version"
        static let contact = "Contact Us"
        static let website = "Visit Website"
    }
    
    // MARK: - FAQ
    enum FAQ {
        static let title = "FAQ"
        static let subtitle = "Frequently Asked Questions"
    }
    
    // MARK: - Onboarding
    enum Onboarding {
        static let welcome = "Welcome to KINDD"
        static let welcomeSubtitle = "Find developmental disability resources in Los Angeles County"
        static let getStarted = "Get Started"
        static let skip = "Skip"
        static let next = "Next"
        static let done = "Done"
        static let locationTitle = "Enable Location"
        static let locationSubtitle = "Find resources near you"
    }
    
    // MARK: - Settings
    enum Settings {
        static let title = "Settings"
        static let resetOnboarding = "Reset Onboarding"
        static let about = "About"
        static let faq = "FAQ"
        static let privacy = "Privacy Policy"
        static let terms = "Terms of Service"
    }
    
    // MARK: - Errors
    enum Error {
        static let network = "Network error. Please check your connection."
        static let loadingFailed = "Failed to load data. Please try again."
        static let locationDenied = "Location access denied. Enable in Settings."
    }
    
    // MARK: - Chat / AI Assistant
    enum Chat {
        static let title = "Ask KiNDD"
        static let inputPlaceholder = "Ask about services, providers..."
        static let send = "Send"
        static let cancel = "Cancel"
        static let clearChat = "Clear Chat"
        static let clearConfirmTitle = "Clear Conversation"
        static let clearConfirmMessage = "This will delete all messages. This action cannot be undone."
        static let welcomeTitle = "Hi! I'm KiNDD"
        static let welcomeSubtitle = "Your AI assistant for finding developmental disability resources in LA County"
        static let welcomeHint = "Try asking me about:"
        static let suggestion1 = "Find ABA providers near me"
        static let suggestion2 = "Which Regional Center serves my ZIP code?"
        static let suggestion3 = "What insurance covers ABA therapy?"
        static let suggestion4 = "How do I get started with Early Intervention?"
        static let findProviders = "Find providers"
        static let assessment = "Assessment"
        static let myRC = "My RC"
        static let earlyStart = "Early Start"
        static let insurance = "Insurance"
        static let waitlists = "Waitlists"
        static let age3Transition = "Age 3 transition"
        static let speech = "Speech"
        static let copied = "Copied!"
        static let thinking = "Thinking..."
    }
}
