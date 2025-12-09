//
//  APIService.swift
//  CHLA-iOS
//
//  API service layer for Django backend integration
//

import Foundation
import CoreLocation

/// Main API service for communicating with the Django backend
actor APIService {
    // MARK: - Configuration

    /// API environment configuration
    enum Environment {
        case development
        case production

        var baseURL: String {
            switch self {
            case .development:
                return "http://localhost:8000/api"
            case .production:
                return "https://api.kinddhelp.com/api"
            }
        }
    }

    /// Current environment - change this for different builds
    /// Using production API (AWS) for both debug and release builds
    private let environment: Environment = .production

    /// Shared singleton instance
    static let shared = APIService()

    /// JSON decoder configured for Django API responses
    private let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()

    /// URL session with timeout configuration
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        return URLSession(configuration: config)
    }()

    private init() {}

    // MARK: - Base URL

    var baseURL: String {
        environment.baseURL
    }

    // MARK: - Health Check

    /// Check API health status
    func healthCheck() async throws -> HealthCheckResponse {
        let url = URL(string: "\(baseURL)/health/")!
        return try await fetch(url: url)
    }

    // MARK: - Provider Endpoints

    /// Get all providers (paginated)
    func getProviders() async throws -> [Provider] {
        let url = URL(string: "\(baseURL)/providers-v2/")!
        let response: PaginatedResponse<Provider> = try await fetch(url: url)
        return response.results
    }

    /// Get a single provider by ID
    func getProvider(id: UUID) async throws -> Provider {
        let url = URL(string: "\(baseURL)/providers-v2/\(id.uuidString)/")!
        return try await fetch(url: url)
    }

    /// Search providers near a location
    func getProvidersNearby(
        latitude: Double,
        longitude: Double,
        radiusMiles: Double = 15
    ) async throws -> [Provider] {
        var components = URLComponents(string: "\(baseURL)/providers-v2/nearby/")!
        components.queryItems = [
            URLQueryItem(name: "lat", value: String(latitude)),
            URLQueryItem(name: "lng", value: String(longitude)),
            URLQueryItem(name: "radius", value: String(radiusMiles))
        ]
        return try await fetch(url: components.url!)
    }

    /// Comprehensive provider search with filters
    func searchProviders(
        query: String? = nil,
        latitude: Double? = nil,
        longitude: Double? = nil,
        radiusMiles: Double = 15,
        insurance: String? = nil,
        ageGroup: String? = nil,
        diagnosis: String? = nil,
        therapyTypes: [String] = []
    ) async throws -> [Provider] {
        var components = URLComponents(string: "\(baseURL)/providers-v2/comprehensive_search/")!
        var queryItems: [URLQueryItem] = []

        if let query = query, !query.isEmpty {
            queryItems.append(URLQueryItem(name: "q", value: query))
        }

        if let lat = latitude, let lng = longitude {
            queryItems.append(URLQueryItem(name: "lat", value: String(lat)))
            queryItems.append(URLQueryItem(name: "lng", value: String(lng)))
            queryItems.append(URLQueryItem(name: "radius", value: String(radiusMiles)))
        }

        if let insurance = insurance {
            queryItems.append(URLQueryItem(name: "insurance", value: insurance))
        }

        if let ageGroup = ageGroup {
            queryItems.append(URLQueryItem(name: "age", value: ageGroup))
        }

        if let diagnosis = diagnosis {
            queryItems.append(URLQueryItem(name: "diagnosis", value: diagnosis))
        }

        for therapy in therapyTypes {
            queryItems.append(URLQueryItem(name: "therapy", value: therapy))
        }

        components.queryItems = queryItems.isEmpty ? nil : queryItems
        return try await fetch(url: components.url!)
    }

    /// Get providers by regional center ZIP code
    func getProvidersByRegionalCenter(
        zipCode: String,
        insurance: String? = nil,
        ageGroup: String? = nil,
        diagnosis: String? = nil,
        therapy: String? = nil
    ) async throws -> RegionalCenterSearchResponse {
        var components = URLComponents(string: "\(baseURL)/providers-v2/by_regional_center/")!
        var queryItems = [URLQueryItem(name: "zip_code", value: zipCode)]

        if let insurance = insurance {
            queryItems.append(URLQueryItem(name: "insurance", value: insurance))
        }
        if let ageGroup = ageGroup {
            queryItems.append(URLQueryItem(name: "age", value: ageGroup))
        }
        if let diagnosis = diagnosis {
            queryItems.append(URLQueryItem(name: "diagnosis", value: diagnosis))
        }
        if let therapy = therapy {
            queryItems.append(URLQueryItem(name: "therapy", value: therapy))
        }

        components.queryItems = queryItems
        return try await fetch(url: components.url!)
    }

    // MARK: - Regional Center Endpoints

    /// Get all regional centers (paginated)
    func getRegionalCenters() async throws -> [RegionalCenter] {
        let url = URL(string: "\(baseURL)/regional-centers/")!
        let response: PaginatedResponse<RegionalCenter> = try await fetch(url: url)
        return response.results
    }

    /// Get a single regional center by ID
    func getRegionalCenter(id: Int) async throws -> RegionalCenter {
        let url = URL(string: "\(baseURL)/regional-centers/\(id)/")!
        return try await fetch(url: url)
    }

    /// Find regional center by ZIP code
    func getRegionalCenterByZip(zipCode: String) async throws -> RegionalCenter {
        var components = URLComponents(string: "\(baseURL)/regional-centers/by_zip_code/")!
        components.queryItems = [URLQueryItem(name: "zip_code", value: zipCode)]
        return try await fetch(url: components.url!)
    }

    /// Find nearby regional centers
    func getRegionalCentersNearby(
        latitude: Double,
        longitude: Double,
        radiusMiles: Double = 25
    ) async throws -> [RegionalCenter] {
        var components = URLComponents(string: "\(baseURL)/regional-centers/nearby/")!
        components.queryItems = [
            URLQueryItem(name: "lat", value: String(latitude)),
            URLQueryItem(name: "lng", value: String(longitude)),
            URLQueryItem(name: "radius", value: String(radiusMiles))
        ]
        return try await fetch(url: components.url!)
    }

    /// Get service area boundaries as GeoJSON (real polygon data)
    func getServiceAreaBoundaries() async throws -> GeoJSONFeatureCollection {
        let url = URL(string: "\(baseURL)/regional-centers/service_areas/")!
        return try await fetch(url: url)
    }

    /// Get providers for a specific regional center
    func getProvidersForRegionalCenter(id: Int) async throws -> [Provider] {
        let url = URL(string: "\(baseURL)/regional-centers/\(id)/providers/")!
        return try await fetch(url: url)
    }

    // MARK: - Reference Data Endpoints

    /// Get all insurance carriers
    func getInsuranceCarriers() async throws -> [InsuranceCarrier] {
        let url = URL(string: "\(baseURL)/insurance-carriers/")!
        return try await fetch(url: url)
    }

    /// Get all funding sources
    func getFundingSources() async throws -> [FundingSource] {
        let url = URL(string: "\(baseURL)/funding-sources/")!
        return try await fetch(url: url)
    }

    /// Get all service delivery models
    func getServiceDeliveryModels() async throws -> [ServiceDeliveryModel] {
        let url = URL(string: "\(baseURL)/service-models/")!
        return try await fetch(url: url)
    }

    // MARK: - Private Helpers

    /// Generic fetch method for GET requests
    private func fetch<T: Decodable>(url: URL) async throws -> T {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.addValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIServiceError.invalidResponse
        }

        // Log for debugging in development
        #if DEBUG
        print("📡 API Request: \(url.absoluteString)")
        print("📊 Status: \(httpResponse.statusCode)")
        #endif

        guard 200...299 ~= httpResponse.statusCode else {
            // Try to parse error message from response
            if let apiError = try? decoder.decode(APIError.self, from: data) {
                throw APIServiceError.serverError(
                    statusCode: httpResponse.statusCode,
                    message: apiError.error
                )
            }
            throw APIServiceError.httpError(statusCode: httpResponse.statusCode)
        }

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            #if DEBUG
            print("❌ Decoding error: \(error)")
            if let jsonString = String(data: data, encoding: .utf8) {
                print("📄 Response data: \(jsonString.prefix(500))...")
            }
            #endif
            throw APIServiceError.decodingError(error)
        }
    }
}

// MARK: - API Service Errors

enum APIServiceError: LocalizedError {
    case invalidResponse
    case httpError(statusCode: Int)
    case serverError(statusCode: Int, message: String)
    case decodingError(Error)
    case networkError(Error)

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Invalid response from server"
        case .httpError(let statusCode):
            return "Server error (status \(statusCode))"
        case .serverError(_, let message):
            return message
        case .decodingError(let error):
            return "Data parsing error: \(error.localizedDescription)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        }
    }
}
