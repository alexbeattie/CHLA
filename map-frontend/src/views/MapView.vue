<template>
  <div class="map-app">
    <!-- Funding Info Modal -->
    <funding-info-panel
      :showModal="showFundingInfo"
      @close="toggleFundingInfo"
    />

    <!-- Sidebar (always on left) -->
    <div class="sidebar-container">
      <div class="sidebar">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h3 class="mb-0">Location Finder</h3>
          <button
            class="btn btn-sm btn-outline-primary funding-info-btn"
            @click="toggleFundingInfo"
            title="Funding Information"
          >
            <!-- <i class="bi bi-info-circle"></i> Funding Info -->
          </button>
        </div>

        <!-- Display Type Selector -->
        <div class="mb-3">
          <div class="btn-group w-100 d-flex">
            <button
              class="btn flex-grow-1"
              :class="{
                'btn-primary': displayType === 'regionalCenters',
                'btn-outline-primary': displayType !== 'regionalCenters',
              }"
              @click="setDisplayType('regionalCenters')"
            >
              Centers
            </button>
            <button
              class="btn flex-grow-1"
              :class="{
                'btn-primary': displayType === 'providers',
                'btn-outline-primary': displayType !== 'providers',
              }"
              @click="setDisplayType('providers')"
            >
              Providers
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

          <!-- Service Areas Info -->
          <small
            v-if="showServiceAreas && serviceAreasLoaded"
            class="text-success d-block mt-2"
          >
            <i class="bi bi-check-circle-fill"></i>
            {{ serviceAreas?.features?.length || 0 }} county-based service areas
            loaded
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
            <button
              class="btn btn-sm btn-outline-primary"
              @click="toggleUserPanel"
            >
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
          <div
            class="mb-2"
            v-if="userLocation.latitude && userLocation.longitude"
          >
            <div class="d-flex justify-content-between align-items-center">
              <label class="form-label mb-0"
                >Distance Radius: <strong>{{ radius }} miles</strong></label
              >
              <span class="badge bg-info"
                >{{ countLocationsInRadius }} found</span
              >
            </div>
            <input
              type="range"
              v-model.number="radius"
              class="form-range"
              min="1"
              max="50"
              step="1"
              @change="updateFilteredLocations"
            />
            <div class="d-flex justify-content-between">
              <small>1 mile</small>
              <small>50 miles</small>
            </div>
          </div>

          <!-- Filter Options for Providers -->
          <div v-if="displayType === 'providers'" class="mb-3">
            <div class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                v-model="filterOptions.acceptsInsurance"
                id="acceptsInsurance"
                @change="updateFilteredLocations"
              />
              <label class="form-check-label" for="acceptsInsurance">
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
                Accepts Regional Center
              </label>
            </div>
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
                Matches My Diagnosis
              </label>
            </div>
          </div>

          <!-- Reset Button -->
          <button
            @click="resetFilters"
            class="btn btn-secondary btn-sm w-100 mt-2"
          >
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
            :selected-location="selectedLocation"
            @select="selectLocation"
          />
        </div>
      </div>
    </div>

    <!-- Map Container -->
    <div class="map-container-wrapper">
      <div id="map" class="map-container"></div>

      <!-- Selected Location Details -->
      <location-detail
        v-if="selectedLocation"
        :location="selectedLocation"
        @close="closeLocationDetails"
      />
    </div>
  </div>
</template>

