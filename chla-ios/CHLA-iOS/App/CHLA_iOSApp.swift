//
//  CHLA_iOSApp.swift
//  CHLA-iOS
//
//  Healthcare Provider Map for LA County Regional Centers
//

import SwiftUI
import WidgetKit
import AppIntents

@main
struct CHLA_iOSApp: App {
    @StateObject private var appState = AppState()
    @Environment(\.openURL) private var openURL

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .onOpenURL { url in
                    handleDeepLink(url)
                }
                .onReceive(NotificationCenter.default.publisher(for: .openChatFromIntent)) { note in
                    appState.openChat(prompt: note.userInfo?["prompt"] as? String)
                }
        }
    }

    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "kindd" else { return }

        switch url.host {
        case "call":
            if let shared = UserDefaults(suiteName: AppState.appGroupID),
               let phone = shared.string(forKey: "widget.centerPhone") {
                let digits = phone.filter(\.isNumber)
                if let telURL = URL(string: "tel://\(digits)") {
                    openURL(telURL)
                }
            }
        case "chat":
            appState.openChat()
        default:
            break
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

/// Where a family is in the developmental-services process; drives the home
/// screen's next-step guidance and onboarding
enum JourneyStage: String, CaseIterable, Identifiable {
    case justDiagnosed
    case waitingIntake
    case receivingServices
    case exploring

    var id: String { rawValue }

    var label: String {
        switch self {
        case .justDiagnosed: return "We just got a diagnosis"
        case .waitingIntake: return "Waiting for intake or evaluation"
        case .receivingServices: return "Already receiving services"
        case .exploring: return "Just exploring"
        }
    }

    var icon: String {
        switch self {
        case .justDiagnosed: return "sparkles"
        case .waitingIntake: return "hourglass"
        case .receivingServices: return "checkmark.seal.fill"
        case .exploring: return "binoculars.fill"
        }
    }
}

/// Global application state managing user preferences and session data
@MainActor
class AppState: ObservableObject {
    /// Shared container for the widget extension
    static let appGroupID = "group.com.nddresources.map"

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
    @Published var showChat = false
    @Published var pendingChatPrompt: String?
    @Published var userJourneyStage: String?

    var journeyStage: JourneyStage? {
        userJourneyStage.flatMap(JourneyStage.init(rawValue:))
    }

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
        self.userJourneyStage = UserDefaults.standard.string(forKey: "userJourneyStage")

        syncSharedDefaults()
    }

    /// Mirrors the widget-relevant state into the App Group container so the
    /// home-screen widget always shows the current regional center
    func syncSharedDefaults() {
        guard let shared = UserDefaults(suiteName: Self.appGroupID) else { return }

        shared.set(userRegionalCenterName, forKey: "widget.centerName")
        shared.set(userRegionalCenterShortName, forKey: "widget.centerShortName")
        shared.set(userZipCode, forKey: "widget.zipCode")
        shared.set(userJourneyStage, forKey: "widget.journeyStage")

        if let shortName = userRegionalCenterShortName,
           let match = RegionalCenterMatcher.shared.laRegionalCenters.first(where: {
               $0.shortName.replacingOccurrences(of: "/", with: "").uppercased()
                   == shortName.replacingOccurrences(of: "/", with: "").uppercased()
           }) {
            shared.set(match.phone, forKey: "widget.centerPhone")
        } else {
            shared.removeObject(forKey: "widget.centerPhone")
        }
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

        syncSharedDefaults()
        WidgetCenter.shared.reloadAllTimelines()
    }

    func saveJourneyStage(_ stage: JourneyStage) {
        userJourneyStage = stage.rawValue
        UserDefaults.standard.set(stage.rawValue, forKey: "userJourneyStage")

        syncSharedDefaults()
        WidgetCenter.shared.reloadAllTimelines()
    }

    func completeOnboarding() {
        UserDefaults.standard.set(true, forKey: "hasCompletedOnboarding")
        isOnboarding = false
    }

    func resetOnboarding() {
        UserDefaults.standard.set(false, forKey: "hasCompletedOnboarding")
        isOnboarding = true
    }

    /// Open the chat sheet, optionally sending a question immediately
    func openChat(prompt: String? = nil) {
        pendingChatPrompt = prompt
        showChat = true
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

// MARK: - App Intents

extension Notification.Name {
    static let openChatFromIntent = Notification.Name("openChatFromIntent")
}

/// Siri / Spotlight / Shortcuts: dial the family's matched regional center
struct CallRegionalCenterIntent: AppIntent {
    static let title: LocalizedStringResource = "Call My Regional Center"
    static let description = IntentDescription("Calls the regional center that serves your family.")
    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard let shared = UserDefaults(suiteName: AppState.appGroupID),
              let phone = shared.string(forKey: "widget.centerPhone"),
              !phone.isEmpty else {
            return .result(dialog: "You haven't been matched to a regional center yet. Enter your ZIP code in KiNDD first.")
        }

        let digits = phone.filter(\.isNumber)
        guard let telURL = URL(string: "tel://\(digits)") else {
            return .result(dialog: "That phone number doesn't look right. Open KiNDD to see your regional center's contact details.")
        }

        await UIApplication.shared.open(telURL)
        return .result(dialog: "Calling your regional center.")
    }
}

/// Siri / Spotlight / Shortcuts: open the KiNDD assistant, optionally with a question
struct AskKiNDDIntent: AppIntent {
    static let title: LocalizedStringResource = "Ask KiNDD"
    static let description = IntentDescription("Opens the KiNDD assistant to help with developmental services.")
    static let openAppWhenRun = true

    @Parameter(title: "Question")
    var question: String?

    @MainActor
    func perform() async throws -> some IntentResult {
        var userInfo: [AnyHashable: Any] = [:]
        if let question, !question.isEmpty {
            userInfo["prompt"] = question
        }
        NotificationCenter.default.post(name: .openChatFromIntent, object: nil, userInfo: userInfo)
        return .result()
    }
}

/// Surfaces both intents in Spotlight and Siri with zero user setup
struct KiNDDShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: CallRegionalCenterIntent(),
            phrases: [
                "Call my regional center in \(.applicationName)",
                "Call my regional center with \(.applicationName)"
            ],
            shortTitle: "Call Regional Center",
            systemImageName: "phone.fill"
        )
        AppShortcut(
            intent: AskKiNDDIntent(),
            phrases: [
                "Ask \(.applicationName)",
                "Ask \(.applicationName) a question"
            ],
            shortTitle: "Ask KiNDD",
            systemImageName: "sparkles"
        )
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
