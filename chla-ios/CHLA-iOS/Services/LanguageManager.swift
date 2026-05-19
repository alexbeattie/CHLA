//
//  LanguageManager.swift
//  CHLA-iOS
//
//  Manages app language preference with support for English and Spanish
//

import SwiftUI
import Foundation

// MARK: - Supported Languages
enum AppLanguage: String, CaseIterable, Identifiable, Sendable {
    case system = "system"
    case english = "en"
    case spanish = "es"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .system: return "System Default"
        case .english: return "English"
        case .spanish: return "Español"
        }
    }

    var localeIdentifier: String? {
        switch self {
        case .system: return nil
        case .english: return "en"
        case .spanish: return "es"
        }
    }
}

// MARK: - Language Manager
@MainActor
class LanguageManager: ObservableObject {
    static let shared = LanguageManager()

    private let userDefaultsKey = "appLanguage"

    @Published var currentLanguage: AppLanguage {
        didSet {
            saveLanguagePreference()
            applyLanguage()
        }
    }

    /// The effective locale based on current language setting
    var effectiveLocale: Locale {
        if let identifier = currentLanguage.localeIdentifier {
            return Locale(identifier: identifier)
        }
        return Locale.current
    }

    private init() {
        // Load saved preference or default to system
        if let saved = UserDefaults.standard.string(forKey: userDefaultsKey),
           let language = AppLanguage(rawValue: saved) {
            self.currentLanguage = language
        } else {
            self.currentLanguage = .system
        }
    }

    private func saveLanguagePreference() {
        UserDefaults.standard.set(currentLanguage.rawValue, forKey: userDefaultsKey)
    }

    private func applyLanguage() {
        // Force update of any cached strings
        objectWillChange.send()
    }
}

// MARK: - Thread-Safe Localization Helper
/// Provides thread-safe access to localized strings
enum LocalizationHelper {
    private static let userDefaultsKey = "appLanguage"

    /// Get the current language bundle (thread-safe)
    static var currentBundle: Bundle {
        // Read from UserDefaults directly for thread safety
        let languageCode: String? = {
            guard let saved = UserDefaults.standard.string(forKey: userDefaultsKey),
                  let language = AppLanguage(rawValue: saved) else {
                return nil
            }
            return language.localeIdentifier
        }()

        guard let code = languageCode,
              let path = Bundle.main.path(forResource: code, ofType: "lproj"),
              let bundle = Bundle(path: path) else {
            return Bundle.main
        }
        return bundle
    }

    /// Get a localized string for the given key (thread-safe)
    static func localizedString(for key: String) -> String {
        return NSLocalizedString(key, bundle: currentBundle, comment: "")
    }
}
