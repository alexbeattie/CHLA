//
//  IntroView.swift
//  CHLA-iOS
//
//  What KiNDD is and how it handles your answers. Reopenable from the
//  onboarding how-to step, Settings, and About.
//

import SwiftUI

struct IntroView: View {
    let onFinish: () -> Void

    @Environment(\.colorScheme) private var colorScheme

    static let privacyPolicyURL = URL(string: "https://kinddhelp.org/privacy")!
    static let supportEmail = "support@kinddhelp.org"

    private struct IntroSection: Identifiable {
        let key: String
        let icon: String
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
        IntroSection(key: "urgent", icon: "phone"),
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
        .defaultScrollAnchor(UserDefaults.standard.bool(forKey: "debugScrollBottom") ? .bottom : nil)
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
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: section.icon)
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(Theme.indigo)
                .frame(width: 24)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 6) {
                Text(L10n.Intro.heading(section.key))
                    .font(.system(.headline, design: .rounded))
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)

                Text(Self.linkified(L10n.Intro.body(section.key)))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Theme.cardSurface)
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color.primary.opacity(0.06), lineWidth: 1)
        }
    }

    private var finishButton: some View {
        Button(action: onFinish) {
            Text(L10n.Intro.done)
                .font(.system(.body, design: .rounded).weight(.semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Capsule().fill(Theme.accentGradient))
                .shadow(color: Theme.indigo.opacity(0.35), radius: 12, y: 6)
        }
        .buttonStyle(.pressable)
    }

    /// Turns the support address and the "Privacy Policy" phrase into links.
    static func linkified(_ text: String) -> AttributedString {
        var attributed = AttributedString(text)
        let links: [(String, URL)] = [
            (supportEmail, URL(string: "mailto:\(supportEmail)")!),
            (L10n.Intro.privacyLinkLabel, privacyPolicyURL)
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

/// Settings, About, and the onboarding how-to step present the intro as a sheet.
extension View {
    func introSheet(isPresented: Binding<Bool>) -> some View {
        sheet(isPresented: isPresented) {
            IntroView { isPresented.wrappedValue = false }
        }
    }
}

#Preview {
    IntroView {}
}
