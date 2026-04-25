//
//  String+Localization.swift
//  CHLA-iOS
//
//  Localization helper for SwiftUI with dynamic language switching
//

import Foundation

extension String {
    /// Returns the localized version of this string key using the app's current language (thread-safe)
    var localized: String {
        LocalizationHelper.localizedString(for: self)
    }
    
    /// Returns the localized version with format arguments
    func localizedWithArgs(_ arguments: CVarArg...) -> String {
        String(format: self.localized, arguments: arguments)
    }
}

// MARK: - Localization Keys
/// Centralized localization - uses Localizable.strings with dynamic language support
enum L10n {
    // MARK: - Common
    enum Common {
        static var search: String { "common.search".localized }
        static var cancel: String { "common.cancel".localized }
        static var close: String { "common.close".localized }
        static var save: String { "common.save".localized }
        static var loading: String { "common.loading".localized }
        static var error: String { "common.error".localized }
        static var refresh: String { "common.refresh".localized }
        static var clearAll: String { "common.clearAll".localized }
        static var seeAll: String { "common.seeAll".localized }
        static var continueText: String { "common.continue".localized }
        static var settings: String { "common.settings".localized }
    }
    
    // MARK: - Navigation
    enum Nav {
        static var home: String { "nav.home".localized }
        static var map: String { "nav.map".localized }
        static var regions: String { "nav.regions".localized }
        static var browse: String { "nav.browse".localized }
        static var more: String { "nav.more".localized }
    }
    
    // MARK: - Home
    enum Home {
        static var resourceNavigator: String { "home.resourceNavigator".localized }
        static var tagline: String { "home.tagline".localized }
        static var enterZip: String { "home.enterZip".localized }
        static var quickActions: String { "home.quickActions".localized }
        static var nearMe: String { "home.nearMe".localized }
        static var useLocation: String { "home.useLocation".localized }
        static var map: String { "home.map".localized }
        static var explore: String { "home.explore".localized }
        static var browse: String { "home.browse".localized }
        static var allResources: String { "home.allResources".localized }
        static var therapyTypes: String { "home.therapyTypes".localized }
        static var yourRegionalCenter: String { "home.yourRegionalCenter".localized }
        static var laCountyRegionalCenters: String { "home.laCountyRegionalCenters".localized }
        static var sevenRegionalCenters: String { "home.7RegionalCenters".localized }
        static var findYourCenter: String { "home.findYourCenter".localized }
        static var atAGlance: String { "home.atAGlance".localized }
        static var resources: String { "home.resources".localized }
        static var centers: String { "home.centers".localized }
        static var free: String { "home.free".localized }
        static var always: String { "home.always".localized }
        static var changePreferences: String { "home.changePreferences".localized }
        static var changePreferencesMessage: String { "home.changePreferencesMessage".localized }
    }
    
    // MARK: - Resources
    enum Resources {
        static var title: String { "resources.title".localized }
        static var searchPrompt: String { "resources.searchPrompt".localized }
        static var loading: String { "resources.loading".localized }
        static var noResults: String { "resources.noResults".localized }
        static var noResultsDescription: String { "resources.noResultsDescription".localized }
        static func found(_ count: Int) -> String {
            let key = count == 1 ? "resources.foundSingular" : "resources.foundPlural"
            return String(format: key.localized, count)
        }
        static var clearSearch: String { "resources.clearSearch".localized }
        static var popularSearches: String { "resources.popularSearches".localized }
        static var sortBy: String { "resources.sortBy".localized }
        static var distance: String { "resources.distance".localized }
        static var name: String { "resources.name".localized }
    }
    
    // MARK: - Provider
    enum Provider {
        static var away: String { "provider.away".localized }
        static var directions: String { "provider.directions".localized }
        static var website: String { "provider.website".localized }
        static var services: String { "provider.services".localized }
        static var ages: String { "provider.ages".localized }
        static var accepts: String { "provider.accepts".localized }
        static var more: String { "provider.more".localized }
        static var call: String { "provider.call".localized }
        static var getDirections: String { "provider.getDirections".localized }
    }
    
    // MARK: - Regional Centers
    enum RC {
        static var title: String { "rc.title".localized }
        static var laCounty: String { "rc.laCounty".localized }
        static var findByZip: String { "rc.findByZip".localized }
        static var viewOnMap: String { "rc.viewOnMap".localized }
        static var contact: String { "rc.contact".localized }
        static var website: String { "rc.website".localized }
        static var servesAreas: String { "rc.servesAreas".localized }
    }
    
    // MARK: - Filters
    enum Filters {
        static var title: String { "filters.title".localized }
        static var therapyType: String { "filters.therapyType".localized }
        static var ageGroup: String { "filters.ageGroup".localized }
        static var insurance: String { "filters.insurance".localized }
        static var diagnosis: String { "filters.diagnosis".localized }
        static var apply: String { "filters.apply".localized }
        static var reset: String { "filters.reset".localized }
    }
    
