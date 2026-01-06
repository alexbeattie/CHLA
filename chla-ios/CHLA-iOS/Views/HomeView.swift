//
//  HomeView.swift
//  NDD Resources
//
//  Beautiful homepage with quick search and feature cards
//

import SwiftUI
import CoreLocation

struct HomeView: View {
    @EnvironmentObject var appState: AppState
    @ObservedObject var visibilityManager = UIVisibilityManager.shared
    @StateObject private var locationService = LocationService()

    @State private var zipCode = ""
    @State private var detectedCenter: RegionalCenterMatcher.RegionalCenterInfo?
    @FocusState private var isZipFocused: Bool
    @State private var userRegionalCenter: RegionalCenterMatcher.RegionalCenterInfo?
    @State private var lastDragValue: CGFloat = 0
    @State private var animateHero = false
    @State private var showSettingsMenu = false
    @State private var showResetConfirmation = false

    private let therapyTypes = [
        ("ABA therapy", "brain.head.profile", Color(hex: "6366F1")),
        ("Speech therapy", "mouth.fill", Color(hex: "EC4899")),
        ("Occupational therapy", "hand.raised.fill", Color(hex: "10B981")),
        ("Physical therapy", "figure.walk", Color(hex: "F59E0B"))
    ]

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                // Hero Section
                heroSection

