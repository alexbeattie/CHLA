//
//  FullMapView.swift
//  CHLA-iOS
//
//  Full-screen map view for viewing resource/center locations
//

import SwiftUI
import MapKit

struct FullMapView: View {
    let title: String
    let coordinate: CLLocationCoordinate2D
    let address: String

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    @State private var cameraPosition: MapCameraPosition
    @State private var showDirectionsSheet = false

    init(title: String, coordinate: CLLocationCoordinate2D, address: String) {
        self.title = title
        self.coordinate = coordinate
        self.address = address
        self._cameraPosition = State(initialValue: .region(MKCoordinateRegion(
            center: coordinate,
            span: MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
        )))
    }

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                // Full map
                Map(position: $cameraPosition) {
                    Marker(title, coordinate: coordinate)
                        .tint(Color.accentBlue)
                }
                .mapStyle(.standard(elevation: .realistic))
                .mapControls {
                    MapUserLocationButton()
                    MapCompass()
                    MapScaleView()
                }
                .ignoresSafeArea(edges: .bottom)

                // Bottom card with address and actions
                VStack(spacing: 0) {
                    // Drag indicator
                    RoundedRectangle(cornerRadius: 2.5)
                        .fill(Color.secondary.opacity(0.5))
                        .frame(width: 36, height: 5)
                        .padding(.top, 8)

                    VStack(alignment: .leading, spacing: 12) {
                        Text(title)
                            .font(.headline)
                            .lineLimit(2)

                        Text(address)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)

                        HStack(spacing: 12) {
                            // Get Directions button
                            Button {
                                openDirections()
                            } label: {
                                Label("Directions", systemImage: "arrow.triangle.turn.up.right.diamond.fill")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)

                            // Open in Maps button
                            Button {
                                openInMaps()
                            } label: {
                                Label("Open in Maps", systemImage: "map.fill")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                    .padding()
                }
                .background(.ultraThinMaterial)
                .cornerRadius(16, corners: [.topLeft, .topRight])
            }
            .navigationTitle("Location")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Button {
                            openDirections()
                        } label: {
                            Label("Get Directions", systemImage: "arrow.triangle.turn.up.right.diamond.fill")
                        }

                        Button {
                            openInMaps()
                        } label: {
                            Label("Open in Apple Maps", systemImage: "map.fill")
                        }

                        Button {
                            openInGoogleMaps()
                        } label: {
                            Label("Open in Google Maps", systemImage: "globe")
                        }

                        Button {
                            copyAddress()
                        } label: {
                            Label("Copy Address", systemImage: "doc.on.doc")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
    }

    // MARK: - Actions

    private func openDirections() {
        let mapItem = MKMapItem(placemark: MKPlacemark(coordinate: coordinate))
        mapItem.name = title
        mapItem.openInMaps(launchOptions: [
            MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving
        ])
    }

    private func openInMaps() {
        let mapItem = MKMapItem(placemark: MKPlacemark(coordinate: coordinate))
        mapItem.name = title
        mapItem.openInMaps()
    }

    private func openInGoogleMaps() {
        let urlString = "comgooglemaps://?daddr=\(coordinate.latitude),\(coordinate.longitude)&directionsmode=driving"
        if let url = URL(string: urlString), UIApplication.shared.canOpenURL(url) {
            openURL(url)
        } else {
            // Fallback to Google Maps web
            let webURL = URL(string: "https://www.google.com/maps/dir/?api=1&destination=\(coordinate.latitude),\(coordinate.longitude)")!
            openURL(webURL)
        }
    }

    private func copyAddress() {
        UIPasteboard.general.string = address
    }
}

// MARK: - Corner Radius Extension
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

#Preview {
    FullMapView(
        title: "ABC Therapy Center",
        coordinate: CLLocationCoordinate2D(latitude: 34.0522, longitude: -118.2437),
        address: "123 Main St, Los Angeles, CA 90001"
    )
}
