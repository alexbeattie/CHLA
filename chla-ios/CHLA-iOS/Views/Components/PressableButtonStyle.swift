//
//  PressableButtonStyle.swift
//  CHLA-iOS
//
//  App-wide press feedback for buttons and tappable cards
//

import SwiftUI

/// Scales the label down the instant a touch lands and springs back on
/// release, so every pressable element visibly responds to the finger.
/// Keep the scale subtle (0.95-0.98). When Reduce Motion is on, the scale
/// is replaced by a brief opacity dip.
struct PressableButtonStyle: ButtonStyle {
    var scale: CGFloat = 0.97

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(reduceMotion ? 1 : (configuration.isPressed ? scale : 1))
            .opacity(reduceMotion && configuration.isPressed ? 0.6 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 1), value: configuration.isPressed)
    }
}

extension ButtonStyle where Self == PressableButtonStyle {
    /// Standard press feedback for buttons, chips, and rows
    static var pressable: PressableButtonStyle { PressableButtonStyle() }

    /// Gentler variant for large cards, where 0.97 reads as too much movement
    static var pressableCard: PressableButtonStyle { PressableButtonStyle(scale: 0.985) }
}
