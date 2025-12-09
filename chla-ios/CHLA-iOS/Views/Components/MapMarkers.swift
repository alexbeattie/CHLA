//
//  MapMarkers.swift
//  NDD Resources
//
//  Simple marker components
//

import SwiftUI
import MapKit

// MARK: - Destination Marker (For Directions)

struct DestinationMarker: View {
    let name: String

    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: "mappin.circle.fill")
                .font(.title)
                .foregroundColor(.red)

            Image(systemName: "arrowtriangle.down.fill")
                .font(.caption2)
                .foregroundColor(.red)
                .offset(y: -4)
        }
    }
}

// MARK: - Color Extension for Markers

extension Color {
    static func softMarkerColor(for type: String?) -> Color {
        guard let type = type?.lowercased() else { return .blue }

        if type.contains("aba") { return .blue }
        if type.contains("speech") { return .purple }
        if type.contains("occupational") { return .green }
        if type.contains("physical") { return .orange }

        return .blue
    }
}

#Preview {
    DestinationMarker(name: "Test")
        .padding()
}
