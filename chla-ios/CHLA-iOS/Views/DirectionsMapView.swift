//
//  DirectionsMapView.swift
//  CHLA-iOS
//
//  In-app turn-by-turn directions using MapKit
//

import SwiftUI
import MapKit
import CoreLocation

struct DirectionsMapView: View {
    let destinationName: String
    let destinationCoordinate: CLLocationCoordinate2D
    let destinationAddress: String

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @StateObject private var viewModel: DirectionsViewModel
    @State private var showExternalMapsSheet = false

    init(destinationName: String, destinationCoordinate: CLLocationCoordinate2D, destinationAddress: String) {
        self.destinationName = destinationName
        self.destinationCoordinate = destinationCoordinate
        self.destinationAddress = destinationAddress
        _viewModel = StateObject(wrappedValue: DirectionsViewModel(
            destination: destinationCoordinate,
            destinationName: destinationName
        ))
    }

    var body: some View {
        NavigationStack {
            ZStack {
                // Map
                mapView

                // Overlays
                VStack(spacing: 0) {
                    // Current instruction at top
                    if let step = viewModel.currentStep {
                        currentStepCard(step: step)
                    }

                    Spacer()

                    // Bottom controls
                    bottomOverlay
                }
            }
            .navigationTitle("Directions")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    if viewModel.route != nil {
                        Button {
                            viewModel.showStepsList = true
                        } label: {
                            Image(systemName: "list.bullet")
                        }
                    }
                }
            }
            .sheet(isPresented: $viewModel.showStepsList) {
                stepsListSheet
            }
            .onAppear {
                viewModel.startDirections()
            }
        }
    }

    // MARK: - Map View

    private var mapView: some View {
        Map(position: $viewModel.cameraPosition) {
            // User location
            UserAnnotation()

            // Destination
            Annotation(destinationName, coordinate: destinationCoordinate) {
                DestinationMarker(name: destinationName)
            }
            .annotationTitles(.hidden)

            // Route line
            if let route = viewModel.route {
                MapPolyline(route.polyline)
                    .stroke(.blue, lineWidth: 5)
            }
        }
        .mapControls {
            MapUserLocationButton()
            MapCompass()
        }
    }

    // MARK: - Current Step Card

    private func currentStepCard(step: MKRoute.Step) -> some View {
        HStack(spacing: 16) {
            // Direction icon
            Image(systemName: viewModel.iconForInstruction(step.instructions))
                .font(.title2)
                .foregroundColor(.blue)
                .frame(width: 44, height: 44)
                .background(Color.blue.opacity(0.1))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(step.instructions)
                    .font(.headline)
                    .lineLimit(2)

                if step.distance > 0 {
                    Text(viewModel.formatDistance(step.distance))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            // Navigation arrows
            HStack(spacing: 4) {
                if viewModel.currentStepIndex > 0 {
                    Button {
                        viewModel.previousStep()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.title3)
                            .foregroundColor(.blue)
                    }
                }

                if viewModel.currentStepIndex < viewModel.stepsCount - 1 {
                    Button {
                        viewModel.nextStep()
                    } label: {
                        Image(systemName: "chevron.right")
                            .font(.title3)
                            .foregroundColor(.blue)
                    }
                }
            }
        }
        .padding()
        .background(.regularMaterial)
    }

    // MARK: - Bottom Overlay

    @ViewBuilder
    private var bottomOverlay: some View {
        if viewModel.isLoading {
            loadingCard
        } else if let error = viewModel.errorMessage {
            errorCard(message: error)
        } else if let route = viewModel.route {
            routeSummaryCard(route: route)
        }
    }

    private var loadingCard: some View {
        HStack(spacing: 12) {
            ProgressView()
            Text("Getting directions...")
                .font(.subheadline)
        }
        .padding()
        .background(.regularMaterial)
        .cornerRadius(12)
        .padding()
    }

    private func errorCard(message: String) -> some View {
        VStack(spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Button("Try Again") {
                viewModel.startDirections()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
        .background(.regularMaterial)
        .cornerRadius(12)
        .padding()
    }

    private func routeSummaryCard(route: MKRoute) -> some View {
        VStack(spacing: 12) {
            // Destination
            HStack {
                Image(systemName: "mappin.circle.fill")
                    .foregroundColor(.red)
                Text(destinationName)
                    .font(.headline)
                    .lineLimit(1)
                Spacer()
            }

            Divider()

            // Stats
            HStack(spacing: 24) {
                VStack {
                    Text(viewModel.formatTime(route.expectedTravelTime))
                        .font(.title3.bold())
                    Text("Drive")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                VStack {
                    Text(viewModel.formatDistance(route.distance))
                        .font(.title3.bold())
                    Text("Distance")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Button {
                    showExternalMapsSheet = true
                } label: {
                    Image(systemName: "arrow.triangle.turn.up.right.diamond.fill")
                        .font(.title2)
                }
                .buttonStyle(.bordered)
            }
        }
        .padding()
        .background(.regularMaterial)
        .cornerRadius(16)
        .padding()
        .confirmationDialog("Open in Maps", isPresented: $showExternalMapsSheet, titleVisibility: .visible) {
            Button("Apple Maps") {
                openInAppleMaps()
            }
            Button("Google Maps") {
                openInGoogleMaps()
            }
            Button("Waze") {
                openInWaze()
            }
            Button("Copy Address") {
                UIPasteboard.general.string = destinationAddress
                let impact = UIImpactFeedbackGenerator(style: .medium)
                impact.impactOccurred()
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("Get directions to \(destinationName)")
        }
    }

    // MARK: - External Maps

    private func openInAppleMaps() {
        let destination = MKMapItem(placemark: MKPlacemark(coordinate: destinationCoordinate))
        destination.name = destinationName
        destination.openInMaps(launchOptions: [
            MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving
        ])
    }

    private func openInGoogleMaps() {
        let lat = destinationCoordinate.latitude
        let lng = destinationCoordinate.longitude
        let urlString = "comgooglemaps://?daddr=\(lat),\(lng)&directionsmode=driving"
        if let url = URL(string: urlString), UIApplication.shared.canOpenURL(url) {
            openURL(url)
        } else {
            // Fallback to web
            let webURL = "https://www.google.com/maps/dir/?api=1&destination=\(lat),\(lng)"
            if let url = URL(string: webURL) {
                openURL(url)
            }
        }
    }

    private func openInWaze() {
        let lat = destinationCoordinate.latitude
        let lng = destinationCoordinate.longitude
        let urlString = "waze://?ll=\(lat),\(lng)&navigate=yes"
        if let url = URL(string: urlString), UIApplication.shared.canOpenURL(url) {
            openURL(url)
        } else {
            // Fallback to web
            let webURL = "https://waze.com/ul?ll=\(lat),\(lng)&navigate=yes"
            if let url = URL(string: webURL) {
                openURL(url)
            }
        }
    }

    // MARK: - Steps List Sheet

    private var stepsListSheet: some View {
        NavigationStack {
            List {
                if let route = viewModel.route {
                    Section {
                        HStack {
                            Text(destinationName)
                                .font(.headline)
                            Spacer()
                            Text(viewModel.formatTime(route.expectedTravelTime))
                                .foregroundColor(.secondary)
                        }
                    }

                    Section("Directions") {
                        ForEach(Array(viewModel.steps.enumerated()), id: \.offset) { index, step in
                            Button {
                                viewModel.goToStep(index)
                                viewModel.showStepsList = false
                            } label: {
                                HStack(spacing: 12) {
                                    Text("\(index + 1)")
                                        .font(.caption.bold())
                                        .foregroundColor(.white)
                                        .frame(width: 24, height: 24)
                                        .background(index == viewModel.currentStepIndex ? Color.blue : Color.gray)
                                        .clipShape(Circle())

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(step.instructions)
                                            .font(.subheadline)
                                            .foregroundColor(.primary)
                                        if step.distance > 0 {
                                            Text(viewModel.formatDistance(step.distance))
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("All Steps")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        viewModel.showStepsList = false
                    }
                }
            }
        }
    }
}

// MARK: - Directions View Model

@MainActor
class DirectionsViewModel: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var route: MKRoute?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var currentStepIndex = 0
    @Published var showStepsList = false
    @Published var cameraPosition: MapCameraPosition

    private let destination: CLLocationCoordinate2D
    private let destinationName: String
    private let locationManager = CLLocationManager()
    private var userLocation: CLLocation?
    private var retryCount = 0
    private let maxRetries = 3

    var steps: [MKRoute.Step] {
        route?.steps.filter { !$0.instructions.isEmpty } ?? []
    }

    var stepsCount: Int { steps.count }

    var currentStep: MKRoute.Step? {
        guard currentStepIndex < steps.count else { return nil }
        return steps[currentStepIndex]
    }

    init(destination: CLLocationCoordinate2D, destinationName: String) {
        self.destination = destination
        self.destinationName = destinationName
        self.cameraPosition = .region(MKCoordinateRegion(
            center: destination,
            span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
        ))
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func startDirections() {
        isLoading = true
        errorMessage = nil
        retryCount = 0

        // Request location
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()

        // Give time for location
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.attemptRouteCalculation()
        }
    }

    private func attemptRouteCalculation() {
        guard let userLocation = userLocation else {
            if retryCount < maxRetries {
                retryCount += 1
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
                    self?.attemptRouteCalculation()
                }
            } else {
                isLoading = false
                errorMessage = "Could not get your location. Please check location permissions."
            }
            return
        }

        calculateRoute(from: userLocation.coordinate)
    }

    private func calculateRoute(from source: CLLocationCoordinate2D) {
        let request = MKDirections.Request()
        request.source = MKMapItem(placemark: MKPlacemark(coordinate: source))
        request.destination = MKMapItem(placemark: MKPlacemark(coordinate: destination))
        request.transportType = .automobile

        MKDirections(request: request).calculate { [weak self] response, error in
            DispatchQueue.main.async {
                self?.isLoading = false

                if let error = error {
                    self?.errorMessage = "Could not calculate route. \(error.localizedDescription)"
                    return
                }

                guard let route = response?.routes.first else {
                    self?.errorMessage = "No route found."
                    return
                }

                self?.route = route
                self?.currentStepIndex = 0
                self?.zoomToOverview()
            }
        }
    }

    // MARK: - CLLocationManagerDelegate

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        Task { @MainActor in
            self.userLocation = location
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location error: \(error)")
    }

    // MARK: - Navigation

    func nextStep() {
        guard currentStepIndex < stepsCount - 1 else { return }
        currentStepIndex += 1
        zoomToCurrentStep()
    }

    func previousStep() {
        guard currentStepIndex > 0 else { return }
        currentStepIndex -= 1
        zoomToCurrentStep()
    }

    func goToStep(_ index: Int) {
        guard index >= 0 && index < stepsCount else { return }
        currentStepIndex = index
        zoomToCurrentStep()
    }

    func zoomToCurrentStep() {
        guard let step = currentStep else { return }
        withAnimation {
            cameraPosition = .region(MKCoordinateRegion(
                center: step.polyline.coordinate,
                span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            ))
        }
    }

    func zoomToOverview() {
        guard let route = route else { return }
        let rect = route.polyline.boundingMapRect
        let padded = rect.insetBy(dx: -rect.width * 0.1, dy: -rect.height * 0.1)
        withAnimation {
            cameraPosition = .rect(padded)
        }
    }

    // MARK: - Formatting

    func formatTime(_ seconds: TimeInterval) -> String {
        let hours = Int(seconds) / 3600
        let minutes = (Int(seconds) % 3600) / 60

        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes) min"
        }
    }

    func formatDistance(_ meters: CLLocationDistance) -> String {
        let miles = meters / 1609.34
        if miles < 0.1 {
            return "\(Int(meters * 3.28084)) ft"
        } else if miles < 10 {
            return String(format: "%.1f mi", miles)
        } else {
            return String(format: "%.0f mi", miles)
        }
    }

    func iconForInstruction(_ instruction: String) -> String {
        let lower = instruction.lowercased()
        if lower.contains("left") { return "arrow.turn.up.left" }
        if lower.contains("right") { return "arrow.turn.up.right" }
        if lower.contains("u-turn") { return "arrow.uturn.left" }
        if lower.contains("merge") { return "arrow.merge" }
        if lower.contains("exit") { return "arrow.up.right" }
        if lower.contains("arrive") || lower.contains("destination") { return "flag.fill" }
        return "arrow.up"
    }
}

#Preview {
    DirectionsMapView(
        destinationName: "ABC Therapy Center",
        destinationCoordinate: CLLocationCoordinate2D(latitude: 34.0522, longitude: -118.2437),
        destinationAddress: "123 Main St, Los Angeles, CA 90001"
    )
}
