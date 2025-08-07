<template>
  <div class="map-app">
    <!-- Sidebar (always on left) -->
    <div class="sidebar-container">
      <div class="sidebar">
        <h3 class="mb-3">Location Finder</h3>

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
          <input
            type="text"
            v-model="searchText"
            class="form-control"
            placeholder="Search locations..."
          />
        </div>

        <!-- Filter Section -->
        <div class="filter-group mb-3">
          <h5>Filters</h5>

          <!-- We've removed the Category Filter as it's not relevant for providers -->

          <!-- Radius Filter (when geolocation is available) -->
          <div class="mb-2" v-if="userLocation.latitude && userLocation.longitude">
            <label class="form-label">Distance Radius: {{ radius }} miles</label>
            <input
              type="range"
              v-model.number="radius"
              class="form-range"
              min="1"
              max="50"
              step="1"
            />
          </div>

          <!-- Reset Button -->
          <button @click="resetFilters" class="btn btn-secondary btn-sm w-100 mt-2">
            Reset Filters
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
import {
  sampleCategories,
  sampleLocations,
  sampleUserProfile,
} from "@/assets/sampleData";

// Flag to use actual API data instead of sample data
const USE_LOCAL_DATA_ONLY = false;

export default {
  name: "MapView",

  components: {
    UserInfoPanel,
    LocationList,
    LocationDetail,
  },

  data() {
    return {
      // Display type
      displayType: "providers", // 'regionalCenters' or 'providers'

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
        latitude: null,
        longitude: null,
        accuracy: null,
      },
      radius: 5, // km

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
      searchDebounceTimer: null, // Timer for debouncing search text changes
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

    // Get the current data array based on display type
    currentData() {
      return this.displayType === "locations" ? this.locations : this.regionalCenters;
    },

    // Filtered regional centers with search, filter criteria, and user data applied
    filteredRegionalCenters() {
      if (this.displayType !== "regionalCenters" || !this.regionalCenters.length)
        return [];

      // Start with all centers
      let filtered = this.regionalCenters;

      // Apply text search filter
      if (this.searchText && this.searchText.trim() !== "") {
        console.log("Filtering regional centers with search:", this.searchText);
        const searchLower = this.searchText.toLowerCase().trim();

        filtered = filtered.filter((center) => {
          // Check each field that exists and might contain the search text
          for (const [key, value] of Object.entries(center)) {
            // Skip null/undefined values and non-string values
            if (value === null || value === undefined || typeof value !== "string") {
              continue;
            }

            // Check if this field contains the search text
            if (value.toLowerCase().includes(searchLower)) {
              return true;
            }
          }

          // If we got here, no match was found in any field
          return false;
        });

        console.log(
          `Found ${filtered.length} regional centers matching "${searchLower}"`
        );
      }

      // Check if we have relevant user data to filter by
      if (this.hasUserData) {
        // If we have user address/location, prioritize by proximity
        if (this.userLocation.latitude && this.userLocation.longitude) {
          // Sort by distance (closest first)
          filtered.sort((a, b) => {
            const distA = a.distance || Number.MAX_VALUE;
            const distB = b.distance || Number.MAX_VALUE;
            return distA - distB;
          });
        }

        // Add diagnosis-based priority markers (doesn't filter out, just annotates)
        filtered = filtered.map((center) => {
          const enhancedCenter = { ...center };

          // Add recommendation flag based on client's diagnosis
          if (this.userData.diagnosis) {
            // Default recommendation level is 0
            enhancedCenter.recommendationLevel = 0;

            // Increase recommendation level for centers with matching services
            // Age-based recommendations
            if (this.userData.age) {
              const age = parseInt(this.userData.age);

              // Early intervention services for younger children
              if (
                age < 5 &&
                center.service_area &&
                center.service_area.toLowerCase().includes("early")
              ) {
                enhancedCenter.recommendationLevel += 2;
              }

              // School-age services
              if (age >= 5 && age <= 21) {
                enhancedCenter.recommendationLevel += 1;
              }

              // Adult services
              if (
                age > 21 &&
                center.service_area &&
                center.service_area.toLowerCase().includes("adult")
              ) {
                enhancedCenter.recommendationLevel += 2;
              }
            }

            // Diagnosis-based recommendations
            const diagnosisLower = this.userData.diagnosis.toLowerCase();

            // Autism specific recommendations
            if (diagnosisLower.includes("autism") || diagnosisLower === "autism") {
              enhancedCenter.recommendationLevel += 2;
              enhancedCenter.diagnosisMatch = true;
            }

            // Label centers with high recommendation levels
            if (enhancedCenter.recommendationLevel >= 3) {
              enhancedCenter.isHighlyRecommended = true;
            }
          }

          return enhancedCenter;
        });
      }

      return filtered;
    },

    filteredProviders() {
      if (this.displayType !== "providers" || !this.providers.length) return [];

      // Start with all providers
      let filtered = this.providers;

      // Apply text search filter
      if (this.searchText && this.searchText.trim() !== "") {
        console.log("Filtering providers with search:", this.searchText);
        const searchLower = this.searchText.toLowerCase().trim();

        filtered = filtered.filter((provider) => {
          // Check each field that exists and might contain the search text
          for (const [key, value] of Object.entries(provider)) {
            // Skip null/undefined values and non-string values
            if (value === null || value === undefined || typeof value !== "string") {
              continue;
            }

            // Check if this field contains the search text
            if (value.toLowerCase().includes(searchLower)) {
              return true;
            }
          }

          // If we got here, no match was found in any field
          return false;
        });

        console.log(`Found ${filtered.length} providers matching "${searchLower}"`);
      }

      // Apply user data-based filtering if available
      if (this.hasUserData) {
        // Get user city/area if available from address
        let userArea = "";
        if (this.userData.address) {
          // Extract city or area from address if possible
          const addressParts = this.userData.address.split(",");
          if (addressParts.length > 1) {
            userArea = addressParts[1].trim().toLowerCase();
          }
        }

        // Mark providers that cover the user's area
        filtered = filtered.map((provider) => {
          const enhancedProvider = { ...provider };

          // Check if provider covers user's area
          if (userArea && provider.coverage_areas) {
            const coverageAreasLower = provider.coverage_areas.toLowerCase();
            if (coverageAreasLower.includes(userArea)) {
              enhancedProvider.servesUserArea = true;
              enhancedProvider.recommendationLevel = 2;
            }
          }

          // Add recommendation based on diagnosis
          if (this.userData.diagnosis) {
            const diagnosisLower = this.userData.diagnosis.toLowerCase();
            enhancedProvider.recommendationLevel =
              enhancedProvider.recommendationLevel || 0;

            // Autism-specific providers for autism diagnosis
            if (
              (diagnosisLower.includes("autism") || diagnosisLower === "autism") &&
              provider.name.toLowerCase().includes("autism")
            ) {
              enhancedProvider.recommendationLevel += 2;
              enhancedProvider.diagnosisMatch = true;
            }

            // Age-based recommendations
            if (this.userData.age) {
              const age = parseInt(this.userData.age);

              // Early intervention for young children
              if (
                age < 5 &&
                (provider.name.toLowerCase().includes("early") ||
                  provider.name.toLowerCase().includes("child"))
              ) {
                enhancedProvider.recommendationLevel += 1;
              }

              // Adult services for adults
              if (age > 21 && provider.name.toLowerCase().includes("adult")) {
                enhancedProvider.recommendationLevel += 1;
              }
            }

            // Mark highly recommended providers
            if (enhancedProvider.recommendationLevel >= 3) {
              enhancedProvider.isHighlyRecommended = true;
            }
          }

          return enhancedProvider;
        });

        // Sort by recommendation level if we have user data
        filtered.sort((a, b) => {
          const recA = a.recommendationLevel || 0;
          const recB = b.recommendationLevel || 0;
          return recB - recA; // Sort by highest recommendation first
        });
      }

      return filtered;
    },

    filteredLocations() {
      if (this.displayType !== "locations" || !this.locations.length) return [];

      return this.locations.filter((location) => {
        // Category filter
        if (
          this.selectedCategory &&
          String(location.category) !== String(this.selectedCategory)
        ) {
          return false;
        }

        // Search text
        if (this.searchText) {
          const searchLower = this.searchText.toLowerCase();
          const nameMatch = location.name.toLowerCase().includes(searchLower);
          const descMatch =
            location.description &&
            location.description.toLowerCase().includes(searchLower);
          const addressMatch = location.address.toLowerCase().includes(searchLower);
          const cityMatch = location.city.toLowerCase().includes(searchLower);

          if (!nameMatch && !descMatch && !addressMatch && !cityMatch) {
            return false;
          }
        }

        return true;
      });
    },
  },

  watch: {
    // Update markers when filtered data changes
    filteredLocations: {
      handler() {
        this.$nextTick(() => {
          this.updateMarkers();
        });
      },
      deep: true,
    },
    filteredRegionalCenters: {
      handler() {
        this.$nextTick(() => {
          this.updateMarkers();
        });
      },
      deep: true,
    },
    filteredProviders: {
      handler() {
        this.$nextTick(() => {
          this.updateMarkers();
        });
      },
      deep: true,
    },
    // Update locations when radius changes
    radius() {
      if (this.userLocation.latitude && this.userLocation.longitude) {
        this.fetchNearbyLocations();
      }
    },
    
    // Watch for city searches in the search text
    searchText: {
      handler(newValue) {
        // Debounce the search to avoid too many API calls
        if (this.searchDebounceTimer) {
          clearTimeout(this.searchDebounceTimer);
        }
        
        this.searchDebounceTimer = setTimeout(() => {
          this.handleSearchTextChange(newValue);
        }, 500);
      },
      immediate: false
    }
  },

  mounted() {
    console.log("Vue app mounted");

    // Initialize the app components
    this.$nextTick(() => {
      try {
        // Try to initialize the map
        this.initMap();

        // Handle window resize to make sure map resizes properly
        window.addEventListener("resize", this.handleResize);
      } catch (e) {
        console.error("Error initializing map:", e);

        // If map fails, still try to load location data
        this.fetchCategories();
        this.fetchAllLocations();
      }
    });
  },

  beforeUnmount() {
    // Clean up resize listener when component is destroyed
    window.removeEventListener("resize", this.handleResize);
  },

  // Add a created hook to set initial state
  created() {
    console.log("Vue app created");

    // Initialize with empty arrays to prevent null reference errors
    this.categories = [];
    this.locations = [];
    this.markers = [];

    // Load saved user data if available
    this.loadUserData();
  },

  methods: {
    // Set display type and handle data loading
    setDisplayType(type) {
      // Update display type
      this.displayType = type;

      // Ensure we have data for the selected type
      if (type === "locations" && this.locations.length === 0) {
        // If we're showing locations but don't have any loaded yet
        if (this.userLocation.latitude && this.userLocation.longitude) {
          this.fetchNearbyLocations();
        } else {
          this.fetchAllLocations();
        }
      } else if (type === "regionalCenters" && this.regionalCenters.length === 0) {
        // If we're showing regional centers but don't have any loaded yet
        this.fetchRegionalCenters();
      } else if (type === "providers" && this.providers.length === 0) {
        // If we're showing providers but don't have any loaded yet
        this.fetchProviders();
      }

      // Update markers on the map
      this.$nextTick(() => {
        this.updateMarkers();
      });
    },

    // Fetch providers from the API with filtering
    async fetchProviders() {
      this.loading = true;
      this.error = null;

      try {
        console.log("Fetching providers");

        // Use sample data if configured to bypass API or for testing
        if (USE_LOCAL_DATA_ONLY) {
          // Create sample providers based on real data structure
          const sampleProviders = [
            {
              id: 1,
              name: "A & H BEHAVIORAL THERAPY",
              phone: "909-665-7070, 818-823-1515",
              coverage_areas:
                "SAN FERNANDO VALLEY, SAN GABRIEL VALLEY, LONG BEACH, INGLEWOOD, COMPTON",
              areas: [
                "SAN FERNANDO VALLEY",
                "SAN GABRIEL VALLEY",
                "LONG BEACH",
                "INGLEWOOD",
                "COMPTON",
              ],
              age_groups_served: "0-5, 6-12, 13-18, 19+",
              diagnoses_served: "Autism, ADHD, Learning Disabilities",
              address: "123 Main St",
              city: "Los Angeles",
              state: "CA",
              zip_code: "90001",
              latitude: 34.05,
              longitude: -118.25,
            },
            {
              id: 2,
              name: "A CHANGE IN TRAJECTORY, INC.",
              phone: "818-235-1414",
              coverage_areas:
                "ANTELOPE VALLEY, CENTRAL LOS ANGELES, POMONA VALLEY, SAN FERNANDO VALLEY",
              areas: [
                "ANTELOPE VALLEY",
                "CENTRAL LOS ANGELES",
                "POMONA VALLEY",
                "SAN FERNANDO VALLEY",
              ],
              age_groups_served: "6-12, 13-18, 19+",
              diagnoses_served: "Autism, Developmental Delay",
              address: "456 Center Blvd",
              city: "Sherman Oaks",
              state: "CA",
              zip_code: "91423",
              latitude: 34.15,
              longitude: -118.45,
            },
            {
              id: 3,
              name: "ABA ENHANCEMENT INC.",
              phone: "951-317-5950",
              coverage_areas:
                "LONG BEACH, LAKEWOOD, HUNTINGTON PARK, DOWNEY, PICO RIVERA, LA VERNE, CLAREMONT, DIAMOND BAR, POMONA",
              areas: [
                "LONG BEACH",
                "LAKEWOOD",
                "HUNTINGTON PARK",
                "DOWNEY",
                "PICO RIVERA",
                "LA VERNE",
                "CLAREMONT",
                "DIAMOND BAR",
                "POMONA",
              ],
              age_groups_served: "0-5, 6-12",
              diagnoses_served: "Autism, Speech Delay, Sensory Processing",
              address: "789 Broadway",
              city: "Long Beach",
              state: "CA",
              zip_code: "90802",
              latitude: 33.77,
              longitude: -118.19,
            },
            {
              id: 4,
              name: "AUTISM BEHAVIOR INTERVENTION",
              phone: "844-423-8872",
              coverage_areas:
                "CENTRAL LOS ANGELES, HOLLYWOOD-WILSHIRE, SOUTH CENTRAL LOS ANGELES, SOUTH EAST LOS ANGELES",
              areas: [
                "CENTRAL LOS ANGELES",
                "HOLLYWOOD-WILSHIRE",
                "SOUTH CENTRAL LOS ANGELES",
                "SOUTH EAST LOS ANGELES",
              ],
              age_groups_served: "0-5, 6-12, 13-18",
              diagnoses_served: "Autism, Social Communication Disorder",
              address: "1010 Wilshire Blvd",
              city: "Los Angeles",
              state: "CA",
              zip_code: "90017",
              latitude: 34.05,
              longitude: -118.26,
            },
            {
              id: 5,
              name: "AUTISM LEARNING PARTNERS",
              phone: "855-295-3276",
              coverage_areas:
                "FOOTHILL, PASADENA, GLENDALE, SANTA CLARITA, SAN FERNANDO VALLEY, WEST LA",
              areas: [
                "FOOTHILL",
                "PASADENA",
                "GLENDALE",
                "SANTA CLARITA",
                "SAN FERNANDO VALLEY",
                "WEST LA",
              ],
              age_groups_served: "0-5, 6-12, 13-18, 19+",
              diagnoses_served: "Autism, ADHD, Behavioral Challenges",
              address: "200 S Los Robles Ave",
              city: "Pasadena",
              state: "CA",
              zip_code: "91101",
              latitude: 34.14,
              longitude: -118.14,
            },
          ];

          // Perform local filtering based on user data
          let filtered = [...sampleProviders];

          // Filter by diagnosis if available
          if (this.userData.diagnosis) {
            const diagnosis = this.userData.diagnosis.toLowerCase();
            filtered = filtered.filter(
              (provider) =>
                provider.diagnoses_served &&
                provider.diagnoses_served.toLowerCase().includes(diagnosis)
            );
          }

          // Filter by age if available
          if (this.userData.age) {
            const age = parseInt(this.userData.age);
            if (!isNaN(age)) {
              filtered = filtered.filter((provider) => {
                if (!provider.age_groups_served) return false;

                const ageRanges = provider.age_groups_served.split(",");
                return ageRanges.some((range) => {
                  range = range.trim();
                  if (range.includes("-")) {
                    const [min, max] = range
                      .split("-")
                      .map((n) => n.replace("+", "").trim());
                    const minAge = parseInt(min);
                    const maxAge = max.includes("+") ? 100 : parseInt(max);
                    return age >= minAge && age <= maxAge;
                  } else if (range.includes("+")) {
                    const minAge = parseInt(range.replace("+", "").trim());
                    return age >= minAge;
                  }
                  return false;
                });
              });
            }
          }

          // Add distance property if user location is available
          if (this.userLocation.latitude && this.userLocation.longitude) {
            // For sample data, we'll calculate actual distances
            filtered = filtered.map((provider) => {
              // Calculate distance using Haversine formula
              const userLat = this.userLocation.latitude;
              const userLng = this.userLocation.longitude;
              const providerLat = provider.latitude;
              const providerLng = provider.longitude;

              // Convert to radians
              const lat1 = (userLat * Math.PI) / 180;
              const lng1 = (userLng * Math.PI) / 180;
              const lat2 = (providerLat * Math.PI) / 180;
              const lng2 = (providerLng * Math.PI) / 180;

              // Haversine formula
              const dlng = lng2 - lng1;
              const dlat = lat2 - lat1;
              const a =
                Math.sin(dlat / 2) ** 2 +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlng / 2) ** 2;
              const c = 2 * Math.asin(Math.sqrt(a));
              const distance = 3959 * c; // Earth's radius in miles * c

              return {
                ...provider,
                distance: parseFloat(distance.toFixed(1)),
              };
            });

            // Filter by radius
            filtered = filtered.filter((provider) => provider.distance <= this.radius);

            // Sort by distance
            filtered.sort((a, b) => a.distance - b.distance);
          }

          this.providers = filtered;
          console.log(`Loaded ${this.providers.length} sample providers`);
          this.loading = false;
          return;
        }

        // Otherwise try to fetch from API with proper filtering
        try {
          let url = "/api/providers/";
          const params = {};

          // If we have user location, fetch nearby providers
          if (this.userLocation.latitude && this.userLocation.longitude) {
            url = "/api/providers/nearby/";
            params.lat = this.userLocation.latitude;
            params.lng = this.userLocation.longitude;
            params.radius = this.radius;

            // Add age filter if available
            if (this.userData.age) {
              params.age = this.userData.age;
            }

            // Add diagnosis filter if available
            if (this.userData.diagnosis) {
              params.diagnosis = this.userData.diagnosis;
            }
          }
          // If we have diagnosis but no location, fetch by diagnosis
          else if (this.userData.diagnosis) {
            url = "/api/providers/by_diagnosis/";
            params.diagnosis = this.userData.diagnosis;
          }
          // If we have age but no location, fetch by age group
          else if (this.userData.age) {
            url = "/api/providers/by_age_group/";
            params.age = this.userData.age;
          }

          const response = await axios.get(url, { params });

          console.log("Providers API response:", response.data);

          // Check if response has expected format
          if (response.data && response.data.error) {
            console.error("API returned error:", response.data.error);
            throw new Error(response.data.error);
          } else if (response.data && response.data.results) {
            this.providers = response.data.results;
          } else if (Array.isArray(response.data)) {
            this.providers = response.data;
          } else {
            console.error("Unexpected API response format:", response.data);
            throw new Error("Unexpected API response format");
          }
        } catch (apiError) {
          console.error(
            "Error fetching providers from API, using sample data:",
            apiError
          );

          // Fallback to sample data - simplified version
          this.providers = [
            {
              id: 1,
              name: "A & H BEHAVIORAL THERAPY",
              phone: "909-665-7070, 818-823-1515",
              coverage_areas:
                "SAN FERNANDO VALLEY, SAN GABRIEL VALLEY, LONG BEACH, INGLEWOOD, COMPTON",
              age_groups_served: "0-5, 6-12, 13-18, 19+",
              diagnoses_served: "Autism, ADHD, Learning Disabilities",
            },
            {
              id: 2,
              name: "A CHANGE IN TRAJECTORY, INC.",
              phone: "818-235-1414",
              coverage_areas:
                "ANTELOPE VALLEY, CENTRAL LOS ANGELES, POMONA VALLEY, SAN FERNANDO VALLEY",
              age_groups_served: "6-12, 13-18, 19+",
              diagnoses_served: "Autism, Developmental Delay",
            },
            {
              id: 3,
              name: "ABA ENHANCEMENT INC.",
              phone: "951-317-5950",
              coverage_areas:
                "LONG BEACH, LAKEWOOD, HUNTINGTON PARK, DOWNEY, PICO RIVERA, LA VERNE, CLAREMONT, DIAMOND BAR, POMONA",
              age_groups_served: "0-5, 6-12",
              diagnoses_served: "Autism, Speech Delay, Sensory Processing",
            },
          ];
        }

        console.log(`Retrieved ${this.providers.length} providers`);
        this.loading = false;
      } catch (error) {
        console.error("Error in fetchProviders method:", error);
        this.loading = false;
        this.error = "Failed to load providers";
      }
    },

    async fetchRegionalCenters() {
      this.loading = true;
      this.error = null;

      try {
        console.log("Fetching regional centers");

        // Use sample data if configured to bypass API
        if (USE_LOCAL_DATA_ONLY) {
          // Create some sample regional centers data for testing
          const sampleRegionalCenters = [
            {
              id: 1,
              name: "Southern California Regional Center",
              address: "2000 E Imperial Hwy",
              city: "Los Angeles",
              state: "CA",
              zip_code: "90059",
              latitude: 33.9341,
              longitude: -118.24,
              description: "Serving Los Angeles County",
              phone: "323-555-1000",
              website: "https://example.com/scrc",
              email: "info@scrc.example.com",
              region: "Southern California",
              service_area: "Los Angeles County and surrounding areas",
            },
            {
              id: 2,
              name: "Central Regional Center",
              address: "1600 9th St",
              city: "Sacramento",
              state: "CA",
              zip_code: "95814",
              latitude: 38.5816,
              longitude: -121.4944,
              description: "Serving Sacramento County",
              phone: "916-555-2000",
              website: "https://example.com/crc",
              email: "info@crc.example.com",
              region: "Central California",
              service_area: "Sacramento County and surrounding areas",
            },
            {
              id: 3,
              name: "Northern California Regional Center",
              address: "900 Lafayette St",
              city: "Santa Clara",
              state: "CA",
              zip_code: "95050",
              latitude: 37.3541,
              longitude: -121.9552,
              description: "Serving Santa Clara County",
              phone: "408-555-3000",
              website: "https://example.com/ncrc",
              email: "info@ncrc.example.com",
              region: "Northern California",
              service_area: "Santa Clara County and surrounding areas",
            },
          ];

          // If we have user location, calculate distances
          if (this.userLocation.latitude && this.userLocation.longitude) {
            this.regionalCenters = sampleRegionalCenters.map((center) => {
              const userLat = this.userLocation.latitude;
              const userLng = this.userLocation.longitude;
              const centerLat = center.latitude;
              const centerLng = center.longitude;

              // Simple distance calculation (rough approximation)
              const distance = Math.sqrt(
                Math.pow((centerLat - userLat) * 111, 2) +
                  Math.pow(
                    (centerLng - userLng) * 111 * Math.cos(userLat * (Math.PI / 180)),
                    2
                  )
              ).toFixed(1);

              return {
                ...center,
                distance: parseFloat(distance),
              };
            });
          } else {
            this.regionalCenters = sampleRegionalCenters;
          }

          console.log(`Loaded ${this.regionalCenters.length} sample regional centers`);
          this.loading = false;
          return;
        }

        // Otherwise try to fetch from API with fallback
        try {
          const response = await axios.get("/api/regional-centers/");

          console.log("Regional centers API response:", response.data);

          // Check if response is paginated or has error
          if (response.data && response.data.error) {
            console.error("API returned error:", response.data.error);
            throw new Error(response.data.error);
          } else if (response.data && response.data.results) {
            this.regionalCenters = response.data.results;
          } else if (Array.isArray(response.data)) {
            this.regionalCenters = response.data;
          } else {
            console.error("Unexpected API response format:", response.data);
            throw new Error("Unexpected API response format");
          }
        } catch (apiError) {
          console.error(
            "Error fetching regional centers from API, using sample data:",
            apiError
          );

          // Create fallback sample data
          this.regionalCenters = [
            {
              id: 1,
              name: "Southern California Regional Center",
              address: "2000 E Imperial Hwy",
              city: "Los Angeles",
              state: "CA",
              zip_code: "90059",
              latitude: 33.9341,
              longitude: -118.24,
              description: "Serving Los Angeles County",
              phone: "323-555-1000",
              website: "https://example.com/scrc",
              email: "info@scrc.example.com",
              region: "Southern California",
              service_area: "Los Angeles County and surrounding areas",
            },
            {
              id: 2,
              name: "Central Regional Center",
              address: "1600 9th St",
              city: "Sacramento",
              state: "CA",
              zip_code: "95814",
              latitude: 38.5816,
              longitude: -121.4944,
              description: "Serving Sacramento County",
              phone: "916-555-2000",
              website: "https://example.com/crc",
              email: "info@crc.example.com",
              region: "Central California",
              service_area: "Sacramento County and surrounding areas",
            },
            {
              id: 3,
              name: "Northern California Regional Center",
              address: "900 Lafayette St",
              city: "Santa Clara",
              state: "CA",
              zip_code: "95050",
              latitude: 37.3541,
              longitude: -121.9552,
              description: "Serving Santa Clara County",
              phone: "408-555-3000",
              website: "https://example.com/ncrc",
              email: "info@ncrc.example.com",
              region: "Northern California",
              service_area: "Santa Clara County and surrounding areas",
            },
          ];
        }

        console.log(`Retrieved ${this.regionalCenters.length} regional centers`);
        this.loading = false;
      } catch (error) {
        console.error("Error in fetchRegionalCenters method:", error);
        this.loading = false;
        this.error = "Failed to load regional centers";
      }
    },

    // Map initialization
    initMap() {
      // Set Mapbox access token from environment variables or use fallback
      mapboxgl.accessToken =
        import.meta.env.VITE_MAPBOX_TOKEN ||
        "pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg";

      // Create Mapbox map
      this.map = new mapboxgl.Map({
        container: "map",
        style: "mapbox://styles/mapbox/streets-v12",
        center: [-118.2437, 34.0522], // Los Angeles coordinates
        zoom: 11,
      });

      // Add navigation control
      this.map.addControl(new mapboxgl.NavigationControl(), "top-right");

      // Add geolocation control
      this.map.addControl(
        new mapboxgl.GeolocateControl({
          positionOptions: {
            enableHighAccuracy: true,
          },
          trackUserLocation: true,
          showUserHeading: true,
        }),
        "top-right"
      );

      // Get user location for nearby search
      this.getUserLocation();

      // When map loads, fetch data
      this.map.on("load", () => {
        this.fetchCategories();
      });
    },

    // Location data fetching
    getUserLocation() {
      if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            // Store user location
            this.userLocation = {
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
              accuracy: position.coords.accuracy,
            };

            // Fetch nearby locations
            this.fetchNearbyLocations();
          },
          (error) => {
            console.error("Geolocation error:", error);
            // Fallback to fetching all locations
            this.fetchAllLocations();
          }
        );
      } else {
        // Fallback if geolocation not supported
        this.fetchAllLocations();
      }
    },

    async fetchCategories() {
      try {
        console.log("Fetching categories...");

        // Use sample data if configured to bypass API
        if (USE_LOCAL_DATA_ONLY) {
          console.log("Using sample categories data (API calls disabled)");
          this.categories = sampleCategories;
          return;
        }

        // Otherwise try to fetch from API with fallback
        try {
          const response = await axios.get("/api/categories/");
          console.log("Categories API response:", response.data);

          // Check if response is valid
          if (response.data && Array.isArray(response.data)) {
            // Filter out any null/undefined categories
            this.categories = response.data.filter((category) => category && category.id);
            console.log(`Loaded ${this.categories.length} categories`);
          } else if (
            response.data &&
            response.data.results &&
            Array.isArray(response.data.results)
          ) {
            // Handle paginated response
            this.categories = response.data.results.filter(
              (category) => category && category.id
            );
            console.log(
              `Loaded ${this.categories.length} categories from paginated results`
            );
          } else {
            console.warn("Invalid categories data format:", response.data);
            this.categories = sampleCategories;
          }
        } catch (apiError) {
          console.error(
            "Error fetching categories from API, using sample categories:",
            apiError
          );
          this.categories = sampleCategories;
        }
      } catch (error) {
        console.error("Error in fetchCategories method:", error);
        this.categories = sampleCategories;
      }
    },

    async fetchAllLocations() {
      this.loading = true;
      this.error = null;

      try {
        console.log("Fetching all locations");

        // Use sample data if configured to bypass API
        if (USE_LOCAL_DATA_ONLY) {
          console.log("Using sample locations data (API calls disabled)");
          this.locations = sampleLocations;
          console.log(`Loaded ${this.locations.length} sample locations`);
          this.loading = false;
          return;
        }

        // Otherwise try to fetch from API with fallback
        try {
          const response = await axios.get("/api/locations/");

          console.log("All locations API response:", response.data);

          // Check if response is paginated or has error
          if (response.data && response.data.error) {
            console.error("API returned error:", response.data.error);
            throw new Error(response.data.error);
          } else if (response.data && response.data.results) {
            this.locations = response.data.results;
          } else if (Array.isArray(response.data)) {
            this.locations = response.data;
          } else {
            console.error("Unexpected API response format:", response.data);
            throw new Error("Unexpected API response format");
          }
        } catch (apiError) {
          console.error(
            "Error fetching locations from API, using sample data:",
            apiError
          );
          this.locations = sampleLocations;
        }

        console.log(`Retrieved ${this.locations.length} locations`);
        this.loading = false;
      } catch (error) {
        console.error("Error in fetchAllLocations method:", error);
        this.locations = sampleLocations;
        this.loading = false;
      }
    },

    async fetchNearbyLocations() {
      if (!this.userLocation.latitude || !this.userLocation.longitude) {
        console.log("No user location available, fetching all locations instead");
        return this.fetchAllLocations();
      }

      this.loading = true;
      this.error = null;

      try {
        console.log(
          `Fetching nearby locations with lat=${this.userLocation.latitude}, lng=${this.userLocation.longitude}, radius=${this.radius}km`
        );

        // Use sample data with calculated distances if configured to bypass API
        if (USE_LOCAL_DATA_ONLY) {
          console.log(
            "Using sample locations data with distance calculation (API calls disabled)"
          );

          // Calculate distances for all sample locations
          this.locations = sampleLocations
            .map((location) => {
              // Calculate actual distance based on user location
              const userLat = this.userLocation.latitude;
              const userLng = this.userLocation.longitude;
              const locationLat = location.latitude;
              const locationLng = location.longitude;

              // Simple distance calculation (rough approximation)
              const distance = Math.sqrt(
                Math.pow((locationLat - userLat) * 111, 2) +
                  Math.pow(
                    (locationLng - userLng) * 111 * Math.cos(userLat * (Math.PI / 180)),
                    2
                  )
              ).toFixed(1);

              return {
                ...location,
                distance: parseFloat(distance),
              };
            })
            .filter((location) => {
              // Filter by radius
              return location.distance <= this.radius;
            });

          console.log(
            `Calculated distances for ${this.locations.length} locations within ${this.radius}km radius`
          );
          this.loading = false;
          return;
        }

        // Otherwise try to fetch from API with fallback
        try {
          const response = await axios.get("/api/locations/nearby/", {
            params: {
              lat: this.userLocation.latitude,
              lng: this.userLocation.longitude,
              radius: this.radius,
            },
          });

          console.log("Nearby locations API response:", response.data);

          // Check if response is paginated or has an error
          if (response.data && response.data.error) {
            console.error("API returned error:", response.data.error);
            throw new Error(response.data.error);
          } else if (response.data && response.data.results) {
            this.locations = response.data.results;
          } else if (Array.isArray(response.data)) {
            this.locations = response.data;
          } else {
            console.error("Unexpected API response format:", response.data);
            throw new Error("Unexpected API response format");
          }
        } catch (apiError) {
          console.error(
            "Error fetching nearby locations from API, using sample data with distance calculation:",
            apiError
          );

          // Use sample data with calculated distances
          this.locations = sampleLocations
            .map((location) => {
              // Calculate actual distance based on user location
              const userLat = this.userLocation.latitude;
              const userLng = this.userLocation.longitude;
              const locationLat = location.latitude;
              const locationLng = location.longitude;

              // Simple distance calculation (rough approximation)
              const distance = Math.sqrt(
                Math.pow((locationLat - userLat) * 111, 2) +
                  Math.pow(
                    (locationLng - userLng) * 111 * Math.cos(userLat * (Math.PI / 180)),
                    2
                  )
              ).toFixed(1);

              return {
                ...location,
                distance: parseFloat(distance),
              };
            })
            .filter((location) => {
              // Filter by radius
              return location.distance <= this.radius;
            });
        }

        console.log(`Retrieved ${this.locations.length} nearby locations`);
        this.loading = false;
      } catch (error) {
        console.error("Error in fetchNearbyLocations method:", error);

        // Use sample data with calculated distances as fallback
        this.locations = sampleLocations
          .map((location) => {
            // Calculate distance based on user location
            const userLat = this.userLocation.latitude;
            const userLng = this.userLocation.longitude;
            const locationLat = location.latitude;
            const locationLng = location.longitude;

            // Simple distance calculation
            const distance = Math.sqrt(
              Math.pow((locationLat - userLat) * 111, 2) +
                Math.pow(
                  (locationLng - userLng) * 111 * Math.cos(userLat * (Math.PI / 180)),
                  2
                )
            ).toFixed(1);

            return {
              ...location,
              distance: parseFloat(distance),
            };
          })
          .filter((location) => location.distance <= this.radius);

        this.loading = false;
      }
    },

    // Map markers management
    updateMarkers() {
      // Get the appropriate data based on display type
      let items = [];
      if (this.displayType === "locations") {
        items = this.filteredLocations;
      } else if (this.displayType === "regionalCenters") {
        items = this.filteredRegionalCenters;
      } else if (this.displayType === "providers") {
        // Filter providers that have coordinates
        items = this.filteredProviders.filter(
          (provider) => provider.latitude && provider.longitude
        );
      }

      console.log(`Updating markers for ${items.length} ${this.displayType}`);

      // Remove existing markers
      this.markers.forEach((marker) => {
        try {
          marker.remove();
        } catch (e) {
          console.error("Error removing marker:", e);
        }
      });
      this.markers = [];

      // Add markers for filtered items (locations or regional centers)
      items.forEach((item) => {
        try {
          // Skip if no coordinates
          if (!item.latitude || !item.longitude) {
            console.warn(`Item ${item.id} is missing coordinates, skipping marker`);
            return;
          }

          // Parse coordinates safely
          let lat, lng;
          try {
            lat = parseFloat(item.latitude);
            lng = parseFloat(item.longitude);

            // Check for valid coordinates
            if (
              isNaN(lat) ||
              isNaN(lng) ||
              lat < -90 ||
              lat > 90 ||
              lng < -180 ||
              lng > 180
            ) {
              console.warn(`Item ${item.id} has invalid coordinates: ${lat}, ${lng}`);
              return;
            }
          } catch (e) {
            console.error(`Error parsing coordinates for item ${item.id}:`, e);
            return;
          }

          // Get rating stars for locations
          let ratingDisplay = "";
          if (this.displayType === "locations" && item.rating) {
            const stars = "★".repeat(Math.round(item.rating || 0));
            const emptyStars = "☆".repeat(5 - Math.round(item.rating || 0));
            ratingDisplay = `<p><span style="color: gold;">${stars}${emptyStars}</span></p>`;
          }

          // Check if this is a recommended item based on user data
          const isRecommended = item.recommendationLevel && item.recommendationLevel > 0;
          const isHighlyRecommended = item.isHighlyRecommended;
          const matchesDiagnosis = item.diagnosisMatch;
          const servesUserArea = item.servesUserArea;

          // Create recommendation badge if applicable
          let recommendationBadge = "";
          if (isHighlyRecommended) {
            recommendationBadge = `<div style="background-color: #28a745; color: white; padding: 2px 5px; border-radius: 3px; display: inline-block; margin-bottom: 5px; font-size: 12px;">Highly Recommended</div>`;
          } else if (isRecommended) {
            recommendationBadge = `<div style="background-color: #17a2b8; color: white; padding: 2px 5px; border-radius: 3px; display: inline-block; margin-bottom: 5px; font-size: 12px;">Recommended</div>`;
          }

          // Add diagnosis match badge if applicable
          let diagnosisBadge = "";
          if (matchesDiagnosis) {
            diagnosisBadge = `<div style="background-color: #dc3545; color: white; padding: 2px 5px; border-radius: 3px; display: inline-block; margin-bottom: 5px; margin-left: 5px; font-size: 12px;">Matches Diagnosis</div>`;
          }

          // Add service area badge if applicable
          let areaBadge = "";
          if (servesUserArea) {
            areaBadge = `<div style="background-color: #6c757d; color: white; padding: 2px 5px; border-radius: 3px; display: inline-block; margin-bottom: 5px; margin-left: 5px; font-size: 12px;">Serves Your Area</div>`;
          }

          // Create popup content with recommendation information
          const popupContent = `
            <div style="min-width: 250px; padding: 10px;">
              <div style="display: flex; flex-wrap: wrap;">
                ${recommendationBadge}
                ${diagnosisBadge}
                ${areaBadge}
              </div>
              <h5>${
                this.displayType === "regionalCenters"
                  ? item.regional_center || "Unnamed Center"
                  : item.name || "Unnamed Item"
              }</h5>
              <p>${item.address || ""}, ${item.city || ""}</p>
              ${ratingDisplay}
              ${
                this.displayType === "regionalCenters" && item.county_served
                  ? `<p>County: ${item.county_served}</p>`
                  : ""
              }
              ${
                this.displayType === "regionalCenters" && item.office_type
                  ? `<p>Office: ${item.office_type}</p>`
                  : ""
              }
              ${item.distance ? `<p>Distance: ${item.distance.toFixed(1)} miles</p>` : ""}
              ${
                this.userData.diagnosis && this.displayType === "regionalCenters"
                  ? `<p><strong>Services for ${this.userData.diagnosis}:</strong> ${
                      matchesDiagnosis ? "Yes" : "General services"
                    }</p>`
                  : ""
              }
              ${
                this.userData.age &&
                this.displayType === "regionalCenters" &&
                item.recommendationLevel > 0
                  ? `<p><strong>Age-appropriate services:</strong> Yes</p>`
                  : ""
              }
            </div>
          `;

          // Create popup
          const popup = new mapboxgl.Popup({ offset: 25 }).setHTML(popupContent);

          // Create marker with color based on category and recommendation level
          const el = document.createElement("div");
          el.className = "marker";

          // Enhanced marker style based on recommendation level
          if (this.displayType === "regionalCenters") {
            if (isHighlyRecommended) {
              // Highly recommended regional centers (green with animation)
              el.style.backgroundColor = "#28a745"; // Green
              el.style.width = "24px";
              el.style.height = "24px";
              el.style.animation = "pulse 1.5s infinite";
            } else if (isRecommended) {
              // Recommended regional centers (light blue)
              el.style.backgroundColor = "#17a2b8"; // Cyan
              el.style.width = "22px";
              el.style.height = "22px";
            } else {
              // Standard regional centers (purple)
              el.style.backgroundColor = "#8A2BE2"; // Purple
              el.style.width = "20px";
              el.style.height = "20px";
            }
          } else {
            // Regular locations use category colors
            el.style.backgroundColor = this.getCategoryColor(item.category);
            el.style.width = "20px";
            el.style.height = "20px";
          }

          // Common styles
          el.style.borderRadius = "50%";
          el.style.border = "2px solid white";
          el.style.boxShadow = "0 0 5px rgba(0,0,0,0.3)";

          // Add pulse animation style for highly recommended items
          if (isHighlyRecommended) {
            const styleElement = document.createElement("style");
            styleElement.textContent = `
              @keyframes pulse {
                0% { transform: scale(1); opacity: 1; }
                50% { transform: scale(1.2); opacity: 0.9; }
                100% { transform: scale(1); opacity: 1; }
              }
            `;
            document.head.appendChild(styleElement);
          }

          // Create marker
          const marker = new mapboxgl.Marker(el)
            .setLngLat([lng, lat])
            .setPopup(popup)
            .addTo(this.map);

          // Add click event to marker
          marker.getElement().addEventListener("click", () => {
            this.selectLocation(item);
          });

          // Store marker and item data together
          marker.locationId = item.id;
          this.markers.push(marker);
        } catch (e) {
          console.error(`Error creating marker for item ${item.id}:`, e);
        }
      });

      // Fit map to markers if we have any
      if (this.markers.length > 0) {
        try {
          const bounds = new mapboxgl.LngLatBounds();
          let boundsAdded = false;

          // Use the same items array we used for adding markers
          items.forEach((item) => {
            try {
              const lat = parseFloat(item.latitude);
              const lng = parseFloat(item.longitude);

              if (
                !isNaN(lat) &&
                !isNaN(lng) &&
                lat >= -90 &&
                lat <= 90 &&
                lng >= -180 &&
                lng <= 180
              ) {
                bounds.extend([lng, lat]);
                boundsAdded = true;
              }
            } catch (e) {
              console.warn(`Error adding item ${item.id} to bounds:`, e);
            }
          });

          if (boundsAdded) {
            this.map.fitBounds(bounds, { padding: 50 });
          }
        } catch (e) {
          console.error("Error fitting bounds:", e);
        }
      }

      console.log(`Added ${this.markers.length} markers to the map`);
    },

    // Location selection with improved handling to prevent layout issues
    selectLocation(location) {
      try {
        console.log("Selecting location:", location);

        // If the same location is already selected, just return
        if (this.selectedLocation && this.selectedLocation.id === location.id) {
          return;
        }

        // First, close any currently open popups
        this.markers.forEach((marker) => {
          if (marker._popup && marker._popup.isOpen()) {
            marker.togglePopup();
          }
        });

        // Short delay before updating state to avoid UI jank
        setTimeout(() => {
          // Set the selected location
          this.selectedLocation = location;

          // Parse coordinates safely
          let lat, lng;
          try {
            lat = parseFloat(location.latitude);
            lng = parseFloat(location.longitude);

            // Check for valid coordinates
            if (
              isNaN(lat) ||
              isNaN(lng) ||
              lat < -90 ||
              lat > 90 ||
              lng < -180 ||
              lng > 180
            ) {
              console.warn(
                `Cannot center map: Location ${location.id} has invalid coordinates: ${lat}, ${lng}`
              );
              return;
            }

            // Detect device type
            const isMobile = window.innerWidth < 768;
            const isSmallScreen = window.innerWidth < 576;

            // Adjust viewport behavior based on device
            if (isMobile) {
              // On mobile, we use a longer delay to ensure the UI has stabilized
              setTimeout(() => {
                this.map.flyTo({
                  center: [lng, lat],
                  zoom: isSmallScreen ? 14 : 15, // Less zoom on very small screens
                  speed: 1.0, // Slower speed for smoother transition
                  curve: 1.2,
                  essential: true, // Mark as essential for performance
                });

                // Show the popup after the map movement has completed
                this.map.once("moveend", () => {
                  const marker = this.markers.find((m) => m.locationId === location.id);
                  if (marker && marker._popup && !marker._popup.isOpen()) {
                    marker.togglePopup();
                  }
                });
              }, 400);
            } else {
              // On desktop, center with a slight delay for a smoother experience
              setTimeout(() => {
                this.map.flyTo({
                  center: [lng, lat],
                  zoom: 15,
                  speed: 1.2,
                  essential: true,
                });

                // Show the popup slightly after map movement begins
                setTimeout(() => {
                  const marker = this.markers.find((m) => m.locationId === location.id);
                  if (marker && marker._popup && !marker._popup.isOpen()) {
                    marker.togglePopup();
                  }
                }, 300);
              }, 50);
            }
          } catch (e) {
            console.error(`Error parsing coordinates for location ${location.id}:`, e);
          }
        }, 10);
      } catch (e) {
        console.error("Error selecting location:", e);
      }
    },

    closeLocationDetails() {
      // For a smoother experience, first close popups, then update state
      if (this.selectedLocation) {
        try {
          // Find and close any open popups
          this.markers.forEach((marker) => {
            if (marker._popup && marker._popup.isOpen()) {
              marker.togglePopup();
            }
          });

          // Short delay to ensure UI updates don't conflict
          setTimeout(() => {
            // Clear selected location
            this.selectedLocation = null;
          }, 50);
        } catch (e) {
          console.error("Error closing location details:", e);
          // Fallback: update state directly
          this.selectedLocation = null;
        }
      }
    },

    resetFilters() {
      this.selectedCategory = "";
      this.searchText = "";
    },

    // User information methods
    toggleUserPanel() {
      this.showUserPanel = !this.showUserPanel;
    },

    saveUserData(userData) {
      // Update user data
      this.userData = { ...userData };

      // Save to localStorage for persistence (always good as a fallback)
      localStorage.setItem("userData", JSON.stringify(this.userData));

      // Skip API call if we're in local-only mode
      if (USE_LOCAL_DATA_ONLY) {
        console.log("Saved user data to localStorage only (API calls disabled)");

        // Get geocoded location from address if needed
        if (
          this.userData.address &&
          (!this.userLocation.latitude || !this.userLocation.longitude)
        ) {
          this.geocodeUserAddress();
        }

        // Hide panel after saving
        this.showUserPanel = false;
        return;
      }

      // Try to save using the REST API
      axios
        .post("/api/users/profiles/update_me/", {
          age: this.userData.age ? parseInt(this.userData.age) : null,
          address: this.userData.address,
          diagnosis: this.userData.diagnosis.toLowerCase(),
          other_diagnosis:
            this.userData.diagnosis === "Other" ? this.userData.otherDiagnosis : "",
        })
        .then((response) => {
          console.log("User profile updated:", response.data);

          // Get geocoded location from address if needed
          if (
            this.userData.address &&
            (!this.userLocation.latitude || !this.userLocation.longitude)
          ) {
            this.geocodeUserAddress();
          }

          // Hide panel after saving
          this.showUserPanel = false;
        })
        .catch((error) => {
          console.error("Error updating user profile:", error);

          // If API call fails, still keep localStorage data
          alert(
            "There was an issue saving to the server, but your data has been saved locally."
          );

          // Get geocoded location from address if needed
          if (
            this.userData.address &&
            (!this.userLocation.latitude || !this.userLocation.longitude)
          ) {
            this.geocodeUserAddress();
          }

          // Hide panel after saving
          this.showUserPanel = false;
        });
    },

    geocodeUserAddress() {
      // Use Mapbox's geocoding API since we're already using Mapbox for maps
      if (!this.userData.address) {
        console.error("No address to geocode");
        return;
      }

      console.log("Geocoding address:", this.userData.address);

      // Setup simple fake coordinates for local-only mode (Los Angeles area)
      if (USE_LOCAL_DATA_ONLY) {
        console.log("Simulating geocoding in local-only mode");

        // Generate random coordinates near downtown LA
        const baseLat = 34.052;
        const baseLng = -118.243;
        const randomLat = baseLat + (Math.random() * 0.1 - 0.05);
        const randomLng = baseLng + (Math.random() * 0.1 - 0.05);

        // Update user location with fake coordinates
        this.userLocation = {
          latitude: randomLat,
          longitude: randomLng,
          accuracy: null,
        };

        console.log("Set simulated coordinates:", this.userLocation);

        // Center map on the new coordinates
        if (this.map) {
          this.map.flyTo({
            center: [randomLng, randomLat],
            zoom: 13,
          });
        }

        // Fetch nearby locations based on the new coordinates
        this.fetchNearbyLocations();
        return;
      }

      // Otherwise use the real Mapbox Geocoding API
      // Format the address for the API
      const encodedAddress = encodeURIComponent(this.userData.address);

      // Use Mapbox Geocoding API
      const geocodingUrl = `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodedAddress}.json?access_token=${mapboxgl.accessToken}`;

      axios
        .get(geocodingUrl)
        .then((response) => {
          console.log("Geocoding response:", response.data);

          if (response.data.features && response.data.features.length > 0) {
            // Get the first result's coordinates [longitude, latitude]
            const coordinates = response.data.features[0].center;

            // Update user location
            this.userLocation = {
              longitude: coordinates[0],
              latitude: coordinates[1],
              accuracy: null,
            };

            console.log("Updated user location from geocoding:", this.userLocation);

            // Center map on the new coordinates
            if (this.map) {
              this.map.flyTo({
                center: coordinates,
                zoom: 13,
              });
            }

            // Fetch nearby locations based on the new coordinates
            this.fetchNearbyLocations();

            // Also update user profile in backend with geocoded coordinates
            this.updateUserCoordinates(coordinates[1], coordinates[0]);
          } else {
            console.warn(
              "No geocoding results found for address:",
              this.userData.address
            );
          }
        })
        .catch((error) => {
          console.error("Error geocoding address:", error);
        });
    },
    
    // Handle search text changes to detect city searches
    async handleSearchTextChange(searchValue) {
      if (!searchValue || searchValue.trim().length < 3) {
        return;
      }
      
      const searchTerm = searchValue.trim().toLowerCase();
      
      // List of common city names to detect
      const cityPatterns = [
        'los angeles', 'la', 'san diego', 'san francisco', 'sf', 'sacramento',
        'san jose', 'oakland', 'long beach', 'anaheim', 'santa ana', 'riverside',
        'pasadena', 'glendale', 'burbank', 'santa monica', 'beverly hills',
        'compton', 'inglewood', 'torrance', 'fullerton', 'orange', 'irvine',
        'pomona', 'ontario', 'corona', 'palmdale', 'lancaster', 'el monte',
        'downey', 'costa mesa', 'carlsbad', 'west covina', 'norwalk', 'berkeley',
        'vallejo', 'fairfield', 'richmond', 'antioch', 'daly city', 'ventura',
        'santa barbara', 'fresno', 'bakersfield', 'stockton', 'modesto', 'oxnard',
        'escondido', 'sunnyvale', 'hayward', 'salinas', 'visalia', 'chula vista',
        'oceanside', 'santa rosa', 'rancho cucamonga', 'concord', 'roseville'
      ];
      
      // Check if the search term matches a city
      const isCity = cityPatterns.some(city => 
        searchTerm === city || 
        searchTerm.startsWith(city + ' ') ||
        searchTerm.endsWith(' ' + city)
      );
      
      if (isCity) {
        console.log('🔍 Searching for city:', searchTerm);
        const geocodeResult = await this.geocodeAddressForSearch(searchTerm);
        console.log('Geocoding result:', geocodeResult);
        
        if (geocodeResult) {
          // Update user location with the geocoded city
          this.userLocation = {
            latitude: geocodeResult.lat,
            longitude: geocodeResult.lng,
            accuracy: null
          };
          
          // Center map on the city
          if (this.map) {
            this.map.flyTo({
              center: [geocodeResult.lng, geocodeResult.lat],
              zoom: 12
            });
          }
          
          // Fetch providers in this area
          if (this.displayType === 'providers') {
            this.fetchProviders();
          } else if (this.displayType === 'regionalCenters') {
            this.fetchRegionalCenters();
          }
        }
      }
    },
    
    // Geocode an address for search purposes
    async geocodeAddressForSearch(searchTerm) {
      try {
        // Add California to the search to ensure we get CA results
        const searchQuery = searchTerm.includes('california') || searchTerm.includes('ca') 
          ? searchTerm 
          : `${searchTerm}, California`;
          
        const encodedAddress = encodeURIComponent(searchQuery);
        const geocodingUrl = `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodedAddress}.json?access_token=${mapboxgl.accessToken}&country=US&types=place`;
        
        const response = await axios.get(geocodingUrl);
        
        if (response.data && response.data.features && response.data.features.length > 0) {
          const feature = response.data.features[0];
          const [lng, lat] = feature.center;
          
          console.log(`Found ${searchTerm} coordinates:`, {lat, lng});
          
          return {
            lat: lat,
            lng: lng,
            name: feature.place_name
          };
        }
        
        return null;
      } catch (error) {
        console.error('Error geocoding search term:', error);
        return null;
      }
    },

    updateUserCoordinates(latitude, longitude) {
      // Update user profile with geocoded coordinates
      if (!latitude || !longitude) return;

      // Skip API call in local-only mode
      if (USE_LOCAL_DATA_ONLY) {
        console.log("Skipping API call to update coordinates (API calls disabled)");
        return;
      }

      axios
        .post("/api/users/profiles/update_me/", {
          latitude: latitude,
          longitude: longitude,
        })
        .then((response) => {
          console.log("Updated user coordinates in profile:", response.data);
        })
        .catch((error) => {
          console.error("Error updating user coordinates:", error);
        });
    },

    loadUserData() {
      // Use sample data if configured to bypass API
      if (USE_LOCAL_DATA_ONLY) {
        console.log("Using sample user profile data (API calls disabled)");

        // Use sample user profile
        this.userData = {
          age: sampleUserProfile.age || "",
          address: sampleUserProfile.address || "",
          diagnosis: sampleUserProfile.diagnosis_display || "",
          otherDiagnosis: sampleUserProfile.other_diagnosis || "",
        };

        // Set user location from sample data if available
        if (sampleUserProfile.latitude && sampleUserProfile.longitude) {
          this.userLocation = {
            latitude: sampleUserProfile.latitude,
            longitude: sampleUserProfile.longitude,
            accuracy: null,
          };

          // Center map and fetch nearby locations
          if (this.map) {
            this.map.flyTo({
              center: [sampleUserProfile.longitude, sampleUserProfile.latitude],
              zoom: 13,
            });
            this.fetchNearbyLocations();
          }
        }

        return;
      }

      // First try to load from the API if API calls are not disabled
      try {
        axios
          .get("/api/users/profiles/me/")
          .then((response) => {
            console.log("Loaded user profile from API:", response.data);

            // Map API response to our userData object
            if (response.data) {
              this.userData = {
                age: response.data.age || "",
                address: response.data.address || "",
                diagnosis: response.data.diagnosis_display || "",
                otherDiagnosis: response.data.other_diagnosis || "",
              };

              // If we have coordinates, update the user location
              if (response.data.latitude && response.data.longitude) {
                this.userLocation = {
                  latitude: response.data.latitude,
                  longitude: response.data.longitude,
                  accuracy: null,
                };

                // Center map and fetch nearby locations
                if (this.map) {
                  this.map.flyTo({
                    center: [response.data.longitude, response.data.latitude],
                    zoom: 13,
                  });
                  this.fetchNearbyLocations();
                }
              }
            }
          })
          .catch((error) => {
            console.warn(
              "Could not load user profile from API, falling back to localStorage:",
              error
            );
            this.loadUserDataFromLocalStorage();
          });
      } catch (error) {
        console.error("Error in loadUserData method:", error);
        this.loadUserDataFromLocalStorage();
      }
    },

    loadUserDataFromLocalStorage() {
      // Fallback: Load user data from localStorage if available
      const savedData = localStorage.getItem("userData");
      if (savedData) {
        try {
          const parsedData = JSON.parse(savedData);
          this.userData = { ...this.userData, ...parsedData };
          console.log("Loaded user data from localStorage:", this.userData);
        } catch (e) {
          console.error("Error loading saved user data from localStorage:", e);
        }
      }
    },

    // Handle window resize events
    // Get a color based on category ID
    getCategoryColor(categoryId) {
      // Default fallback color
      if (!categoryId) return "#3388ff";

      // Predefined colors for different categories
      const colors = {
        1: "#FF5733", // Red-orange
        2: "#33A8FF", // Light blue
        3: "#FF33A8", // Pink
        4: "#A833FF", // Purple
        5: "#33FFA8", // Mint
        6: "#FFD433", // Yellow
        7: "#33FF57", // Green
        8: "#8B4513", // Brown
      };

      // Convert to number and use modulo for any category IDs not explicitly defined
      const id = parseInt(categoryId);
      if (isNaN(id)) return "#3388ff";

      // Return color if defined, or calculate one based on ID
      return colors[id] || `hsl(${(id * 50) % 360}, 70%, 50%)`;
    },

    handleResize() {
      // Avoid excessive resize event firing with debounce
      if (this.resizeTimeout) {
        clearTimeout(this.resizeTimeout);
      }

      this.resizeTimeout = setTimeout(() => {
        if (this.map) {
          // Notify the map that the container size has changed
          this.map.resize();

          // If there's a selected location, re-center the map
          if (this.selectedLocation) {
            try {
              const lat = parseFloat(this.selectedLocation.latitude);
              const lng = parseFloat(this.selectedLocation.longitude);

              if (
                !isNaN(lat) &&
                !isNaN(lng) &&
                lat >= -90 &&
                lat <= 90 &&
                lng >= -180 &&
                lng <= 180
              ) {
                this.map.setCenter([lng, lat]);
              }
            } catch (e) {
              console.error("Error re-centering map after resize:", e);
            }
          }
        }
      }, 300); // Wait 300ms after last resize event
    },
  },
};
</script>

