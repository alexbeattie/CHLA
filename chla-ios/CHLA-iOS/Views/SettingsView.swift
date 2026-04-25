//
//  SettingsView.swift
//  CHLA-iOS
//
//  Settings and preferences view
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var locationService = LocationService()
    @ObservedObject private var languageManager = LanguageManager.shared

    @State private var showResetAlert = false
    @State private var apiHealthy = false
    @State private var isCheckingAPI = false

    /// App version from bundle
    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }

    var body: some View {
        NavigationStack {
            List {
                // Language Selection - Prominent at top
                Section {
                    Picker(selection: $languageManager.currentLanguage) {
                        ForEach(AppLanguage.allCases) { language in
                            HStack {
                                Text(language.flag)
                                Text(language.displayName)
                            }
                            .tag(language)
                        }
                    } label: {
                        Label {
                            Text(L10n.Language.title)
                        } icon: {
                            Image(systemName: "globe")
                                .foregroundColor(.blue)
                        }
                    }
                } header: {
                    Text(L10n.Language.title)
                } footer: {
                    Text("Choose your preferred language for the app interface")
                }

                // Search Preferences
                Section {
                    Picker(selection: $appState.userAudienceType) {
                        Label("Family", systemImage: "person.2.fill").tag("family")
                        Label("Clinician", systemImage: "clipboard.fill").tag("clinician")
                    } label: {
                        Label("App Mode", systemImage: "person.crop.circle.badge.checkmark")
                    }
                    .onChange(of: appState.userAudienceType) { _, newValue in
                        appState.saveUserContext(audienceType: newValue)
                    }

                    Button {
                        appState.resetOnboarding()
                    } label: {
                        Label("Edit Profile & Onboarding", systemImage: "person.text.rectangle")
                    }

                    NavigationLink {
                        FiltersEditView(filters: $appState.searchFilters)
                    } label: {
                        Label("Search Filters", systemImage: "slider.horizontal.3")
                    }

                    Picker(selection: $appState.searchFilters.radiusMiles) {
                        Text("5 miles").tag(5.0)
                        Text("10 miles").tag(10.0)
                        Text("15 miles").tag(15.0)
                        Text("25 miles").tag(25.0)
                        Text("50 miles").tag(50.0)
                    } label: {
                        Label("Default Radius", systemImage: "scope")
                    }
                } header: {
                    Text("Search Preferences")
                }

                // Location
                Section {
                    HStack {
                        Label("Location Access", systemImage: "location.fill")
                        Spacer()
                        Text(locationStatusText)
                            .foregroundStyle(.secondary)
                    }

                    if !locationService.hasLocationPermission {
                        Button {
                            if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                                UIApplication.shared.open(settingsURL)
                            }
                        } label: {
                            Label("Open Settings", systemImage: "gear")
                        }
                    }
                } header: {
                    Text("Location")
                }

                // API Status
                Section {
                    HStack {
                        Label("API Status", systemImage: "server.rack")
                        Spacer()
                        if isCheckingAPI {
                            ProgressView()
                                .scaleEffect(0.8)
                        } else {
                            HStack(spacing: 6) {
                                Circle()
                                    .fill(apiHealthy ? Color.green : Color.red)
                                    .frame(width: 10, height: 10)
                                Text(apiHealthy ? "Connected" : "Disconnected")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    Button {
                        Task { await checkAPIHealth() }
                    } label: {
                        Label("Check Connection", systemImage: "arrow.clockwise")
                    }
                } header: {
                    Text("Connection")
                }

                // Information
                Section {
                    NavigationLink {
                        ClinicianResourcesView()
                    } label: {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Clinician Workspace")
                                Text("Referral-focused Regional Center tools")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: "clipboard.fill")
                                .foregroundColor(.teal)
                        }
                    }

                    NavigationLink {
                        AboutView()
                    } label: {
                        Label("About NDD Resources", systemImage: "info.circle.fill")
                    }

                    NavigationLink {
                        FAQView()
                    } label: {
                        Label("FAQ", systemImage: "questionmark.circle.fill")
                    }

                    Link(destination: URL(string: "https://kinddhelp.com")!) {
                        Label("Website", systemImage: "globe")
                    }

                } header: {
                    Text("Information")
                }

                // Regional Centers
                Section {
                    NavigationLink {
                        RegionalCentersView()
                    } label: {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Regional Centers")
                                Text("View all 7 LA County centers")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: "building.2.fill")
                                .foregroundColor(.purple)
                        }
                    }
                } header: {
                    Text("Regional Centers")
                }

                // About App
                Section {
                    HStack {
                        Label(L10n.About.version, systemImage: "app.badge")
                        Spacer()
                        Text(appVersion)
                            .foregroundStyle(.secondary)
                    }

                    HStack {
                        Label("Resource Data", systemImage: "building.2")
                        Spacer()
                        Text("370+ Resources")
                            .foregroundStyle(.secondary)
                    }
                } header: {
                    Text(L10n.About.title)
                }

                // Reset
                Section {
                    Button(role: .destructive) {
                        showResetAlert = true
                    } label: {
                        Label("Reset Onboarding", systemImage: "arrow.counterclockwise")
                    }

                    Button(role: .destructive) {
                        appState.searchFilters = SearchFilters()
                    } label: {
                        Label("Reset Filters", systemImage: "xmark.circle")
                    }
                } header: {
                    Text("Reset")
                } footer: {
                    Text("Resetting onboarding will show the welcome screens again on next launch.")
                }
            }
            .navigationTitle("Settings")
            .alert("Reset Onboarding?", isPresented: $showResetAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Reset", role: .destructive) {
                    appState.resetOnboarding()
                }
            } message: {
                Text("This will show the welcome screens again on next launch.")
            }
            .onAppear {
                Task { await checkAPIHealth() }
            }
        }
    }

    // MARK: - Helpers

    private var locationStatusText: String {
        switch locationService.authorizationStatus {
        case .notDetermined:
            return "Not Set"
        case .restricted:
            return "Restricted"
        case .denied:
            return "Denied"
        case .authorizedAlways:
            return "Always"
        case .authorizedWhenInUse:
            return "When In Use"
        @unknown default:
            return "Unknown"
        }
    }

    private func checkAPIHealth() async {
        isCheckingAPI = true
        do {
            let response = try await APIService.shared.healthCheck()
            apiHealthy = response.isHealthy
        } catch {
            apiHealthy = false
            print("❌ API health check failed: \(error)")
        }
        isCheckingAPI = false
    }
}

