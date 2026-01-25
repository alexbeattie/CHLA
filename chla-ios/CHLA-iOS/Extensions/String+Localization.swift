//
//  String+Localization.swift
//  CHLA-iOS
//
//  Localization helper for SwiftUI
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

// MARK: - Localization Keys

/// Centralized localization keys for type-safety
enum L10n {
    // MARK: - Common
    enum Common {
        static let search = "common.search".localized
        static let cancel = "common.cancel".localized
        static let close = "common.close".localized
        static let save = "common.save".localized
        static let loading = "common.loading".localized
        static let error = "common.error".localized
        static let refresh = "common.refresh".localized
        static let clearAll = "common.clearAll".localized
        static let seeAll = "common.seeAll".localized
        static let continueText = "common.continue".localized
        static let settings = "common.settings".localized
    }
    
    // MARK: - Navigation
    enum Nav {
        static let home = "nav.home".localized
        static let map = "nav.map".localized
        static let regions = "nav.regions".localized
        static let browse = "nav.browse".localized
        static let more = "nav.more".localized
    }
    
    // MARK: - Home
    enum Home {
        static let resourceNavigator = "home.resourceNavigator".localized
        static let tagline = "home.tagline".localized
        static let enterZip = "home.enterZip".localized
        static let quickActions = "home.quickActions".localized
        static let nearMe = "home.nearMe".localized
        static let useLocation = "home.useLocation".localized
        static let map = "home.map".localized
        static let explore = "home.explore".localized
        static let browse = "home.browse".localized
        static let allResources = "home.allResources".localized
        static let therapyTypes = "home.therapyTypes".localized
        static let yourRegionalCenter = "home.yourRegionalCenter".localized
        static let laCountyRegionalCenters = "home.laCountyRegionalCenters".localized
        static let sevenRegionalCenters = "home.7RegionalCenters".localized
        static let findYourCenter = "home.findYourCenter".localized
        static let atAGlance = "home.atAGlance".localized
        static let resources = "home.resources".localized
        static let centers = "home.centers".localized
        static let free = "home.free".localized
        static let always = "home.always".localized
        static let changePreferences = "home.changePreferences".localized
        static let changePreferencesMessage = "home.changePreferencesMessage".localized
    }
    
    // MARK: - Resources
    enum Resources {
        static let title = "resources.title".localized
        static let searchPrompt = "resources.searchPrompt".localized
        static let loading = "resources.loading".localized
        static let noResults = "resources.noResults".localized
        static let noResultsDescription = "resources.noResultsDescription".localized
        static func found(_ count: Int) -> String {
            "resources.found".localized(with: count)
        }
        static let clearSearch = "resources.clearSearch".localized
        static let popularSearches = "resources.popularSearches".localized
        static let sortBy = "resources.sortBy".localized
        static let distance = "resources.distance".localized
        static let name = "resources.name".localized
    }
    
    // MARK: - Provider
    enum Provider {
        static let away = "provider.away".localized
        static let directions = "provider.directions".localized
        static let website = "provider.website".localized
        static let services = "provider.services".localized
        static let ages = "provider.ages".localized
        static let accepts = "provider.accepts".localized
        static let more = "provider.more".localized
        static let call = "provider.call".localized
        static let getDirections = "provider.getDirections".localized
    }
    
    // MARK: - Regional Centers
    enum RC {
        static let title = "rc.title".localized
        static let laCounty = "rc.laCounty".localized
        static let findByZip = "rc.findByZip".localized
        static let viewOnMap = "rc.viewOnMap".localized
        static let contact = "rc.contact".localized
        static let website = "rc.website".localized
        static let servesAreas = "rc.servesAreas".localized
    }
    
    // MARK: - Filters
    enum Filters {
        static let title = "filters.title".localized
        static let therapyType = "filters.therapyType".localized
        static let ageGroup = "filters.ageGroup".localized
        static let insurance = "filters.insurance".localized
        static let diagnosis = "filters.diagnosis".localized
        static let apply = "filters.apply".localized
        static let reset = "filters.reset".localized
    }
    
    // MARK: - About
    enum About {
        static let title = "about.title".localized
        static let mission = "about.mission".localized
        static let missionText = "about.missionText".localized
        static let version = "about.version".localized
        static let contact = "about.contact".localized
        static let website = "about.website".localized
    }
    
    // MARK: - FAQ
    enum FAQ {
        static let title = "faq.title".localized
        static let subtitle = "faq.subtitle".localized
    }
    
    // MARK: - Onboarding
    enum Onboarding {
        static let welcome = "onboarding.welcome".localized
        static let welcomeSubtitle = "onboarding.welcomeSubtitle".localized
        static let getStarted = "onboarding.getStarted".localized
        static let skip = "onboarding.skip".localized
        static let next = "onboarding.next".localized
        static let done = "onboarding.done".localized
        static let locationTitle = "onboarding.locationTitle".localized
        static let locationSubtitle = "onboarding.locationSubtitle".localized
    }
    
    // MARK: - Settings
    enum Settings {
        static let title = "settings.title".localized
        static let resetOnboarding = "settings.resetOnboarding".localized
        static let about = "settings.about".localized
        static let faq = "settings.faq".localized
        static let privacy = "settings.privacy".localized
        static let terms = "settings.terms".localized
    }
    
    // MARK: - Errors
    enum Error {
        static let network = "error.network".localized
        static let loadingFailed = "error.loadingFailed".localized
        static let locationDenied = "error.locationDenied".localized
    }
}
