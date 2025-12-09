//
//  Formatters.swift
//  CHLA-iOS
//
//  Utility formatters for the app
//

import Foundation

// MARK: - Distance Formatter
struct DistanceFormatter {
    /// Format distance in miles
    static func format(_ miles: Double) -> String {
        if miles < 0.1 {
            return "< 0.1 mi"
        } else if miles < 10 {
            return String(format: "%.1f mi", miles)
        } else {
            return String(format: "%.0f mi", miles)
        }
    }

    /// Format distance with full text
    static func formatLong(_ miles: Double) -> String {
        if miles < 0.1 {
            return "Less than 0.1 miles"
        } else if miles < 1 {
            return String(format: "%.1f miles", miles)
        } else if miles == 1 {
            return "1 mile"
        } else if miles < 10 {
            return String(format: "%.1f miles", miles)
        } else {
            return String(format: "%.0f miles", miles)
        }
    }
}

// MARK: - Phone Formatter
struct PhoneFormatter {
    /// Format a phone number string
    static func format(_ phone: String?) -> String? {
        guard let phone = phone, !phone.isEmpty else { return nil }

        // Extract digits only
        let digits = phone.filter { $0.isNumber }

        // Format US phone numbers
        if digits.count == 10 {
            let areaCode = digits.prefix(3)
            let prefix = digits.dropFirst(3).prefix(3)
            let line = digits.suffix(4)
            return "(\(areaCode)) \(prefix)-\(line)"
        } else if digits.count == 11 && digits.first == "1" {
            // Handle 1 + 10 digit format
            let withoutCountry = String(digits.dropFirst())
            return format(withoutCountry)
        }

        return phone
    }

    /// Create a tel: URL from a phone number
    static func url(for phone: String?) -> URL? {
        guard let phone = phone else { return nil }
        let digits = phone.filter { $0.isNumber }
        guard !digits.isEmpty else { return nil }
        return URL(string: "tel://\(digits)")
    }
}

// MARK: - Address Formatter
struct AddressFormatter {
    /// Format address components into a single string
    static func format(
        street: String?,
        suite: String? = nil,
        city: String?,
        state: String?,
        zipCode: String?
    ) -> String {
        var parts: [String] = []

        if let street = street, !street.isEmpty {
            parts.append(street)
        }

        if let suite = suite, !suite.isEmpty {
            parts.append("Suite \(suite)")
        }

        var cityStateZip: [String] = []
        if let city = city, !city.isEmpty {
            cityStateZip.append(city)
        }
        if let state = state, !state.isEmpty {
            cityStateZip.append(state)
        }
        if let zipCode = zipCode, !zipCode.isEmpty {
            cityStateZip.append(zipCode)
        }

        if !cityStateZip.isEmpty {
            parts.append(cityStateZip.joined(separator: ", ").replacingOccurrences(of: ", ", with: " ", range: nil))
        }

        return parts.joined(separator: ", ")
    }

    /// Extract ZIP code from an address string
    static func extractZipCode(from address: String) -> String? {
        // Look for 5-digit ZIP code pattern
        let pattern = "\\b\\d{5}\\b"
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return nil }

        let range = NSRange(address.startIndex..., in: address)
        if let match = regex.firstMatch(in: address, options: [], range: range) {
            if let swiftRange = Range(match.range, in: address) {
                return String(address[swiftRange])
            }
        }

        return nil
    }
}

// MARK: - Date Formatter
extension DateFormatter {
    /// Standard date formatter for the app
    static let standard: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    /// Full date and time formatter
    static let full: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        formatter.timeStyle = .short
        return formatter
    }()

    /// Relative date formatter (e.g., "Today", "Yesterday")
    static let relative: RelativeDateTimeFormatter = {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter
    }()
}

// MARK: - List Formatter
struct ListFormatter {
    /// Join items with commas and "and" for the last item
    static func format(_ items: [String]) -> String {
        switch items.count {
        case 0:
            return ""
        case 1:
            return items[0]
        case 2:
            return "\(items[0]) and \(items[1])"
        default:
            let allButLast = items.dropLast().joined(separator: ", ")
            return "\(allButLast), and \(items.last!)"
        }
    }

    /// Join items with bullets
    static func formatBulleted(_ items: [String]) -> String {
        items.joined(separator: " • ")
    }
}