<style scoped>
/* RESET EVERYTHING FOR CLEAN STRUCTURE */
/* Main layout structure */
.map-app {
  display: flex;
  flex-direction: row;
  height: 100vh;
  width: 100%;
  /* overflow: hidden; */
}

/* Sidebar container */
.sidebar-container {
  width: 350px;
  flex: 0 0 350px;
  height: 100vh;
  background-color: #f8f9fa;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  z-index: 5;
}

/* Sidebar content */
.sidebar {
  height: 100%;
  width: 100%;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  -webkit-overflow-scrolling: touch;
}

/* Map container */
.map-container-wrapper {
  flex: 1;
  height: 100vh;
  position: relative;
}

.map-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

/* Filter and results sections */
.filter-group {
  margin-bottom: 15px;
  background-color: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  transition: padding 0.3s ease;
}

.results-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 150px; /* Reduced from 200px */
  max-height: none; /* Let it grow naturally based on content */
  overflow: visible;
  position: relative;
}

.results-title {
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Custom scrollbar styling */
.sidebar::-webkit-scrollbar {
  width: 6px;
}

.sidebar::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 10px;
}

.sidebar::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* Mobile layout - switch to vertical arrangement */
@media (max-width: 768px) {
  .map-app {
    flex-direction: column;
    height: 100vh;
    width: 100%;
  }

  .sidebar-container {
    width: 100%;
    height: 45vh;
    flex: 0 0 45vh;
    order: 2; /* Sidebar goes below on mobile */
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1); /* Shadow at top for visual separation */
  }

  .sidebar {
    padding: 15px;
    height: 100%;
    overflow-y: auto;
  }

  .map-container-wrapper {
    height: 55vh;
    flex: 0 0 55vh;
    order: 1; /* Map goes on top on mobile */
  }

  .map-container {
    height: 100%;
  }
}

/* Add space at bottom of sidebar for mobile scrolling */
.sidebar::after {
  content: "";
  display: block;
  height: 40px;
}

/* Adjust the results section for mobile */
.results-section {
  min-height: 100px; /* Reduced minimum height for mobile */
}

/* Small screen refinements */
@media (max-width: 576px) {
  .sidebar {
    padding: 12px;
  }

  .filter-group {
    padding: 12px;
    margin-bottom: 12px; /* More compact spacing */
  }

  h3 {
    font-size: 1.4rem;
    margin-bottom: 12px;
  }

  /* More space for content, less padding */
  .sidebar > h3.mb-3 {
    margin-bottom: 10px !important;
  }

  .map-container-wrapper {
    height: 50vh;
    flex: 0 0 50vh;
  }

  .sidebar-container {
    height: 50vh;
    flex: 0 0 50vh;
  }
}

/* Fix for iOS Safari viewport issues */
@supports (-webkit-touch-callout: none) {
  .map-app {
    height: -webkit-fill-available;
  }

  .sidebar {
    -webkit-overflow-scrolling: touch;
  }
}
</style>
// TODO: Add more category colors in future releases
