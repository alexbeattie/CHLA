<template>
  <div class="map-app">
    <!-- Sidebar (always on left) -->
    <div class="sidebar-container">
      <div class="sidebar">
        <h3 class="mb-3">Location Finder</h3>

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

          <!-- Category Filter -->
          <div class="mb-2">
            <label class="form-label">Category</label>
            <select v-model="selectedCategory" class="form-select">
              <option value="">All Categories</option>
              <option
                v-for="category in categories"
                :key="category?.id || 'none'"
                :value="category?.id"
                v-if="category && category.id"
              >
                {{ category?.name || "Unknown" }}
              </option>
            </select>
          </div>

          <!-- Radius Filter (when geolocation is available) -->
          <div class="mb-2" v-if="userLocation.latitude && userLocation.longitude">
            <label class="form-label">Distance Radius: {{ radius }}km</label>
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
          <h5 class="results-title">Results ({{ filteredLocations.length }})</h5>

          <!-- Location List -->
          <location-list
            :locations="filteredLocations"
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

// Flag to completely bypass API calls during development
const USE_LOCAL_DATA_ONLY = true;

export default {
  name: "MapView",

  components: {
    UserInfoPanel,
    LocationList,
    LocationDetail,
  },

  data() {
    return {
      // Map data
      map: null,
      markers: [],

      // Locations
      locations: [],
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

    filteredLocations() {
      if (!this.locations.length) return [];

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
    // Update markers when filtered locations change
    filteredLocations: {
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
      console.log(`Updating markers for ${this.filteredLocations.length} locations`);

      // Remove existing markers
      this.markers.forEach((marker) => {
        try {
          marker.remove();
        } catch (e) {
          console.error("Error removing marker:", e);
        }
      });
      this.markers = [];

      // Add markers for filtered locations
      this.filteredLocations.forEach((location) => {
        try {
          // Skip if no coordinates
          if (!location.latitude || !location.longitude) {
            console.warn(
              `Location ${location.id} is missing coordinates, skipping marker`
            );
            return;
          }

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
                `Location ${location.id} has invalid coordinates: ${lat}, ${lng}`
              );
              return;
            }
          } catch (e) {
            console.error(`Error parsing coordinates for location ${location.id}:`, e);
            return;
          }

          // Get rating stars
          const stars = "★".repeat(Math.round(location.rating || 0));
          const emptyStars = "☆".repeat(5 - Math.round(location.rating || 0));

          // Create popup content
          const popupContent = `
            <div style="min-width: 200px; padding: 5px;">
              <h5>${location.name || "Unnamed Location"}</h5>
              <p>${location.address || ""}, ${location.city || ""}</p>
              <p><span style="color: gold;">${stars}${emptyStars}</span></p>
            </div>
          `;

          // Create popup
          const popup = new mapboxgl.Popup({ offset: 25 }).setHTML(popupContent);

          // Create marker with color based on category
          const el = document.createElement("div");
          el.className = "marker";
          el.style.backgroundColor = this.getCategoryColor(location.category);
          el.style.width = "20px";
          el.style.height = "20px";
          el.style.borderRadius = "50%";
          el.style.border = "2px solid white";
          el.style.boxShadow = "0 0 5px rgba(0,0,0,0.3)";

          // Create marker
          const marker = new mapboxgl.Marker(el)
            .setLngLat([lng, lat])
            .setPopup(popup)
            .addTo(this.map);

          // Add click event to marker
          marker.getElement().addEventListener("click", () => {
            this.selectLocation(location);
          });

          // Store marker and location data together
          marker.locationId = location.id;
          this.markers.push(marker);
        } catch (e) {
          console.error(`Error creating marker for location ${location.id}:`, e);
        }
      });

      // Fit map to markers if we have any
      if (this.markers.length > 0) {
        try {
          const bounds = new mapboxgl.LngLatBounds();
          let boundsAdded = false;

          this.filteredLocations.forEach((location) => {
            try {
              const lat = parseFloat(location.latitude);
              const lng = parseFloat(location.longitude);

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
              console.warn(`Error adding location ${location.id} to bounds:`, e);
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
