//
//  KiNDDWidgets.swift
//  KiNDDWidgets
//
//  Home-screen widget: the family's regional center, one tap from a call
//

import WidgetKit
import SwiftUI

// MARK: - Shared data

/// Reads the state the app mirrors into the App Group container
struct RegionalCenterEntry: TimelineEntry {
    let date: Date
    let centerName: String?
    let centerShortName: String?
    let centerPhone: String?
    let journeyStage: String?

    var isMatched: Bool { centerName != nil }

    var nextStepLine: String? {
        switch journeyStage {
        case "justDiagnosed": return "Next step: request an intake evaluation"
        case "waitingIntake": return "Next step: prepare for your intake"
        case "receivingServices": return "Next step: get more from your IPP"
        default: return nil
        }
    }

    static func current() -> RegionalCenterEntry {
        let shared = UserDefaults(suiteName: "group.com.nddresources.map")
        return RegionalCenterEntry(
            date: Date(),
            centerName: shared?.string(forKey: "widget.centerName"),
            centerShortName: shared?.string(forKey: "widget.centerShortName"),
            centerPhone: shared?.string(forKey: "widget.centerPhone"),
            journeyStage: shared?.string(forKey: "widget.journeyStage")
        )
    }

    static let placeholder = RegionalCenterEntry(
        date: Date(),
        centerName: "Westside Regional Center",
        centerShortName: "WRC",
        centerPhone: "(310) 258-4000",
        journeyStage: "justDiagnosed"
    )
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> RegionalCenterEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (RegionalCenterEntry) -> Void) {
        completion(context.isPreview ? .placeholder : .current())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<RegionalCenterEntry>) -> Void) {
        // Content only changes when the app writes new state, which triggers an
        // explicit reload; the daily refresh is just a safety net
        let refresh = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        completion(Timeline(entries: [.current()], policy: .after(refresh)))
    }
}

// MARK: - Palette (self-contained; mirrors the app's Theme)

private enum WTheme {
    static let indigo = Color(red: 0.388, green: 0.400, blue: 0.945)
    static let violet = Color(red: 0.545, green: 0.361, blue: 0.965)

    static var brandGradient: LinearGradient {
        LinearGradient(
            colors: [indigo, violet],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

// MARK: - Views

struct KiNDDWidgetsEntryView: View {
    @Environment(\.widgetFamily) private var family
    var entry: RegionalCenterEntry

    var body: some View {
        switch family {
        case .accessoryRectangular:
            accessoryRectangular
        case .systemMedium:
            medium
        default:
            small
        }
    }

    // MARK: Small - the phone number on the home screen

    private var small: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 4) {
                Image(systemName: "sparkles")
                    .font(.system(size: 11, weight: .semibold))
                Text("KiNDD")
                    .font(.system(size: 12, weight: .bold, design: .rounded))
            }
            .foregroundStyle(.white.opacity(0.85))

            Spacer(minLength: 0)

            if entry.isMatched {
                Text(entry.centerShortName ?? "")
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)

                Text(entry.centerPhone ?? "")
                    .font(.system(size: 13, weight: .semibold, design: .rounded))
                    .foregroundStyle(.white.opacity(0.9))
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)

                HStack(spacing: 4) {
                    Image(systemName: "phone.fill")
                        .font(.system(size: 9, weight: .bold))
                    Text("Tap to call")
                        .font(.system(size: 11, weight: .medium))
                }
                .foregroundStyle(.white.opacity(0.75))
            } else {
                Text("Get matched to your regional center")
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .foregroundStyle(.white)

                Text("Open KiNDD")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(.white.opacity(0.75))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .containerBackground(for: .widget) { WTheme.brandGradient }
        .widgetURL(URL(string: entry.isMatched ? "kindd://call" : "kindd://chat"))
    }

    // MARK: Medium - center, next step, and both actions

    private var medium: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 5) {
                Text("Your Regional Center")
                    .font(.system(size: 10, weight: .semibold))
                    .textCase(.uppercase)
                    .kerning(0.6)
                    .foregroundStyle(.secondary)

                if entry.isMatched {
                    Text(entry.centerName ?? "")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundStyle(.primary)
                        .lineLimit(2)
                        .minimumScaleFactor(0.8)

                    if let nextStep = entry.nextStepLine {
                        Text(nextStep)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(WTheme.violet)
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                } else {
                    Text("Not matched yet")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundStyle(.primary)

                    Text("Enter your ZIP in KiNDD to find who serves your family")
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }

                Spacer(minLength: 0)

                HStack(spacing: 8) {
                    if entry.isMatched, let phone = entry.centerPhone {
                        Link(destination: URL(string: "kindd://call")!) {
                            HStack(spacing: 5) {
                                Image(systemName: "phone.fill")
                                    .font(.system(size: 10, weight: .bold))
                                Text(phone)
                                    .font(.system(size: 12, weight: .semibold, design: .rounded))
                                    .minimumScaleFactor(0.8)
                                    .lineLimit(1)
                            }
                            .foregroundStyle(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(Capsule().fill(WTheme.brandGradient))
                        }
                    }

                    Link(destination: URL(string: "kindd://chat")!) {
                        HStack(spacing: 4) {
                            Image(systemName: "sparkles")
                                .font(.system(size: 10, weight: .semibold))
                            Text("Ask KiNDD")
                                .font(.system(size: 12, weight: .semibold, design: .rounded))
                        }
                        .foregroundStyle(WTheme.violet)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(Capsule().strokeBorder(WTheme.violet.opacity(0.45), lineWidth: 1))
                    }
                }
            }

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .containerBackground(for: .widget) {
            ZStack(alignment: .topTrailing) {
                Color(uiColor: .systemBackground)

                LinearGradient(
                    colors: [WTheme.indigo.opacity(0.14), .clear],
                    startPoint: .topTrailing,
                    endPoint: .bottomLeading
                )
            }
        }
    }

    // MARK: Lock screen

    private var accessoryRectangular: some View {
        VStack(alignment: .leading, spacing: 1) {
            HStack(spacing: 3) {
                Image(systemName: "sparkles")
                    .font(.system(size: 10, weight: .semibold))
                Text(entry.isMatched ? (entry.centerShortName ?? "KiNDD") : "KiNDD")
                    .font(.system(size: 13, weight: .bold, design: .rounded))
            }

            if entry.isMatched {
                Text(entry.centerPhone ?? "")
                    .font(.system(size: 12, weight: .medium))
                Text("Tap to call")
                    .font(.system(size: 10))
                    .foregroundStyle(.secondary)
            } else {
                Text("Get matched")
                    .font(.system(size: 12, weight: .medium))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .containerBackground(for: .widget) { Color.clear }
        .widgetURL(URL(string: entry.isMatched ? "kindd://call" : "kindd://chat"))
    }
}

// MARK: - Widget

struct KiNDDWidgets: Widget {
    let kind: String = "KiNDDWidgets"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            KiNDDWidgetsEntryView(entry: entry)
        }
        .configurationDisplayName("Your Regional Center")
        .description("Your regional center's phone number, one tap away.")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryRectangular])
    }
}

#Preview(as: .systemSmall) {
    KiNDDWidgets()
} timeline: {
    RegionalCenterEntry.placeholder
}

#Preview(as: .systemMedium) {
    KiNDDWidgets()
} timeline: {
    RegionalCenterEntry.placeholder
}