                // Main Content
                VStack(spacing: 24) {
                    // Quick Actions
                    quickActionsSection

                    // Therapy Types
                    therapyTypesSection

                    // Your Regional Center
                    if let center = userRegionalCenter {
                        yourRegionalCenterSection(center)
                    }

                    // Browse Regional Centers
                    browseCentersSection

                    // Stats
                    statsSection

                    // Bottom spacing for tab bar
                    Color.clear.frame(height: 100)
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)
            }
        }
        .background(
            LinearGradient(
                colors: [
                    Color(hex: "F8FAFC"),
                    Color(hex: "F1F5F9")
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .simultaneousGesture(
            DragGesture()
                .onChanged { value in
                    let delta = value.translation.height - lastDragValue
                    if delta < -15 {
                        visibilityManager.hideUI()
                    } else if delta > 15 {
                        visibilityManager.showUI()
                    }
                    lastDragValue = value.translation.height
                }
                .onEnded { _ in
                    lastDragValue = 0
                }
        )
        .onAppear {
            withAnimation(.easeOut(duration: 0.8)) {
                animateHero = true
            }

            // Try to get user's regional center from location
            if locationService.hasLocationPermission {
                if let location = locationService.currentLocation {
                    userRegionalCenter = RegionalCenterMatcher.shared.findRegionalCenter(for: location.coordinate)
                }
            }
        }
        .onTapGesture {
            // Dismiss keyboard when tapping outside
            isZipFocused = false
        }
    }

    private func performZipSearch() {
        guard zipCode.count == 5 else { return }

        // Dismiss keyboard
        isZipFocused = false

        // Navigate to map with search
        NotificationCenter.default.post(
            name: .searchByZip,
            object: nil,
            userInfo: ["zip": zipCode]
        )
        appState.selectedTab = 1 // Go to Map
    }

    // MARK: - Hero Section
    private var heroSection: some View {
        ZStack {
            // Light clean gradient background
            LinearGradient(
                colors: [
                    Color.white,
                    Color(hex: "F1F5F9"),
                    Color(hex: "E2E8F0")
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            // Decorative subtle circles
            Circle()
                .fill(Color(hex: "3B82F6").opacity(0.06))
                .frame(width: 300, height: 300)
                .offset(x: 150, y: -100)
                .blur(radius: 20)

            Circle()
                .fill(Color(hex: "EC4899").opacity(0.04))
                .frame(width: 200, height: 200)
                .offset(x: -120, y: 50)
                .blur(radius: 20)

            // Content
            VStack(spacing: 16) {
                // Top bar with settings
                HStack {
                    Spacer()

                    Menu {
                        Button {
                            showResetConfirmation = true
                        } label: {
                            Label("Change My Preferences", systemImage: "slider.horizontal.3")
                        }

                        Divider()

                        Button {
                            appState.selectedTab = 4 // Go to More tab
                        } label: {
                            Label("Settings", systemImage: "gear")
                        }
                    } label: {
                        Image(systemName: "sparkle")
                            .font(.system(size: 20, weight: .medium))
                            .foregroundColor(Color(hex: "6366F1"))
                            .padding(12)
                            .background {
                                Circle()
                                    .fill(Color(hex: "6366F1").opacity(0.1))
                                Circle()
                                    .stroke(Color(hex: "6366F1").opacity(0.2), lineWidth: 0.5)
                            }
                    }
                    .padding(.trailing, 20)
                }
                .padding(.top, 50)

                // KiNDD Logo
                Image("KiNDDLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 56)
                    .scaleEffect(animateHero ? 1 : 0.5)
                    .opacity(animateHero ? 1 : 0)

                // Tagline
                VStack(spacing: 8) {
                    Text("Resource Navigator")
                        .font(.system(size: 22, weight: .semibold, design: .rounded))
                        .foregroundColor(Color(hex: "1E293B"))

                    Text("Find developmental disability services\nin Los Angeles County")
                        .font(.subheadline)
                        .foregroundColor(Color(hex: "64748B"))
                        .multilineTextAlignment(.center)
                }
                .opacity(animateHero ? 1 : 0)
                .offset(y: animateHero ? 0 : 20)

                // Inline ZIP Search
                VStack(spacing: 12) {
                    HStack(spacing: 12) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(Color(hex: "6366F1"))

                        TextField("Enter ZIP code", text: $zipCode)
                            .keyboardType(.numberPad)
                            .font(.body)
                            .foregroundColor(Color(hex: "1E293B"))
                            .focused($isZipFocused)
                            .onChange(of: zipCode) { _, newZip in
                                // Limit to 5 digits
                                if newZip.count > 5 {
                                    zipCode = String(newZip.prefix(5))
                                }
                                // Detect regional center
                                if newZip.count == 5 {
                                    detectedCenter = RegionalCenterMatcher.shared.findRegionalCenter(forZipCode: newZip)
                                } else {
                                    detectedCenter = nil
                                }
                            }

                        if !zipCode.isEmpty {
                            Button {
                                zipCode = ""
                                detectedCenter = nil
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(Color(hex: "94A3B8"))
                            }
                        }

                        // Search button
                        Button {
                            performZipSearch()
                        } label: {
                            Image(systemName: "arrow.right.circle.fill")
                                .font(.system(size: 24))
                                .foregroundColor(zipCode.count == 5 ? Color(hex: "6366F1") : Color(hex: "CBD5E1"))
                        }
                        .disabled(zipCode.count != 5)
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 14)
                    .background {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(.white)
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(isZipFocused ? Color(hex: "6366F1") : Color(hex: "E2E8F0"), lineWidth: isZipFocused ? 2 : 1)
                    }
                    .shadow(color: .black.opacity(0.08), radius: 12, y: 6)

                    // Show detected regional center
                    if let center = detectedCenter {
                        HStack(spacing: 8) {
                            Circle()
                                .fill(center.uiColor)
                                .frame(width: 8, height: 8)

                            Text(center.name)
                                .font(.caption)
                                .fontWeight(.medium)
                                .foregroundColor(Color(hex: "64748B"))

                            Image(systemName: "checkmark.circle.fill")
                                .font(.caption)
                                .foregroundColor(.green)
                        }
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 8)
                .opacity(animateHero ? 1 : 0)
                .offset(y: animateHero ? 0 : 30)
                .animation(.easeOut(duration: 0.2), value: detectedCenter != nil)

                Spacer()
                    .frame(height: 24)
            }
        }
        .frame(height: 380)
        .confirmationDialog(
            "Change My Preferences",
            isPresented: $showResetConfirmation,
            titleVisibility: .visible
        ) {
            Button("Continue") {
                appState.resetOnboarding()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will guide you through the setup to update your location, age group, and therapy preferences.")
        }
        .clipShape(
            RoundedCorner(radius: 32, corners: [.bottomLeft, .bottomRight])
        )
        .shadow(color: .black.opacity(0.06), radius: 20, y: 10)
    }

    // MARK: - Quick Actions
    private var quickActionsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Quick Actions")
                .font(.title3.weight(.semibold))
                .foregroundColor(.primary)

            HStack(spacing: 12) {
                QuickActionCard(
                    icon: "location.fill",
                    title: "Near Me",
                    subtitle: "Use location",
                    color: Color(hex: "3B82F6")
                ) {
                    NotificationCenter.default.post(name: .useMyLocation, object: nil)
                    appState.selectedTab = 1
                }

                QuickActionCard(
                    icon: "map.fill",
                    title: "Map",
                    subtitle: "Explore",
                    color: Color(hex: "10B981")
                ) {
                    appState.selectedTab = 1
                }

                QuickActionCard(
                    icon: "list.bullet",
                    title: "Browse",
                    subtitle: "All providers",
                    color: Color(hex: "8B5CF6")
                ) {
                    appState.selectedTab = 3
                }
            }
        }
    }

    // MARK: - Therapy Types
    private var therapyTypesSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Therapy Types")
                    .font(.title3.weight(.semibold))

                Spacer()

                Button {
                    appState.selectedTab = 3 // Browse tab
                } label: {
                    Text("See All")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(Color(hex: "6366F1"))
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(therapyTypes, id: \.0) { therapy in
                        TherapyTypeCard(
                            name: therapy.0,
                            icon: therapy.1,
                            color: therapy.2
                        ) {
                            // Navigate to filtered list
                            appState.searchFilters.therapyTypes = [therapy.0]
                            appState.selectedTab = 3
                        }
                    }
                }
                .padding(.horizontal, 2)
            }
        }
    }

    // MARK: - Your Regional Center
    private func yourRegionalCenterSection(_ center: RegionalCenterMatcher.RegionalCenterInfo) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Your Regional Center")
                    .font(.title3.weight(.semibold))

                Spacer()

                Image(systemName: "location.fill")
                    .font(.caption)
                    .foregroundColor(Color(hex: "10B981"))
            }

            Button {
                appState.selectedTab = 2 // Regions tab
            } label: {
                HStack(spacing: 16) {
                    // Icon
                    ZStack {
                        Circle()
                            .fill(center.uiColor.opacity(0.15))
                            .frame(width: 56, height: 56)

                        Text(center.shortName)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(center.uiColor)
                    }

                    // Info
                    VStack(alignment: .leading, spacing: 4) {
                        Text(center.name)
                            .font(.headline)
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.leading)

                        Text(center.phone)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.secondary)
                }
                .padding(16)
                .background {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color(.systemBackground))
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(center.uiColor.opacity(0.2), lineWidth: 1)
                }
                .shadow(color: .black.opacity(0.04), radius: 8, y: 4)
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Browse Centers
    private var browseCentersSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("LA County Regional Centers")
                .font(.title3.weight(.semibold))

            Button {
                appState.selectedTab = 2 // Regions tab
            } label: {
                HStack(spacing: 16) {
                    // Stacked circles
                    ZStack {
                        ForEach(0..<4, id: \.self) { index in
                            Circle()
                                .fill(Color.regionalCenterColor(at: index))
                                .frame(width: 32, height: 32)
                                .offset(x: CGFloat(index) * 12)
                        }
                    }
                    .frame(width: 80, alignment: .leading)

                    VStack(alignment: .leading, spacing: 4) {
                        Text("7 Regional Centers")
                            .font(.headline)
                            .foregroundColor(.primary)

                        Text("Find your center by location")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.secondary)
                }
                .padding(16)
                .background {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color(.systemBackground))
                }
                .shadow(color: .black.opacity(0.04), radius: 8, y: 4)
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Stats Section
    private var statsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("At a Glance")
                .font(.title3.weight(.semibold))

            HStack(spacing: 12) {
                StatCard(
                    value: "370+",
                    label: "Providers",
                    icon: "building.2.fill",
                    color: Color(hex: "6366F1")
                )

                StatCard(
                    value: "7",
                    label: "Centers",
                    icon: "mappin.circle.fill",
                    color: Color(hex: "EC4899")
                )

                StatCard(
                    value: "Free",
                    label: "Always",
                    icon: "heart.fill",
                    color: Color(hex: "10B981")
                )
            }
        }
    }
}

