//
//  AboutView.swift
//  NDD Resources
//
//  About page with stable layout
//

import SwiftUI

struct AboutView: View {
    @EnvironmentObject var appState: AppState

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
    }

    // MARK: - Hero Section
    private var heroSection: some View {
        VStack(spacing: 12) {
            Text("About NDD Resources")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)

            Text("Helping families find ABA therapy and developmental disability services in LA County")
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.9))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .background(Color.accentBlue)
    }

    // MARK: - Mission Section
    private var missionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Our Mission", systemImage: "heart.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            Text("We create hope and build healthier futures by connecting families with autism and developmental disability services.")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Text("NDD Resources is a free, interactive map to help families find ABA therapy resources across Los Angeles County.")
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
            Label("What We Do", systemImage: "sparkles")
                .font(.headline)
                .foregroundColor(.accentBlue)

            VStack(spacing: 10) {
                FeatureRow(icon: "map.fill", title: "Interactive Map", description: "Visualize resource locations and boundaries")
                FeatureRow(icon: "location.fill", title: "Location Search", description: "Find resources by ZIP or location")
                FeatureRow(icon: "slider.horizontal.3", title: "Smart Filters", description: "Filter by insurance, therapy, age")
                FeatureRow(icon: "building.2.fill", title: "Regional Centers", description: "Find your assigned center")
                FeatureRow(icon: "phone.fill", title: "Quick Contact", description: "Call, email, get directions")
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
            Label("LA County Regional Centers", systemImage: "building.columns.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            Text("Regional Centers are nonprofit agencies that provide services to individuals with developmental disabilities.")
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

            Text("Your Regional Center is determined by your ZIP code.")
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
            Label("How It Works", systemImage: "questionmark.circle.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            VStack(alignment: .leading, spacing: 10) {
                StepRow(number: "1", text: "Enter your location or ZIP code")
                StepRow(number: "2", text: "See your Regional Center on the map")
                StepRow(number: "3", text: "Filter by insurance, therapy, etc.")
                StepRow(number: "4", text: "Contact resources directly")
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
            Label("Who We Serve", systemImage: "person.3.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            Text("Families seeking services for:")
                .font(.caption)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 6) {
                DiagnosisRow(text: "Autism Spectrum Disorder")
                DiagnosisRow(text: "Developmental Delays")
                DiagnosisRow(text: "Intellectual Disabilities")
                DiagnosisRow(text: "Communication Disorders")
                DiagnosisRow(text: "Learning Disabilities")
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
            Label("Free to Use", systemImage: "gift.fill")
                .font(.headline)
                .foregroundColor(.accentBlue)

            Text("No fees, registration, or subscriptions required.")
                .font(.caption)
                .foregroundStyle(.secondary)

            HStack {
                StatBox(value: "370+", label: "Resources")
                StatBox(value: "7", label: "Centers")
                StatBox(value: "Free", label: "Always")
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
            Text("Ready to Find a Resource?")
                .font(.headline)
                .foregroundColor(.white)

            Button {
                appState.navigateToMap()
            } label: {
                Label("Explore the Map", systemImage: "map.fill")
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
