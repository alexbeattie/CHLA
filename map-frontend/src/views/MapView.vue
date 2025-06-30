<template>
  <div class="map-app">
    <!-- Funding Info Modal -->
    <funding-info-panel :showModal="showFundingInfo" @close="toggleFundingInfo" />

    <!-- Sidebar (always on left) -->
    <div class="sidebar-container">
      <div class="sidebar">
        <!-- CHLA Header -->
        <div class="chla-header">
          <div class="chla-logo-container">
            <img
              src="@/assets/chla-logo.svg"
              alt="Children's Hospital Los Angeles"
              class="chla-logo"
            />
          </div>
          <div class="chla-mission">
            <h1 class="chla-title">Provider Network Map</h1>
            <p class="chla-tagline">We create hope and build healthier futures</p>
          </div>
          <button
            class="btn btn-sm chla-info-btn"
            @click="toggleFundingInfo"
            title="Funding Information"
          >
            <i class="bi bi-info-circle"></i>
          </button>
        </div>

        <!-- Display Type Selector -->
        <div class="mb-3">
          <div class="btn-group w-100 d-flex chla-btn-group">
            <button
              class="btn flex-grow-1 chla-btn"
              :class="{
                'btn-chla-primary': displayType === 'regionalCenters',
                'btn-chla-outline': displayType !== 'regionalCenters',
              }"
              @click="setDisplayType('regionalCenters')"
            >
              <i class="bi bi-building me-1"></i>
              Regional Centers
            </button>
            <button
              class="btn flex-grow-1 chla-btn"
              :class="{
                'btn-chla-primary': displayType === 'providers',
                'btn-chla-outline': displayType !== 'providers',
              }"
              @click="setDisplayType('providers')"
            >
              <i class="bi bi-hospital me-1"></i>
              Healthcare Providers
            </button>
          </div>
        </div>

        <!-- Service Areas Controls -->
        <div class="mb-3">
          <div class="form-check">
            <input
              class="form-check-input"
              type="checkbox"
              v-model="showServiceAreas"
              id="showServiceAreas"
              @change="toggleServiceAreas"
            />
            <label class="form-check-label" for="showServiceAreas">
              <i class="bi bi-map"></i> Show Service Areas
              <span
                v-if="showServiceAreas && !serviceAreasLoaded"
                class="spinner-border spinner-border-sm ms-2"
                role="status"
              >
                <span class="visually-hidden">Loading...</span>
              </span>
            </label>
          </div>

          <!-- Pin Service Areas Option -->
          <div class="form-check mt-2" v-if="showServiceAreas">
            <input
              class="form-check-input"
              type="checkbox"
              v-model="pinServiceAreas"
              id="pinServiceAreas"
              @change="togglePinServiceAreas"
            />
            <label class="form-check-label" for="pinServiceAreas">
              <i class="bi bi-pin-map-fill"></i> Pin to Map (Always Visible)
            </label>
          </div>

          <!-- Location Status -->
          <div
            class="alert mb-3"
            :class="userLocation.detected ? 'alert-success' : 'alert-warning'"
          >
            <div class="d-flex align-items-center">
              <i
                :class="userLocation.detected ? 'bi bi-geo-alt-fill' : 'bi bi-geo-alt'"
              ></i>
              <div class="ms-2 flex-grow-1">
                <strong v-if="userLocation.detected">📍 Location Detected</strong>
                <strong v-else>⚠️ Using Default Location</strong>
                <div class="small">
                  {{ userData.address || "California" }}
                  <span
                    v-if="userLocation.accuracy && userLocation.detected"
                    class="text-muted"
                  >
                    (±{{ Math.round(userLocation.accuracy) }}m)
                  </span>
                </div>
                <div v-if="userLocation.error" class="small text-muted">
                  {{ userLocation.error }}
                </div>
              </div>
            </div>
            <div v-if="!userLocation.detected" class="small mt-2">
              <em
                >Use the location button (🎯) on the map to enable precise location
                detection</em
              >
            </div>
          </div>

          <!-- Service Areas Info -->
          <small
            v-if="showServiceAreas && serviceAreasLoaded"
            class="text-success d-block mt-2"
          >
            <i class="bi bi-check-circle-fill"></i>
            {{ serviceAreas?.features?.length || 0 }} county-based service areas loaded
            <span v-if="pinServiceAreas" class="badge bg-primary ms-2">
              <i class="bi bi-pin-fill"></i> PINNED
            </span>
          </small>

          <!-- Service Areas Legend -->
          <div v-if="showServiceAreas && serviceAreasLoaded" class="mt-2">
            <small class="text-muted">
              <strong>🗺️ California Counties by Service Level:</strong><br />
              • <span style="color: #e8e8e8">■</span> No regional centers<br />
              • <span style="color: #a8d5e5">■</span> 1-2 regional centers<br />
              • <span style="color: #5dade2">■</span> 3-5 regional centers<br />
              • <span style="color: #3498db">■</span> 6-10 regional centers<br />
              • <span style="color: #1f618d">■</span> 11+ regional centers<br />
              <small
                ><em
                  >Click counties for regional center details<br />
                  Provider markers shown when relevant</em
                ></small
              >
            </small>
          </div>
        </div>

        <!-- User Information Panel -->
        <user-info-panel
          :user-data="userData"
          :show-panel="showUserPanel"
          @toggle="toggleUserPanel"
          @save="saveUserData"
        />

        <!-- User Info Summary - shown when panel is collapsed but data exists -->
        <div v-if="!showUserPanel && hasUserData" class="alert alert-info mb-3">
          <div class="d-flex justify-content-between align-items-center">
            <span>
              <strong>{{ userData.age ? "Age: " + userData.age : "" }}</strong>
              {{ userData.diagnosis ? " | " + userData.diagnosis : "" }}
            </span>
            <button class="btn btn-sm btn-outline-primary" @click="toggleUserPanel">
              Edit
            </button>
          </div>
        </div>

        <!-- Search Box -->
        <div class="mb-3">
          <div class="input-group">
            <input
              type="text"
              v-model="searchText"
              class="form-control"
              placeholder="Search locations..."
              @input="debounceSearch"
            />
            <button
              class="btn btn-outline-secondary"
              type="button"
              @click="searchText = ''"
              v-if="searchText"
              title="Clear search"
            >
              <i class="bi bi-x"></i>
            </button>
          </div>
          <small class="text-muted mt-1" v-if="searchText">
            <i class="bi bi-info-circle-fill me-1"></i>
            Searching by name, address, and service details
          </small>
        </div>

        <!-- Filter Section -->
        <div class="filter-group mb-3">
          <h5>Filters</h5>

          <!-- Radius Filter (when geolocation is available) -->
          <div class="mb-2" v-if="userLocation.latitude && userLocation.longitude">
            <div class="d-flex justify-content-between align-items-center">
              <label class="form-label mb-0"
                >Distance Radius: <strong>{{ radius }} miles</strong></label
              >
              <span class="badge bg-info">{{ countLocationsInRadius }} found</span>
            </div>
            <input
              type="range"
              v-model.number="radius"
              class="form-range"
              min="5"
              max="75"
              step="5"
              @change="updateFilteredLocations"
            />
            <div class="d-flex justify-content-between">
              <small>5 miles</small>
              <small>75 miles</small>
            </div>
          </div>

          <!-- Filter Options for Providers -->
          <div v-if="displayType === 'providers'" class="mb-3">
            <h6 class="text-muted mb-2">Payment & Funding</h6>
            <div class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                v-model="filterOptions.acceptsInsurance"
                id="acceptsInsurance"
                @change="updateFilteredLocations"
              />
              <label class="form-check-label" for="acceptsInsurance">
                <i class="bi bi-credit-card me-1"></i>
                Accepts Insurance
              </label>
            </div>
            <div class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                v-model="filterOptions.acceptsRegionalCenter"
                id="acceptsRegionalCenter"
                @change="updateFilteredLocations"
              />
              <label class="form-check-label" for="acceptsRegionalCenter">
                <i class="bi bi-building me-1"></i>
                Accepts Regional Center
              </label>
            </div>
            <div class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                v-model="filterOptions.acceptsPrivatePay"
                id="acceptsPrivatePay"
                @change="updateFilteredLocations"
              />
              <label class="form-check-label" for="acceptsPrivatePay">
                <i class="bi bi-wallet2 me-1"></i>
                Accepts Private Pay
              </label>
            </div>

            <h6 class="text-muted mb-2 mt-3">Service Matching</h6>
            <div class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                v-model="filterOptions.matchesDiagnosis"
                id="matchesDiagnosis"
                :disabled="!userData.diagnosis"
                @change="updateFilteredLocations"
              />
              <label class="form-check-label" for="matchesDiagnosis">
                <i class="bi bi-person-check me-1"></i>
                Matches My Diagnosis
                <small class="text-muted d-block" v-if="userData.diagnosis">
                  ({{ userData.diagnosis }})
                </small>
              </label>
            </div>
            <div class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                v-model="filterOptions.matchesAge"
                id="matchesAge"
                :disabled="!userData.age"
                @change="updateFilteredLocations"
              />
              <label class="form-check-label" for="matchesAge">
                <i class="bi bi-calendar-check me-1"></i>
                Serves My Age Group
                <small class="text-muted d-block" v-if="userData.age">
                  (Age {{ userData.age }})
                </small>
              </label>
            </div>
          </div>

          <!-- Reset Button -->
          <button @click="resetFilters" class="btn btn-secondary btn-sm w-100 mt-2">
            <i class="bi bi-arrow-counterclockwise me-1"></i> Reset Filters
          </button>
        </div>

        <!-- Results Section -->
        <div class="results-section">
          <h5 class="results-title">
            {{
              displayType === "locations"
                ? "Locations"
                : displayType === "regionalCenters"
                ? "Regional Centers"
                : "Providers"
            }}
            ({{
              displayType === "locations"
                ? filteredLocations.length
                : displayType === "regionalCenters"
                ? filteredRegionalCenters.length
                : filteredProviders.length
            }})
          </h5>

          <!-- Location List -->
          <location-list
            :locations="
              displayType === 'locations'
                ? filteredLocations
                : displayType === 'regionalCenters'
                ? filteredRegionalCenters
                : filteredProviders
            "
            :loading="loading"
            :error="error"
            @center-on-location="centerMapOnLocation"
          />
        </div>
      </div>
    </div>

    <!-- Map Container -->
    <div class="map-container-wrapper">
      <div id="map" class="map-container"></div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import mapboxgl from "mapbox-gl";