// MARK: - Quick Action Card
struct QuickActionCard: View {
    let icon: String
    let title: String
    let subtitle: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(color.opacity(0.12))
                        .frame(width: 48, height: 48)

                    Image(systemName: icon)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(color)
                }

                VStack(spacing: 2) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.primary)

                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.systemBackground))
            }
            .shadow(color: .black.opacity(0.04), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Therapy Type Card
struct TherapyTypeCard: View {
    let name: String
    let icon: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(color.opacity(0.12))
                        .frame(width: 56, height: 56)

                    Image(systemName: icon)
                        .font(.system(size: 24, weight: .medium))
                        .foregroundColor(color)
                }

                Text(name)
                    .font(.caption.weight(.medium))
                    .foregroundColor(.primary)
                    .lineLimit(1)
            }
            .frame(width: 90)
            .padding(.vertical, 16)
            .padding(.horizontal, 8)
            .background {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.systemBackground))
            }
            .shadow(color: .black.opacity(0.04), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Stat Card
struct StatCard: View {
    let value: String
    let label: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundColor(color)

            Text(value)
                .font(.title2.weight(.bold))
                .foregroundColor(.primary)

            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .background {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(.systemBackground))
        }
        .shadow(color: .black.opacity(0.04), radius: 8, y: 4)
    }
}

