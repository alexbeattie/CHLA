//
//  LocationService.swift
//  CHLA-iOS
//
//  Location services for GPS and geocoding
//

import Foundation
import CoreLocation
import Combine

/// Service for managing device location and geocoding
@MainActor
class LocationService: NSObject, ObservableObject {
    // MARK: - Published Properties

    @Published var currentLocation: CLLocation?
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    @Published var locationError: Error?
    @Published var isLoading = false

    // MARK: - Private Properties

    private let locationManager = CLLocationManager()
    private let geocoder = CLGeocoder()

    // MARK: - Computed Properties

    var hasLocationPermission: Bool {
        authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways
    }

    var shouldRequestPermission: Bool {
        authorizationStatus == .notDetermined
    }

    var coordinate: CLLocationCoordinate2D? {
        currentLocation?.coordinate
    }

    // MARK: - Initialization

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        authorizationStatus = locationManager.authorizationStatus
    }

    // MARK: - Public Methods

    /// Request location permission from the user
    func requestPermission() {
        locationManager.requestWhenInUseAuthorization()
    }

    /// Request a single location update
    func requestLocation() {
        guard hasLocationPermission else {
            locationError = LocationError.permissionDenied
            return
        }

        isLoading = true
        locationError = nil
        locationManager.requestLocation()
    }

    /// Start continuous location updates
    func startUpdating() {
        guard hasLocationPermission else {
            locationError = LocationError.permissionDenied
            return
        }

        locationManager.startUpdatingLocation()
    }

    /// Stop location updates
    func stopUpdating() {
        locationManager.stopUpdatingLocation()
    }

    /// Geocode an address string to coordinates
    func geocode(address: String) async throws -> CLLocationCoordinate2D {
        let placemarks = try await geocoder.geocodeAddressString(address)

        guard let location = placemarks.first?.location else {
            throw LocationError.geocodingFailed
        }

        return location.coordinate
    }

    /// Reverse geocode coordinates to an address
    func reverseGeocode(coordinate: CLLocationCoordinate2D) async throws -> String {
        let location = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
        let placemarks = try await geocoder.reverseGeocodeLocation(location)

        guard let placemark = placemarks.first else {
            throw LocationError.geocodingFailed
        }

        var addressParts: [String] = []

        if let street = placemark.thoroughfare {
            if let number = placemark.subThoroughfare {
                addressParts.append("\(number) \(street)")
            } else {
                addressParts.append(street)
            }
        }

        if let city = placemark.locality {
            addressParts.append(city)
        }

        if let state = placemark.administrativeArea {
            addressParts.append(state)
        }

        if let postalCode = placemark.postalCode {
            addressParts.append(postalCode)
        }

        return addressParts.joined(separator: ", ")
    }

    /// Get ZIP code from coordinates
    func getZipCode(for coordinate: CLLocationCoordinate2D) async throws -> String {
        let location = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
        let placemarks = try await geocoder.reverseGeocodeLocation(location)

        guard let postalCode = placemarks.first?.postalCode else {
            throw LocationError.geocodingFailed
        }

        return postalCode
    }

    /// Calculate distance between two coordinates in miles
    func distance(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) -> Double {
        let fromLocation = CLLocation(latitude: from.latitude, longitude: from.longitude)
        let toLocation = CLLocation(latitude: to.latitude, longitude: to.longitude)
        return fromLocation.distance(from: toLocation) / 1609.34 // Convert meters to miles
    }
}

// MARK: - CLLocationManagerDelegate

extension LocationService: CLLocationManagerDelegate {
    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        Task { @MainActor in
            guard let location = locations.last else { return }
            self.currentLocation = location
            self.isLoading = false
            self.locationError = nil
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in
            self.isLoading = false

            if let clError = error as? CLError {
                switch clError.code {
                case .denied:
                    self.locationError = LocationError.permissionDenied
                case .locationUnknown:
                    self.locationError = LocationError.locationUnavailable
                default:
                    self.locationError = LocationError.unknown(error)
                }
            } else {
                self.locationError = LocationError.unknown(error)
            }
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            self.authorizationStatus = manager.authorizationStatus

            // Auto-request location if permission was just granted
            if self.hasLocationPermission && self.currentLocation == nil {
                self.requestLocation()
            }
        }
    }
}

// MARK: - Location Errors

enum LocationError: LocalizedError {
    case permissionDenied
    case locationUnavailable
    case geocodingFailed
    case unknown(Error)

    var errorDescription: String? {
        switch self {
        case .permissionDenied:
            return "Location access was denied. Please enable location in Settings."
        case .locationUnavailable:
            return "Unable to determine your location. Please try again."
        case .geocodingFailed:
            return "Unable to find that address. Please try a different search."
        case .unknown(let error):
            return error.localizedDescription
        }
    }
}
