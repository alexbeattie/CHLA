//
//  Color+Extensions.swift
//  CHLA-iOS
//
//  Custom colors for the app
//

import SwiftUI

extension Color {
    // MARK: - Brand Colors

    /// Primary accent blue - used for interactive elements
    static let accentBlue = Color(red: 0.24, green: 0.47, blue: 0.85)

    /// Secondary accent purple
    static let accentPurple = Color(red: 0.55, green: 0.35, blue: 0.85)

    /// Success green
    static let successGreen = Color(red: 0.25, green: 0.75, blue: 0.45)

    /// Warning orange
    static let warningOrange = Color(red: 0.95, green: 0.60, blue: 0.20)

    /// Error red
    static let errorRed = Color(red: 0.90, green: 0.30, blue: 0.30)

    // MARK: - Background Colors

    /// Primary background (adapts to light/dark mode)
    static let backgroundPrimary = Color(uiColor: .systemBackground)

    /// Secondary background for cards and sections
    static let backgroundSecondary = Color(uiColor: .secondarySystemBackground)

    /// Tertiary background for nested elements
    static let backgroundTertiary = Color(uiColor: .tertiarySystemBackground)

    // MARK: - Text Colors

    /// Primary text color
    static let textPrimary = Color(uiColor: .label)

    /// Secondary text color
    static let textSecondary = Color(uiColor: .secondaryLabel)

    /// Tertiary text color
    static let textTertiary = Color(uiColor: .tertiaryLabel)

    // MARK: - Regional Center Colors

    /// Colors for different regional centers on the map
    static let regionalCenterColors: [Color] = [
        Color(red: 0.24, green: 0.47, blue: 0.85),  // Blue
        Color(red: 0.55, green: 0.35, blue: 0.85),  // Purple
        Color(red: 0.25, green: 0.75, blue: 0.45),  // Green
        Color(red: 0.95, green: 0.60, blue: 0.20),  // Orange
        Color(red: 0.85, green: 0.35, blue: 0.55),  // Pink
        Color(red: 0.35, green: 0.70, blue: 0.75),  // Teal
        Color(red: 0.65, green: 0.50, blue: 0.30),  // Brown
        Color(red: 0.45, green: 0.55, blue: 0.70)  // Steel Blue
    ]

    /// Get a color for a regional center by index
    static func regionalCenterColor(at index: Int) -> Color {
        regionalCenterColors[index % regionalCenterColors.count]
    }

    // MARK: - Provider Type Colors

    /// Color for ABA therapy providers
    static let providerABA = Color(red: 0.24, green: 0.47, blue: 0.85)

    /// Color for Speech therapy providers
    static let providerSpeech = Color(red: 0.55, green: 0.35, blue: 0.85)

    /// Color for Occupational therapy providers
    static let providerOT = Color(red: 0.25, green: 0.75, blue: 0.45)

    /// Color for Physical therapy providers
    static let providerPT = Color(red: 0.95, green: 0.60, blue: 0.20)

    /// Get color for provider type
    static func providerColor(for type: String?) -> Color {
        guard let type = type?.lowercased() else { return .accentBlue }

        if type.contains("aba") { return .providerABA }
        if type.contains("speech") { return .providerSpeech }
        if type.contains("occupational") { return .providerOT }
        if type.contains("physical") { return .providerPT }

        return .accentBlue
    }
}

// MARK: - Color Hex Support
extension Color {
    /// Initialize a color from a hex string
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)

        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