// MARK: - Filters Edit View
struct FiltersEditView: View {
    @Binding var filters: SearchFilters
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Section("Age Group") {
                Picker("Age Group", selection: $filters.ageGroup) {
                    Text("Any").tag(String?.none)
                    ForEach(SearchFilters.ageGroups, id: \.self) { age in
                        Text(age).tag(Optional(age))
                    }
                }
                .pickerStyle(.wheel)
            }

            Section("Diagnosis") {
                Picker("Diagnosis", selection: $filters.diagnosis) {
                    Text("Any").tag(String?.none)
                    ForEach(SearchFilters.diagnoses, id: \.self) { diagnosis in
                        Text(diagnosis).tag(Optional(diagnosis))
                    }
                }
            }

            Section("Insurance") {
                Picker("Insurance", selection: $filters.insurance) {
                    Text("Any").tag(String?.none)
                    ForEach(SearchFilters.insuranceOptions, id: \.self) { insurance in
                        Text(insurance).tag(Optional(insurance))
                    }
                }
            }

            Section("Therapy Types") {
                ForEach(SearchFilters.therapyTypes, id: \.self) { therapy in
                    Toggle(therapy, isOn: Binding(
                        get: { filters.therapyTypes.contains(therapy) },
                        set: { isOn in
                            if isOn {
                                filters.therapyTypes.append(therapy)
                            } else {
                                filters.therapyTypes.removeAll { $0 == therapy }
                            }
                        }
                    ))
                }
            }
        }
        .navigationTitle("Edit Filters")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    SettingsView()
        .environmentObject(AppState())
}