import UserInfoPanel from "@/components/UserInfoPanel.vue";
import LocationList from "@/components/LocationList.vue";
import FundingInfoPanel from "@/components/FundingInfoPanel.vue";
import {
  sampleCategories,
  sampleLocations,
  sampleUserProfile,
} from "@/assets/sampleData";

// Flag to use actual API data instead of sample data - set to false to query the database
const USE_LOCAL_DATA_ONLY = false;

export default {
  name: "MapView",

  components: {
    UserInfoPanel,
    LocationList,
    FundingInfoPanel,
  },

  data() {
    return {
      // Modal visibility
      showFundingInfo: false,

      // Display type
      displayType: "providers", // 'regionalCenters' or 'providers'

      // Service areas
      showServiceAreas: false,
      pinServiceAreas: false,
      serviceAreas: null,
      serviceAreasLoaded: false,

      // Filter options
      filterOptions: {
        acceptsInsurance: false,
        acceptsRegionalCenter: false,
        acceptsPrivatePay: false,
        matchesDiagnosis: false,
        matchesAge: false,
      },

      // Search debounce
      searchDebounce: null,

      // Map data
      map: null,
      markers: [],

      // Locations
      locations: [],
      regionalCenters: [], // Array to store regional centers
      providers: [], // Array to store providers

      // Categories and filters
      categories: [],
      category: null, // Add this property explicitly to avoid warnings
      selectedCategory: "",
      searchText: "",

      // User location (will be determined by geolocation or fallback)
      userLocation: {
        latitude: null,
        longitude: null,
        accuracy: null,
        detected: false,
        error: null,
      },
      radius: 15, // miles (increased from 5 to find more results)

      // User information
      userData: {
        age: "",
        address: "",
        diagnosis: "",
        otherDiagnosis: "",
      },
      showUserPanel: true, // Show by default

      // UI state
      loading: true,
      error: null,

      // Layout handling
      resizeTimeout: null,
    };
  },

  computed: {
    hasUserData() {
      // Check if user has entered any meaningful data
      return (
        (this.userData.age && this.userData.age !== "") ||
        (this.userData.address && this.userData.address !== "") ||
        (this.userData.diagnosis && this.userData.diagnosis !== "")
      );
    },

    // Count of locations within radius
    countLocationsInRadius() {
      if (this.displayType === "providers") {
        return this.filteredProviders.length;
      } else if (this.displayType === "regionalCenters") {
        return this.filteredRegionalCenters.length;
      } else {
        return this.filteredLocations.length;
      }
    },

    // Filtered providers - since we use API-level filtering, just return the providers
    filteredProviders() {
      if (!this.providers.length) return [];

      // Since we're using API-level filtering in fetchProviders(),
      // the this.providers array already contains the correctly filtered results
      // No need for additional client-side filtering
      console.log(
        `Returning ${this.providers.length} providers from API (already filtered)`
      );
      return this.providers;
    },

    // Filtered regional centers
    filteredRegionalCenters() {
      return this.regionalCenters;
    },

    // Filtered locations
    filteredLocations() {
      return this.locations;
    },
  },

  created() {
    console.log("Vue app created");

    // Initialize with empty arrays to prevent null reference errors
    this.categories = [];
    this.locations = [];
    this.markers = [];
    this.providers = [];

    // Load saved user data if available
    this.loadUserData();

    // Detect user location first, then fetch providers
    this.detectUserLocation();
  },

  mounted() {
    console.log("Vue app mounted");

    // Initialize the map
    this.$nextTick(() => {
      try {
        this.initMap();
      } catch (e) {
        console.error("Error initializing map:", e);
      }
    });
  },

  methods: {
    // Toggle funding info modal
    toggleFundingInfo() {
      this.showFundingInfo = !this.showFundingInfo;
    },

    // Update filtered locations based on filters
    updateFilteredLocations() {
      console.log("Updating filtered locations with filters:", this.filterOptions);

      // If any provider-specific filters are enabled, automatically switch to providers
      const hasProviderFilters =
        this.filterOptions.acceptsInsurance ||
        this.filterOptions.acceptsRegionalCenter ||
        this.filterOptions.acceptsPrivatePay ||
        this.filterOptions.matchesDiagnosis ||
        this.filterOptions.matchesAge;

      if (hasProviderFilters && this.displayType !== "providers") {
        console.log("🔄 Provider filters applied, switching to providers view");
        this.displayType = "providers";
      }

      // Log current filter state for debugging
      console.log("🎛️ Current filter state:", {
        acceptsInsurance: this.filterOptions.acceptsInsurance,
        acceptsRegionalCenter: this.filterOptions.acceptsRegionalCenter,
        matchesDiagnosis: this.filterOptions.matchesDiagnosis,
        displayType: this.displayType,
      });

      // If we're showing providers, refetch from API with filters
      if (this.displayType === "providers") {
        this.fetchProviders();
      } else {
        // For other types, just update markers
        this.$nextTick(() => {
          this.updateMarkers();
        });
      }
    },

    // Debounce search to prevent too many updates
    debounceSearch() {
      if (this.searchDebounce) {
        clearTimeout(this.searchDebounce);
      }

      this.searchDebounce = setTimeout(() => {
        this.updateFilteredLocations();
      }, 300);
    },

    // Set display type
    setDisplayType(type) {
      this.displayType = type;

      // Fetch data if needed
      if (type === "providers" && this.providers.length === 0) {
        this.fetchProviders();
      } else if (type === "regionalCenters" && this.regionalCenters.length === 0) {
        this.fetchRegionalCenters();
      }

      // Update markers
      this.$nextTick(() => {
        this.updateMarkers();
      });
    },

    // Reset all filters
    resetFilters() {
      this.selectedCategory = "";
      this.searchText = "";
      this.radius = 15; // Reset to 15 miles for better coverage

      // Reset filter options
      this.filterOptions = {
        acceptsInsurance: false,
        acceptsRegionalCenter: false,
        acceptsPrivatePay: false,
        matchesDiagnosis: false,
        matchesAge: false,
      };

      // Re-fetch data with reset filters (maintain current display type)
      console.log(`Resetting filters, maintaining display type: ${this.displayType}`);
      if (this.displayType === "providers") {
        this.fetchProviders();
      } else if (this.displayType === "regionalCenters") {
        this.fetchRegionalCenters();
      } else {
        this.updateFilteredLocations();
      }
    },

    // User panel methods
    toggleUserPanel() {
      this.showUserPanel = !this.showUserPanel;
    },

    saveUserData(userData) {
      this.userData = { ...userData };
      this.showUserPanel = false;

      // Apply filters with new user data
      this.updateFilteredLocations();
    },

    loadUserData() {
      // Use sample data for testing
      this.userData = {
        age: "5",
        address: "Los Angeles, CA",
        diagnosis: "Autism",
        otherDiagnosis: "",
      };
    },

    // Detect user location using browser geolocation API
    async detectUserLocation() {
      console.log("🌍 Detecting user location...");

      if (!navigator.geolocation) {
        console.warn("⚠️ Geolocation not supported by this browser");
        this.setFallbackLocation("Geolocation not supported");
        return;
      }

      const options = {
        enableHighAccuracy: true,
        timeout: 10000, // 10 seconds
        maximumAge: 300000, // 5 minutes
      };

      try {
        const position = await new Promise((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject, options);
        });

        const { latitude, longitude, accuracy } = position.coords;

        console.log(
          `✅ Location detected: ${latitude}, ${longitude} (accuracy: ${accuracy}m)`
        );

        this.userLocation = {
          latitude: latitude,
          longitude: longitude,
          accuracy: accuracy,
          detected: true,
          error: null,
        };

        // Update user address data based on location
        this.reverseGeocode(latitude, longitude);

        // Initialize map with detected location
        this.initMap();

        // Fetch providers for the detected location
        setTimeout(() => {
          this.fetchProviders();
        }, 500);
      } catch (error) {
        console.warn("⚠️ Geolocation failed:", error.message);
        let errorMessage = "Location detection failed";

        switch (error.code) {
          case error.PERMISSION_DENIED:
            errorMessage = "Location access denied by user";
            break;
          case error.POSITION_UNAVAILABLE:
            errorMessage = "Location information unavailable";
            break;
          case error.TIMEOUT:
            errorMessage = "Location request timed out";
            break;
        }

        this.setFallbackLocation(errorMessage);
      }
    },

    // Set fallback location (default to California center for statewide coverage)
    setFallbackLocation(error = null) {
      console.log("🏠 Using fallback location (California center)");

      this.userLocation = {
        latitude: 36.7783, // California geographic center
        longitude: -119.4179,
        accuracy: null,
        detected: false,
        error: error,
      };

      // Update user address to reflect fallback
      this.userData.address = "California (location detection failed)";

      // Initialize map with fallback location
      this.initMap();

      // Fetch providers for the fallback location
      setTimeout(() => {
        this.fetchProviders();
      }, 500);
    },

    // Reverse geocode to get address from coordinates
    async reverseGeocode(latitude, longitude) {
      try {
        const response = await fetch(
          `https://api.mapbox.com/geocoding/v5/mapbox.places/${longitude},${latitude}.json?access_token=${mapboxgl.accessToken}&types=place,region`
        );
        const data = await response.json();

        if (data.features && data.features.length > 0) {
          const place = data.features[0];
          const city = place.context?.find((c) => c.id.includes("place"))?.text || "";
          const state = place.context?.find((c) => c.id.includes("region"))?.text || "";

          const address =
            `${city}, ${state}`.replace(/^, |, $/, "") ||
            `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;

          console.log(`🏠 Detected address: ${address}`);
          this.userData.address = address;
        }
      } catch (error) {
        console.warn("⚠️ Reverse geocoding failed:", error);
        this.userData.address = `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
      }
    },

    // Map initialization
    initMap() {
      // Ensure we have valid coordinates before initializing
      if (!this.userLocation.latitude || !this.userLocation.longitude) {
        console.warn(
          "⚠️ No valid coordinates for map initialization, using California center"
        );
        this.userLocation.latitude = 36.7783;
        this.userLocation.longitude = -119.4179;
      }

      // Set Mapbox access token
      mapboxgl.accessToken =
        import.meta.env.VITE_MAPBOX_TOKEN ||
        "pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg";

      console.log(
        `🗺️ Initializing map at: ${this.userLocation.latitude}, ${this.userLocation.longitude}`
      );

      // Create Mapbox map
      this.map = new mapboxgl.Map({
        container: "map",
        style: "mapbox://styles/mapbox/streets-v12",
        center: [this.userLocation.longitude, this.userLocation.latitude],
        zoom: this.userLocation.detected ? 12 : 6, // Zoom in more if we detected exact location
      });

      // Add navigation controls
      this.map.addControl(new mapboxgl.NavigationControl(), "top-right");

      // Add geolocation control for users to manually update their location
      this.map.addControl(
        new mapboxgl.GeolocateControl({
          positionOptions: {
            enableHighAccuracy: true,
          },
          fitBoundsOptions: {
            maxZoom: 15,
          },
          trackUserLocation: false,
          showAccuracyCircle: true,
        }),
        "top-right"
      );

      // When map loads, update markers
      this.map.on("load", () => {
        console.log("Map loaded successfully");
        this.updateMarkers();

        // If service areas are already enabled, add them now
        if (this.showServiceAreas && this.serviceAreasLoaded) {
          console.log("Map loaded and service areas are enabled, adding them now");
          this.addServiceAreasToMap();
        }
      });

      console.log("Map initialization complete");
    },

    // Fetch provider data
    async fetchProviders() {
      this.loading = true;
      this.error = null;

      try {
        console.log("Fetching providers from API");
        const apiBaseUrl = "http://127.0.0.1:8001/api"; // Temporarily use direct URL to bypass proxy

        // If using local data, use sample data
        if (USE_LOCAL_DATA_ONLY) {
          console.log("Using local data instead of API");
          // Hard-coded sample data with acceptance flags
          const sampleProviders = [
            {
              id: 1,
              name: "A & H BEHAVIORAL THERAPY",
              phone: "909-665-7070",
              coverage_areas: "SAN FERNANDO VALLEY, HOLLYWOOD",
              age_groups_served: "0-5, 6-12, 13-18, 19+",
              diagnoses_served: "Autism, ADHD, Learning Disabilities",
              address: "123 Main St, Los Angeles, CA",
              city: "Los Angeles",
              state: "CA",
              zip_code: "90001",
              latitude: 34.052,
              longitude: -118.243,
              accepts_insurance: true,
              accepts_regional_center: true,
            },
            {
              id: 2,
              name: "A CHANGE IN TRAJECTORY, INC.",
              phone: "818-235-1414",
              coverage_areas: "VALLEY, CENTRAL LA",
              age_groups_served: "6-12, 13-18, 19+",
              diagnoses_served: "Autism, Developmental Delay, Learning Disability",
              address: "456 Center Blvd, Sherman Oaks, CA",
              city: "Sherman Oaks",
              state: "CA",
              zip_code: "91423",
              latitude: 34.15,
              longitude: -118.45,
              accepts_insurance: true,
              accepts_regional_center: false,
            },
            {
              id: 3,
              name: "ABA ENHANCEMENT INC.",
              phone: "951-317-5950",
              coverage_areas: "LONG BEACH, SOUTH LA",
              age_groups_served: "0-5, 6-12",
              diagnoses_served: "Autism, Speech Delay, Sensory Processing",
              address: "789 Broadway, Long Beach, CA",
              city: "Long Beach",
              state: "CA",
              zip_code: "90802",
              latitude: 33.77,
              longitude: -118.19,
              accepts_insurance: false,
              accepts_regional_center: true,
            },
          ];

          // Set providers
          this.providers = sampleProviders;
          console.log(`Loaded ${this.providers.length} sample providers`);
        } else {
          // Use the comprehensive search endpoint for better filtering capabilities
          let queryParams = new URLSearchParams();

          // Check if we're doing specific filtering (beyond location)
          const hasSpecificFilters =
            this.filterOptions.acceptsInsurance ||
            this.filterOptions.acceptsRegionalCenter ||
            this.filterOptions.acceptsPrivatePay ||
            this.filterOptions.matchesDiagnosis ||
            this.filterOptions.matchesAge;

          // Add search text if available
          if (this.searchText && this.searchText.trim() !== "") {
            queryParams.append("q", this.searchText.trim());
          }

          // Always add location/radius if available (for geographic relevance)
          if (this.userLocation.latitude && this.userLocation.longitude) {
            queryParams.append("lat", this.userLocation.latitude);
            queryParams.append("lng", this.userLocation.longitude);
            queryParams.append("radius", this.radius);
          }

          // Only add user profile filters if specific filters are enabled
          if (this.filterOptions.matchesAge && this.userData.age) {
            queryParams.append("age", this.userData.age);
          }

          if (this.filterOptions.matchesDiagnosis && this.userData.diagnosis) {
            queryParams.append("diagnosis", this.userData.diagnosis);
          }

          // Add insurance filter options only when explicitly checked
          if (this.filterOptions.acceptsInsurance) {
            queryParams.append("insurance", "insurance");
          }

          if (this.filterOptions.acceptsRegionalCenter) {
            queryParams.append("insurance", "regional center");
          }

          if (this.filterOptions.acceptsPrivatePay) {
            queryParams.append("insurance", "private pay");
          }

          // Add specialization filter for diagnosis matching only when enabled
          if (this.filterOptions.matchesDiagnosis && this.userData.diagnosis) {
            queryParams.append("specialization", this.userData.diagnosis);
          }

          // Always use comprehensive search endpoint (it handles both filtered and unfiltered)
          const url = `${apiBaseUrl}/providers/comprehensive_search/?${queryParams.toString()}`;

          if (hasSpecificFilters) {
            console.log(`🔍 Fetching FILTERED providers from API: ${url}`);
            console.log("🎛️ Active filters:", {
              acceptsInsurance: this.filterOptions.acceptsInsurance,
              acceptsRegionalCenter: this.filterOptions.acceptsRegionalCenter,
              acceptsPrivatePay: this.filterOptions.acceptsPrivatePay,
              matchesDiagnosis: this.filterOptions.matchesDiagnosis,
              matchesAge: this.filterOptions.matchesAge,
              hasSearchText: !!(this.searchText && this.searchText.trim() !== ""),
            });

            // Special debugging for insurance filter bug
            if (this.filterOptions.acceptsInsurance) {
              console.log("🚨 INSURANCE FILTER ACTIVE - Watch for invalid coordinates!");
            }
          } else {
            console.log(`📋 Fetching ALL providers in radius from API: ${url}`);
            console.log("🎛️ No specific filters - showing all providers in area");
          }

          const response = await axios.get(url);
          console.log("API Response:", response);

          // Handle regular JSON array response
          if (response.data && Array.isArray(response.data)) {
            this.providers = response.data;
            console.log(
              `✅ Loaded ${this.providers.length} providers from API (direct array)`
            );
            console.log("Filter status:", {
              acceptsInsurance: this.filterOptions.acceptsInsurance,
              acceptsRegionalCenter: this.filterOptions.acceptsRegionalCenter,
              matchesDiagnosis: this.filterOptions.matchesDiagnosis,
            });

            // Special debugging for insurance filter - log ALL providers
            if (this.filterOptions.acceptsInsurance) {
              console.log("🚨 INSURANCE FILTER RESULTS - All providers:");
              this.providers.forEach((provider, index) => {
                console.log(`Provider ${index + 1}: ${provider.name}`, {
                  latitude: provider.latitude,
                  longitude: provider.longitude,
                  lat_type: typeof provider.latitude,
                  lng_type: typeof provider.longitude,
                  accepts_insurance: provider.accepts_insurance,
                });
              });
            }

            // Debug: Log first provider details
            if (this.providers.length > 0) {
              console.log("🔍 First provider details:", {
                name: this.providers[0].name,
                latitude: this.providers[0].latitude,
                longitude: this.providers[0].longitude,
                lat_type: typeof this.providers[0].latitude,
                lng_type: typeof this.providers[0].longitude,
              });
            }
          }
          // Handle paginated response
          else if (response.data && Array.isArray(response.data.results)) {
            this.providers = response.data.results;
            console.log(
              `✅ Loaded ${this.providers.length} providers from API (paginated)`
            );
            console.log("Filter status:", {
              acceptsInsurance: this.filterOptions.acceptsInsurance,
              acceptsRegionalCenter: this.filterOptions.acceptsRegionalCenter,
              matchesDiagnosis: this.filterOptions.matchesDiagnosis,
            });
          } else {
            console.error("Unexpected API response format:", response.data);
            throw new Error("Unexpected API response format");
          }

          // Convert string coordinates to numbers and validate
          console.log(
            `🔄 Processing ${this.providers.length} providers for coordinate conversion`
          );
          this.providers.forEach((provider, index) => {
            const debugInfo = {
              name: provider.name,
              original_lat: provider.latitude,
              original_lng: provider.longitude,
              lat_type: typeof provider.latitude,
              lng_type: typeof provider.longitude,
            };

            if (index < 3) {
              // Log first 3 providers in detail
              console.log(`Processing provider ${index + 1}:`, debugInfo);
            }

            // Convert string coordinates to numbers
            if (provider.latitude && provider.longitude) {
              provider.latitude = parseFloat(provider.latitude);
              provider.longitude = parseFloat(provider.longitude);
            }

            // Check for invalid coordinates and provide fallback
            if (
              !provider.latitude ||
              !provider.longitude ||
              isNaN(provider.latitude) ||
              isNaN(provider.longitude) ||
              provider.latitude === 0 ||
              provider.longitude === 0
            ) {
              console.warn(
                `Provider ${provider.name} (ID: ${provider.id}) has invalid coordinates, SKIPPING:`,
                {
                  latitude: provider.latitude,
                  longitude: provider.longitude,
                  original: debugInfo,
                }
              );
              // Instead of fallback, mark as invalid so we skip it in marker creation
              provider._coordinatesInvalid = true;
            } else {
              console.log(
                `✅ Provider ${provider.name}: lat=${provider.latitude}, lng=${provider.longitude}`
              );
              provider._coordinatesInvalid = false;
            }
          });

          console.log(`Loaded ${this.providers.length} providers from API`);
        }
      } catch (error) {
        console.error("Error loading providers:", error);
        this.error = "Failed to load providers";

        // Provide sample data in case of error for better user experience
        this.providers = [
          {
            id: 1,
            name: "A & H BEHAVIORAL THERAPY (Sample)",
            phone: "909-665-7070",
            coverage_areas: "SAN FERNANDO VALLEY, HOLLYWOOD",
            age_groups_served: "0-5, 6-12, 13-18, 19+",
            diagnoses_served: "Autism, ADHD, Learning Disabilities",
            address: "123 Main St, Los Angeles, CA",
            city: "Los Angeles",
            state: "CA",
            zip_code: "90001",
            latitude: 34.052,
            longitude: -118.243,
            accepts_insurance: true,
            accepts_regional_center: true,
          },
        ];
      } finally {
        this.loading = false;

        // Force marker update after data change with proper timing
        console.log(`🎯 About to update markers with ${this.providers.length} providers`);
        this.$nextTick(() => {
          this.updateMarkers();
          console.log(`🗺️ Markers updated for ${this.providers.length} providers`);
        });
      }
    },

    // Fetch regional centers
    async fetchRegionalCenters() {
      this.loading = true;
      this.error = null;

      try {
        console.log("Fetching regional centers");
        const apiBaseUrl = "http://127.0.0.1:8001/api"; // Temporarily use direct URL to bypass proxy

        // If using local data, use sample data
        if (USE_LOCAL_DATA_ONLY) {
          console.log("Using local data instead of API");
          const sampleRegionalCenters = [
            {
              id: 1,
              name: "Far Northern Regional Center",
              address: "421 Chestnut Street",
              city: "Mt. Shasta",
              state: "CA",
              zip_code: "96067",
              latitude: 41.31,
              longitude: -122.31,
              phone: "530-555-1000",
            },
            {
              id: 2,
              name: "North Bay Regional Center",
              address: "519 E Street",
              city: "Santa Rosa",
              state: "CA",
              zip_code: "95404",
              latitude: 38.444,
              longitude: -122.714,
              phone: "707-555-2000",
            },
          ];

          this.regionalCenters = sampleRegionalCenters;
        } else {
          // Fetch from API
          const url = `${apiBaseUrl}/regional-centers/`;
          console.log(`Fetching regional centers from API: ${url}`);

          try {
            const response = await axios.get(url);
            console.log("API Response:", response);

            if (response.data && Array.isArray(response.data)) {
              this.regionalCenters = response.data;
            } else if (response.data && Array.isArray(response.data.results)) {
              this.regionalCenters = response.data.results;
            } else {
              console.error("Unexpected API response format:", response.data);
              throw new Error("Unexpected API response format");
            }

            console.log(
              `Loaded ${this.regionalCenters.length} regional centers from API`
            );
          } catch (apiError) {
            console.error(
              "Error fetching from API, falling back to sample data:",
              apiError
            );
            throw apiError;
          }
        }
      } catch (error) {
        console.error("Error loading regional centers:", error);
        this.error = "Failed to load regional centers";

        // Provide sample data in case of error for better user experience
        this.regionalCenters = [
          {
            id: 1,
            name: "Far Northern Regional Center (Sample)",
            address: "421 Chestnut Street",
            city: "Mt. Shasta",
            state: "CA",
            zip_code: "96067",
            latitude: 41.31,
            longitude: -122.31,
            phone: "530-555-1000",
          },
        ];
      } finally {
        this.loading = false;
        this.updateMarkers();
      }
    },

    // Fetch service areas
    async fetchServiceAreas() {
      if (this.serviceAreasLoaded) {
        console.log("Service areas already loaded, skipping fetch");
        return; // Already loaded
      }

      try {
        console.log("Fetching service areas from API...");
        const apiBaseUrl = "http://127.0.0.1:8001/api"; // Temporarily use direct URL to bypass proxy
        const url = `${apiBaseUrl}/regional-centers/service_areas/`;

        const response = await axios.get(url);
        console.log("Service areas API Response status:", response.status);
        console.log("Service areas API Response data type:", typeof response.data);

        if (response.data && response.data.type === "FeatureCollection") {
          this.serviceAreas = response.data;
          this.serviceAreasLoaded = true;
          console.log(
            `Successfully loaded ${response.data.features.length} service areas`
          );
        } else {
          console.error("Unexpected service areas API response format:", response.data);
          throw new Error("Invalid API response format");
        }
      } catch (error) {
        console.error("Error loading service areas:", error);
        this.serviceAreasLoaded = false;
        this.serviceAreas = null;

        // Show user-friendly error
        if (error.response) {
          console.error(
            "API responded with error:",
            error.response.status,
            error.response.data
          );
        } else if (error.request) {
          console.error("No response received from API");
        } else {
          console.error("Error setting up request:", error.message);
        }
      }
    },

    // Toggle service areas visibility
    async toggleServiceAreas() {
      console.log("toggleServiceAreas called, showServiceAreas:", this.showServiceAreas);

      if (!this.map) {
        console.error("Map not initialized yet");
        return;
      }

      if (this.showServiceAreas) {
        console.log("Showing service areas...");
        console.log("serviceAreasLoaded:", this.serviceAreasLoaded);
        console.log("serviceAreas type:", typeof this.serviceAreas);
        console.log("serviceAreas:", this.serviceAreas);

        // Load service areas if not already loaded
        if (!this.serviceAreasLoaded) {
          console.log("Loading service areas...");
          await this.fetchServiceAreas();
          console.log("After fetch - serviceAreasLoaded:", this.serviceAreasLoaded);
          console.log("After fetch - serviceAreas:", this.serviceAreas);
        }

        if (this.serviceAreas && this.serviceAreas.features) {
          console.log(
            "Adding service areas to map, features count:",
            this.serviceAreas.features.length
          );
          this.addServiceAreasToMap();
          // Update markers to hide redundant regional center markers
          this.updateMarkers();
        } else {
          console.error("No service areas data available");
          console.error("serviceAreas exists:", !!this.serviceAreas);
          console.error(
            "serviceAreas.features exists:",
            !!(this.serviceAreas && this.serviceAreas.features)
          );
          console.error("serviceAreas structure:", this.serviceAreas);
        }
      } else {
        console.log("Hiding service areas...");
        this.removeServiceAreasFromMap();
        // Update markers to show regional center markers again if needed
        this.updateMarkers();
      }
    },

    // Add service areas to map
    async addServiceAreasToMap() {
      console.log("addServiceAreasToMap called");

      if (!this.map) {
        console.error("Map not available");
        return;
      }

      if (!this.serviceAreas || !this.serviceAreas.features) {
        console.error("Service areas data not available");
        console.error("serviceAreas:", this.serviceAreas);
        console.error("serviceAreas type:", typeof this.serviceAreas);
        console.error(
          "serviceAreas.features:",
          this.serviceAreas ? this.serviceAreas.features : "serviceAreas is null"
        );
        return;
      }

      console.log("Map loaded state:", this.map.loaded());

      // Ensure map is loaded before adding layers
      if (!this.map.loaded()) {
        console.log("Map not loaded yet, waiting...");
        this.map.once("load", () => {
          console.log("Map loaded, now adding service areas");
          this.addServiceAreasToMap();
        });
        return;
      }

      try {
        // Remove existing layers first to avoid conflicts
        this.removeServiceAreasFromMap();

        console.log("Adding service areas source and layers...");
        console.log(`Service areas data type: ${this.serviceAreas.type}`);
        console.log(`Number of features: ${this.serviceAreas.features.length}`);
        console.log(
          "Sample feature IDs:",
          this.serviceAreas.features.slice(0, 5).map((f) => f.id)
        );
        console.log(
          "All unique IDs:",
          [...new Set(this.serviceAreas.features.map((f) => f.id))].slice(0, 20)
        );

        // Add California counties source using our free API
        const countiesResponse = await fetch(
          "http://127.0.0.1:8001/api/california-counties/"
        );
        const countiesData = await countiesResponse.json();

        // Enhance counties data with regional center counts
        if (this.serviceAreas && this.serviceAreas.features) {
          countiesData.features.forEach((county) => {
            const countyName = county.properties.name || "";
            const regionalCentersInCounty = this.serviceAreas.features.filter(
              (sa) =>
                sa.properties.county_served &&
                sa.properties.county_served
                  .toLowerCase()
                  .includes(countyName.toLowerCase())
            );
            county.properties.regional_center_count = regionalCentersInCounty.length;
            county.properties.has_service = regionalCentersInCounty.length > 0;
          });
        }

        this.map.addSource("counties", {
          type: "geojson",
          data: countiesData,
        });

        // Add our service areas source for regional center data
        this.map.addSource("service-areas", {
          type: "geojson",
          data: this.serviceAreas,
        });

        // Add real California county boundaries
        this.map.addLayer({
          id: "california-counties-fill",
          type: "fill",
          source: "counties",
          paint: {
            "fill-color": [
              "case",
              // Counties with no regional centers - light gray
              ["==", ["get", "regional_center_count"], 0],
              "#E8E8E8",
              // Counties with 1-2 regional centers - light blue
              ["<=", ["get", "regional_center_count"], 2],
              "#A8D5E5",
              // Counties with 3-5 regional centers - medium blue
              ["<=", ["get", "regional_center_count"], 5],
              "#5DADE2",
              // Counties with 6-10 regional centers - stronger blue
              ["<=", ["get", "regional_center_count"], 10],
              "#3498DB",
              // Counties with 11+ regional centers - dark blue
              "#1F618D",
            ],
            "fill-opacity": [
              "case",
              ["boolean", ["feature-state", "hover"], false],
              0.8, // Higher opacity on hover
              0.6, // Default opacity
            ],
          },
        });

        // Add real California county boundaries outlines
        this.map.addLayer({
          id: "california-counties-outline",
          type: "line",
          source: "counties",
          paint: {
            "line-color": "#2c3e50",
            "line-width": [
              "case",
              ["boolean", ["feature-state", "hover"], false],
              3, // Thicker on hover
              1.5, // Default width
            ],
            "line-opacity": 0.8,
          },
        });

        // No service area overlays - data will be integrated into counties

        // Add click event to show California county info with regional center data
        this.map.on("click", "california-counties-fill", (e) => {
          const feature = e.features[0];
          const countyName = feature.properties.name || "Unknown";

          // Find regional centers serving this county
          const regionalCentersInCounty = this.serviceAreas
            ? this.serviceAreas.features.filter(
                (sa) =>
                  sa.properties.county_served &&
                  sa.properties.county_served
                    .toLowerCase()
                    .includes(countyName.toLowerCase())
              )
            : [];

          let regionalCenterInfo = "";
          if (regionalCentersInCounty.length > 0) {
            regionalCenterInfo = `
              <div style="margin-top: 12px; padding-top: 12px; border-top: 1px solid #bdc3c7;">
                <h6 style="
                  color: #27ae60;
                  margin: 0 0 10px 0;
                  font-size: 14px;
                  font-weight: 600;
                ">🏛️ Regional Centers (${regionalCentersInCounty.length})</h6>
                <div style="display: flex; flex-direction: column; gap: 8px;">
                  ${regionalCentersInCounty
                    .slice(0, 3)
                    .map(
                      (rc) => `
                    <div style="
                      padding: 8px;
                      background: #f8f9fa;
                      border-radius: 6px;
                      border-left: 3px solid #27ae60;
                    ">
                      <div style="font-weight: 600; font-size: 13px; color: #2c3e50; margin-bottom: 4px;">
                        ${rc.properties.regional_center || "Regional Center"}
                      </div>
                      <div style="font-size: 12px; color: #555; display: flex; flex-direction: column; gap: 2px;">
                        <span>📞 ${rc.properties.telephone || "Contact for info"}</span>
                        <span>🏢 ${rc.properties.office_type || "Main Office"}</span>
                      </div>
                    </div>
                  `
                    )
                    .join("")}
                </div>
                ${
                  regionalCentersInCounty.length > 3
                    ? `<div style="margin-top: 8px; font-size: 12px; color: #7f8c8d; font-style: italic;">
                        + ${regionalCentersInCounty.length - 3} more centers
                       </div>`
                    : ""
                }
              </div>
            `;
          } else {
            regionalCenterInfo = `
               <div style="margin-top: 12px; padding-top: 12px; border-top: 1px solid #bdc3c7;">
                 <div style="
                   padding: 12px;
                   background: #fff3cd;
                   border-radius: 6px;
                   border-left: 3px solid #ffc107;
                   font-size: 13px;
                   color: #856404;
                 ">
                   <div style="font-weight: 600; margin-bottom: 4px;">
                     ⚠️ No regional centers found for this county
                   </div>
                   <div style="font-size: 12px;">
                     Residents may need to access services in neighboring counties.
                   </div>
                 </div>
               </div>
             `;
          }

          const popup = new mapboxgl.Popup({
            maxWidth: "90vw",
            closeOnClick: true,
            closeButton: true,
          })
            .setLngLat(e.lngLat)
            .setHTML(
              `
              <div style="
                width: 320px;
                max-width: 90vw;
                padding: 12px;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                line-height: 1.4;
                word-wrap: break-word;
                overflow-wrap: break-word;
              ">
                <h5 style="
                  color: #2c3e50;
                  margin: 0 0 12px 0;
                  padding-bottom: 8px;
                  border-bottom: 2px solid #3498db;
                  font-size: 17px;
                  font-weight: 600;
                  word-wrap: break-word;
                ">
                  📍 ${countyName} County
                </h5>
                <div style="display: flex; flex-direction: column; gap: 8px; margin-bottom: 10px;">
                  <div style="font-size: 13px;"><strong>🏛️ State:</strong> <span style="color: #555;">California</span></div>
                  <div style="font-size: 13px;"><strong>📊 Regional Centers:</strong> <span style="color: #555;">${regionalCentersInCounty.length} centers</span></div>
                  <div style="font-size: 13px;"><strong>📐 Area:</strong> <span style="color: #555;">Real county boundaries</span></div>
                </div>
                ${regionalCenterInfo}
              </div>
            `
            )
            .addTo(this.map);
        });

        // Hover effects for California counties
        let hoveredCountyId = null;

        this.map.on("mouseenter", "california-counties-fill", (e) => {
          this.map.getCanvas().style.cursor = "pointer";

          if (e.features.length > 0) {
            if (hoveredCountyId !== null) {
              this.map.setFeatureState(
                {
                  source: "counties",
                  id: hoveredCountyId,
                },
                { hover: false }
              );
            }
            hoveredCountyId = e.features[0].id;
            this.map.setFeatureState(
              {
                source: "counties",
                id: hoveredCountyId,
              },
              { hover: true }
            );
          }
        });

        this.map.on("mouseleave", "california-counties-fill", () => {
          this.map.getCanvas().style.cursor = "";

          if (hoveredCountyId !== null) {
            this.map.setFeatureState(
              {
                source: "counties",
                id: hoveredCountyId,
              },
              { hover: false }
            );
          }
          hoveredCountyId = null;
        });

        console.log("Service areas successfully added to map");
        console.log(
          "Map layers now:",
          this.map.getStyle().layers.map((l) => l.id)
        );
        console.log("Map sources now:", Object.keys(this.map.getStyle().sources));
      } catch (error) {
        console.error("Error adding service areas to map:", error);
      }
    },

    // Remove service areas from map
    removeServiceAreasFromMap() {
      console.log("removeServiceAreasFromMap called");

      if (!this.map) {
        console.log("Map not available for removal");
        return;
      }

      try {
        // Remove event listeners first
        this.map.off("click", "california-counties-fill");
        this.map.off("mouseenter", "california-counties-fill");
        this.map.off("mouseleave", "california-counties-fill");

        // Remove California county layers
        if (this.map.getLayer("california-counties-fill")) {
          this.map.removeLayer("california-counties-fill");
          console.log("Removed california-counties-fill layer");
        }
        if (this.map.getLayer("california-counties-outline")) {
          this.map.removeLayer("california-counties-outline");
          console.log("Removed california-counties-outline layer");
        }

        // Remove sources
        if (this.map.getSource("counties")) {
          this.map.removeSource("counties");
          console.log("Removed counties source");
        }
        if (this.map.getSource("service-areas")) {
          this.map.removeSource("service-areas");
          console.log("Removed service-areas source");
        }

        console.log("Service areas successfully removed from map");
      } catch (error) {
        console.error("Error removing service areas from map:", error);
      }
    },

    // Toggle pin service areas (make them persistent)
    togglePinServiceAreas() {
      console.log("togglePinServiceAreas called, pinServiceAreas:", this.pinServiceAreas);

      if (this.pinServiceAreas) {
        // When pinning, ensure service areas are always visible
        if (!this.showServiceAreas) {
          this.showServiceAreas = true;
          this.toggleServiceAreas();
        }

        // Auto-load on map changes if pinned
        this.enableAutoLoad();
      } else {
        // When unpinning, disable auto-load
        this.disableAutoLoad();
      }
    },

    // Enable auto-loading of service areas when pinned
    enableAutoLoad() {
      if (this.map && this.pinServiceAreas) {
        // Ensure service areas are always visible when map loads
        this.map.on("style.load", this.autoAddServiceAreas);
        this.map.on("sourcedata", this.autoAddServiceAreas);
      }
    },

    // Disable auto-loading when unpinned
    disableAutoLoad() {
      if (this.map) {
        this.map.off("style.load", this.autoAddServiceAreas);
        this.map.off("sourcedata", this.autoAddServiceAreas);
      }
    },

    // Auto-add service areas when map reloads (for pinned mode)
    autoAddServiceAreas() {
      if (this.pinServiceAreas && this.serviceAreasLoaded && this.serviceAreas) {
        // Small delay to ensure map is ready
        setTimeout(() => {
          if (!this.map.getSource("service-areas")) {
            this.addServiceAreasToMap();
          }
        }, 100);
      }
    },

    // Update map markers
    updateMarkers() {
      // Prevent recursive calls
      if (this._updatingMarkers) {
        console.log("⚠️ updateMarkers already in progress, skipping");
        return;
      }
      this._updatingMarkers = true;

      console.log("🚀 updateMarkers() called");
      console.log(`Current display type: ${this.displayType}`);
      console.log(`providers.length: ${this.providers.length}`);
      console.log(`filteredProviders.length: ${this.filteredProviders.length}`);

      // Check map state
      if (!this.map) {
        console.error("❌ Map not initialized, skipping marker update");
        this._updatingMarkers = false;
        return;
      }

      console.log("🗺️ Map state:", {
        loaded: this.map.loaded(),
        style_loaded: this.map.isStyleLoaded && this.map.isStyleLoaded(),
        zoom: this.map.getZoom(),
        center: this.map.getCenter(),
      });

      // Clear existing markers more aggressively
      console.log(`🧹 Clearing ${this.markers.length} existing markers`);
      this.markers.forEach((marker, index) => {
        console.log(`🗑️ Removing marker ${index + 1}`);
        try {
          marker.remove();
        } catch (e) {
          console.warn(`Error removing marker ${index + 1}:`, e);
        }
      });
      this.markers = [];

      // More aggressive DOM cleanup
      const existingMarkers = document.querySelectorAll(".mapboxgl-marker");
      console.log(`🧽 Found ${existingMarkers.length} DOM markers before cleanup`);
      existingMarkers.forEach((marker, index) => {
        console.log(`🧽 Removing DOM marker ${index + 1}`);
        marker.remove();
      });

      // Double check cleanup worked
      const remainingMarkers = document.querySelectorAll(".mapboxgl-marker");
      if (remainingMarkers.length > 0) {
        console.warn(
          `⚠️ Still ${remainingMarkers.length} markers remaining after cleanup!`
        );
      } else {
        console.log(`✅ All markers successfully cleaned up`);
      }

      // When service areas are enabled, don't show regional center markers
      // since counties provide this information, BUT always show provider markers
      if (this.showServiceAreas && this.displayType === "regionalCenters") {
        console.log(
          "Service areas enabled - hiding regional center markers (data integrated in counties)"
        );
        return;
      }

      // Get the appropriate data based on display type
      let items = [];
      if (this.displayType === "providers") {
        items = this.filteredProviders;
        console.log(
          `Showing ${items.length} provider markers (always visible regardless of service areas)`
        );
        console.log(
          "First few providers:",
          items
            .slice(0, 2)
            .map((p) => ({ name: p.name, lat: p.latitude, lng: p.longitude }))
        );
      } else if (this.displayType === "regionalCenters") {
        items = this.filteredRegionalCenters;
        console.log(`Showing ${items.length} regional center markers`);
      } else {
        items = this.filteredLocations;
        console.log(`Showing ${items.length} location markers`);
      }

      console.log(
        `Display type: ${this.displayType}, Service areas enabled: ${this.showServiceAreas}`
      );

      // Ensure map is completely stable before creating ANY markers
      const mapLoaded = this.map.loaded();
      const styleLoaded = this.map.isStyleLoaded ? this.map.isStyleLoaded() : false;

      if (!mapLoaded || !styleLoaded) {
        console.warn(
          `🚨 Map not ready for marker creation (loaded: ${mapLoaded}, style: ${styleLoaded}). Retrying in 100ms...`
        );
        // Release the lock BEFORE the recursive call
        this._updatingMarkers = false;
        setTimeout(() => {
          this.updateMarkers();
        }, 100);
        return;
      }

      console.log("🗺️ Map confirmed ready for marker creation");

      // NEW APPROACH: Add markers sequentially with delays to prevent coordinate system conflicts
      console.log("🎯 Using sequential marker creation to avoid coordinate conflicts");

      const addMarkersSequentially = async () => {
        let markersCreated = 0;
        let markersSkipped = 0;

        for (let index = 0; index < items.length; index++) {
          const item = items[index];

          // Add a small delay between each marker
          if (index > 0) {
            await new Promise((resolve) => setTimeout(resolve, 50));
          }

          const result = await this.createSingleMarker(item, index, items.length);
          if (result.created) {
            markersCreated++;
          } else {
            markersSkipped++;
          }
        }

        console.log(`📊 Sequential Marker Creation Summary:`);
        console.log(`   - Total items processed: ${items.length}`);
        console.log(`   - Markers created: ${markersCreated}`);
        console.log(`   - Markers skipped: ${markersSkipped}`);
        console.log(
          `   - Success rate: ${((markersCreated / items.length) * 100).toFixed(1)}%`
        );

        // Fit map to markers if we have any
        if (this.markers.length > 0) {
          setTimeout(() => {
            const bounds = new mapboxgl.LngLatBounds();
            let boundsCount = 0;

            items.forEach((item) => {
              const lat = parseFloat(item.latitude);
              const lng = parseFloat(item.longitude);
              if (!isNaN(lat) && !isNaN(lng) && lat && lng) {
                bounds.extend([lng, lat]);
                boundsCount++;
              }
            });

            if (boundsCount > 0) {
              try {
                this.map.fitBounds(bounds, {
                  padding: 100,
                  maxZoom: 15,
                });
              } catch (error) {
                console.error("Error fitting bounds:", error);
              }
            }
          }, 100);
        }

        // Release the lock
        this._updatingMarkers = false;
      };

      // Start sequential marker creation
      addMarkersSequentially().catch((error) => {
        console.error("Error in sequential marker creation:", error);
        this._updatingMarkers = false;
      });

      return; // Exit here since we're using the new sequential approach
    },

    // Create a single marker with enhanced error handling and timing
    async createSingleMarker(item, index, totalItems) {
      try {
        console.log(`🔄 Processing item ${index + 1}/${totalItems}: ${item.name}`);

        // Skip items marked as having invalid coordinates
        if (item._coordinatesInvalid) {
          console.log(`⏭️ Skipping ${item.name} due to invalid coordinates`);
          return { created: false, reason: "invalid_coordinates_flag" };
        }

        // Ensure coordinates are numbers and valid
        let lat = parseFloat(item.latitude);
        let lng = parseFloat(item.longitude);

        // Enhanced Debug logging
        console.log(`🔍 Item ${index + 1} coordinates debug for ${item.name}:`, {
          original_lat: item.latitude,
          original_lng: item.longitude,
          lat_type: typeof item.latitude,
          lng_type: typeof item.longitude,
          parsed_lat: lat,
          parsed_lng: lng,
          lat_isNaN: isNaN(lat),
          lng_isNaN: isNaN(lng),
          lat_is_zero: lat === 0,
          lng_is_zero: lng === 0,
          accepts_insurance: item.accepts_insurance,
        });

        // Check for invalid coordinates
        if (
          isNaN(lat) ||
          isNaN(lng) ||
          lat === 0 ||
          lng === 0 ||
          !lat ||
          !lng ||
          lat === null ||
          lng === null ||
          lat === undefined ||
          lng === undefined
        ) {
          console.warn(`⚠️ Invalid coordinates for ${item.name}, skipping marker:`, {
            lat,
            lng,
            original_lat: item.latitude,
            original_lng: item.longitude,
          });
          return { created: false, reason: "invalid_coordinates" };
        }

        // Validate coordinates are within reasonable bounds for CA
        if (lat < 32 || lat > 42 || lng > -114 || lng < -125) {
          console.warn(
            `⚠️ Coordinates out of CA bounds for ${item.name}: ${lat}, ${lng}`
          );
          return { created: false, reason: "out_of_bounds" };
        }

        console.log(`✅ Creating marker for ${item.name} at lat=${lat}, lng=${lng}`);

        // Double-check coordinates before creating marker
        const finalLng = Number(lng);
        const finalLat = Number(lat);

        if (isNaN(finalLng) || isNaN(finalLat)) {
          console.error(`🚨 Final coordinate check failed for ${item.name}:`, {
            finalLat,
            finalLng,
          });
          return { created: false, reason: "final_coordinate_check_failed" };
        }

        // Special check for [0,0] coordinates (top-left bug)
        if (finalLng === 0 || finalLat === 0) {
          console.error(
            `🚨🚨🚨 FOUND THE BUG! Provider "${item.name}" has coordinates [${finalLng}, ${finalLat}]`
          );
          return { created: false, reason: "zero_coordinates" };
        }

        // Enhanced map readiness check with timeout
        const mapLoaded = this.map.loaded();
        const styleLoaded = this.map.isStyleLoaded ? this.map.isStyleLoaded() : false;

        if (!mapLoaded || !styleLoaded) {
          console.warn(
            `⚠️ Map not ready for ${item.name} (loaded: ${mapLoaded}, style: ${styleLoaded})`
          );
          return { created: false, reason: "map_not_ready" };
        }

        // Test map projection BEFORE creating marker
        try {
          const testPoint = this.map.project([finalLng, finalLat]);
          if (!testPoint || isNaN(testPoint.x) || isNaN(testPoint.y)) {
            console.error(
              `🚨 Map projection failed for ${item.name} at [${finalLng}, ${finalLat}]`
            );
            return { created: false, reason: "projection_failed" };
          }
          console.log(
            `🗺️ Map projection test for ${item.name}: ${JSON.stringify(testPoint)}`
          );
        } catch (error) {
          console.error(`🚨 Map projection error for ${item.name}:`, error);
          return { created: false, reason: "projection_error" };
        }

        // Create marker element with CHLA branding
        const el = document.createElement("div");
        el.className = "marker chla-marker";
        el.style.width = "28px";
        el.style.height = "28px";
        el.style.borderRadius = "50%";

        // Use CHLA brand colors
        if (this.displayType === "providers") {
          if (item.accepts_insurance) {
            el.style.background = "linear-gradient(135deg, #4DAA50 0%, #5aba4b 100%)"; // Green for insurance
          } else {
            el.style.background = "linear-gradient(135deg, #0D9DDB 0%, #22b2e7 100%)"; // Light blue for providers
          }
        } else {
          el.style.background = "linear-gradient(135deg, #004877 0%, #0D9DDB 100%)"; // CHLA blue gradient for regional centers
        }

        el.style.border = "3px solid white";
        el.style.boxShadow = "0 4px 12px rgba(0, 72, 119, 0.3)";
        el.style.cursor = "pointer";
        el.style.zIndex = "1000";
        el.style.transition = "all 0.3s ease";
        el.setAttribute("data-provider", item.name);
        el.setAttribute("data-coordinates", `${finalLat},${finalLng}`);

        // Add hover effect
        el.addEventListener("mouseenter", () => {
          el.style.transform = "scale(1.2)";
          el.style.zIndex = "1001";
        });

        el.addEventListener("mouseleave", () => {
          el.style.transform = "scale(1)";
          el.style.zIndex = "1000";
        });

        // Create popup
        const popup = new mapboxgl.Popup({
          offset: 25,
          maxWidth: "90vw",
          closeOnClick: true,
          closeButton: true,
        }).setHTML(this.createPopupContent(item, finalLat, finalLng));

        // Create and add marker to map
        console.log(`🎯 CREATING MARKER: ${item.name} at [${finalLng}, ${finalLat}]`);
        const marker = new mapboxgl.Marker(el)
          .setLngLat([finalLng, finalLat])
          .setPopup(popup)
          .addTo(this.map);

        // Immediate position verification
        const markerLngLat = marker.getLngLat();
        console.log(
          `🔍 MARKER ACTUAL POSITION: ${item.name} at [${markerLngLat.lng}, ${markerLngLat.lat}]`
        );

        // Store marker
        this.markers.push(marker);

        // DOM position check after a small delay
        setTimeout(() => {
          const markerInDom = document.querySelector(`[data-provider="${item.name}"]`);
          if (markerInDom) {
            const rect = markerInDom.getBoundingClientRect();
            console.log(`📍 Marker "${item.name}" DOM position:`, {
              x: rect.x,
              y: rect.y,
              coordinates_used: `[${finalLng}, ${finalLat}]`,
              is_top_left_corner: rect.x < 100 && rect.y < 100,
            });

            if (rect.x < 100 && rect.y < 100) {
              console.error(
                `🚨 MARKER IN TOP-LEFT CORNER! ${item.name} at DOM position [${rect.x}, ${rect.y}]`
              );
            }
          }
        }, 50);

        console.log(
          `📌 Marker successfully created for ${item.name}. Total markers: ${this.markers.length}`
        );
        return { created: true, marker };
      } catch (error) {
        console.error(`🚨 Error creating marker for ${item.name}:`, error);
        return { created: false, reason: "exception", error };
      }
    },

    // Create popup content for markers
    createPopupContent(item, finalLat, finalLng) {
      const itemType = this.displayType === "providers" ? "Provider" : "Regional Center";
      const title = item.name || item.regional_center || `${itemType}`;

      if (this.displayType === "providers") {
        const fullAddress = [item.address, item.city, item.state, item.zip_code]
          .filter(Boolean)
          .join(", ");
        const mapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${finalLat},${finalLng}`;

        return `
          <div style="width: 400px; max-width: 90vw; padding: 20px; font-family: 'Futura Std', 'Arial', 'Calibri', sans-serif; line-height: 1.5; background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%); border-radius: 12px;">
            <div style="background: linear-gradient(135deg, #004877 0%, #0D9DDB 100%); margin: -20px -20px 16px -20px; padding: 16px 20px; border-radius: 12px 12px 0 0;">
              <h4 style="color: white; margin: 0; font-size: 18px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; text-shadow: 0 2px 4px rgba(0,0,0,0.2);">
                🏥 ${title}
              </h4>
            </div>
            <div style="margin-bottom: 16px; padding: 12px; background: ${
              item.accepts_insurance
                ? "linear-gradient(135deg, #4DAA50 0%, #5aba4b 100%)"
                : "linear-gradient(135deg, #FFC923 0%, #ffcc0a 100%)"
            }; border-radius: 8px; color: ${
          item.accepts_insurance ? "white" : "#4C280F"
        };">
              <div style="font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 8px;">
                ${
                  item.accepts_insurance
                    ? '<span style="font-size: 16px;">✓</span> Accepts Insurance'
                    : '<span style="font-size: 16px;">⚠</span> Insurance Status Unknown'
                }
              </div>
            </div>
            ${
              fullAddress
                ? `<div style="margin-bottom: 16px; padding: 12px; background: #f8f9fa; border-radius: 8px; border-left: 4px solid #004877;">
                     <strong style="color: #004877; display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                       📍 Address
                     </strong>
                     <span style="color: #495057;">${fullAddress}</span>
                   </div>`
                : ""
            }
            ${
              item.phone
                ? `<div style="margin-bottom: 16px; padding: 12px; background: #f8f9fa; border-radius: 8px; border-left: 4px solid #0D9DDB;">
                     <strong style="color: #004877; display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                       📱 Phone
                     </strong>
                     <a href="tel:${item.phone}" style="color: #0D9DDB; text-decoration: none; font-weight: 600;">${item.phone}</a>
                   </div>`
                : ""
            }
            <div style="display: flex; gap: 12px; margin-top: 20px; padding-top: 16px; border-top: 2px solid #FFC923;">
              <a href="${mapsUrl}" target="_blank" 
                 style="background: linear-gradient(135deg, #004877 0%, #0D9DDB 100%); 
                        color: white; 
                        padding: 12px 16px; 
                        border-radius: 8px; 
                        text-decoration: none; 
                        font-size: 13px; 
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        flex: 1;
                        text-align: center;
                        transition: all 0.3s ease;
                        box-shadow: 0 2px 8px rgba(0, 72, 119, 0.3);">
                🗺️ Get Directions
              </a>
              ${
                item.phone
                  ? `<a href="tel:${item.phone}" 
                       style="background: linear-gradient(135deg, #4DAA50 0%, #5aba4b 100%); 
                              color: white; 
                              padding: 12px 16px; 
                              border-radius: 8px; 
                              text-decoration: none; 
                              font-size: 13px; 
                              font-weight: 600;
                              text-transform: uppercase;
                              letter-spacing: 0.5px;
                              flex: 1;
                              text-align: center;
                              transition: all 0.3s ease;
                              box-shadow: 0 2px 8px rgba(77, 170, 80, 0.3);">
                        📞 Call Now
                     </a>`
                  : ""
              }
            </div>
          </div>
        `;
      } else {
        // Regional Centers popup with CHLA branding
        const fullAddress = [item.address, item.city, item.state, item.zip_code]
          .filter(Boolean)
          .join(", ");
        const mapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${finalLat},${finalLng}`;

        return `
          <div style="width: 400px; max-width: 90vw; padding: 20px; font-family: 'Futura Std', 'Arial', 'Calibri', sans-serif; line-height: 1.5; background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%); border-radius: 12px;">
            <div style="background: linear-gradient(135deg, #004877 0%, #0D9DDB 100%); margin: -20px -20px 16px -20px; padding: 16px 20px; border-radius: 12px 12px 0 0;">
              <h4 style="color: white; margin: 0; font-size: 18px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; text-shadow: 0 2px 4px rgba(0,0,0,0.2);">
                🏢 ${title}
              </h4>
            </div>
            ${
              fullAddress
                ? `<div style="margin-bottom: 16px; padding: 12px; background: #f8f9fa; border-radius: 8px; border-left: 4px solid #004877;">
                     <strong style="color: #004877; display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                       📍 Address
                     </strong>
                     <span style="color: #495057;">${fullAddress}</span>
                   </div>`
                : ""
            }
            ${
              item.phone || item.telephone
                ? `<div style="margin-bottom: 16px; padding: 12px; background: #f8f9fa; border-radius: 8px; border-left: 4px solid #0D9DDB;">
                     <strong style="color: #004877; display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                       📱 Phone
                     </strong>
                     <a href="tel:${
                       item.phone || item.telephone
                     }" style="color: #0D9DDB; text-decoration: none; font-weight: 600;">${
                    item.phone || item.telephone
                  }</a>
                   </div>`
                : ""
            }
            ${
              item.office_type
                ? `<div style="margin-bottom: 16px; padding: 12px; background: #f8f9fa; border-radius: 8px; border-left: 4px solid #FFC923;">
                     <strong style="color: #004877; display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                       🏢 Office Type
                     </strong>
                     <span style="color: #495057;">${item.office_type}</span>
                   </div>`
                : ""
            }
            <div style="display: flex; gap: 12px; margin-top: 20px; padding-top: 16px; border-top: 2px solid #FFC923;">
              <a href="${mapsUrl}" target="_blank" 
                 style="background: linear-gradient(135deg, #004877 0%, #0D9DDB 100%); 
                        color: white; 
                        padding: 12px 16px; 
                        border-radius: 8px; 
                        text-decoration: none; 
                        font-size: 13px; 
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        flex: 1;
                        text-align: center;
                        transition: all 0.3s ease;
                        box-shadow: 0 2px 8px rgba(0, 72, 119, 0.3);">
                🗺️ Get Directions
              </a>
              ${
                item.phone || item.telephone
                  ? `<a href="tel:${
                      item.phone || item.telephone
                    }" style="background: linear-gradient(135deg, #4DAA50 0%, #5aba4b 100%); 
                              color: white; 
                              padding: 12px 16px; 
                              border-radius: 8px; 
                              text-decoration: none; 
                              font-size: 13px; 
                              font-weight: 600;
                              text-transform: uppercase;
                              letter-spacing: 0.5px;
                              flex: 1;
                              text-align: center;
                              transition: all 0.3s ease;
                              box-shadow: 0 2px 8px rgba(77, 170, 80, 0.3);">
                        📞 Call Now
                     </a>`
                  : ""
              }
            </div>
          </div>
        `;
      }
    },

    // Center map on location when clicked in list
    centerMapOnLocation(location) {
      const lat = parseFloat(location.latitude);
      const lng = parseFloat(location.longitude);
      if (!isNaN(lat) && !isNaN(lng) && lat && lng) {
        this.map.flyTo({
          center: [lng, lat],
          zoom: 14,
        });
      }
    },
  },
};
</script>