    // MARK: - About
    enum About {
        static var title: String { "about.title".localized }
        static var pageTitle: String { "about.pageTitle".localized }
        static var heroSubtitle: String { "about.heroSubtitle".localized }
        static var ourMission: String { "about.ourMission".localized }
        static var missionText1: String { "about.missionText1".localized }
        static var missionText2: String { "about.missionText2".localized }
        static var whatWeDo: String { "about.whatWeDo".localized }
        static var featureMap: String { "about.featureMap".localized }
        static var featureMapDesc: String { "about.featureMapDesc".localized }
        static var featureLocation: String { "about.featureLocation".localized }
        static var featureLocationDesc: String { "about.featureLocationDesc".localized }
        static var featureFilters: String { "about.featureFilters".localized }
        static var featureFiltersDesc: String { "about.featureFiltersDesc".localized }
        static var featureRC: String { "about.featureRC".localized }
        static var featureRCDesc: String { "about.featureRCDesc".localized }
        static var featureContact: String { "about.featureContact".localized }
        static var featureContactDesc: String { "about.featureContactDesc".localized }
        static var laCountyRC: String { "about.laCountyRC".localized }
        static var rcDescription: String { "about.rcDescription".localized }
        static var rcZipNote: String { "about.rcZipNote".localized }
        static var howItWorks: String { "about.howItWorks".localized }
        static var step1: String { "about.step1".localized }
        static var step2: String { "about.step2".localized }
        static var step3: String { "about.step3".localized }
        static var step4: String { "about.step4".localized }
        static var whoWeServe: String { "about.whoWeServe".localized }
        static var servingFamilies: String { "about.servingFamilies".localized }
        static var autism: String { "about.autism".localized }
        static var developmental: String { "about.developmental".localized }
        static var intellectual: String { "about.intellectual".localized }
        static var communication: String { "about.communication".localized }
        static var learning: String { "about.learning".localized }
        static var freeToUse: String { "about.freeToUse".localized }
        static var freeDescription: String { "about.freeDescription".localized }
        static var readyToFind: String { "about.readyToFind".localized }
        static var exploreMap: String { "about.exploreMap".localized }
        static var servingLA: String { "about.servingLA".localized }
        // Kept for backwards compatibility
        static var mission: String { "about.mission".localized }
        static var missionText: String { "about.missionText".localized }
        static var version: String { "about.version".localized }
        static var contact: String { "about.contact".localized }
        static var website: String { "about.website".localized }
    }
    
    // MARK: - FAQ
    enum FAQ {
        static var title: String { "faq.title".localized }
        static var subtitle: String { "faq.subtitle".localized }
        static var pageTitle: String { "faq.pageTitle".localized }
        static var heroSubtitle: String { "faq.heroSubtitle".localized }
        static var abaBasics: String { "faq.abaBasics".localized }
        static var regionalCenters: String { "faq.regionalCenters".localized }
        static var insuranceFunding: String { "faq.insuranceFunding".localized }
        static var usingApp: String { "faq.usingApp".localized }
        static var sources: String { "faq.sources".localized }
        static var sourcesIntro: String { "faq.sourcesIntro".localized }
        static var disclaimer: String { "faq.disclaimer".localized }
    }
    
    // MARK: - Onboarding
    enum Onboarding {
        static var welcome: String { "onboarding.welcome".localized }
        static var welcomeSubtitle: String { "onboarding.welcomeSubtitle".localized }
        static var getStarted: String { "onboarding.getStarted".localized }
        static var skip: String { "onboarding.skip".localized }
        static var next: String { "onboarding.next".localized }
        static var done: String { "onboarding.done".localized }
        static var locationTitle: String { "onboarding.locationTitle".localized }
        static var locationSubtitle: String { "onboarding.locationSubtitle".localized }
    }
    
    // MARK: - Settings
    enum Settings {
        static var title: String { "settings.title".localized }
        static var resetOnboarding: String { "settings.resetOnboarding".localized }
        static var about: String { "settings.about".localized }
        static var faq: String { "settings.faq".localized }
        static var privacy: String { "settings.privacy".localized }
        static var terms: String { "settings.terms".localized }
    }
    
    // MARK: - Errors
    enum Error {
        static var network: String { "error.network".localized }
        static var loadingFailed: String { "error.loadingFailed".localized }
        static var locationDenied: String { "error.locationDenied".localized }
    }
    
    // MARK: - Chat / AI Assistant
    enum Chat {
        static var title: String { "chat.title".localized }
        static var inputPlaceholder: String { "chat.inputPlaceholder".localized }
        static var send: String { "chat.send".localized }
        static var cancel: String { "chat.cancel".localized }
        static var clearChat: String { "chat.clearChat".localized }
        static var clearConfirmTitle: String { "chat.clearConfirmTitle".localized }
        static var clearConfirmMessage: String { "chat.clearConfirmMessage".localized }
        static var welcomeTitle: String { "chat.welcomeTitle".localized }
        static var welcomeSubtitle: String { "chat.welcomeSubtitle".localized }
        static var welcomeHint: String { "chat.welcomeHint".localized }
        static var suggestion1: String { "chat.suggestion1".localized }
        static var suggestion2: String { "chat.suggestion2".localized }
        static var suggestion3: String { "chat.suggestion3".localized }
        static var suggestion4: String { "chat.suggestion4".localized }
        static var findProviders: String { "chat.findProviders".localized }
        static var assessment: String { "chat.assessment".localized }
        static var myRC: String { "chat.myRC".localized }
        static var earlyStart: String { "chat.earlyStart".localized }
        static var insurance: String { "chat.insurance".localized }
        static var waitlists: String { "chat.waitlists".localized }
        static var age3Transition: String { "chat.age3Transition".localized }
        static var speech: String { "chat.speech".localized }
        static var copied: String { "chat.copied".localized }
        static var thinking: String { "chat.thinking".localized }
        // Prompt strings for quick action buttons
        static var assessmentPrompt: String { "chat.assessmentPrompt".localized }
        static var waitlistsPrompt: String { "chat.waitlistsPrompt".localized }
        static var age3Prompt: String { "chat.age3Prompt".localized }
        static var speechPrompt: String { "chat.speechPrompt".localized }
    }
    
    // MARK: - Language
    enum Language {
        static var title: String { "language.title".localized }
        static var system: String { "language.system".localized }
        static var english: String { "language.english".localized }
        static var spanish: String { "language.spanish".localized }
    }
}
