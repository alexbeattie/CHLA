//
//  Haptics.swift
//  CHLA-iOS
//
//  Central haptic feedback for the app
//

import UIKit

/// Single entry point for haptic feedback. Generators are cached and kept
/// prepared so feedback fires the moment a touch lands, and every call site
/// shares the same intensity vocabulary instead of choosing a style inline.
enum Haptics {
    private static let lightGenerator = UIImpactFeedbackGenerator(style: .light)
    private static let mediumGenerator = UIImpactFeedbackGenerator(style: .medium)
    private static let notificationGenerator = UINotificationFeedbackGenerator()

    /// Light impact - selections, chips, toggles, dismissals
    static func tap() {
        lightGenerator.impactOccurred()
        lightGenerator.prepare()
    }

    /// Medium impact - commits, sends, primary actions
    static func action() {
        mediumGenerator.impactOccurred()
        mediumGenerator.prepare()
    }

    /// Notification - a meaningful operation completed
    static func success() {
        notificationGenerator.notificationOccurred(.success)
        notificationGenerator.prepare()
    }

    /// Notification - a meaningful operation failed
    static func error() {
        notificationGenerator.notificationOccurred(.error)
        notificationGenerator.prepare()
    }
}
