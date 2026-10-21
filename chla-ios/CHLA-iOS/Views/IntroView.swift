//
//  IntroView.swift
//  CHLA-iOS
//
//  What KiNDD is and how it handles your answers. First onboarding screen,
//  and reopenable from Settings and About.
//

import SwiftUI

struct IntroView: View {
    enum Mode {
        /// First screen of onboarding; the button advances to the first question
        case onboarding
        /// Reopened later; the button dismisses
        case reopened
    }

    let mode: Mode
    let onFinish: () -> Void

    @Environment(\.colorScheme) private var colorScheme

    static let privacyPolicyURL = URL(string: "https://kinddhelp.com/privacy")!
    static let supportEmail = "support@kinddhelp.org"

    private struct IntroSection: Identifiable {
        let key: String
        let icon: String
        var isUrgent = false
        var id: String { key }
    }

    private let sections: [IntroSection] = [
        IntroSection(key: "howItWorks", icon: "questionmark.circle"),
        IntroSection(key: "services", icon: "list.bullet.rectangle"),
        IntroSection(key: "audience", icon: "person.2"),
        IntroSection(key: "afterQuestions", icon: "arrow.right.circle"),
        IntroSection(key: "privacy", icon: "lock.shield"),
        IntroSection(key: "language", icon: "globe"),
        IntroSection(key: "keepInMind", icon: "info.circle"),
        IntroSection(key: "providers", icon: "building.2"),
        IntroSection(key: "urgent", icon: "exclamationmark.triangle.fill", isUrgent: true),
        IntroSection(key: "help", icon: "heart")
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header

                ForEach(sections) { section in
                    sectionCard(section)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 12)
        }
        .tint(Theme.indigoDeep)
        .safeAreaInset(edge: .bottom) {
            finishButton
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 20)
                .background(.bar)
        }
        .background {
            ZStack(alignment: .top) {
                Theme.canvas
                Theme.topWash(dark: colorScheme == .dark)
                    .frame(height: 360)
                    .frame(maxHeight: .infinity, alignment: .top)
            }
            .ignoresSafeArea()
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image("KiNDDLogo")
                .resizable()
                .scaledToFit()
                .frame(height: 48)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background {
                    if colorScheme == .dark {
                        RoundedRectangle(cornerRadius: 12, style: .continuous).fill(.white)
                    }
                }
                .accessibilityHidden(true)

            Text(L10n.Intro.title)
                .font(.system(.title, design: .rounded).weight(.bold))
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)

            Text(L10n.Intro.lead)
                .font(.body)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.bottom, 4)
    }

    private func sectionCard(_ section: IntroSection) -> some View {
        let tint: Color = section.isUrgent ? .red : Theme.indigo

        return HStack(alignment: .top, spacing: 12) {
            Image(systemName: section.icon)
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(tint)
                .frame(width: 24)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 6) {
                Text(L10n.Intro.heading(section.key))
                    .font(.system(.headline, design: .rounded))
                    .foregroundColor(section.isUrgent ? .red : .primary)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)

                Text(linkified(L10n.Intro.body(section.key)))
                    .font(.subheadline)
                    .foregroundStyle(section.isUrgent ? .primary : .secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(section.isUrgent ? Color.red.opacity(0.10) : Theme.cardSurface)
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(section.isUrgent ? Color.red.opacity(0.45) : Color.primary.opacity(0.06),
                        lineWidth: section.isUrgent ? 1.5 : 1)
        }
    }

    private var finishButton: some View {
        Button(action: onFinish) {
            HStack(spacing: 8) {
                Text(mode == .onboarding ? L10n.Intro.getStarted : L10n.Intro.done)
                    .font(.system(.body, design: .rounded).weight(.semibold))

                if mode == .onboarding {
                    Image(systemName: "arrow.right")
                        .font(.system(size: 14, weight: .semibold))
                        .accessibilityHidden(true)
                }
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Capsule().fill(Theme.accentGradient))
            .shadow(color: Theme.indigo.opacity(0.35), radius: 12, y: 6)
        }
        .buttonStyle(.pressable)
    }

    /// Turns the support address and the "Privacy Policy" phrase into links.
    private func linkified(_ text: String) -> AttributedString {
        var attributed = AttributedString(text)
        let links: [(String, URL)] = [
            (Self.supportEmail, URL(string: "mailto:\(Self.supportEmail)")!),
            (L10n.Intro.privacyLinkLabel, Self.privacyPolicyURL)
        ]
        for (phrase, url) in links {
            var searchStart = attributed.startIndex
            while let range = attributed[searchStart...].range(of: phrase) {
                attributed[range].link = url
                attributed[range].underlineStyle = .single
                searchStart = range.upperBound
            }
        }
        return attributed
    }
}

/// Settings and About present the intro as a sheet whose button reads "Done".
extension View {
    func introSheet(isPresented: Binding<Bool>) -> some View {
        sheet(isPresented: isPresented) {
            IntroView(mode: .reopened) { isPresented.wrappedValue = false }
        }
    }
}

#Preview {
    IntroView(mode: .onboarding) {}
}