<script>
import axios from "axios";
import mapboxgl from "mapbox-gl";
import UserInfoPanel from "@/components/UserInfoPanel.vue";
import LocationList from "@/components/LocationList.vue";
import LocationDetail from "@/components/LocationDetail.vue";
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
    LocationDetail,
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
        matchesDiagnosis: false,
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
      selectedLocation: null,

      // Categories and filters
      categories: [],
      category: null, // Add this property explicitly to avoid warnings
      selectedCategory: "",
      searchText: "",

      // User location
      userLocation: {
        latitude: 34.0522,
        longitude: -118.2437,
        accuracy: null,
      },
      radius: 5, // miles

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

    // Fetch providers immediately to ensure we have data
    setTimeout(() => {
      this.fetchProviders();
    }, 100);
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
      console.log(
        "Updating filtered locations with filters:",
        this.filterOptions
      );

      // If any provider-specific filters are enabled, automatically switch to providers
      const hasProviderFilters =
        this.filterOptions.acceptsInsurance ||
        this.filterOptions.acceptsRegionalCenter ||
        this.filterOptions.matchesDiagnosis;

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
      } else if (
        type === "regionalCenters" &&
        this.regionalCenters.length === 0
      ) {
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
      this.radius = 5;

      // Reset filter options
      this.filterOptions = {
        acceptsInsurance: false,
        acceptsRegionalCenter: false,
        matchesDiagnosis: false,
      };

      // Re-fetch data with reset filters (maintain current display type)
      console.log(
        `Resetting filters, maintaining display type: ${this.displayType}`
      );
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

    // Map initialization
    initMap() {
      // Set Mapbox access token
      mapboxgl.accessToken =
        import.meta.env.VITE_MAPBOX_TOKEN ||
        "pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg";

      // Create Mapbox map
      this.map = new mapboxgl.Map({
        container: "map",
        style: "mapbox://styles/mapbox/streets-v12",
        center: [this.userLocation.longitude, this.userLocation.latitude], // Los Angeles by default
        zoom: 10,
      });

      // Add navigation controls
      this.map.addControl(new mapboxgl.NavigationControl(), "top-right");

      // When map loads, update markers
      this.map.on("load", () => {
        console.log("Map loaded successfully");
        this.updateMarkers();

        // If service areas are already enabled, add them now
        if (this.showServiceAreas && this.serviceAreasLoaded) {
          console.log(
            "Map loaded and service areas are enabled, adding them now"
          );
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
              diagnoses_served:
                "Autism, Developmental Delay, Learning Disability",
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

          // Add search text if available
          if (this.searchText && this.searchText.trim() !== "") {
            queryParams.append("q", this.searchText.trim());
          }

          // Add user location if available
          if (this.userLocation.latitude && this.userLocation.longitude) {
            queryParams.append("lat", this.userLocation.latitude);
            queryParams.append("lng", this.userLocation.longitude);
            queryParams.append("radius", this.radius);
          }

          // Add user profile data if available
          if (this.userData.age) {
            queryParams.append("age", this.userData.age);
          }

          if (this.userData.diagnosis) {
            queryParams.append("diagnosis", this.userData.diagnosis);
          }

          // Add filter options directly as query parameters
          if (this.filterOptions.acceptsInsurance) {
            queryParams.append("accepts_insurance", "true");
          }

          if (this.filterOptions.acceptsRegionalCenter) {
            queryParams.append("accepts_regional_center", "true");
          }

          // Use the basic providers endpoint which supports our filter parameters
          const url = `${apiBaseUrl}/providers/?${queryParams.toString()}`;
          console.log(`Fetching providers from API: ${url}`);
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
          this.providers.forEach((provider, index) => {
            // Convert string coordinates to numbers
            if (provider.latitude && provider.longitude) {
              provider.latitude = parseFloat(provider.latitude);
              provider.longitude = parseFloat(provider.longitude);
            }

            if (
              !provider.latitude ||
              !provider.longitude ||
              isNaN(provider.latitude) ||
              isNaN(provider.longitude)
            ) {
              console.warn(
                `Provider ${provider.name} (ID: ${provider.id}) has invalid coordinates:`,
                {
                  latitude: provider.latitude,
                  longitude: provider.longitude,
                }
              );
            } else {
              console.log(
                `Provider ${provider.name}: lat=${provider.latitude}, lng=${provider.longitude}`
              );
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

        // Force marker update after data change
        console.log(
          `🎯 About to update markers with ${this.providers.length} providers`
        );
        this.$nextTick(() => {
          this.updateMarkers();
          console.log(
            `🗺️ Markers updated for ${this.providers.length} providers`
          );
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
        console.log(
          "Service areas API Response data type:",
          typeof response.data
        );

        if (response.data && response.data.type === "FeatureCollection") {
          this.serviceAreas = response.data;
          this.serviceAreasLoaded = true;
          console.log(
            `Successfully loaded ${response.data.features.length} service areas`
          );
        } else {
          console.error(
            "Unexpected service areas API response format:",
            response.data
          );
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
      console.log(
        "toggleServiceAreas called, showServiceAreas:",
        this.showServiceAreas
      );

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
          console.log(
            "After fetch - serviceAreasLoaded:",
            this.serviceAreasLoaded
          );
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
          this.serviceAreas
            ? this.serviceAreas.features
            : "serviceAreas is null"
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
            county.properties.regional_center_count =
              regionalCentersInCounty.length;
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
                        <span>📞 ${
                          rc.properties.telephone || "Contact for info"
                        }</span>
                        <span>🏢 ${
                          rc.properties.office_type || "Main Office"
                        }</span>
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
        console.log(
          "Map sources now:",
          Object.keys(this.map.getStyle().sources)
        );
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
      console.log(
        "togglePinServiceAreas called, pinServiceAreas:",
        this.pinServiceAreas
      );

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
      if (
        this.pinServiceAreas &&
        this.serviceAreasLoaded &&
        this.serviceAreas
      ) {
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
      // Clear existing markers
      this.markers.forEach((marker) => {
        marker.remove();
      });
      this.markers = [];

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

      // Add markers for each item
      items.forEach((item) => {
        // Ensure coordinates are numbers and valid
        const lat = parseFloat(item.latitude);
        const lng = parseFloat(item.longitude);

        if (isNaN(lat) || isNaN(lng) || !lat || !lng) {
          console.warn(
            `Skipping marker for ${item.name} due to invalid coordinates:`,
            {
              original_lat: item.latitude,
              original_lng: item.longitude,
              parsed_lat: lat,
              parsed_lng: lng,
            }
          );
          return;
        }

        console.log(
          `Creating marker for ${item.name} at lat=${lat}, lng=${lng}`
        );

        // Create marker element
        const el = document.createElement("div");
        el.className = "marker";
        el.style.width = "20px";
        el.style.height = "20px";
        el.style.borderRadius = "50%";
        el.style.backgroundColor =
          this.displayType === "providers" ? "#007bff" : "#28a745";
        el.style.border = "2px solid white";
        el.style.boxShadow = "0 0 5px rgba(0,0,0,0.3)";

        // Add pulse animation if the item has special attributes
        const isSpecial =
          this.displayType === "providers" &&
          ((this.filterOptions.matchesDiagnosis &&
            item.diagnoses_served &&
            item.diagnoses_served
              .toLowerCase()
              .includes(this.userData.diagnosis.toLowerCase())) ||
            (this.filterOptions.acceptsInsurance && item.accepts_insurance) ||
            (this.filterOptions.acceptsRegionalCenter &&
              item.accepts_regional_center));

        if (isSpecial) {
          el.classList.add("pulse-marker");
        }

        // Create popup content with better information
        const itemType =
          this.displayType === "providers" ? "Provider" : "Regional Center";
        const title = item.name || item.regional_center || `${itemType}`;

        let popupContent = "";

        if (this.displayType === "providers") {
          // Enhanced provider popup with proper sizing
          popupContent = `
            <div style="
              width: 300px; 
              max-width: 85vw; 
              padding: 12px; 
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              line-height: 1.4;
              word-wrap: break-word;
              overflow-wrap: break-word;
            ">
              <h5 style="
                color: #2c3e50; 
                margin: 0 0 10px 0; 
                padding-bottom: 8px;
                border-bottom: 2px solid #3498db;
                font-size: 16px;
                font-weight: 600;
                word-wrap: break-word;
              ">
                🏥 ${title}
              </h5>
              <div style="display: flex; flex-direction: column; gap: 8px;">
                ${
                  item.address
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">📍 Address:</strong><br/>
                        <span style="color: #555; margin-left: 16px; display: block;">${item.address}</span>
                       </div>`
                    : ""
                }
                ${
                  item.phone
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">📞 Phone:</strong> 
                        <a href="tel:${item.phone}" style="color: #3498db; text-decoration: none;">${item.phone}</a>
                       </div>`
                    : ""
                }
                ${
                  item.diagnoses_served
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">🎯 Diagnoses:</strong><br/>
                        <span style="color: #555; margin-left: 16px; display: block;">${item.diagnoses_served}</span>
                       </div>`
                    : ""
                }
                ${
                  item.age_groups_served
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">👥 Age Groups:</strong> 
                        <span style="color: #555;">${item.age_groups_served}</span>
                       </div>`
                    : ""
                }
                ${
                  item.coverage_areas
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">🗺️ Coverage:</strong><br/>
                        <span style="color: #555; margin-left: 16px; display: block;">${item.coverage_areas}</span>
                       </div>`
                    : ""
                }
                <div style="
                  margin-top: 12px; 
                  padding-top: 10px; 
                  border-top: 1px solid #ecf0f1;
                  display: flex;
                  flex-direction: column;
                  gap: 6px;
                ">
                  <span style="
                    font-size: 12px; 
                    font-weight: 500;
                    ${
                      item.accepts_insurance
                        ? "color: #27ae60;"
                        : "color: #e74c3c;"
                    }
                  ">
                    ${
                      item.accepts_insurance
                        ? "✓ Accepts Insurance"
                        : "✗ No Insurance"
                    }
                  </span>
                  <span style="
                    font-size: 12px; 
                    font-weight: 500;
                    ${
                      item.accepts_regional_center
                        ? "color: #27ae60;"
                        : "color: #e74c3c;"
                    }
                  ">
                    ${
                      item.accepts_regional_center
                        ? "✓ Accepts Regional Center"
                        : "✗ No Regional Center"
                    }
                  </span>
                </div>
              </div>
            </div>
          `;
        } else {
          // Regional center popup (when service areas not enabled) with proper sizing
          popupContent = `
            <div style="
              width: 280px; 
              max-width: 85vw; 
              padding: 12px; 
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              line-height: 1.4;
              word-wrap: break-word;
              overflow-wrap: break-word;
            ">
              <h5 style="
                color: #2c3e50; 
                margin: 0 0 10px 0; 
                padding-bottom: 8px;
                border-bottom: 2px solid #27ae60;
                font-size: 16px;
                font-weight: 600;
                word-wrap: break-word;
              ">
                🏛️ ${title}
              </h5>
              <div style="display: flex; flex-direction: column; gap: 8px;">
                ${
                  item.address
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">📍 Address:</strong><br/>
                        <span style="color: #555; margin-left: 16px; display: block;">${item.address}</span>
                       </div>`
                    : ""
                }
                ${
                  item.phone || item.telephone
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">📞 Phone:</strong> 
                        <a href="tel:${
                          item.phone || item.telephone
                        }" style="color: #27ae60; text-decoration: none;">${
                        item.phone || item.telephone
                      }</a>
                       </div>`
                    : ""
                }
                ${
                  item.county_served
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">🗺️ County:</strong> 
                        <span style="color: #555;">${item.county_served}</span>
                       </div>`
                    : ""
                }
                ${
                  item.office_type
                    ? `<div style="font-size: 13px;">
                        <strong style="color: #2c3e50;">🏢 Office Type:</strong> 
                        <span style="color: #555;">${item.office_type}</span>
                       </div>`
                    : ""
                }
              </div>
            </div>
          `;
        }

        // Create popup with better sizing options
        const popup = new mapboxgl.Popup({
          offset: 25,
          maxWidth: "90vw",
          closeOnClick: true,
          closeButton: true,
        }).setHTML(popupContent);

        // Create and add marker to map
        const marker = new mapboxgl.Marker(el)
          .setLngLat([lng, lat])
          .setPopup(popup)
          .addTo(this.map);

        // Add click handler
        marker.getElement().addEventListener("click", () => {
          this.selectLocation(item);
        });

        // Store marker
        this.markers.push(marker);
      });

      // Fit map to markers if we have any
      if (this.markers.length > 0) {
        const bounds = new mapboxgl.LngLatBounds();

        items.forEach((item) => {
          const lat = parseFloat(item.latitude);
          const lng = parseFloat(item.longitude);
          if (!isNaN(lat) && !isNaN(lng) && lat && lng) {
            bounds.extend([lng, lat]);
          }
        });

        this.map.fitBounds(bounds, { padding: 50 });
      }
    },

    // Select a location
    selectLocation(location) {
      this.selectedLocation = location;

      // Center map on selected location
      const lat = parseFloat(location.latitude);
      const lng = parseFloat(location.longitude);
      if (!isNaN(lat) && !isNaN(lng) && lat && lng) {
        this.map.flyTo({
          center: [lng, lat],
          zoom: 14,
        });
      }
    },

    // Close location details panel
    closeLocationDetails() {
      this.selectedLocation = null;
    },
  },
};
</script>

<style>
.map-app {
  display: flex;
  height: 100vh;
  width: 100%;
  position: relative;
}

.sidebar-container {
  flex: 0 0 350px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  z-index: 5;
  background: white;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
}

.map-container-wrapper {
  flex: 1;
  position: relative;
}

.map-container {
  width: 100%;
  height: 100%;
}

/* Service areas toggle styling */
.form-check-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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
</style>