<style>
/* CHLA Typography */
* {
  font-family: "Futura Std", "Arial", "Calibri", sans-serif !important;
}

.map-app {
  display: flex;
  height: 100vh;
  width: 100%;
  position: relative;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
}

.sidebar-container {
  flex: 0 0 380px;
  box-shadow: 0 4px 20px rgba(0, 72, 119, 0.15);
  z-index: 5;
  background: #ffffff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-radius: 0 12px 12px 0;
  border-right: 3px solid #004877;
}

.sidebar {
  padding: 0;
  flex: 1;
  overflow-y: auto;
}

/* CHLA Header Styling */
.chla-header {
  background: linear-gradient(135deg, #004877 0%, #0d9ddb 100%);
  color: white;
  padding: 20px;
  margin: 0;
  position: relative;
  overflow: hidden;
}

.chla-header::before {
  content: "";
  position: absolute;
  top: -50%;
  right: -20px;
  width: 100px;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(255, 203, 35, 0.1));
  transform: rotate(15deg);
}

.chla-logo-container {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.chla-logo {
  height: 60px;
  max-width: 100%;
  filter: brightness(0) invert(1);
}

.chla-mission {
  text-align: center;
  position: relative;
  z-index: 2;
}

.chla-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 8px 0;
  text-transform: uppercase;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.chla-tagline {
  font-size: 14px;
  margin: 0;
  opacity: 0.9;
  font-style: italic;
  font-weight: 300;
}

.chla-info-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.chla-info-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
  color: white;
}

