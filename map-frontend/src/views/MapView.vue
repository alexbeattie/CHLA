<template>
  <div class="map-app">
    <!-- Funding Info Modal -->
    <funding-info-panel :showModal="showFundingInfo" @close="toggleFundingInfo" />

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
            <i class="bi bi-info-circle"></i> Funding Info
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

    // Filtered providers with filter criteria applied
    filteredProviders() {
      if (!this.providers.length) return [];

      // Start with all providers
      let filtered = [...this.providers];

      // Apply text search filter
      if (this.searchText && this.searchText.trim() !== "") {
        const searchLower = this.searchText.toLowerCase().trim();

        filtered = filtered.filter((provider) => {
          // Check name
          if (provider.name && provider.name.toLowerCase().includes(searchLower))
            return true;

          // Check address
          if (provider.address && provider.address.toLowerCase().includes(searchLower))
            return true;

          // Check diagnoses served
          if (
            provider.diagnoses_served &&
            provider.diagnoses_served.toLowerCase().includes(searchLower)
          )
            return true;

          return false;
        });
      }

      // Apply additional filter options
      if (this.filterOptions.acceptsInsurance) {
        filtered = filtered.filter((provider) => provider.accepts_insurance === true);
      }

      if (this.filterOptions.acceptsRegionalCenter) {
        filtered = filtered.filter(
          (provider) => provider.accepts_regional_center === true
        );
      }

      if (this.filterOptions.matchesDiagnosis && this.userData.diagnosis) {
        filtered = filtered.filter((provider) => {
          if (!provider.diagnoses_served) return false;
          const diagnosisLower = this.userData.diagnosis.toLowerCase();
          return provider.diagnoses_served.toLowerCase().includes(diagnosisLower);
        });
      }

      return filtered;
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
      console.log("Updating filtered locations with filters:", this.filterOptions);

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
      this.radius = 5;

      // Reset filter options
      this.filterOptions = {
        acceptsInsurance: false,
        acceptsRegionalCenter: false,
        matchesDiagnosis: false,
      };

      // Re-fetch data with reset filters
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
        this.updateMarkers();
      });
    },

    // Fetch provider data
    async fetchProviders() {
      this.loading = true;
      this.error = null;

      try {
        console.log("Fetching providers from API");
        const apiBaseUrl =
          import.meta.env.VITE_API_BASE_URL || "http://localhost:8000/api";

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

          // Add filter options
          if (this.filterOptions.acceptsInsurance) {
            queryParams.append("insurance", "any");
          }

          if (this.filterOptions.acceptsRegionalCenter) {
            queryParams.append("funding_source", "Regional Center");
          }

          // Request GeoJSON format if we're displaying on map
          queryParams.append("format", "geojson");

          let response;
          let url;

          try {
            // First try with the comprehensive search endpoint
            url = `${apiBaseUrl}/providers/comprehensive_search/?${queryParams.toString()}`;
            console.log(`Fetching providers from API: ${url}`);
            response = await axios.get(url);
            console.log("API Response:", response);
          } catch (apiError) {
            console.log(
              "Comprehensive search endpoint not available, trying standard endpoint",
              apiError
            );

            try {
              // Fall back to a regular nearby search
              url = `${apiBaseUrl}/providers/nearby/?${queryParams.toString()}`;
              response = await axios.get(url);
              console.log("API Response from nearby endpoint:", response);
            } catch (fallbackError) {
              // Final fallback to just list all providers
              url = `${apiBaseUrl}/providers/`;
              response = await axios.get(url);
              console.log("API Response from standard endpoint:", response);
            }
          }

          // Handle GeoJSON response
          if (response.data && response.data.type === "FeatureCollection") {
            // Convert GeoJSON to our provider format
            this.providers = response.data.features.map((feature) => {
              return {
                id: feature.id || feature.properties.id,
                ...feature.properties,
                latitude: feature.geometry ? feature.geometry.coordinates[1] : null,
                longitude: feature.geometry ? feature.geometry.coordinates[0] : null,
              };
            });
          }
          // Handle regular JSON array response
          else if (response.data && Array.isArray(response.data)) {
            this.providers = response.data;
          }
          // Handle paginated response
          else if (response.data && Array.isArray(response.data.results)) {
            this.providers = response.data.results;
          } else {
            console.error("Unexpected API response format:", response.data);
            throw new Error("Unexpected API response format");
          }

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
        this.updateMarkers();
      }
    },

    // Fetch regional centers
    async fetchRegionalCenters() {
      this.loading = true;
      this.error = null;

      try {
        console.log("Fetching regional centers");
        const apiBaseUrl =
          import.meta.env.VITE_API_BASE_URL || "http://localhost:8000/api";

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

    // Update map markers
    updateMarkers() {
      // Clear existing markers
      this.markers.forEach((marker) => {
        marker.remove();
      });
      this.markers = [];

      // Get the appropriate data based on display type
      let items = [];
      if (this.displayType === "providers") {
        items = this.filteredProviders;
      } else if (this.displayType === "regionalCenters") {
        items = this.filteredRegionalCenters;
      } else {
        items = this.filteredLocations;
      }

      console.log(`Updating markers for ${items.length} items`);

      // Add markers for each item
      items.forEach((item) => {
        if (!item.latitude || !item.longitude) return;

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
            (this.filterOptions.acceptsRegionalCenter && item.accepts_regional_center));

        if (isSpecial) {
          el.classList.add("pulse-marker");
        }

        // Create popup content
        const popupContent = `
          <div style="min-width: 200px;">
            <h5>${item.name || "Unnamed Location"}</h5>
            <p>${item.address || ""}</p>
            ${item.diagnoses_served ? `<p>Diagnoses: ${item.diagnoses_served}</p>` : ""}
            ${item.phone ? `<p>Phone: ${item.phone}</p>` : ""}
            ${item.accepts_insurance ? "<p>✓ Accepts Insurance</p>" : ""}
            ${item.accepts_regional_center ? "<p>✓ Accepts Regional Center</p>" : ""}
          </div>
        `;

        // Create popup
        const popup = new mapboxgl.Popup({ offset: 25 }).setHTML(popupContent);

        // Create and add marker to map
        const marker = new mapboxgl.Marker(el)
          .setLngLat([item.longitude, item.latitude])
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
          if (item.latitude && item.longitude) {
            bounds.extend([item.longitude, item.latitude]);
          }
        });

        this.map.fitBounds(bounds, { padding: 50 });
      }
    },

    // Select a location
    selectLocation(location) {
      this.selectedLocation = location;

      // Center map on selected location
      if (location.latitude && location.longitude) {
        this.map.flyTo({
          center: [location.longitude, location.latitude],
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
</style>