// MARK: - ZIP Search Sheet
struct ZipSearchSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var zipCode = ""
    @State private var detectedCenter: RegionalCenterMatcher.RegionalCenterInfo?
    @FocusState private var isZipFocused: Bool

    let onSearch: (String) -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                // Search Field
                VStack(alignment: .leading, spacing: 8) {
                    Text("Enter ZIP Code")
                        .font(.headline)

                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)

                        TextField("90210", text: $zipCode)
                            .keyboardType(.numberPad)
                            .font(.title2)
                            .focused($isZipFocused)
                            .onChange(of: zipCode) { _, newZip in
                                if newZip.count == 5 {
                                    detectedCenter = RegionalCenterMatcher.shared.findRegionalCenter(forZipCode: newZip)
                                } else {
                                    detectedCenter = nil
                                }
                            }

                        if !zipCode.isEmpty {
                            Button {
                                zipCode = ""
                                detectedCenter = nil
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                }

                // Detected Regional Center
                if let center = detectedCenter {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Regional Center")
                            .font(.caption.weight(.medium))
                            .foregroundColor(.secondary)

                        HStack(spacing: 12) {
                            Circle()
                                .fill(center.uiColor)
                                .frame(width: 12, height: 12)

                            Text(center.name)
                                .font(.subheadline.weight(.medium))

                            Spacer()

                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                        }
                        .padding()
                        .background(center.uiColor.opacity(0.1))
                        .cornerRadius(12)
                    }
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }

                Spacer()

                // Search Button
                Button {
                    onSearch(zipCode)
                    dismiss()
                } label: {
                    Text("Search Providers")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            zipCode.count == 5
                                ? Color(hex: "6366F1")
                                : Color.gray
                        )
                        .cornerRadius(12)
                }
                .disabled(zipCode.count != 5)
            }
            .padding(20)
            .navigationTitle("Find Providers")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            isZipFocused = true
        }
    }
}

// MARK: - Notification Names
extension Notification.Name {
    static let searchByZip = Notification.Name("searchByZip")
}

#Preview {
    HomeView()
        .environmentObject(AppState())
}
