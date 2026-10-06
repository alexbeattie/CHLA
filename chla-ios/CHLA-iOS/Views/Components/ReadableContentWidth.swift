//
//  ReadableContentWidth.swift
//  CHLA-iOS
//
//  Keeps phone-shaped content readable when the window is wide
//

import SwiftUI

/// iPhone windows can be far wider than a phone (unfolded iPhone Duo,
/// landscape, resized iPhone Mirroring). Content caps at a reading width and
/// centers instead of stretching edge to edge; the cap grows with Dynamic Type
/// the way UIKit's readable content guide does. Narrow windows are unaffected.
struct ReadableContentWidth: ViewModifier {
    @ScaledMetric(relativeTo: .body) private var maxWidth: CGFloat = 680

    func body(content: Content) -> some View {
        content
            .frame(maxWidth: maxWidth)
            .frame(maxWidth: .infinity)
    }
}

extension View {
    /// Centers content in a readable-width column when the window is wide
    func readableContentWidth() -> some View {
        modifier(ReadableContentWidth())
    }
}
