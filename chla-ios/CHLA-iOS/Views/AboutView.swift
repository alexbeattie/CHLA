//
//  AboutView.swift
//  NDD Resources
//
//  About page with stable layout
//

import SwiftUI

struct AboutView: View {
    @EnvironmentObject var appState: AppState
    @ObservedObject var visibilityManager = UIVisibilityManager.shared
    @State private var lastDragValue: CGFloat = 0

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                heroSection

                VStack(alignment: .leading, spacing: 20) {
                    missionSection
                    featuresSection
                    regionalCentersSection
                    howItWorksSection
                    whoWeServeSection
                    statsSection
                    ctaSection
                }
                .padding(20)
            }
        }
        .background(Color(.systemGroupedBackground))
        .simultaneousGesture(
            DragGesture()
                .onChanged { value in
                    let delta = value.translation.height - lastDragValue
                    if delta < -10 {
                        visibilityManager.hideUI()
                    } else if delta > 10 {
                        visibilityManager.showUI()
                    }
                    lastDragValue = value.translation.height
                }
                .onEnded { _ in
                    lastDragValue = 0
                }
        )
    }

    // MARK: - Hero Section
    private var heroSection: some View {
        VStack(spacing: 12) {
            // Safe area spacer for status bar
            Color.clear
                .frame(height: 50)

            Text(L10n.About.pageTitle)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)

            Text(L10n.About.heroSubtitle)
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.9))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity)
        .padding(.bottom, 32)
        .background(Color.accentBlue)
    }

    // MARK: - Mission Section
    private var missionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(L10n.About.ourMission, systemImage: "heart.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            Text(L10n.About.missionText1)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Text(L10n.About.missionText2)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }

    // MARK: - Features Section
    private var featuresSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(L10n.About.whatWeDo, systemImage: "info.circle")
                .font(.headline)
                .foregroundColor(.accentBlue)

            VStack(spacing: 10) {
                FeatureRow(icon: "map.fill", title: L10n.About.featureMap, description: L10n.About.featureMapDesc)
                FeatureRow(icon: "location.fill", title: L10n.About.featureLocation, description: L10n.About.featureLocationDesc)
                FeatureRow(icon: "slider.horizontal.3", title: L10n.About.featureFilters, description: L10n.About.featureFiltersDesc)
                FeatureRow(icon: "building.2.fill", title: L10n.About.featureRC, description: L10n.About.featureRCDesc)
                FeatureRow(icon: "phone.fill", title: L10n.About.featureContact, description: L10n.About.featureContactDesc)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }

    // MARK: - Regional Centers Section
    private var regionalCentersSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(L10n.About.laCountyRC, systemImage: "building.columns.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            Text(L10n.About.rcDescription)
                .font(.caption)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 6) {
                RCListItem(name: "San Gabriel/Pomona", shortName: "SG/PRC")
                RCListItem(name: "Harbor", shortName: "HRC")
                RCListItem(name: "North LA County", shortName: "NLACRC")
                RCListItem(name: "Eastern LA", shortName: "ELARC")
                RCListItem(name: "South Central LA", shortName: "SCLARC")
                RCListItem(name: "Westside", shortName: "WRC")
                RCListItem(name: "Frank D. Lanterman", shortName: "FDLRC")
            }

            Text(L10n.About.rcZipNote)
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.accentBlue.opacity(0.05))
        .cornerRadius(12)
    }

    // MARK: - How It Works Section
    private var howItWorksSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(L10n.About.howItWorks, systemImage: "questionmark.circle.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            VStack(alignment: .leading, spacing: 10) {
                StepRow(number: "1", text: L10n.About.step1)
                StepRow(number: "2", text: L10n.About.step2)
                StepRow(number: "3", text: L10n.About.step3)
                StepRow(number: "4", text: L10n.About.step4)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }

    // MARK: - Who We Serve Section
    private var whoWeServeSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(L10n.About.whoWeServe, systemImage: "person.3.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            Text(L10n.About.servingFamilies)
                .font(.caption)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 6) {
                DiagnosisRow(text: L10n.About.autism)
                DiagnosisRow(text: L10n.About.developmental)
                DiagnosisRow(text: L10n.About.intellectual)
                DiagnosisRow(text: L10n.About.communication)
                DiagnosisRow(text: L10n.About.learning)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }

    // MARK: - Stats Section
    private var statsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(L10n.About.freeToUse, systemImage: "gift.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            Text(L10n.About.freeDescription)
                .font(.caption)
                .foregroundStyle(.secondary)

            HStack {
                StatBox(value: "370+", label: L10n.Home.resources)
                StatBox(value: "7", label: L10n.Home.centers)
                StatBox(value: L10n.Home.free, label: L10n.Home.always)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.accentBlue.opacity(0.05))
        .cornerRadius(12)
    }

    // MARK: - CTA Section
    private var ctaSection: some View {
        VStack(spacing: 12) {
            Text(L10n.About.readyToFind)
                .font(.headline)
                .foregroundColor(.white)

            Button {
                appState.navigateToMap()
            } label: {
                Label(L10n.About.exploreMap, systemImage: "map.fill")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.accentBlue)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(Color.white)
                    .cornerRadius(8)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Color.accentBlue)
        .cornerRadius(12)
    }
}

// MARK: - Supporting Views

struct FeatureRow: View {
    let icon: String
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundColor(.accentBlue)
                .frame(width: 20, height: 20, alignment: .center)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 0)
        }
    }
}

struct RCListItem: View {
    let name: String
    let shortName: String

    var body: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(Color.accentBlue)
                .frame(width: 6, height: 6)

            Text(name)
                .font(.subheadline)

            Spacer(minLength: 0)

            Text(shortName)
                .font(.caption)
                .foregroundColor(.accentBlue)
                .fontWeight(.medium)
        }
    }
}

struct StepRow: View {
    let number: String
    let text: String

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Text(number)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(width: 20, height: 20)
                .background(Color.accentBlue)
                .clipShape(Circle())

            Text(text)
                .font(.subheadline)

            Spacer(minLength: 0)
        }
    }
}

struct DiagnosisRow: View {
    let text: String

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            Image(systemName: "checkmark.circle.fill")
                .font(.caption)
                .foregroundColor(.green)
                .frame(width: 20, height: 20, alignment: .center)

            Text(text)
                .font(.subheadline)

            Spacer(minLength: 0)
        }
    }
}

struct StatBox: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(.accentBlue)
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color(.systemBackground))
        .cornerRadius(8)
    }
}

#Preview {
    AboutView()
        .environmentObject(AppState())
}
