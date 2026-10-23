//
//  TabBarClearance.swift
//  CHLA-iOS
//
//  Keeps tab content clear of the floating tab bar
//

import SwiftUI
import UIKit

extension EnvironmentValues {
    /// Space kept free above the bottom safe area for the floating tab bar,
    /// margin included. Zero outside the main tab container.
    @Entry var tabBarClearance: CGFloat = 0
}

extension View {
    /// Keeps this tab's content clear of the floating tab bar. For tab
    /// content that is not inside a NavigationStack.
    func tabBarClearance() -> some View {
        modifier(TabBarClearance())
    }

    /// Keeps every page of the enclosing NavigationStack clear of the floating
    /// tab bar, pushed pages included. Apply to the stack's root content.
    func navigationTabBarClearance() -> some View {
        modifier(NavigationTabBarClearance())
    }
}

private struct TabBarClearance: ViewModifier {
    @Environment(\.tabBarClearance) private var clearance

    func body(content: Content) -> some View {
        content.safeAreaPadding(.bottom, clearance)
    }
}

/// SwiftUI safe-area changes made outside a NavigationStack do not reach the
/// pages inside it, root or pushed. UIKit does pass a navigation controller's
/// additionalSafeAreaInsets on to every page it shows, the same way a system
/// tab bar's height reaches them.
private struct NavigationTabBarClearance: ViewModifier {
    @Environment(\.tabBarClearance) private var clearance

    func body(content: Content) -> some View {
        content.background {
            NavigationControllerBottomInset(inset: clearance)
        }
    }
}

private struct NavigationControllerBottomInset: UIViewRepresentable {
    let inset: CGFloat

    func makeUIView(context: Context) -> InsetView {
        let view = InsetView()
        view.isUserInteractionEnabled = false
        return view
    }

    func updateUIView(_ view: InsetView, context: Context) {
        view.inset = inset
    }

    final class InsetView: UIView {
        var inset: CGFloat = 0 {
            didSet { applyInset() }
        }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            applyInset()
        }

        private func applyInset() {
            guard window != nil else { return }
            var responder = next
            while let current = responder, !(current is UIViewController) {
                responder = current.next
            }
            guard let navigationController = (responder as? UIViewController)?.navigationController,
                  navigationController.additionalSafeAreaInsets.bottom != inset else { return }
            navigationController.additionalSafeAreaInsets.bottom = inset
        }
    }
}