/* Content Padding */
.sidebar > div:not(.chla-header) {
  padding: 0 20px;
}

.sidebar > div:first-of-type:not(.chla-header) {
  padding-top: 20px;
}

/* CHLA Button Styling */
.chla-btn-group {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 72, 119, 0.15);
}

.chla-btn {
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border: none !important;
  transition: all 0.3s ease;
  border-radius: 0 !important;
}

.btn-chla-primary {
  background: linear-gradient(135deg, #004877 0%, #0d9ddb 100%) !important;
  color: white !important;
  box-shadow: inset 0 2px 4px rgba(255, 255, 255, 0.2);
}

.btn-chla-primary:hover {
  background: linear-gradient(135deg, #003866 0%, #0a8ac4 100%) !important;
  transform: translateY(-1px);
}

.btn-chla-outline {
  background: white !important;
  color: #004877 !important;
  border: 2px solid #004877 !important;
}

.btn-chla-outline:hover {
  background: #004877 !important;
  color: white !important;
  transform: translateY(-1px);
}

/* CHLA Alert Styling */
.alert {
  border-radius: 8px;
  border: none;
  font-weight: 500;
}

.alert-success {
  background: linear-gradient(135deg, #4daa50 0%, #5aba4b 100%);
  color: white;
}

.alert-warning {
  background: linear-gradient(135deg, #ffc923 0%, #ffcc0a 100%);
  color: #4c280f;
}

.alert-info {
  background: linear-gradient(135deg, #0d9ddb 0%, #22b2e7 100%);
  color: white;
}

/* Form Controls */
.form-control {
  border-radius: 8px;
  border: 2px solid #e9ecef;
  transition: all 0.3s ease;
}

.form-control:focus {
  border-color: #004877;
  box-shadow: 0 0 0 0.2rem rgba(0, 72, 119, 0.25);
}

.form-check-input:checked {
  background-color: #004877;
  border-color: #004877;
}

.form-range::-webkit-slider-thumb {
  background: #004877;
}

.form-range::-moz-range-thumb {
  background: #004877;
}

/* Badges */
.badge {
  border-radius: 6px;
  font-weight: 600;
}

.bg-info {
  background: #0d9ddb !important;
}

.bg-primary {
  background: #004877 !important;
}

/* Reset Button */
.btn-secondary {
  background: linear-gradient(135deg, #6c757d 0%, #5a6268 100%);
  border: none;
  border-radius: 8px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn-secondary:hover {
  background: linear-gradient(135deg, #5a6268 0%, #495057 100%);
  transform: translateY(-1px);
}

/* Section Headers */
h5,
h6 {
  color: #004877;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.results-title {
  color: #004877;
  border-bottom: 2px solid #ffc923;
  padding-bottom: 8px;
  margin-bottom: 16px;
}

.map-container-wrapper {
  flex: 1;
  position: relative;
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: 12px 0 0 12px;
  overflow: hidden;
}

/* Service areas toggle styling */
.form-check-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #004877;
  font-weight: 500;
}

/* Map marker pulse animation */
@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.pulse-marker {
  animation: pulse 2s infinite;
}

/* Fix popup z-index to appear above markers with stronger specificity */
:global(.mapboxgl-popup) {
  z-index: 999999 !important;
  position: fixed !important;
}

:global(.mapboxgl-popup-content) {
  z-index: 999999 !important;
  position: relative !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3) !important;
  border: 2px solid #3498db !important;
}

:global(.mapboxgl-popup-tip) {
  z-index: 999998 !important;
  border-top-color: #3498db !important;
}

:global(.mapboxgl-popup-close-button) {
  z-index: 999999 !important;
  background: #e74c3c !important;
  color: white !important;
  border-radius: 50% !important;
  width: 25px !important;
  height: 25px !important;
  line-height: 23px !important;
  font-size: 14px !important;
  font-weight: bold !important;
}

/* Ensure markers have much lower z-index than popups */
:global(.mapboxgl-marker) {
  z-index: 100 !important;
}

/* Additional popup styling to ensure visibility */
:global(.mapboxgl-popup-anchor-top .mapboxgl-popup-tip),
:global(.mapboxgl-popup-anchor-top-left .mapboxgl-popup-tip),
:global(.mapboxgl-popup-anchor-top-right .mapboxgl-popup-tip) {
  border-bottom-color: #3498db !important;
}

:global(.mapboxgl-popup-anchor-bottom .mapboxgl-popup-tip),
:global(.mapboxgl-popup-anchor-bottom-left .mapboxgl-popup-tip),
:global(.mapboxgl-popup-anchor-bottom-right .mapboxgl-popup-tip) {
  border-top-color: #3498db !important;
}
</style>
