import SwiftUI
import MapKit

struct RegionalCentersMiniMapCard: View {
    let serviceAreas: [ServiceAreaFeature]
    let highlightedCenterShortName: String?
    let isLoading: Bool
    let onTap: () -> Void

    private let overviewRegion = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 34.02, longitude: -118.24),
        span: MKCoordinateSpan(latitudeDelta: 0.72, longitudeDelta: 0.72)
    )

    private var highlightedFeature: ServiceAreaFeature? {
        guard let highlightedCenterShortName else { return nil }
        return serviceAreas.first { normalizedShortName($0.shortName) == normalizedShortName(highlightedCenterShortName) }
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 14) {
                header
                mapCard
            }
            .padding(18)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color(.systemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(Color.primary.opacity(0.05), lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.05), radius: 14, y: 6)
        }
        .buttonStyle(.plain)
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text("LA County Regional Centers")
                    .font(.headline)
                    .foregroundColor(.primary)

                Text("Explore service boundaries and find the right regional center for your family.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 8)

            VStack(alignment: .trailing, spacing: 8) {
                if let highlightedFeature {
                    Text("Likely region")
                        .font(.caption2.weight(.semibold))
                        .foregroundColor(.secondary)

                    HStack(spacing: 6) {
                        Circle()
                            .fill(highlightedFeature.strokeColor)
                            .frame(width: 8, height: 8)

                        Text(highlightedFeature.shortName)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.primary)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(highlightedFeature.strokeColor.opacity(0.14))
                    .clipShape(Capsule())
                }

                HStack(spacing: 4) {
                    Text("View Map")
                    Image(systemName: "arrow.up.right")
                }
                .font(.caption.weight(.semibold))
                .foregroundColor(Color(hex: "6366F1"))
            }
        }
    }

    private var mapCard: some View {
        ZStack(alignment: .bottomLeading) {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color(hex: "EAF3FF"))

            if serviceAreas.isEmpty {
                loadingState
            } else {
                mapPreview
            }
        }
        .frame(height: 220)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var loadingState: some View {
        VStack(spacing: 10) {
            if isLoading {
                ProgressView()
                    .tint(Color(hex: "6366F1"))
            } else {
                Image(systemName: "map")
                    .font(.system(size: 28, weight: .medium))
                    .foregroundColor(Color(hex: "6366F1").opacity(0.8))
            }

            Text(isLoading ? "Loading regional center boundaries..." : "Map preview unavailable")
                .font(.subheadline.weight(.medium))
                .foregroundColor(.primary)

            Text("Open the full map to browse all LA County service areas.")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(
            LinearGradient(
                colors: [Color.white.opacity(0.4), Color.clear],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private var mapPreview: some View {
        ZStack(alignment: .bottomLeading) {
            Map(initialPosition: .region(overviewRegion), interactionModes: []) {
                ForEach(serviceAreas) { feature in
                    ForEach(Array(feature.allMapPolygons.enumerated()), id: \.offset) { _, polygon in
                        let isHighlighted = isHighlighted(feature)
                        MapPolygon(polygon)
                            .foregroundStyle(feature.fillColor.opacity(isHighlighted ? 0.3 : 0.16))
                            .stroke(feature.strokeColor.opacity(isHighlighted ? 1 : 0.82), lineWidth: isHighlighted ? 3 : 1.6)
                    }
                }

                ForEach(serviceAreas) { feature in
                    if let coordinate = feature.centerCoordinate {
                        Annotation(feature.shortName, coordinate: coordinate) {
                            RegionalCenterMiniBadge(
                                shortName: feature.shortName,
                                color: feature.strokeColor,
                                isHighlighted: isHighlighted(feature)
                            )
                        }
                    }
                }
            }
            .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
            .allowsHitTesting(false)
            .overlay(alignment: .topLeading) {
                LinearGradient(
                    colors: [Color.white.opacity(0.38), Color.clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 70)
            }

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: "map.fill")
                        .foregroundColor(Color(hex: "6366F1"))

                    Text("Real service area map")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(.primary)
                }

                Text("\(serviceAreas.count) LA regional centers")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .padding(12)
        }
    }

    private func isHighlighted(_ feature: ServiceAreaFeature) -> Bool {
        guard let highlightedFeature else { return false }
        return highlightedFeature.id == feature.id
    }

    private func normalizedShortName(_ shortName: String) -> String {
        shortName.replacingOccurrences(of: "/", with: "").uppercased()
    }
}

private struct RegionalCenterMiniBadge: View {
    let shortName: String
    let color: Color
    let isHighlighted: Bool

    var body: some View {
        VStack(spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: isHighlighted ? 18 : 14, height: isHighlighted ? 18 : 14)
                .overlay(
                    Circle()
                        .stroke(Color.white, lineWidth: 3)
                )
                .shadow(color: color.opacity(0.35), radius: isHighlighted ? 6 : 3, y: 2)

            Text(shortName)
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(.primary)
                .padding(.horizontal, 6)
                .padding(.vertical, 3)
                .background(.ultraThinMaterial)
                .clipShape(Capsule())
        }
    }
}
