<template>
  <div class="map-app" :class="{ authenticated: isAuthenticated }">
    <!-- Top Navigation Bar -->
    <nav class="top-navbar" v-show="!showOnboarding">
      <div class="navbar-content">
        <button
          class="mobile-menu-btn d-md-none"
          @click="toggleMobileSidebar"
          :class="{ active: showMobileSidebar }"
        >
          <i class="bi bi-list"></i>
        </button>

        <div class="navbar-brand">
          <span class="kindd-text-logo">KINDD</span>
          <span class="brand-separator d-none d-md-inline">|</span>
          <span class="brand-subtitle d-none d-md-inline">ABA Provider Map</span>
        </div>

        <!-- Regional Center Legend (Horizontal) - Always visible -->
        <div class="navbar-legend d-none d-lg-flex">
          <div class="legend-compact">
            <button class="legend-toggle-btn" @click="toggleRCLegend">
              <i class="bi bi-map-fill"></i>
              <span>Regional Centers</span>
              <i :class="showRCLegend ? 'bi bi-chevron-up' : 'bi bi-chevron-down'"></i>
            </button>
            <div v-if="showRCLegend" class="legend-dropdown">
              <div
                v-for="rc in laRegionalCentersList"
                :key="rc.name"
                class="legend-item-compact"
                :class="{ 'is-active': selectedRegionalCenterName === rc.name }"
                @click="handleRegionalCenterSelect(rc)"
              >
                <div class="color-dot" :style="{ backgroundColor: rc.color }"></div>
                <span class="rc-name-short">{{ rc.abbreviation || rc.name }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="navbar-actions">
          <button class="btn-icon d-md-none" @click="toggleSearch">
            <i class="bi bi-search"></i>
          </button>
          <button class="btn-icon" @click="toggleUserMenu" v-if="isAuthenticated">
            <i class="bi bi-person-circle"></i>
          </button>
        </div>
      </div>
    </nav>

    <!-- Search Bar -->
    <div v-if="!showOnboarding" class="search-bar-wrapper">
      <search-bar
        placeholder="Enter city or ZIP code"
        :show-results-summary="true"
        :auto-focus="false"
        @search="handleNewSearch"
        @clear="handleSearchClear"
      />
    </div>

    <!-- Onboarding Flow -->
    <onboarding-flow
      :showOnboarding="showOnboarding"
      @onboarding-complete="handleOnboardingComplete"
      @onboarding-skipped="handleOnboardingSkipped"
      @location-detected="handleLocationDetected"
      @location-manual="handleLocationManual"
      @regional-center-matched="handleRegionalCenterMatched"
    />

    <!-- Funding Info Modal -->
    <funding-info-panel :showModal="showFundingInfo" @close="toggleFundingInfo" />

    <!-- Mobile Backdrop -->
    <div
      v-if="showMobileSidebar"
      class="mobile-backdrop d-md-none"
      @click="toggleMobileSidebar"
    ></div>

    <!-- Sidebar (always on left) -->
    <div class="sidebar-container" :class="{ 'mobile-open': showMobileSidebar }">
      <!-- Unified Scrollable Content -->
      <div class="sidebar-content">
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
            <p class="chla-tagline">We create hope and build healthier futures</p>
          </div>
        </div>

        <!-- Simple Display Toggle -->
        <div class="display-toggle mb-3">
          <div class="btn-group w-100 d-flex">
            <button
              class="btn flex-grow-1"
              :class="{
                'btn-chla-primary': displayType === 'regionalCenters',
                'btn-chla-outline': displayType !== 'regionalCenters',
              }"
              @click="setDisplayType('regionalCenters')"
            >
              <i class="bi bi-building me-1"></i>
              <span>Regional Centers</span>
            </button>
            <button
              class="btn flex-grow-1"
              :class="{
                'btn-chla-primary': displayType === 'providers',
                'btn-chla-outline': displayType !== 'providers',
              }"
              @click="setDisplayType('providers')"
            >
              <i class="bi bi-hospital me-1"></i>
              <span>Services</span>
            </button>
          </div>
        </div>

        <!-- Profile Summary Component (Collapsible) -->
        <div class="collapsible-section mb-3" v-if="userData.age || userData.diagnosis || displayRegionalCenter">
          <div
            class="collapsible-header"
            @click="toggleSection('profile')"
          >
            <div class="d-flex align-items-center">
              <i class="bi bi-person-circle text-primary me-2"></i>
              <strong>Your Profile</strong>
            </div>
            <i
              class="bi toggle-icon"
              :class="sectionsCollapsed.profile ? 'bi-chevron-down' : 'bi-chevron-up'"
            ></i>
          </div>
          <div class="collapsible-content" v-show="!sectionsCollapsed.profile">
            <profile-summary
              :profile="userData"
              :regional-center="displayRegionalCenter"
              @edit-profile="handleEditProfile"
            />
          </div>
        </div>

        <!-- Simple Search (Collapsible) -->
        <div class="collapsible-section mb-3">
          <div
            class="collapsible-header"
            @click="toggleSection('search')"
          >
            <div class="d-flex align-items-center">
              <i class="bi bi-search text-secondary me-2"></i>
              <strong>Search {{ displayType === "providers" ? "Services" : "Regional Centers" }}</strong>
            </div>
            <i
              class="bi toggle-icon"
              :class="sectionsCollapsed.search ? 'bi-chevron-down' : 'bi-chevron-up'"
            ></i>
          </div>
          <div class="collapsible-content" v-show="!sectionsCollapsed.search">
            <div class="info-card-content">
              <div class="input-group">
                <input
                  type="text"
                  class="form-control"
                  v-model.trim="searchText"
                  :placeholder="
                    displayType === 'providers'
                      ? 'ZIP, address, provider, service...'
                      : 'Search locations...'
                  "
                  @keyup.enter="updateFilteredLocations"
                  @input="debounceSearch"
                  @focus="console.log('Search input focused')"
                  @blur="console.log('Search input blurred')"
                />
                <button
                  v-if="searchText && searchText.trim()"
                  class="btn btn-outline-secondary"
                  type="button"
                  @click="clearSearch"
                  title="Clear search"
                >
                  <i class="bi bi-x"></i>
                </button>
                <button
                  class="btn btn-chla-primary"
                  type="button"
                  @click="updateFilteredLocations"
                  :disabled="loading"
                >
                  <i class="bi bi-search" v-if="!loading"></i>
                  <div class="spinner-border spinner-border-sm" role="status" v-else>
                    <span class="visually-hidden">Searching...</span>
                  </div>
                </button>
              </div>
              <div class="small text-muted mt-2" v-if="displayType === 'providers'">
                <em
                  >Try: ZIP (90210), address, provider name, or service (ABA, speech therapy)</em
                >
              </div>
              <!-- Error Message Display -->
              <div v-if="error" class="alert alert-danger mt-2 mb-0 py-2" role="alert">
                <i class="bi bi-exclamation-triangle me-2"></i>
                {{ error }}
              </div>
            </div>
          </div>
        </div>

        <!-- Location Notice -->
        <!-- Location Notice - Only show if location not detected and no profile -->
        <div 
          class="location-notice mb-3" 
          v-if="!userLocation.detected && !userData.age && !userData.diagnosis"
        >
          <div class="form-control border-info bg-info bg-opacity-10">
            <div class="d-flex align-items-start gap-2">
              <i class="bi bi-info-circle text-info mt-1"></i>
              <div class="flex-grow-1">
                <div class="fw-semibold">Showing All LA County Providers</div>
                <div class="small text-muted mt-1">
                  Set up your profile or search by ZIP code for personalized results
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Map View Options - Contextual based on display type -->
        <div class="info-card-section mb-3" v-if="displayType === 'regionalCenters'">
          <div class="form-control info-card border-dark bg-dark bg-opacity-5">
            <div class="info-card-header mb-2">
              <i class="bi bi-layers text-dark me-2"></i>
              <strong>Map Options</strong>
            </div>
            <div class="info-card-content">
              <div class="btn-group-vertical w-100">
                <button
                  class="btn sidebar-action-btn"
                  @click="toggleLARegionalCenters"
                  :class="showLARegionalCenters ? 'btn-chla-primary' : 'btn-chla-outline'"
                >
                  <i
                    class="bi"
                    :class="showLARegionalCenters ? 'bi-building-fill' : 'bi-building'"
                  ></i>
                  <span class="ms-2">
                    {{ showLARegionalCenters ? "Hide" : "Show" }} LA Centers
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Filter Section (Collapsible) -->
        <div class="collapsible-section mb-3">
          <div
            class="collapsible-header"
            @click="toggleSection('filters')"
          >
            <div class="d-flex align-items-center">
              <i class="bi bi-funnel-fill text-warning me-2"></i>
              <strong>Filters</strong>
            </div>
            <i
              class="bi toggle-icon"
              :class="sectionsCollapsed.filters ? 'bi-chevron-down' : 'bi-chevron-up'"
            ></i>
          </div>
          <div class="collapsible-content" v-show="!sectionsCollapsed.filters">
            <div class="info-card-content">
              <!-- Radius Filter (when geolocation is available) -->
              <div class="mb-3" v-if="userLocation.latitude && userLocation.longitude">
                <div class="d-flex justify-content-between align-items-center mb-1">
                  <label class="form-label mb-0 small">
                    <span v-if="searchText && searchText.trim()">
                      Search: "<strong>{{ searchText }}</strong
                      >"
                    </span>
                    <span v-else>
                      Distance Radius: <strong>{{ radius }} miles</strong>
                    </span>
                  </label>
                  <span class="badge bg-info small"
                    >{{ countLocationsInRadius }} found</span
                  >
                </div>
                <input
                  type="range"
                  v-model.number="radius"
                  class="form-range"
                  min="5"
                  max="50"
                  step="5"
                  @change="onRadiusChange"
                />
                <div class="d-flex justify-content-between small text-muted">
                  <span>5 miles</span>
                  <span>50 miles</span>
                </div>
              </div>

              <!-- Filter Panel -->
              <filter-panel
                v-if="filterStore"
                :show-favorites="false"
                :show-summary="true"
                :manual-apply="false"
                @filter-change="handleFilterChange"
                @reset="handleFilterReset"
              />
            </div>
          </div>
        </div>

        <!-- Results Section with Sticky Header -->
        <div class="results-section">
          <!-- Sticky Header -->
          <div class="results-sticky-header">
            <div class="info-card-header">
                <i class="bi bi-list-ul text-success me-2"></i>
                <strong>
                  {{
                    displayType === "locations"
                      ? "Locations"
                      : displayType === "regionalCenters"
                      ? "Regional Centers"
                      : "Providers"
                  }}
                </strong>
                <span class="badge bg-success ms-2">
                  {{
                    displayType === "locations"
                      ? filteredLocations.length
                      : displayType === "regionalCenters"
                      ? filteredRegionalCenters.length
                      : filteredProviders.length
                  }}
                </span>
              </div>
          </div>

          <!-- Scrollable Results Content -->
          <div class="results-content">
            <!-- Regional Centers Toggle List -->
          <div
            v-if="displayType === 'regionalCenters' && showLARegionalCenters"
            class="info-card-section mb-3"
          >
            <div class="form-control info-card border-info bg-info bg-opacity-5">
              <div class="info-card-header mb-2">
                <i class="bi bi-building-fill text-info me-2"></i>
                <strong>LA Regional Centers</strong>
              </div>
              <div class="info-card-content">
                <div class="regional-center-toggles">
                  <div
                    v-for="center in laRegionalCentersList"
                    :key="center.name"
                    class="form-check mb-2"
                  >
                    <input
                      class="form-check-input"
                      type="checkbox"
                      :id="`rc-toggle-${center.name.replace(/\s+/g, '-')}`"
                      :checked="selectedRegionalCenters[center.name] !== false"
                      @change="toggleCenterSelection(center.name)"
                    />
                    <label
                      class="form-check-label d-flex align-items-center"
                      :for="`rc-toggle-${center.name.replace(/\s+/g, '-')}`"
                    >
                      <span
                        class="color-indicator me-2"
                        :style="`background-color: ${center.color}; width: 12px; height: 12px; border-radius: 50%; display: inline-block;`"
                      ></span>
                      <span class="flex-grow-1">{{ center.name }}</span>
                    </label>
                  </div>
                </div>

                <!-- Nearest Regional Centers -->
                <div
                  v-if="nearestRegionalCenters.length > 0"
                  class="mt-3 pt-3"
                  style="border-top: 1px solid rgba(0, 0, 0, 0.08)"
                >
                  <div class="info-card-subtitle mb-2">
                    <i class="bi bi-geo-alt-fill text-primary me-1"></i>
                    <span class="text-muted small">Nearest Regional Centers</span>
                  </div>
                  <div class="nearest-centers-list">
                    <div
                      v-for="(center, index) in nearestRegionalCenters"
                      :key="`nearest-${center.name}-${index}`"
                      class="nearest-center-item"
                    >
                      <div class="d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center">
                          <span
                            class="color-indicator me-2"
                            :style="`background-color: ${center.color}; width: 8px; height: 8px; border-radius: 50%; display: inline-block;`"
                          ></span>
                          <span class="small">{{ center.name }}</span>
                        </div>
                        <span class="badge bg-secondary small"
                          >{{ center.distance }} mi</span
                        >
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Provider List -->
          <provider-list
            v-if="providerStore"
            :providers="providerStore.providers"
            :selected-id="providerStore.selectedProviderId"
            :loading="providerStore.loading"
            :show-distance="true"
            :auto-scroll-to-selected="true"
            @provider-select="handleProviderSelect"
            @get-directions="handleGetDirections"
          />
          </div>
          <!-- End results-content -->
        </div>
        <!-- End results-section -->
      </div>
      <!-- End sidebar-content -->
    </div>
    <!-- End Sidebar Container -->

    <!-- Map Container -->
    <div class="map-container-wrapper" :class="{ 'with-search': showMobileSearch }">
      <!-- Map Canvas -->
      <map-canvas
        :mapbox-token="mapboxAccessToken"
        :center="LA_COUNTY_CENTER"
        :zoom="9.5"
        class="map-container"
        @map-ready="handleMapReady"
        @marker-click="handleMarkerClick"
        @viewport-change="handleViewportChange"
      />

      <!-- Provider Details Overlay -->
      <provider-details
        v-if="providerStore && providerStore.selectedProvider"
        :provider="providerStore.selectedProvider"
        :is-visible="true"
        :show-directions="true"
        class="provider-details-overlay"
        @close="handleDetailsClose"
        @get-directions="handleGetDirections"
      />

      <!-- Directions Panel -->
      <directions-panel
        :visible="showDirections"
        :directions="currentDirections"
        :destination="directionsDestination"
        :loading="directionsLoading"
        :error="directionsError"
        @close="closeDirections"
        @retry="retryDirections"
      />

    </div>
  </div>
</template>

<script>
import axios from "axios";
import mapboxgl from "mapbox-gl";
import FundingInfoPanel from "@/components/FundingInfoPanel.vue";
import OnboardingFlow from "@/components/OnboardingFlow.vue";
import { authService } from "@/services/auth.js";

// Extracted components
import MapCanvas from "@/components/map/MapCanvas.vue";
import SearchBar from "@/components/map/SearchBar.vue";
import ProviderList from "@/components/map/ProviderList.vue";
import ProviderCard from "@/components/map/ProviderCard.vue";
import ProviderDetails from "@/components/map/ProviderDetails.vue";
import DirectionsPanel from "@/components/map/DirectionsPanel.vue";
import FilterPanel from "@/components/map/FilterPanel.vue";
import SidebarPanel from "@/components/SidebarPanel.vue";
import ProfileSummary from "@/components/ProfileSummary.vue";

// Pinia stores
import { useProviderStore } from "@/stores/providerStore";
import { useMapStore } from "@/stores/mapStore";
import { useFilterStore } from "@/stores/filterStore";

// Services
import { getDrivingDirections, getDrivingDistance } from "@/services/mapboxDirections";

// Composables
import { useGeolocation } from "@/composables/useGeolocation";
import { useRegionalCenterData } from "@/composables/useRegionalCenterData";

// Constants and utilities
import {
  LA_COUNTY_CENTER,
  LA_COUNTY_BOUNDS,
  getRegionalCentersList,
  getRegionalCenterColor,
  getRegionalCenterCoordinates,
  getRegionalCenterMapboxCoords
} from "@/constants/regionalCenters";
import {
  calculateDistance,
  extractZipCode,
  isValidZipCode,
  normalizeCoordinates,
  toLngLatArray
} from "@/utils/map/coordinates";

// Flag to use actual API data instead of sample data - set to false to query the database
// Import LA Regional Centers GeoJSON overlay
// Removed hardcoded GeoJSON import - now using API endpoint

const USE_LOCAL_DATA_ONLY = false;

// Static asset URL for LA ZIP codes GeoJSON
const LA_ZIP_CODES_URL = new URL(
  "../assets/all-zip-codes-los-angeles.geojson",
  import.meta.url
).href;

export default {
  name: "MapView",

  components: {
    FundingInfoPanel,
    OnboardingFlow,
    MapCanvas,
    SearchBar,
    ProviderList,
    ProviderCard,
    ProviderDetails,
    DirectionsPanel,
    FilterPanel,
    SidebarPanel,
    ProfileSummary,
  },

  data() {
    return {
      // Week 5B: New components now enabled by default
      useNewComponents: true,

      // Store instances (initialized in created())
      providerStore: null,
      mapStore: null,
      filterStore: null,

      // Composables (initialized in created())
      geolocation: null,
      regionalCenterData: null,

      // Constants
      LA_COUNTY_CENTER,

      // Mapbox token for MapCanvas component
      mapboxAccessToken:
        import.meta.env.VITE_MAPBOX_TOKEN ||
        "pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg",

      // Modal visibility
      showFundingInfo: false,
      showOnboarding: false,
      showMobileSidebar: false,
      showMobileSearch: false,
      showUserMenu: false,

      // Directions panel state
      showDirections: false,
      currentDirections: null,
      directionsDestination: null,
      directionsLoading: false,
      directionsError: null,
      directionsRoute: null,
      lastDirectionsProvider: null,

      // Display type
      displayType: "providers", // 'regionalCenters' or 'providers'

      // Regional Center Legend in navbar
      showRCLegend: false,
      selectedRegionalCenterName: null, // Currently selected RC from legend

      // Collapsible sections state
      sectionsCollapsed: {
        profile: false,
        search: false,
        filters: false,
        mapOptions: false,
      },

      // Service areas
      showServiceAreas: false,
      pinServiceAreas: false,
      serviceAreas: null,
      serviceAreasLoaded: false,
      focusLACounty: false,
      matchedRegionalCenter: null,
      laZipInput: "",
      laZipError: "",

      // User's regional center (fetched from API)
      userRegionalCenter: null,

      // LA Regional Centers overlay
      showLARegionalCenters: false,
      laRegionalCentersData: null,
      // ZIP-view-only mode to simplify the UI while we focus on ZIP colors
      zipViewOnly: false,
      // In-memory GeoJSON used for live recoloring of ZIPs
      coloredZipsData: null,
      // Zip → Regional Center lookup and hover state
      zipToCenter: {},
      zipHoverId: null,
      // Sidebar checkboxes state: center name -> boolean
      selectedRegionalCenters: {},

      // Filter options
      filterOptions: {
        acceptsInsurance: false,
        acceptsRegionalCenter: false,
        matchesDiagnosis: false,
        matchesAge: false,
        diagnoses: [],
        therapies: [],
      },
      diagnosisOptions: [
        "Global Development Delay",
        "Autism Spectrum Disorder",
        "Intellectual Disability",
        "Speech and Language Disorder",
        "ADHD",
      ],
      therapyOptions: [
        "ABA therapy",
        "Speech therapy",
        "Occupational therapy",
        "Physical therapy",
        "Feeding therapy",
        "Parent child interaction therapy/parent training behavior management",
      ],

      // Search debounce
      searchDebounce: null,

      // Map data
      map: null,
      // REMOVED: markers: [] - MapCanvas manages all markers now
      // REMOVED: providers: [] - use providerStore.providers instead

      // Locations
      locations: [],
      regionalCenters: [], // Array to store regional centers (TODO: move to store)

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
      isAdjustingRadius: false, // flag to track radius slider adjustments
      isMapMoving: false, // flag to prevent conflicting map movements

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

      // Map movement tracking
      isMapMoving: false,
    };
  },

  computed: {
    isAuthenticated() {
      return authService.isAuthenticated();
    },
    laRegionalCentersForLegend() {
      const feats = this.laRegionalCentersData?.features || [];
      return feats.filter(
        (f) => f?.properties?.name && f.properties.name !== "Los Angeles County"
      );
    },
    hasUserData() {
      // Check if user has entered any meaningful data
      return (
        (this.userData.age && this.userData.age !== "") ||
        (this.userData.address && this.userData.address !== "") ||
        (this.userData.diagnosis && this.userData.diagnosis !== "")
      );
    },

    // Providers from store (proxy to avoid breaking existing code)
    providers() {
      return this.providerStore?.providers || [];
    },

    // User's matched Regional Center name (from providerStore or userRegionalCenter)
    userRegionalCenterName() {
      // First try providerStore (from ZIP search)
      if (this.providerStore && this.providerStore.regionalCenterInfo) {
        return this.providerStore.regionalCenterInfo.name;
      }
      // Fallback to userRegionalCenter object
      if (this.userRegionalCenter && this.userRegionalCenter.name) {
        return this.userRegionalCenter.name;
      }
      return null;
    },

    // Full regional center info (same logic as legend for sidebar display)
    displayRegionalCenter() {
      // First try providerStore (from ZIP search) - same as legend
      if (this.providerStore && this.providerStore.regionalCenterInfo) {
        return this.providerStore.regionalCenterInfo;
      }
      // Fallback to userRegionalCenter object
      if (this.userRegionalCenter && this.userRegionalCenter.name) {
        return this.userRegionalCenter;
      }
      // Return placeholder if no data
      return {
        name: "Detecting location...",
        address: null,
        phone: null,
        website: null
      };
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
      if (!this.providers.length) {
        return [];
      }

      // Since we're using API-level filtering in fetchProviders(),
      // the this.providers array already contains the correctly filtered results
      // No need for additional client-side filtering
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

    // LA Regional Centers list with colors and abbreviations
    laRegionalCentersList() {
      return getRegionalCentersList();
    },

    // Calculate nearest regional centers based on user location
    nearestRegionalCenters() {
      if (!this.userLocation.latitude || !this.userLocation.longitude) {
        return [];
      }

      // Calculate distances and sort
      const centersWithDistance = this.laRegionalCentersList
        .map((center) => {
          const coords = getRegionalCenterCoordinates(center.name);
          if (!coords) return null;

          // Calculate distance using Haversine formula
          const R = 3959; // Earth's radius in miles
          const lat1 = (this.userLocation.latitude * Math.PI) / 180;
          const lat2 = (coords.lat * Math.PI) / 180;
          const deltaLat = ((coords.lat - this.userLocation.latitude) * Math.PI) / 180;
          const deltaLng = ((coords.lng - this.userLocation.longitude) * Math.PI) / 180;

          const a =
            Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
            Math.cos(lat1) *
              Math.cos(lat2) *
              Math.sin(deltaLng / 2) *
              Math.sin(deltaLng / 2);
          const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
          const distance = R * c;

          return {
            ...center,
            distance: distance.toFixed(1),
          };
        })
        .filter((center) => center !== null)
        .sort((a, b) => parseFloat(a.distance) - parseFloat(b.distance))
        .slice(0, 3); // Show top 3 nearest

      return centersWithDistance;
    },
  },

  async created() {
    console.log("[MapView] Component created");

    // Initialize Pinia stores
    this.providerStore = useProviderStore();
    this.mapStore = useMapStore();
    this.filterStore = useFilterStore();

    // Initialize composables
    const geolocation = useGeolocation();
    const regionalCenterData = useRegionalCenterData();

    // Store composables for use in methods
    this.geolocation = geolocation;
    this.regionalCenterData = regionalCenterData;

    console.log("[MapView] Stores and composables initialized");

    // Initialize with empty arrays to prevent null reference errors
    this.categories = [];
    this.locations = [];
    this.markers = [];
    if (this.providerStore) {
      this.providerStore.providers = [];
    }

    // Fetch regional centers data
    await this.regionalCenterData.fetchRegionalCenters();

    // Check if onboarding should be shown
    this.checkOnboardingStatus();

    // Load saved user data if available (including regional center)
    await this.loadUserData();

    // Location detection is now handled in mounted() hook through getUserZipCode()
    // which is called when loading initial providers. This avoids duplicate geolocation calls.
    // The detectUserLocation() method is still available for manual location refresh.
  },

  mounted() {
    console.log("[MapView] Component mounted");

    // Week 5B: Using new components, no need to call initMap()
    // MapCanvas component handles map initialization
    this.$nextTick(async () => {
      try {
        console.log("[MapView] Starting initialization with new components");

        // Only load data if not showing onboarding
        if (!this.showOnboarding) {
          console.log("[MapView] Loading initial data...");

          // Load regional centers if needed
          if (!Array.isArray(this.regionalCenters) || this.regionalCenters.length === 0) {
            await this.regionalCenterData.fetchRegionalCenters();
          }

          // Load initial providers based on user location
          if (this.providerStore && this.providerStore.providers.length === 0) {
            console.log("[MapView] Loading initial providers...");

            try {
              // Try to detect user's ZIP first
              const userZipCode = await this.getUserZipCode();
              
              if (userZipCode) {
                // Use user's actual ZIP
                console.log(`[MapView] Loading providers for user's ZIP: ${userZipCode}`);
                await this.providerStore.searchByZipCode(userZipCode);
                // Update address with user's ZIP
                this.userData.address = `Los Angeles, CA ${userZipCode}`;
                await this.findUserRegionalCenter();
                
                // Geocode ZIP to enable radius filtering
                await this.ensureZipCodeCoordinates(userZipCode);
              } else {
                // Fall back to default LA County ZIP if detection fails
                const defaultZip = "90001";
                console.log(`[MapView] No user location detected, using default ZIP: ${defaultZip}`);
                await this.providerStore.searchByZipCode(defaultZip);
                // Geocode default ZIP too
                await this.ensureZipCodeCoordinates(defaultZip);
              }
              
              console.log(`[MapView] Loaded ${this.providerStore.providers.length} providers`);
            } catch (error) {
              console.error("[MapView] Error loading initial providers:", error);
              // Fallback to comprehensive search
              await this.providerStore.searchByLocation(LA_COUNTY_CENTER.lat, LA_COUNTY_CENTER.lng, 25);
            }
          }

          console.log("[MapView] Initialization complete with new components!");
          // Stop initial loading spinner
          this.loading = false;
        } else {
          console.log("[MapView] Onboarding showing, skipping data load");
          this.loading = false;
        }
      } catch (e) {
        console.error("[MapView] Error during initialization:", e);
        this.loading = false;
      }
    });
  },

  beforeUnmount() {
    // Cleanup LA ZIP codes overlay on unmount
    try {
      this.removeLAZipCodesFromMap();
    } catch (e) {
      // noop
    }
  },

  methods: {
    // ============================================
    // UI INTERACTION METHODS
    // ============================================

    /**
     * Toggle collapse state for sidebar sections
     */
    toggleSection(section) {
      this.sectionsCollapsed[section] = !this.sectionsCollapsed[section];
    },

    /**
     * Toggle Regional Center Legend in navbar
     */
    toggleRCLegend() {
      this.showRCLegend = !this.showRCLegend;
    },

    // ============================================
    // WEEK 5: NEW COMPONENT ORCHESTRATION METHODS
    // ============================================

    /**
     * Handle search from new SearchBar component
     */
    async handleNewSearch(searchData) {
      console.log("[MapView] New search handler", searchData);

      // Delegate search to provider store
      this.providerStore.search(searchData.query, searchData.type);

      // Center map on search location if provided
      if (searchData.location) {
        this.mapStore.centerOn(searchData.location);
      }

      // Update user address and regional center when searching by location
      if (searchData.query && searchData.type === 'location') {
        this.userData.address = searchData.query;
        console.log("📍 Search triggered address update, finding regional center...");
        await this.findUserRegionalCenter();
      }
    },

    /**
     * Handle search clear from SearchBar
     */
    handleSearchClear() {
      console.log("[MapView] Search cleared");
      this.providerStore.clearSearch();
    },

    /**
     * Handle filter changes from FilterPanel
     */
    async handleFilterChange(filters) {
      console.log("[MapView] Filter change", filters);

      // Filters are already applied to store by FilterPanel
      // Now re-fetch providers with the updated filters
      try {
        const filterParams = this.filterStore.buildFilterParams();
        console.log("[MapView] Re-fetching providers with filters:", filterParams);

        // If we have a user location/ZIP, re-search with filters
        if (this.userData.address || this.userLocation?.latitude) {
          if (this.userData.address) {
            // Re-search by ZIP with filters
            const zipMatch = this.userData.address.match(/\d{5}/);
            if (zipMatch) {
              // Geocode ZIP to get coordinates for radius filtering
              await this.ensureZipCodeCoordinates(zipMatch[0]);
              
              // Use location-based search if we have coordinates, otherwise ZIP search
              if (this.userLocation?.latitude) {
                await this.providerStore.searchByLocation(
                  this.userLocation.latitude,
                  this.userLocation.longitude,
                  this.radius || 25,
                  filterParams
                );
              } else {
                await this.providerStore.searchByZipCode(zipMatch[0], filterParams);
              }
            }
          } else if (this.userLocation?.latitude) {
            // Re-search by location with filters (use current radius)
            await this.providerStore.searchByLocation(
              this.userLocation.latitude,
              this.userLocation.longitude,
              this.radius || 25, // Use current radius
              filterParams
            );
          }

          // MapCanvas watches providerStore and updates markers automatically
          console.log(`✅ Filters applied: ${this.providerStore.providers.length} providers found`);
        }
      } catch (error) {
        console.error("[MapView] Error re-fetching providers:", error);
      }
    },

    /**
     * Handle filter reset from FilterPanel
     */
    async handleFilterReset() {
      console.log("[MapView] Filters reset");
      this.filterStore.resetFilters();

      // Re-fetch providers without filters
      try {
        if (this.userData.address) {
          const zipMatch = this.userData.address.match(/\d{5}/);
          if (zipMatch) {
            await this.providerStore.searchByZipCode(zipMatch[0]);
            // MapCanvas handles marker updates automatically
            console.log(`✅ Filters reset: ${this.providerStore.providers.length} providers shown`);
          }
        } else if (this.userLocation?.latitude) {
          await this.providerStore.searchByLocation(
            this.userLocation.latitude,
            this.userLocation.longitude,
            this.radius || 25
          );
          // MapCanvas handles marker updates automatically
          console.log(`✅ Filters reset: ${this.providerStore.providers.length} providers shown`);
        }
      } catch (error) {
        console.error("[MapView] Error re-fetching providers after reset:", error);
      }
    },

    /**
     * Handle provider selection from ProviderList
     */
    handleProviderSelect(providerId) {
      console.log("[MapView] Provider selected", providerId);

      // Update provider store selection
      this.providerStore.selectProvider(providerId);

      // Center map on selected provider with smooth animation
      const provider = this.providerStore.providers.find(p => p.id === providerId);
      if (provider && provider.latitude && provider.longitude && this.mapInstance) {
        // Use easeTo for smooth, gentle animation instead of jarring snap
        this.mapInstance.easeTo({
          center: [parseFloat(provider.longitude), parseFloat(provider.latitude)],
          zoom: 14,
          duration: 2000,  // 2 second smooth animation
          easing(t) {
            // Smooth bezier ease-in-out curve
            return t * t * (3.0 - 2.0 * t);
          }
        });
      }
    },

    /**
     * Handle map ready event from MapCanvas
     */
    handleMapReady(mapInstance) {
      console.log("[MapView] Map ready from new MapCanvas");
      this.mapInstance = mapInstance;
      this.map = mapInstance; // Also set this.map for compatibility with old methods

      // DON'T auto-fit to providers - keep the LA County view
      // Users should ALWAYS see LA County properly centered and zoomed
      // They can use "Focus on LA County" button if needed

      // Show LA Regional Centers polygons by default
      if (!this.showLARegionalCenters) {
        this.toggleLARegionalCenters();
      }
    },

    /**
     * Handle marker click from MapCanvas
     */
    handleMarkerClick(provider) {
      console.log("[MapView] Marker clicked", provider.id);
      this.handleProviderSelect(provider.id);
    },

    /**
     * Handle viewport change from MapCanvas
     */
    handleViewportChange(viewport) {
      console.log("[MapView] Viewport changed", viewport);
      // Update map store with new viewport
      this.mapStore.setViewport(viewport);
    },

    /**
     * Handle details panel close from ProviderDetails
     */
    handleDetailsClose() {
      console.log("[MapView] Details closed");
      this.providerStore.clearSelection();
    },

    /**
     * Handle get directions from ProviderDetails
     */
    async handleGetDirections(data) {
      console.log("🗺️ [MapView] Getting directions to:", data);
      console.log("🗺️ [MapView] User location:", this.userLocation);
      console.log("🗺️ [MapView] MapStore location:", this.mapStore?.userLocation);

      // Clear existing route from map (but keep panel open)
      this.removeRouteFromMap();
      this.currentDirections = null;
      this.directionsError = null;

      // Small delay to ensure route is removed from map before drawing new one
      await new Promise(resolve => setTimeout(resolve, 100));

      // Extract provider from data (could be direct provider object or coordinates object)
      const provider = data.provider || data;

      // Validate provider has coordinates
      const providerLat = provider.latitude || data.coordinates?.lat;
      const providerLng = provider.longitude || data.coordinates?.lng;

      console.log("🗺️ [MapView] Provider coordinates:", { providerLat, providerLng });

      if (!providerLat || !providerLng) {
        console.error("🗺️ [MapView] Provider location not available");
        this.directionsError = "Provider location not available";
        this.showDirections = true;
        return;
      }

      // Determine origin for directions
      // Origin should ALWAYS be the user's location (GPS or search location)
      // NEVER use map center as it changes when panning/zooming
      let originLat, originLng, originName;

      console.log("🗺️ [MapView] Determining origin for directions...");
      console.log("🗺️ [MapView] - GPS detected:", this.userLocation.detected);
      console.log("🗺️ [MapView] - GPS coords:", this.userLocation.latitude, this.userLocation.longitude);
      console.log("🗺️ [MapView] - MapStore coords:", this.mapStore?.userLocation);

      // First priority: GPS location if detected
      if (this.userLocation.detected && this.userLocation.latitude && this.userLocation.longitude) {
        originLat = this.userLocation.latitude;
        originLng = this.userLocation.longitude;
        originName = "Your Location (GPS)";
        console.log("✅ [MapView] Using GPS location as origin:", { lat: originLat, lng: originLng });
      }
      // Second priority: mapStore user location (from geocoding/ZIP search - the blue marker)
      else if (this.mapStore?.userLocation?.lat && this.mapStore?.userLocation?.lng) {
        originLat = this.mapStore.userLocation.lat;
        originLng = this.mapStore.userLocation.lng;
        originName = "Your Search Location";
        console.log("✅ [MapView] Using mapStore location (blue marker) as origin:", { lat: originLat, lng: originLng });
      }
      // No valid user location available
      else {
        console.error("❌ [MapView] No user location available for directions");
        this.directionsError = "Please enter a ZIP code or allow location access to get directions.";
        this.showDirections = true;
        return;
      }

      console.log("🗺️ [MapView] ===== ROUTE REQUEST =====");
      console.log("🗺️ [MapView] FROM:", originName, [originLng, originLat]);
      console.log("🗺️ [MapView] TO:", provider.name, [providerLng, providerLat]);
      console.log("🗺️ [MapView] =======================");

      // Show loading state
      this.showDirections = true;
      this.directionsLoading = true;
      this.directionsError = null;

      // Set destination info
      this.directionsDestination = {
        name: provider.name || "Selected Location",
        address: this.formatProviderAddress(provider),
      };

      // Store provider for retry
      this.lastDirectionsProvider = provider;

      try {
        // Fetch directions
        console.log("🗺️ [MapView] Calling getDrivingDirections...");
        const directions = await getDrivingDirections(
          [originLng, originLat],
          [providerLng, providerLat]
        );

        console.log("🗺️ [MapView] Received directions:", directions);

        this.currentDirections = directions;
        this.directionsRoute = directions.route;

        console.log("🗺️ [MapView] Drawing route on map...");
        // Draw route on map
        this.drawRouteOnMap(directions.route);

        console.log("🗺️ [MapView] Fitting map to route...");
        // Fit map to show entire route
        this.fitMapToRoute(directions.route);

        console.log("🗺️ [MapView] Directions complete!");
      } catch (error) {
        console.error("🗺️ [MapView] Error fetching directions:", error);
        this.directionsError = "Could not calculate route. Please try again.";
      } finally {
        this.directionsLoading = false;
        console.log("🗺️ [MapView] directionsLoading set to false");
      }
    },

    /**
     * Close directions panel
     */
    closeDirections() {
      this.showDirections = false;
      this.currentDirections = null;
      this.directionsDestination = null;
      this.directionsError = null;

      // Remove route from map
      this.removeRouteFromMap();
    },

    /**
     * Retry getting directions
     */
    retryDirections() {
      if (this.lastDirectionsProvider) {
        this.handleGetDirections(this.lastDirectionsProvider);
      }
    },

    /**
     * Format provider address for display
     */
    formatProviderAddress(provider) {
      if (!provider.address) return "";

      let addressObj = provider.address;

      // If address is a string that looks like JSON, try to parse it
      if (typeof addressObj === "string") {
        try {
          // Check if it looks like JSON
          if (addressObj.trim().startsWith("{")) {
            addressObj = JSON.parse(addressObj);
          } else {
            // It's already a formatted string
            return addressObj;
          }
        } catch (e) {
          // Not valid JSON, return as is
          return addressObj;
        }
      }

      // If it's an object, format it
      if (typeof addressObj === "object" && addressObj !== null) {
        const parts = [];
        if (addressObj.street) parts.push(addressObj.street);
        if (addressObj.city) parts.push(addressObj.city);
        if (addressObj.state) parts.push(addressObj.state);
        if (addressObj.zip) parts.push(addressObj.zip);
        return parts.join(", ");
      }

      return String(addressObj);
    },

    /**
     * Draw route on map
     */
    drawRouteOnMap(route) {
      if (!this.mapInstance) return;

      // Remove existing route if any
      this.removeRouteFromMap();

      // Add route source
      this.mapInstance.addSource("route", {
        type: "geojson",
        data: {
          type: "Feature",
          properties: {},
          geometry: route.geometry,
        },
      });

      // Add route layer (background/outline)
      this.mapInstance.addLayer({
        id: "route-outline",
        type: "line",
        source: "route",
        layout: {
          "line-join": "round",
          "line-cap": "round",
        },
        paint: {
          "line-color": "#1d4ed8",
          "line-width": 8,
          "line-opacity": 0.3,
        },
      });

      // Add route layer (main line)
      this.mapInstance.addLayer({
        id: "route",
        type: "line",
        source: "route",
        layout: {
          "line-join": "round",
          "line-cap": "round",
        },
        paint: {
          "line-color": "#2563eb",
          "line-width": 5,
        },
      });
    },

    /**
     * Remove route from map
     */
    removeRouteFromMap() {
      if (!this.mapInstance) return;

      if (this.mapInstance.getLayer("route")) {
        this.mapInstance.removeLayer("route");
      }
      if (this.mapInstance.getLayer("route-outline")) {
        this.mapInstance.removeLayer("route-outline");
      }
      if (this.mapInstance.getSource("route")) {
        this.mapInstance.removeSource("route");
      }
    },

    /**
     * Fit map to show entire route
     */
    fitMapToRoute(route) {
      if (!this.mapInstance || !route.geometry) return;

      const coordinates = route.geometry.coordinates;

      // Create bounds
      const bounds = coordinates.reduce(
        (bounds, coord) => {
          return bounds.extend(coord);
        },
        new mapboxgl.LngLatBounds(coordinates[0], coordinates[0])
      );

      // Fit map to bounds
      this.mapInstance.fitBounds(bounds, {
        padding: { top: 100, bottom: 100, left: 100, right: 500 }, // Account for directions panel
        duration: 1000,
      });
    },

    /**
     * Fit map bounds to show all providers
     * (Only use this when explicitly requested by user)
     */
    fitMapToProviders() {
      if (!this.mapInstance || !this.providerStore) return;

      const providers = this.providerStore.providersWithCoordinates;
      if (providers.length === 0) return;

      // Create bounds from all provider coordinates
      const bounds = new mapboxgl.LngLatBounds();
      providers.forEach(provider => {
        bounds.extend([provider.longitude, provider.latitude]);
      });

      // Fit map to bounds with padding
      // IMPORTANT: minZoom prevents zooming out too far for bad UX
      this.mapInstance.fitBounds(bounds, {
        padding: 80,
        maxZoom: 12,
        minZoom: 8.5,  // Never zoom out past this (prevents showing entire states)
        duration: 1000  // Smooth animation
      });
    },

    /**
     * Get user's ZIP code from browser geolocation
     * Returns ZIP code string or null
     */
    async getUserZipCode() {
      console.log("[MapView] Attempting to get user's ZIP code from geolocation...");

      // Check if geolocation is available
      if (!navigator.geolocation) {
        console.warn("[MapView] Geolocation not supported");
        return null;
      }

      try {
        // Get user's position
        const position = await new Promise((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject, {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 300000, // 5 minutes
          });
        });

        const { latitude, longitude } = position.coords;
        console.log(`[MapView] Got geolocation: ${latitude}, ${longitude}`);

        // Store user location
        this.userLocation = {
          latitude,
          longitude,
          accuracy: position.coords.accuracy,
          detected: true,
          error: null,
        };

        // Update mapStore so the marker appears
        if (this.mapStore) {
          this.mapStore.setUserLocation(
            { lat: latitude, lng: longitude },
            position.coords.accuracy
          );
        }

        // Reverse geocode to get ZIP code
        const response = await fetch(
          `https://api.mapbox.com/geocoding/v5/mapbox.places/${longitude},${latitude}.json?access_token=${this.mapboxAccessToken}&types=postcode`
        );
        const data = await response.json();

        if (data.features && data.features.length > 0) {
          const zipCode = data.features[0].text;
          console.log(`[MapView] Found ZIP code: ${zipCode}`);

          // Update user data
          this.userData.address = data.features[0].place_name || zipCode;

          return zipCode;
        }

        console.warn("[MapView] No ZIP code found in geocoding response");
        return null;

      } catch (error) {
        console.warn("[MapView] Error getting user ZIP code:", error.message);
        return null;
      }
    },

    /**
     * Handle Regional Center selection from legend
     */
    handleRegionalCenterSelect(rc) {
      console.log("[MapView] Regional Center selected from legend:", rc.name);

      // Toggle selection
      if (this.selectedRegionalCenterName === rc.name) {
        // Deselect if clicking the same one
        this.selectedRegionalCenterName = null;
      } else {
        // Select this regional center
        this.selectedRegionalCenterName = rc.name;

        // Zoom to the regional center
        this.zoomToRegionalCenter(rc.name);

        // Update the regional center visibility if needed
        if (this.showLARegionalCenters) {
          // Ensure this regional center is visible
          if (!(rc.name in this.selectedRegionalCenters)) {
            this.$set
              ? this.$set(this.selectedRegionalCenters, rc.name, true)
              : (this.selectedRegionalCenters[rc.name] = true);
          }
          this.updateRegionalCenterVisibility(rc.name);
          this.updateLAZipCenterVisibility();
        }
      }
    },

    // ============================================
    // EXISTING METHODS (unchanged)
    // ============================================

    toggleCenterSelection(name) {
      if (!(name in this.selectedRegionalCenters)) {
        this.$set
          ? this.$set(this.selectedRegionalCenters, name, true)
          : (this.selectedRegionalCenters[name] = true);
      } else {
        this.selectedRegionalCenters[name] = !this.selectedRegionalCenters[name];
      }

      // Update visibility of the regional center polygon
      this.updateRegionalCenterVisibility(name);

      // Update markers if needed
      // MapCanvas handles marker updates automatically

      // Zoom to the regional center when toggling it on
      if (this.selectedRegionalCenters[name] !== false) {
        this.zoomToRegionalCenter(name);
      }
    },

    zoomToRegionalCenter(name) {
      const coords = getRegionalCenterMapboxCoords(name);
      if (coords && this.map && !this.isMapMoving) {
        this.isMapMoving = true;
        this.map.flyTo({
          center: coords,
          zoom: 12,
          essential: true,
          duration: 1500,
        });
        setTimeout(() => {
          this.isMapMoving = false;
        }, 1600);
      }
    },

    // Update visibility of a specific regional center on the map
    updateRegionalCenterVisibility(centerName) {
      if (!this.map || !this.showLARegionalCenters) return;

      // Get the visibility state
      const isVisible = this.selectedRegionalCenters[centerName] !== false;

      // Update the filter on the regional center layers
      const currentFilter = this.map.getFilter("rc-static-fill");
      if (currentFilter) {
        // Update the visibility by modifying the paint opacity
        // This is a simpler approach than complex filters
        const centerColor = getRegionalCenterColor(centerName);

        // For now, we'll just log the change - full implementation would require
        // more complex filter manipulation
        console.log(`Regional center ${centerName} visibility: ${isVisible}`);
      }
    },
    // Toggle ZIP highlight layer visibility per selected center
    updateLAZipCenterVisibility() {
      if (!this.map || !this.laRegionalCentersData?.features) return;
      for (const feature of this.laRegionalCentersData.features) {
        const name = feature.properties?.name;
        if (!name) continue;
        const layerId = `la-zip-center-${name
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, "-")}-fill`;
        if (this.map.getLayer(layerId)) {
          try {
            this.map.setLayoutProperty(
              layerId,
              "visibility",
              this.selectedRegionalCenters[name] ? "visible" : "none"
            );
          } catch (e) {}
        }
      }
    },
    // Detect which property key in the ZIP GeoJSON holds the 5-digit ZIP code
    async detectLaZipPropertyKey() {
      if (this.laZipPropertyKey) return this.laZipPropertyKey;

      const candidateKeys = [
        "zip",
        "zipcode",
        "ZIP",
        "ZIP_CODE",
        "ZIPCODE",
        "ZCTA5CE10",
        "ZCTA5CE",
        "ZCTA5",
        "GEOID10",
        "GEOID",
        "POSTAL",
        "POSTCODE",
        "POSTALCODE",
        "NAME",
        "Name",
        "name",
      ];

      const isZip = (v) => typeof v === "string" && /^\d{5}$/.test(v.trim());

      try {
        // Try rendered features first (requires layer to exist)
        const rendered = this.map
          ? this.map.queryRenderedFeatures({ layers: ["la-zip-codes-fill"] })
          : [];
        const sample = (rendered || []).slice(0, 100);
        for (const key of candidateKeys) {
          const ok =
            sample.length > 0 &&
            sample.every((f) => {
              const val = f?.properties?.[key];
              if (val === undefined || val === null) return true;
              return isZip(String(val).padStart(5, "0"));
            });
          if (ok) {
            this.laZipPropertyKey = key;
            console.log("Detected ZIP property key from rendered features:", key);
            return key;
          }
        }
      } catch {}

      try {
        // Fallback: fetch a bit of the GeoJSON and inspect
        const res = await fetch(LA_ZIP_CODES_URL);
        const geo = await res.json();
        const feats = geo && geo.features ? geo.features.slice(0, 200) : [];
        for (const key of candidateKeys) {
          const ok =
            feats.length > 0 &&
            feats.every((f) => {
              const val = f?.properties?.[key];
              if (val === undefined || val === null) return true;
              return isZip(String(val).padStart(5, "0"));
            });
          if (ok) {
            this.laZipPropertyKey = key;
            console.log("Detected ZIP property key from source JSON:", key);
            return key;
          }
        }
      } catch (e) {
        console.warn("Could not fetch ZIP GeoJSON to detect key:", e);
      }

      // Default to common Census key
      this.laZipPropertyKey = "ZCTA5CE10";
      console.warn("Falling back to default ZIP key:", this.laZipPropertyKey);
      return this.laZipPropertyKey;
    },

    // Convenience: fetch LA County geometry and show colored ZIPs immediately
    async showLAZipColorsOnly() {
      try {
        const countiesResponse = await fetch(
          `${this.getApiRoot()}/api/california-counties/`
        );
        if (!countiesResponse.ok) return;
        const countiesData = await countiesResponse.json();
        const laCounty = countiesData.features.find(
          (feature) => feature.properties.name === "Los Angeles"
        );
        if (!laCounty) return;
        // Transparent LA county fill for clarity
        try {
          if (!this.map.getSource("la-regional-centers")) {
            this.map.addSource("la-regional-centers", {
              type: "geojson",
              data: {
                type: "FeatureCollection",
                features: [
                  {
                    type: "Feature",
                    properties: { name: "Los Angeles County" },
                    geometry: laCounty.geometry,
                  },
                ],
              },
            });
          }
          if (!this.map.getLayer("la-county-boundary")) {
            this.map.addLayer({
              id: "la-county-boundary",
              type: "fill",
              source: "la-regional-centers",
              paint: { "fill-color": "#e3f2fd", "fill-opacity": 0 },
            });
          }
          if (!this.map.getLayer("la-county-outline")) {
            this.map.addLayer({
              id: "la-county-outline",
              type: "line",
              source: "la-regional-centers",
              paint: { "line-color": "#1976d2", "line-width": 2 },
            });
          }
        } catch (e) {}
        await this.addColoredLAZipsLayer(laCounty);
      } catch (e) {}
    },

    // Ensure we have a mapping of ZIP → Regional Center
    async ensureZipToCenterMap() {
      if (this.zipToCenter && Object.keys(this.zipToCenter).length > 0) return;
      try {
        const response = await fetch(
          `${this.getApiRoot()}/api/regional-centers/service_area_boundaries/`
        );
        if (!response.ok) {
          console.warn("ZIP→Center map fetch failed:", response.status);
          return;
        }
        const data = await response.json();
        this.laRegionalCentersData = data;
        const map = {};
        for (const feature of data.features || []) {
          const name = feature?.properties?.name || "Regional Center";
          const zips = feature?.properties?.zip_codes || [];
          for (const z of zips) {
            map[String(z).padStart(5, "0")] = name;
          }
        }
        // Spatial fallback for any ZIP missing in the lists: nearest center by centroid
        try {
          const rcCenters = (data.features || [])
            .filter((f) => f?.properties?.name)
            .map((f) => ({
              name: f.properties.name,
              // quick centroid from bbox/geometry: use first coordinate as proxy
              lng: Array.isArray(f?.geometry?.coordinates)
                ? f.geometry.coordinates[0]?.[0]?.[0]?.[0] ?? null
                : null,
              lat: Array.isArray(f?.geometry?.coordinates)
                ? f.geometry.coordinates[0]?.[0]?.[0]?.[1] ?? null
                : null,
            }))
            .filter((c) => c.lat !== null && c.lng !== null);
          const dist2 = (a, b) => {
            const dx = (a.lng || 0) - (b.lng || 0);
            const dy = (a.lat || 0) - (b.lat || 0);
            return dx * dx + dy * dy;
          };
          // Build a rough centroid lookup for ZIPs by reading a small sample from GeoJSON
          try {
            const res = await fetch(LA_ZIP_CODES_URL);
            const geo = await res.json();
            const zipKey = await this.detectLaZipPropertyKey();
            for (const f of geo.features || []) {
              const zip = String(f?.properties?.[zipKey] || "").padStart(5, "0");
              if (map[zip]) continue;
              const coord = Array.isArray(f?.geometry?.coordinates)
                ? f.geometry.coordinates[0]?.[0]?.[0]
                : null;
              if (!coord) continue;
              const pt = { lng: coord[0], lat: coord[1] };
              let best = null;
              for (const c of rcCenters) {
                const d = dist2(c, pt);
                if (!best || d < best.d) best = { name: c.name, d };
              }
              if (best) map[zip] = best.name;
            }
          } catch (_) {}
        } catch (_) {}

        this.zipToCenter = map;
        console.log(`Loaded ZIP→Center map entries: ${Object.keys(map).length}`);
      } catch (_) {}
    },

    // Convert HSL to hex color string
    hslToHex(h, s, l) {
      const sNorm = s / 100;
      const lNorm = l / 100;
      const k = (n) => (n + h / 30) % 12;
      const a = sNorm * Math.min(lNorm, 1 - lNorm);
      const f = (n) =>
        lNorm - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)));
      const toHex = (x) =>
        Math.round(x * 255)
          .toString(16)
          .padStart(2, "0");
      return `#${toHex(f(0))}${toHex(f(8))}${toHex(f(4))}`;
    },

    // Add a single colored ZIP layer inside LA County, coloring each ZIP differently
    async addColoredLAZipsLayer(laCounty) {
      try {
        // Load and colorize the ZIP GeoJSON
        let geo = this.coloredZipsData;
        if (!geo) {
          try {
            const res = await fetch(LA_ZIP_CODES_URL);
            geo = await res.json();
            this.coloredZipsData = geo;
          } catch (e) {
            console.warn(
              "Local ZIP GeoJSON load failed; trying backend /api/la-zip-codes/",
              e
            );
            const res2 = await fetch(`${this.getApiRoot()}/api/la-zip-codes/`);
            if (res2.ok) {
              geo = await res2.json();
              this.coloredZipsData = geo;
            } else {
              throw new Error(
                "Failed to load ZIP GeoJSON from both local asset and backend"
              );
            }
          }
        }
        const zipKey = await this.detectLaZipPropertyKey();

        // Ensure we have ZIP → Regional Center mapping
        await this.ensureZipToCenterMap();

        // Assign color by regional center; fallback to ZIP-hash if missing
        const colorized = {
          type: "FeatureCollection",
          features: (geo.features || []).map((f) => {
            const raw = f?.properties?.[zipKey];
            const zip = String(raw || "").padStart(5, "0");
            const rcName = this.zipToCenter[zip] || null;
            let color = getRegionalCenterColor(rcName);
            // If no regional center color, use fallback ZIP-hash color
            if (color === "#95a5a6") {
              let hue = 0;
              for (let i = 0; i < zip.length; i++)
                hue = (hue * 31 + zip.charCodeAt(i)) % 360;
              color = this.hslToHex(hue, 45, 65);
            }
            return {
              ...f,
              properties: {
                ...(f.properties || {}),
                _zipColor: color,
                _zipId: zip,
                _rcName: rcName,
              },
            };
          }),
        };

        // Remove prior colored layers/sources if present
        if (this.map.getLayer("la-zip-codes-colored-fill")) {
          try {
            this.map.removeLayer("la-zip-codes-colored-fill");
          } catch (e) {}
        }
        if (this.map.getLayer("la-zip-codes-colored-outline")) {
          try {
            this.map.removeLayer("la-zip-codes-colored-outline");
          } catch (e) {}
        }
        if (this.map.getSource("la-zip-codes-colored")) {
          try {
            this.map.removeSource("la-zip-codes-colored");
          } catch (e) {}
        }

        // Add colored source clipped to LA County at data level to avoid Mapbox within issues on some platforms
        const clipped = {
          type: "FeatureCollection",
          features: (colorized.features || []).filter(() => true),
        };
        this.map.addSource("la-zip-codes-colored", {
          type: "geojson",
          data: clipped,
        });

        // Hide the base ZIP fill if present
        if (this.map.getLayer("la-zip-codes-fill")) {
          try {
            this.map.setPaintProperty("la-zip-codes-fill", "fill-opacity", 0);
          } catch (e) {}
        }

        // Add colored fill (one color per ZIP)
        this.map.addLayer({
          id: "la-zip-codes-colored-fill",
          type: "fill",
          source: "la-zip-codes-colored",
          paint: {
            "fill-color": ["get", "_zipColor"],
            "fill-opacity": 0.8,
          },
        });

        // Add crisp outlines over the colored fills
        this.map.addLayer({
          id: "la-zip-codes-colored-outline",
          type: "line",
          source: "la-zip-codes-colored",
          paint: {
            "line-color": "#2c3e50",
            "line-width": 1.2,
            "line-opacity": 1,
          },
        });

        // Ensure outline is on top
        try {
          this.map.moveLayer("la-zip-codes-colored-outline");
        } catch (e) {}

        // Interactivity: click to show ZIP + Regional Center name
        this.map.on("mouseenter", "la-zip-codes-colored-fill", () => {
          this.map.getCanvas().style.cursor = "pointer";
        });
        this.map.on("mouseleave", "la-zip-codes-colored-fill", () => {
          this.map.getCanvas().style.cursor = "";
        });
        this.map.on("click", "la-zip-codes-colored-fill", (e) => {
          const f = e.features && e.features[0];
          if (!f) return;
          const zip =
            f.properties?._zipId || f.properties?.ZIP || f.properties?.ZIP_CODE || "";
          const rc = f.properties?._rcName || this.zipToCenter[zip] || "Unassigned";
          new mapboxgl.Popup({ closeOnClick: true })
            .setLngLat(e.lngLat)
            .setHTML(
              `<div style=\"font-size:13px\"><div><strong>ZIP:</strong> ${zip}</div><div><strong>Regional Center:</strong> ${rc}</div></div>`
            )
            .addTo(this.map);
        });
      } catch (e) {
        console.error("Failed to add colored LA ZIPs:", e);
      }
    },
    // Toggle mobile sidebar
    toggleMobileSidebar() {
      this.showMobileSidebar = !this.showMobileSidebar;
      // Close other mobile menus
      this.showMobileSearch = false;
      this.showUserMenu = false;
    },

    // Toggle mobile search
    toggleSearch() {
      this.showMobileSearch = !this.showMobileSearch;
      // Close other mobile menus
      this.showMobileSidebar = false;
      this.showUserMenu = false;
    },

    // Toggle user menu
    toggleUserMenu() {
      this.showUserMenu = !this.showUserMenu;
      // Close other mobile menus
      this.showMobileSidebar = false;
      this.showMobileSearch = false;
    },

    // Perform search
    performSearch() {
      // Trigger the search functionality
      this.debounceSearch();
    },

    // Toggle funding info modal
    toggleFundingInfo() {
      this.showFundingInfo = !this.showFundingInfo;
    },

    // Update filtered locations based on filters
    async updateFilteredLocations() {
      console.log("🔍 Universal search with text:", this.searchText);
      console.log("🎛️ Filters:", this.filterOptions);
      console.log("📏 Current radius:", this.radius);

      // Clear any previous errors
      this.error = null;

      // Set loading state
      this.loading = true;

      try {
        // UNIVERSAL SEARCH: Handle different types of input
        if (this.searchText && this.searchText.trim()) {
          const query = this.searchText.trim();
          
          // Detect if it's a standalone ZIP code (5 digits)
          const zipMatch = query.match(/^\d{5}$/);
          if (zipMatch) {
            const zipCode = zipMatch[0];
            console.log("📮 ZIP code detected:", zipCode);
            
            // Validate ZIP is in LA County (9xxxx range)
            if (!zipCode.startsWith('9')) {
              console.warn("⚠️ ZIP code is outside LA County service area");
              this.error = `ZIP code ${zipCode} is outside our Los Angeles County service area. Please use an LA County ZIP code (90xxx-93xxx).`;
              this.loading = false;
              return;
            }
            
            // Update user address with ZIP
            this.userData.address = `Los Angeles, CA ${zipCode}`;
            
            // Find regional center for this ZIP
            await this.findUserRegionalCenter();
            
            // Load providers for this ZIP's regional center using correct method
            await this.providerStore.searchByZipCode(zipCode);
            
            // Center map on the ZIP code location
            const coords = await this.geocodeTextToCoords(`${zipCode}, CA`);
            if (coords && this.mapInstance) {
              this.mapInstance.easeTo({
                center: [coords.lng, coords.lat],
                zoom: 11,
                duration: 2000,
                easing(t) { return t * t * (3.0 - 2.0 * t); }
              });
            }
            return;
          }
          
          // Check if it looks like an address (contains comma, or street number, or city/state)
          // This catches: "123 Main St", "Main St, LA", "Los Angeles, CA", etc.
          const isAddress = query.match(/,/) || query.match(/^\d+\s+\w/) || query.match(/\b(ca|california|los angeles|la)\b/i);
          
          if (isAddress) {
            console.log("📍 Address detected:", query);
            
            // Only check for ZIP code if it appears AFTER "CA" or at the end
            // This prevents street numbers (15767 Main St) from being mistaken as ZIP codes
            const addressZipMatch = query.match(/\b(?:CA|California)\s+(\d{5})\b/i) || query.match(/,\s*(\d{5})$/);
            if (addressZipMatch && !addressZipMatch[1].startsWith('9')) {
              console.warn("⚠️ ZIP code in address is outside LA County");
              this.error = `ZIP code ${addressZipMatch[1]} is outside our Los Angeles County service area. Please use an LA County address (90xxx-93xxx ZIP codes).`;
              this.loading = false;
              return;
            }
            
            // Try to geocode the address
            const coords = await this.geocodeTextToCoords(query);
            if (coords) {
              // Validate that location is in LA County area
              // LA County bounds: roughly 33.7-34.8 N, -118.9--117.6 W
              const isInLACounty = 
                coords.lat >= 33.7 && coords.lat <= 34.8 &&
                coords.lng >= -118.9 && coords.lng <= -117.6;
              
              if (!isInLACounty) {
                console.warn("⚠️ Location is outside LA County service area");
                this.error = "This location is outside our Los Angeles County service area. Please search for an LA County address.";
                this.loading = false;
                return;
              }
              
              this.userData.address = query;
              await this.findUserRegionalCenter();
              
              // Search providers at this location with current radius
              await this.providerStore.searchByLocation(coords.lat, coords.lng, this.radius);
              
              // Center map on the location (only if not adjusting radius)
              if (this.mapInstance && !this.isAdjustingRadius) {
                this.mapInstance.easeTo({
                  center: [coords.lng, coords.lat],
                  zoom: 12,
                  duration: 2000,
                  easing(t) { return t * t * (3.0 - 2.0 * t); }
                });
              }
            } else {
              this.error = "Could not find that location. Please try a different address.";
            }
            return;
          }
          
          // Text search for providers (names, services, etc.)
          console.log("📝 Text search for providers:", query);
          await this.providerStore.searchWithFilters({
            searchText: query,
            lat: this.userLocation?.latitude || 34.0522,
            lng: this.userLocation?.longitude || -118.2437,
            radius: this.radius
          });
          return;
        }
        
        // RADIUS CHANGE: If no search text but we have searchCoordinates, re-search with new radius
        if (!this.searchText && this.providerStore.searchCoordinates) {
          console.log("📏 Radius changed - re-searching with new radius:", this.radius);
          const coords = this.providerStore.searchCoordinates;
          await this.providerStore.searchByLocation(coords.lat, coords.lng, this.radius);
          return;
        }
      } finally {
        // Always reset loading state and radius adjustment flag
        this.loading = false;
        this.isAdjustingRadius = false;
      }

      // If any provider-specific filters are enabled, automatically switch to providers
      const hasProviderFilters =
        this.filterOptions.acceptsInsurance ||
        this.filterOptions.acceptsRegionalCenter ||
        this.filterOptions.matchesDiagnosis ||
        this.filterOptions.matchesAge;

      if (hasProviderFilters && this.displayType !== "providers") {
        console.log("🔄 Provider filters applied, switching to providers view");
        this.displayType = "providers";
      }

      // If we're showing providers, refetch from API with filters
      if (this.displayType === "providers") {
        // Store current map state to preserve zoom level only when adjusting radius
        const currentMapState = (this.isAdjustingRadius && this.map) ? {
          center: this.map.getCenter(),
          zoom: this.map.getZoom()
        } : null;
        
        if (currentMapState) {
          console.log("🔍 Preserving map state for radius adjustment:", currentMapState);
        }
        
        await this.fetchProviders();
        
          // Restore map state after providers are loaded (only for radius adjustments)
          if (currentMapState && this.map) {
            console.log("🔍 Restoring map state after radius change:", currentMapState);
            this.map.setCenter(currentMapState.center);
            this.map.setZoom(currentMapState.zoom);
          }
          // Reset the flag
          this.isAdjustingRadius = false;
      } else {
        // For other types, just update markers
        this.$nextTick(() => {
          // MapCanvas handles marker updates automatically
        });
      }
    },

    // Debounce search to prevent too many updates
    debounceSearch() {
      console.log('🔍 debounceSearch called, searchText:', this.searchText);
      if (this.searchDebounce) {
        clearTimeout(this.searchDebounce);
      }

      this.searchDebounce = setTimeout(() => {
        console.log('🔍 Executing search with text:', this.searchText);
        // Clear any pending map movements to prevent conflicts
        if (this.map) {
          this.map.stop();
        }
        this.updateFilteredLocations();
      }, 500); // Increased delay to reduce jankiness
    },

    // Handle radius slider changes and adjust map zoom
    async onRadiusChange() {
      // Prevent duplicate calls if already adjusting
      if (this.isAdjustingRadius) {
        console.log('⚠️ Radius change already in progress, skipping');
        return;
      }

      console.log('🔍 Radius changed to:', this.radius, 'miles');
      this.isAdjustingRadius = true;

      // Calculate appropriate zoom level based on radius
      // Mapbox zoom levels: higher number = closer zoom
      // Approximate conversion: zoom 15 = ~0.5mi, zoom 13 = ~2mi, zoom 11 = ~8mi, zoom 9 = ~30mi
      let targetZoom;
      if (this.radius <= 2) targetZoom = 13;
      else if (this.radius <= 5) targetZoom = 12;
      else if (this.radius <= 10) targetZoom = 11;
      else if (this.radius <= 15) targetZoom = 10.5;
      else if (this.radius <= 25) targetZoom = 10;
      else if (this.radius <= 35) targetZoom = 9.5;
      else if (this.radius <= 50) targetZoom = 9;
      else targetZoom = 8.5;

      // Adjust map zoom centered on user's location
      if (this.mapInstance && this.userLocation?.latitude && this.userLocation?.longitude) {
        // Always center on user's location when radius changes
        this.mapInstance.easeTo({
          center: [this.userLocation.longitude, this.userLocation.latitude], // User's location
          zoom: targetZoom,
          duration: 1000, // 1 second smooth transition
          easing(t) {
            // Smooth easing function (ease-in-out)
            return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
          }
        });
        console.log(`📍 Smoothly adjusting map zoom to ${targetZoom} for ${this.radius} mile radius centered on user location (${this.userLocation.latitude}, ${this.userLocation.longitude})`);
      }

      // Re-fetch providers with new radius
      if (this.providerStore && this.userLocation?.latitude && this.userLocation?.longitude) {
        try {
          const filterParams = this.filterStore ? this.filterStore.buildFilterParams() : {};
          console.log('[MapView] Re-fetching providers with new radius:', this.radius, 'miles');

          await this.providerStore.searchByLocation(
            this.userLocation.latitude,
            this.userLocation.longitude,
            this.radius,
            filterParams
          );

          // Check for duplicate IDs
          const providerIds = this.providerStore.providers.map(p => p.id);
          const uniqueIds = new Set(providerIds);
          if (providerIds.length !== uniqueIds.size) {
            console.error(`⚠️ DUPLICATE PROVIDER IDs DETECTED! Total: ${providerIds.length}, Unique: ${uniqueIds.size}`);
            const duplicates = providerIds.filter((id, index) => providerIds.indexOf(id) !== index);
            console.error('Duplicate IDs:', duplicates);
          }

          // Update map markers
          // MapCanvas handles marker updates automatically
          console.log(`✅ Updated providers with ${this.providerStore.providers.length} results within ${this.radius} miles`);
        } catch (error) {
          console.error('[MapView] Error updating radius:', error);
        }
      }

      this.isAdjustingRadius = false;
    },

    // Wait for map to be fully ready
    async waitForMapReady() {
      return new Promise((resolve) => {
        if (this.map && this.map.isStyleLoaded()) {
          resolve();
          return;
        }
        
        const checkMap = () => {
          if (this.map && this.map.isStyleLoaded()) {
            resolve();
          } else {
            setTimeout(checkMap, 100);
          }
        };
        checkMap();
      });
    },

    // Load initial providers with smooth map setup
    async loadInitialProviders() {
      // Load providers without any map movements
      console.log("📊 Loading initial providers...");
      this.isMapMoving = true; // Prevent any map bounds changes
      await this.fetchProviders();
      this.isMapMoving = false;
      
      console.log("✅ Initial load complete - LA County view with providers");
    },
    clearSearch() {
      this.searchText = "";
      this.updateFilteredLocations();
    },
    // Simple geocoder using Nominatim (frontend) to make the search box reliable
    async geocodeTextToCoords(text) {
      // Use composable for geocoding
      return await this.geolocation.geocodeTextToCoords(text);
    },

    /**
     * Ensure we have coordinates for a ZIP code (geocode if needed)
     */
    async ensureZipCodeCoordinates(zipCode) {
      // If we already have coordinates, no need to geocode
      if (this.userLocation?.latitude && this.userLocation?.longitude) {
        console.log(`✅ Already have coordinates: ${this.userLocation.latitude}, ${this.userLocation.longitude}`);
        return;
      }

      try {
        console.log(`📍 Geocoding ZIP ${zipCode} to enable radius filtering...`);
        const coords = await this.geocodeTextToCoords(`${zipCode}, CA`);
        
        if (coords) {
          this.userLocation.latitude = coords.lat;
          this.userLocation.longitude = coords.lng;
          this.userLocation.detected = true;

          // Update mapStore so the marker appears
          if (this.mapStore) {
            this.mapStore.setUserLocation({ lat: coords.lat, lng: coords.lng });
          }

          console.log(`✅ Geocoded ${zipCode} to: ${coords.lat}, ${coords.lng}`);
          console.log(`✅ userLocation now:`, this.userLocation);
          console.log(`✅ Radius slider should now be visible!`);
        } else {
          console.warn(`⚠️ Could not geocode ZIP ${zipCode}`);
        }
      } catch (error) {
        console.error(`❌ Error geocoding ZIP ${zipCode}:`, error);
      }
    },

    getApiRoot() {
      // Use the API base URL from environment variable
      return import.meta.env.VITE_API_BASE_URL || "";
    },

    // Find regional center for user's ZIP code
    async findUserRegionalCenter() {
      console.log("Finding regional center for user address:", this.userData?.address);

      if (!this.userData?.address) {
        console.log("No user address found");
        this.userRegionalCenter = {
          name: "Regional Center (No Address)",
          address: "Unable to determine regional center",
          phone: null,
          website: null,
        };
        return this.userRegionalCenter;
      }

      // Extract ZIP code from address using utility
      const zipCode = extractZipCode(this.userData.address);
      console.log(`Extracted ZIP code: ${zipCode}`);
      
      if (!zipCode) {
        console.log("No ZIP code found in address:", this.userData.address);
        console.log("Address breakdown:", {
          fullAddress: this.userData.address,
          length: this.userData.address.length,
          hasNumbers: /\d/.test(this.userData.address),
          allNumbers: this.userData.address.match(/\d+/g)
        });
        
        // Try to find regional center using coordinates instead
        console.log("Attempting coordinate-based regional center lookup...");
        return await this.findRegionalCenterByCoordinates();
      }
      console.log("Found ZIP code:", zipCode);

      // Use composable to find regional center by ZIP
      const regionalCenter = this.regionalCenterData.findByZipCode(zipCode);

      if (regionalCenter) {
        this.userRegionalCenter = {
          name: regionalCenter.name || regionalCenter.regional_center,
          address: regionalCenter.address,
          phone: regionalCenter.phone,
          website: regionalCenter.website,
          ...regionalCenter,
        };
        console.log("User regional center found via composable:", this.userRegionalCenter);
        return this.userRegionalCenter;
      } else {
        console.log("No regional center found for ZIP code:", zipCode);
        this.userRegionalCenter = {
          name: "Regional Center (Not Found)",
          address: `No regional center found for ZIP code ${zipCode}`,
          phone: null,
          website: null,
        };
        return this.userRegionalCenter;
      }
    },

    // Find regional center using coordinates when ZIP code lookup fails
    async findRegionalCenterByCoordinates() {
      console.log("Finding regional center by coordinates...");

      if (!this.userLocation?.latitude || !this.userLocation?.longitude) {
        console.log("No coordinates available for regional center lookup");
        this.userRegionalCenter = {
          name: "Regional Center (No Coordinates)",
          address: "Unable to determine regional center - no location data",
          phone: null,
          website: null,
        };
        return this.userRegionalCenter;
      }

      // Use composable to find nearest regional center
      const result = this.regionalCenterData.findNearestToCoordinates({
        lat: this.userLocation.latitude,
        lng: this.userLocation.longitude,
      });

      if (result) {
        this.userRegionalCenter = {
          name: result.center.name || result.center.regional_center,
          address: result.center.address,
          phone: result.center.phone,
          website: result.center.website,
          ...result.center,
        };
        console.log(`Found regional center by coordinates: ${result.center.name} (${result.distance.toFixed(2)} miles)`);
        return this.userRegionalCenter;
      } else {
        console.log("No regional center found for coordinates");
        this.userRegionalCenter = {
          name: "Regional Center (Not Found)",
          address: "No regional center found for this location",
          phone: null,
          website: null,
        };
        return this.userRegionalCenter;
      }
    },


    // Set display type
    setDisplayType(type) {
      this.displayType = type;

      // Fetch data if needed
      if (type === "providers" && this.providers.length === 0) {
        this.fetchProviders();
      } else if (type === "regionalCenters" && this.regionalCenters.length === 0) {
        this.regionalCenterData.fetchRegionalCenters();
      }

      // Update markers
      this.$nextTick(() => {
        // MapCanvas handles marker updates automatically
      });

      // When switching to regional centers, center on LA Regional Centers
      if (type === "regionalCenters" && this.map && !this.isMapMoving) {
        // Center on Los Angeles County with appropriate zoom
        const laCenter = [-118.2437, 34.0522]; // LA County center
        this.isMapMoving = true;
        this.map.flyTo({
          center: laCenter,
          zoom: 9.5, // Good zoom level to see all LA Regional Centers
          duration: 1000,
          essential: true,
        });
        setTimeout(() => {
          this.isMapMoving = false;
        }, 1100);

        // Also ensure LA Regional Centers are shown
        if (!this.showLARegionalCenters) {
          this.toggleLARegionalCenters();
        }
      } else if (type === "providers") {
        // When switching back to providers, check if we should refocus
        if (this.map && this.userLocation?.latitude && this.userLocation?.longitude) {
          const laBounds = this.getLACountyBounds();
          const inLA = this.isPointInBounds(
            this.userLocation.longitude,
            this.userLocation.latitude,
            laBounds
          );
          if (inLA) {
            this.map.fitBounds(laBounds, { padding: 40, duration: 600 });
          } else {
            // Otherwise, center on user's location with a reasonable zoom
            const targetZoom = Math.max(this.map.getZoom() || 0, 10);
            this.map.flyTo({
              center: [this.userLocation.longitude, this.userLocation.latitude],
              zoom: targetZoom,
              duration: 600,
            });
          }
        }
        // Ensure service areas are visible and layers are present
        (async () => {
          try {
            if (!this.serviceAreasLoaded) {
              await this.fetchServiceAreas();
            }
            if (this.serviceAreasLoaded) {
              if (
                !this.map.getSource("service-areas") ||
                !this.map.getLayer("california-counties-fill")
              ) {
                this.addServiceAreasToMap();
              }
              // Apply visual highlight to LA only (no camera move)
              this.highlightLACountyVisual();
            }
          } catch (_) {}
        })();
      }
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
        matchesDiagnosis: false,
        matchesAge: false,
      };

      // Re-fetch data with reset filters (maintain current display type)
      console.log(`Resetting filters, maintaining display type: ${this.displayType}`);
      if (this.displayType === "providers") {
        this.fetchProviders();
      } else if (this.displayType === "regionalCenters") {
        this.regionalCenterData.fetchRegionalCenters();
      } else {
        this.updateFilteredLocations();
      }
    },

    handleRegionalCenterMatched(center) {
      this.matchedRegionalCenter = center;
      this.userRegionalCenter = center; // Also set this for the UI

      // Update polygon highlighting to emphasize user's RC
      this.$nextTick(() => {
        this.updateRegionalCenterHighlighting();
      });

      // Center map softly on the matched center
      if (
        center &&
        this.map &&
        center.latitude &&
        center.longitude &&
        !this.isMapMoving
      ) {
        this.isMapMoving = true;
        this.map.flyTo({ center: [center.longitude, center.latitude], zoom: 10 });
        setTimeout(() => {
          this.isMapMoving = false;
        }, 1100);
      }
    },

    showMatchedCenterOnMap() {
      const c = this.matchedRegionalCenter;
      if (!c || !this.map) return;
      const lng = parseFloat(c.longitude);
      const lat = parseFloat(c.latitude);
      if (isNaN(lat) || isNaN(lng)) return;
      this.map.flyTo({ center: [lng, lat], zoom: 11 });
      // Drop a temporary marker
      const el = document.createElement("div");
      el.className = "marker marker-regional-center";
      el.style.width = "14px";
      el.style.height = "14px";
      el.style.background = "#1f618d";
      el.style.border = "2px solid white";
      el.style.borderRadius = "50%";
      const marker = new mapboxgl.Marker(el).setLngLat([lng, lat]).addTo(this.map);
      setTimeout(() => marker.remove(), 8000);
    },

    applyLACountyFocus() {
      // Toggle the state first
      this.focusLACounty = !this.focusLACounty;
      // If toggled on, zoom to LA County bounds and fade other counties
      if (!this.map) return;
      if (this.focusLACounty) {
        // LA County approximate bounds
        const laBounds = [
          [-119.1, 33.3],
          [-117.5, 34.9],
        ];
        this.map.fitBounds(laBounds, { padding: 40, duration: 700 });

        // If counties layer exists, highlight LA
        if (this.map.getLayer("california-counties-fill")) {
          // Add a filter to emphasize Los Angeles
          this.map.setPaintProperty("california-counties-fill", "fill-opacity", [
            "case",
            ["==", ["downcase", ["get", "name"]], "los angeles"],
            0.55,
            0.12,
          ]);
          this.map.setPaintProperty("california-counties-fill", "fill-color", [
            "case",
            ["==", ["downcase", ["get", "name"]], "los angeles"],
            "#e67e22",
            [
              "interpolate",
              ["linear"],
              ["get", "regional_center_count"],
              0,
              "#e8e8e8",
              2,
              "#a8d5e5",
              5,
              "#5dade2",
              10,
              "#3498db",
              11,
              "#1f618d",
            ],
          ]);
        }
      } else {
        // Restore normal styling
        if (this.map.getLayer("california-counties-fill")) {
          this.map.setPaintProperty("california-counties-fill", "fill-opacity", 0.2);
          this.map.setPaintProperty("california-counties-fill", "fill-color", [
            "interpolate",
            ["linear"],
            ["get", "regional_center_count"],
            0,
            "#e8e8e8",
            2,
            "#a8d5e5",
            5,
            "#5dade2",
            10,
            "#3498db",
            11,
            "#1f618d",
          ]);
        }
      }
    },

    highlightLACountyVisual() {
      if (!this.map) return;
      if (this.map.getLayer("california-counties-fill")) {
        // Emphasize LA via color/opacity without adjusting camera
        this.map.setPaintProperty("california-counties-fill", "fill-opacity", [
          "case",
          ["==", ["downcase", ["get", "name"]], "los angeles"],
          0.55,
          0.2,
        ]);
        this.map.setPaintProperty("california-counties-fill", "fill-color", [
          "case",
          ["==", ["downcase", ["get", "name"]], "los angeles"],
          "#e67e22",
          [
            "interpolate",
            ["linear"],
            ["get", "regional_center_count"],
            0,
            "#e8e8e8",
            2,
            "#a8d5e5",
            5,
            "#5dade2",
            10,
            "#3498db",
            11,
            "#1f618d",
          ],
        ]);
      }
    },

    getLACountyBounds() {
      // Approximate LA County bounds
      return [
        [-119.1, 33.3],
        [-117.5, 34.9],
      ];
    },

    isPointInBounds(lng, lat, bounds) {
      if (!Array.isArray(bounds) || bounds.length !== 2) return false;
      const [[minLng, minLat], [maxLng, maxLat]] = bounds;
      return lng >= minLng && lng <= maxLng && lat >= minLat && lat <= maxLat;
    },

    async findRegionalCenterByZip() {
      try {
        this.laZipError = "";
        const zip = (this.laZipInput || "").trim();
        if (!/^\d{5}$/.test(zip)) {
          this.laZipError = "Enter a valid 5-digit ZIP";
          return;
        }
        const apiRoot = this.getApiRoot();
        const url = `${apiRoot}/api/regional-centers/by_location/?location=${zip}&radius=60&limit=5`;

        let center = null;
        try {
          const res = await fetch(url, { headers: { Accept: "application/json" } });
          if (res.ok) {
            const centers = await res.json();
            if (Array.isArray(centers) && centers.length > 0) {
              center = centers[0];
            }
          }
        } catch (e) {
          console.warn("ZIP center lookup API failed; using fallback", e);
        }

        if (!center) {
          try {
            if (this.regionalCenters.length === 0) {
              await this.regionalCenterData.fetchRegionalCenters();
            }
          } catch (_) {}
          let coords = null;
          try {
            coords = await this.basicGeocode(Number(zip));
          } catch (_) {}
          if (!coords) {
            try {
              const gc = await this.geocodeTextToCoords(zip);
              if (gc) coords = { lat: gc.lat, lng: gc.lng };
            } catch (_) {}
          }
          if (coords) {
            try {
              center = this.findNearestRegionalCenterFromList(coords.lat, coords.lng);
            } catch (_) {}
          }
          if (!center) {
            this.laZipError = "No regional center found for that ZIP";
            return;
          }
        }

        this.matchedRegionalCenter = center;
        const lng = parseFloat(center.longitude);
        const lat = parseFloat(center.latitude);
        if (this.map && !isNaN(lat) && !isNaN(lng)) {
          if (this.focusLACounty) {
            this.map.fitBounds(this.getLACountyBounds(), { padding: 40, duration: 0 });
          }
          this.map.flyTo({ center: [lng, lat], zoom: 11, duration: 600 });
          const el = document.createElement("div");
          el.className = "marker marker-regional-center";
          el.style.width = "14px";
          el.style.height = "14px";
          el.style.background = "#1f618d";
          el.style.border = "2px solid white";
          el.style.borderRadius = "50%";
          const marker = new mapboxgl.Marker(el).setLngLat([lng, lat]).addTo(this.map);
          setTimeout(() => marker.remove(), 8000);
        }
      } catch (err) {
        console.error("ZIP lookup handler error:", err);
        this.laZipError = "Lookup failed. Try again.";
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

    async loadUserData() {
      // Load saved user data from localStorage
      const savedProfile = localStorage.getItem("chla-user-profile");

      if (savedProfile) {
        try {
          this.userData = JSON.parse(savedProfile);
          console.log("Loaded saved user profile:", this.userData);
        } catch (e) {
          console.error("Error parsing saved profile:", e);
          // Use default data as fallback
          this.userData = {
            age: "5",
            address: "Los Angeles, CA 90001",
            diagnosis: "Autism",
            otherDiagnosis: "",
          };
        }
      } else {
        // Use sample data for testing with ZIP code
        this.userData = {
          age: "5",
          address: "Los Angeles, CA 90001",
          diagnosis: "Autism",
          otherDiagnosis: "",
        };
      }

      // Find regional center for user's ZIP code
      await this.findUserRegionalCenter();
    },

    // Detect user location using browser geolocation API
    async detectUserLocation() {
      console.log("🌍 Detecting user location...");

      const success = await this.geolocation.detectUserLocation();

      if (success && this.geolocation.userLocation?.value) {
        // Sync composable location to component data
        this.userLocation = { ...this.geolocation.userLocation.value };

        // Update user address if available
        if (this.geolocation.userLocation.value?.address) {
          this.userData.address = this.geolocation.userLocation.value.address;
          
          // Re-find regional center with the newly detected address
          console.log("📍 Address updated, finding regional center...");
          await this.findUserRegionalCenter();
        }

        console.log(`✅ Location detected via composable`);

        // Note: MapCanvas component handles map initialization
        // this.initMap() is deprecated

        // DON'T fetch providers - they're already loaded in mounted()
        // User can manually search their location if they want different results
      } else {
        console.warn("⚠️ Geolocation failed, using fallback");
        this.setFallbackLocation(this.geolocation.userLocation?.value?.error || "Location detection failed");
      }
    },

    // Set fallback location (default to LA County center)
    setFallbackLocation(error = null) {
      console.log("🏠 Using fallback location (LA County center)");

      this.userLocation = {
        latitude: LA_COUNTY_CENTER.lat, // Los Angeles
        longitude: LA_COUNTY_CENTER.lng,
        accuracy: null,
        detected: false,
        error: error,
      };

      // Update user address to reflect fallback
      this.userData.address = "Los Angeles Area (location detection failed)";

      // Set a fallback regional center when location detection fails
      this.userRegionalCenter = {
        name: "Regional Center (Location Detection Failed)",
        address: "Unable to determine specific regional center",
        phone: null,
        website: null,
      };

      // Note: MapCanvas component handles map initialization
      // this.initMap() is deprecated

      // DON'T fetch providers - they're already loaded in mounted()
      // User can manually search if they want
    },

    // Reverse geocode to get address from coordinates
    async reverseGeocode(latitude, longitude) {
      // Use composable for reverse geocoding
      const address = await this.geolocation.reverseGeocode(latitude, longitude);

      if (address) {
        console.log(`🏠 Detected address: ${address}`);
        this.userData.address = address;

        // Find regional center for the detected location
        this.findUserRegionalCenter();
      } else {
        console.warn("⚠️ Reverse geocoding failed");
        this.userData.address = `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
      }
    },

    // Map initialization
    initMap() {
      // Ensure we have valid coordinates before initializing
      if (!this.userLocation.latitude || !this.userLocation.longitude) {
        console.warn(
          "⚠️ No valid coordinates for map initialization, using LA County center"
        );
        this.userLocation.latitude = LA_COUNTY_CENTER.lat;
        this.userLocation.longitude = LA_COUNTY_CENTER.lng;
      }

      // Set Mapbox access token
      const mapboxToken =
        import.meta.env.VITE_MAPBOX_TOKEN ||
        "pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg";
      mapboxgl.accessToken = mapboxToken;

      console.log("🗺️ Mapbox token:", mapboxToken.substring(0, 20) + "...");
      console.log(
        `🗺️ Initializing map at: ${this.userLocation.latitude}, ${this.userLocation.longitude}`
      );

      // Check if map container exists
      const mapContainer = document.getElementById("map");
      if (!mapContainer) {
        console.error("❌ Map container 'map' not found!");
        return;
      }

      try {
        // Create Mapbox map with LA County focus
        this.map = new mapboxgl.Map({
          container: "map",
          style: "mapbox://styles/mapbox/streets-v12",
          center: [-118.2437, 34.0522], // Los Angeles center
          zoom: 10, // Good zoom level for LA County overview
          duration: 0, // Immediate initial positioning
        });

        console.log("✅ Mapbox map instance created successfully");
      } catch (error) {
        console.error("❌ Error creating Mapbox map:", error);
        return;
      }

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
        console.log("✅ Map loaded successfully");
        // MapCanvas handles marker updates automatically

        // If service areas are already enabled, add them now
        if (this.showServiceAreas && this.serviceAreasLoaded) {
          console.log("Map loaded and service areas are enabled, adding them now");
          this.addServiceAreasToMap();
        }

        // Show 7 regional center polygons immediately (not ZIPs)
        this.toggleLARegionalCenters();
      });

      // Add error handling for map loading
      this.map.on("error", (error) => {
        console.error("❌ Map error:", error);
      });

      this.map.on("styleimagemissing", (error) => {
        console.warn("⚠️ Map style image missing:", error);
      });

      console.log("Map initialization complete");
    },

    // Fetch provider data
    async fetchProviders() {
      console.log("🚀 fetchProviders() called");
      console.log("🚀 Current state:", {
        displayType: this.displayType,
        providersLength: this.providers.length,
        showOnboarding: this.showOnboarding,
        filterOptions: this.filterOptions
      });
      
      this.loading = true;
      this.error = null;

      try {
        console.log("📡 Fetching providers from API");

        // If using local data, use sample data
        if (USE_LOCAL_DATA_ONLY) {
          console.log("Using local data instead of API");
          // Hard-coded sample data with acceptance flags
          const sampleProviders = [
            {
              id: 1,
              name: "A & H BEHAVIORAL THERAPY",
              phone: "909-665-7070",
              description: "SAN FERNANDO VALLEY, HOLLYWOOD",
              type: "Service Provider",
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
              name: "BEHAVIORAL INTERVENTION SPECIALISTS",
              phone: "818-555-1234",
              description: "VALLEY, CENTRAL LA",
              type: "Service Provider",
              address: "456 Oak Ave, Burbank, CA",
              city: "Burbank",
              state: "CA",
              zip_code: "91502",
              latitude: 34.181,
              longitude: -118.308,
              accepts_insurance: false,
              accepts_regional_center: true,
            },
            {
              id: 3,
              name: "CENTER FOR AUTISM & RELATED DISORDERS",
              phone: "310-555-5678",
              description: "LONG BEACH, SOUTH LA",
              type: "Service Provider",
              address: "789 Pine St, Long Beach, CA",
              city: "Long Beach",
              state: "CA",
              zip_code: "90802",
              latitude: 33.77,
              longitude: -118.193,
              accepts_insurance: true,
              accepts_regional_center: false,
            },
          ];

          // Set providers
          if (this.providerStore) {
            this.providerStore.providers = sampleProviders;
          }
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
            console.log(`🔍 Adding search query: "${this.searchText.trim()}"`);
          }

          // Add location/radius - prefer client-side geocode of searchText if we can
          let searchLat = this.userLocation.latitude;
          let searchLng = this.userLocation.longitude;

          // Check if search text is a known city/location
          if (this.searchText && this.searchText.trim() !== "") {
            // 1) Try our quick local table
            let searchLocation = this.getLocationFromSearch(this.searchText.trim());
            
            // 2) If it's a ZIP code that needs geocoding, try Nominatim
            if (searchLocation && searchLocation.needsGeocoding) {
              console.log(`🔍 Geocoding ZIP code: ${searchLocation.zipCode}`);
              const nom = await this.geocodeTextToCoords(searchLocation.zipCode);
              if (nom) searchLocation = nom;
            }
            // 3) If that fails, try Nominatim for any other text
            else if (!searchLocation) {
              const nom = await this.geocodeTextToCoords(this.searchText.trim());
              if (nom) searchLocation = nom;
            }
            
            if (searchLocation && searchLocation.lat && searchLocation.lng) {
              searchLat = searchLocation.lat;
              searchLng = searchLocation.lng;
              console.log(
                `🔍 Using geocoded location for "${this.searchText}": ${searchLat}, ${searchLng}`
              );
            } else {
              console.log(`⚠️ Could not geocode "${this.searchText}", using user location`);
            }
          }

          // Always pass location text to backend so it can geocode precisely
          if (this.searchText && this.searchText.trim() !== "") {
            queryParams.append("location", this.searchText.trim());
          }

          // Add location/radius if available (using search location or user location)
          if (searchLat && searchLng) {
            queryParams.append("lat", searchLat);
            queryParams.append("lng", searchLng);
            // Increase radius for search to find more providers
            const searchRadius = this.radius || 25; // Increased from 15 to 25 miles
            queryParams.append("radius", searchRadius);
            console.log(`🔍 Using search radius: ${searchRadius} miles`);
          }

          // Only add user profile filters if specific filters are enabled
          if (this.filterOptions.matchesAge && this.userData.age) {
            queryParams.append("age", this.userData.age);
          }

          if (this.filterOptions.matchesDiagnosis && this.userData.diagnosis) {
            queryParams.append("diagnosis", this.userData.diagnosis);
          }

          // Add enum-based filters
          (this.filterOptions.diagnoses || []).forEach((d) =>
            queryParams.append("diagnosis", d)
          );
          (this.filterOptions.therapies || []).forEach((t) =>
            queryParams.append("therapy", t)
          );

          // Add insurance filter options only when explicitly checked
          if (this.filterOptions.acceptsInsurance) {
            queryParams.append("insurance", "insurance");
          }

          if (this.filterOptions.acceptsRegionalCenter) {
            queryParams.append("insurance", "regional center");
          }

          // Add specialization filter for diagnosis matching only when enabled
          if (this.filterOptions.matchesDiagnosis && this.userData.diagnosis) {
            queryParams.append("specialization", this.userData.diagnosis);
          }

          // Use regional center filtering for ZIP code searches, fallback to comprehensive search
          const isZipSearch = this.searchText && /^\d{5}$/.test(this.searchText.trim());
          let url;

          if (isZipSearch) {
            // Use regional center-based filtering for ZIP searches
            url = `${this.getApiRoot()}/api/providers-v2/by_regional_center/?zip_code=${this.searchText.trim()}&${queryParams.toString()}`;
            console.log(`🎯 Using REGIONAL CENTER filtering for ZIP: ${this.searchText.trim()}`);
          } else {
            // Use comprehensive search endpoint for address/city searches
            url = `${this.getApiRoot()}/api/providers-v2/comprehensive_search/?${queryParams.toString()}`;
          }

          if (hasSpecificFilters) {
            console.log(`🔍 Fetching FILTERED providers from API: ${url}`);
            console.log("🎛️ Active filters:", {
              acceptsInsurance: this.filterOptions.acceptsInsurance,
              acceptsRegionalCenter: this.filterOptions.acceptsRegionalCenter,
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
          console.log("API URL:", url);
          console.log("Response data:", response.data);

          // Handle different response formats
          if (isZipSearch && response.data && response.data.results) {
            // Regional center endpoint returns {results: [...], count: N, regional_center: {...}}
            if (this.providerStore) {
              this.providerStore.providers = response.data.results;
            }
            console.log(
              `✅ Loaded ${this.providers.length} providers from regional center: ${response.data.regional_center?.name || 'Unknown'}`
            );
            console.log(`🎯 Regional center has ${response.data.count} total providers`);
          } else if (response.data && Array.isArray(response.data)) {
            // Handle regular JSON array response
            if (this.providerStore) {
              this.providerStore.providers = response.data;
            }
            console.log(
              `✅ Loaded ${this.providers.length} providers from API (direct array)`
            );
            
            // If no providers found, try fallback strategies
            if (this.providers.length === 0) {
              console.log("🔍 No providers found, trying fallback strategies...");
              
              if (this.searchText && this.searchText.trim() !== "") {
                // Try broader search with just the search term
                console.log("🔍 Trying broader search with search term...");
                const broadUrl = `${this.getApiRoot()}/api/providers-v2/comprehensive_search/?q=${this.searchText}`;
                console.log("🔍 Trying broader search URL:", broadUrl);
                try {
                  const broadResponse = await axios.get(broadUrl);
                  console.log("🔍 Broader search response:", broadResponse.data);
                  if (broadResponse.data && Array.isArray(broadResponse.data) && broadResponse.data.length > 0) {
                    console.log(`🔍 Found ${broadResponse.data.length} providers in broader search`);
                    if (this.providerStore) {
                      this.providerStore.providers = broadResponse.data;
                    }
                  }
                } catch (broadError) {
                  console.log("🔍 Broader search failed:", broadError);
                }
              }
              
              // If still no results, try getting providers within a reasonable radius
              if (this.providers.length === 0) {
                console.log("🔍 No results in broader search, trying to get nearby providers...");
                
                // Try to get providers within a larger radius (50 miles) of the search location
                if (searchLat && searchLng) {
                  const nearbyUrl = `${this.getApiRoot()}/api/providers-v2/comprehensive_search/?lat=${searchLat}&lng=${searchLng}&radius=50`;
                  console.log("🔍 Trying nearby providers URL:", nearbyUrl);
                  try {
                    const nearbyResponse = await axios.get(nearbyUrl);
                    console.log("🔍 Nearby providers response:", nearbyResponse.data);
                    if (nearbyResponse.data && Array.isArray(nearbyResponse.data) && nearbyResponse.data.length > 0) {
                      console.log(`🔍 Found ${nearbyResponse.data.length} nearby providers`);
                      if (this.providerStore) {
                        this.providerStore.providers = nearbyResponse.data;
                      }
                    }
                  } catch (nearbyError) {
                    console.log("🔍 Nearby providers search failed (ignoring):", nearbyError.message);
                    // Silently fail - this is just a fallback
                  }
                }
                
                // If still no results, try getting LA County providers only as last resort
                if (this.providers.length === 0) {
                  console.log("🔍 No nearby providers found, trying to get LA County providers only...");
                  const laCountyUrl = `${this.getApiRoot()}/api/providers-v2/comprehensive_search/?lat=${LA_COUNTY_CENTER.lat}&lng=${LA_COUNTY_CENTER.lng}&radius=50`;
                  try {
                    const laResponse = await axios.get(laCountyUrl);
                    console.log("🔍 LA County providers response:", laResponse.data);
                    if (laResponse.data && Array.isArray(laResponse.data) && laResponse.data.length > 0) {
                      console.log(`🔍 Found ${laResponse.data.length} LA County providers`);
                      if (this.providerStore) {
                        this.providerStore.providers = laResponse.data;
                      }
                    } else {
                      console.log("⚠️ No providers found in LA County area!");
                    }
                  } catch (laError) {
                    console.log("🔍 LA County providers search failed:", laError);
                  }
                }
              }
            }

            // Add search result logging
            if (this.searchText && this.searchText.trim() !== "") {
              console.log(
                `🔍 Search results for "${this.searchText}": ${this.providers.length} matches`
              );
              
              // Debug: Check if providers are in LA County area
              if (this.providers.length > 0) {
                const laCountyBounds = {
                  west: -118.7,
                  east: -118.0,
                  south: 33.7,
                  north: 34.4
                };
                
                const providersInLA = this.providers.filter(provider => {
                  const lng = parseFloat(provider.longitude);
                  const lat = parseFloat(provider.latitude);
                  return lng >= laCountyBounds.west && lng <= laCountyBounds.east &&
                         lat >= laCountyBounds.south && lat <= laCountyBounds.north;
                });
                
                console.log(`🔍 Providers in LA County: ${providersInLA.length}/${this.providers.length}`);
                if (providersInLA.length === 0) {
                  console.log("⚠️ No providers found in LA County area - search may be too broad");
                }
              }
            }

            // If we're showing providers (especially after fallback), set reasonable map bounds
            // Skip map bounds changes during initial load to prevent jankiness
            if (this.providers.length > 0 && !this.isMapMoving) {
              console.log("🔍 Setting reasonable map bounds for providers...");
              
              // Set flag to prevent conflicting movements
              this.isMapMoving = true;
              
              // Stop any existing map movements to prevent conflicts
              if (this.map) {
                this.map.stop();
              }
              
              // If we have a search location, try to center on that first
              if (this.searchText && this.searchText.trim() !== "") {
                const searchLocation = this.getLocationFromSearch(this.searchText.trim());
                if (searchLocation && searchLocation.lat && searchLocation.lng) {
                  console.log("🔍 Centering on search location first");
                  
                  // Use a single smooth movement to search location
                  this.map.flyTo({
                    center: [searchLocation.lng, searchLocation.lat],
                    zoom: 11,
                    duration: 1500, // Slightly longer for smoother movement
                    essential: true // This movement is considered essential
                  });
                  
                  // Reset flag after movement completes
                  setTimeout(() => {
                    this.isMapMoving = false;
                  }, 1600);
                  return; // Don't do bounds calculation if we have a search location
                }
              }
              
              // Calculate bounds from actual provider locations
              const bounds = this.calculateProviderBounds();
              if (bounds) {
                console.log("🔍 Flying to calculated provider bounds:", bounds);
                this.map.fitBounds(bounds, {
                  padding: 50, // Add some padding around the bounds
                  maxZoom: 12, // Don't zoom in too much
                  duration: 1500, // Slightly longer for smoother movement
                  essential: true // This movement is considered essential
                });
              } else {
                // Fallback to LA County area if no valid coordinates
                console.log("🔍 No valid coordinates, using LA County fallback");
                this.map.fitBounds([
                  [-118.7, 33.7], // Southwest corner of LA County
                  [-118.0, 34.4]  // Northeast corner of LA County
                ], {
                  padding: 50,
                  maxZoom: 10,
                  duration: 1500, // Slightly longer for smoother movement
                  essential: true // This movement is considered essential
                });
              }
              
              // Reset flag after movement completes
              setTimeout(() => {
                this.isMapMoving = false;
              }, 1600);
            }
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
            if (this.providerStore) {
              this.providerStore.providers = response.data.results;
            }
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

            // Enhanced coordinate conversion with strict validation
            let lat = null;
            let lng = null;

            // Only attempt conversion if we have truthy values that aren't empty strings
            if (
              provider.latitude &&
              provider.latitude !== "" &&
              provider.latitude !== "null" &&
              provider.latitude !== "undefined"
            ) {
              lat = parseFloat(provider.latitude);
            }

            if (
              provider.longitude &&
              provider.longitude !== "" &&
              provider.longitude !== "null" &&
              provider.longitude !== "undefined"
            ) {
              lng = parseFloat(provider.longitude);
            }

            // Validate converted coordinates are within reasonable California bounds
            const isValidLatitude =
              lat !== null && !isNaN(lat) && lat >= 32.0 && lat <= 42.0;
            const isValidLongitude =
              lng !== null && !isNaN(lng) && lng >= -125.0 && lng <= -114.0;

            if (isValidLatitude && isValidLongitude) {
              provider.latitude = lat;
              provider.longitude = lng;
              provider._coordinatesInvalid = false;
              console.log(
                `✅ Provider ${provider.name}: lat=${provider.latitude}, lng=${provider.longitude}`
              );
            } else {
              console.warn(
                `❌ Provider ${provider.name} (ID: ${provider.id}) has invalid coordinates, SKIPPING:`,
                {
                  original_lat: debugInfo.original_lat,
                  original_lng: debugInfo.original_lng,
                  converted_lat: lat,
                  converted_lng: lng,
                  lat_valid: isValidLatitude,
                  lng_valid: isValidLongitude,
                }
              );
              provider._coordinatesInvalid = true;
            }
          });

          console.log(`Loaded ${this.providers.length} providers from API`);
        }
      } catch (error) {
        // Only show error if we don't already have providers loaded
        if (this.providers && this.providers.length > 0) {
          console.log("⚠️ Search fallback failed, but keeping existing providers:", error.message);
        } else {
          console.error("Error loading providers:", error);
          this.error = "Failed to load providers";
        }

        // Provide sample data in case of error for better user experience
        if (this.providerStore) {
          this.providerStore.providers = [
            {
              id: 1,
              name: "A & H BEHAVIORAL THERAPY (Sample)",
              phone: "909-665-7070",
              description: "SAN FERNANDO VALLEY, HOLLYWOOD",
              type: "Service Provider",
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
        }
      } finally {
        this.loading = false;

        // Force marker update after data change with proper timing
        console.log(`🎯 fetchProviders() completed with ${this.providers.length} providers`);
        console.log(`🎯 About to update markers with ${this.providers.length} providers`);
        console.log(`🎯 Provider details:`, this.providers.slice(0, 2).map(p => ({
          name: p.name,
          lat: p.latitude,
          lng: p.longitude
        })));
        
        this.$nextTick(() => {
          // MapCanvas handles marker updates automatically
          console.log(`🗺️ Markers updated for ${this.providers.length} providers`);
        });
      }
    },

    // Fetch regional centers


    // Fetch service areas
    async fetchServiceAreas() {
      if (this.serviceAreasLoaded) {
        console.log("Service areas already loaded, skipping fetch");
        return; // Already loaded
      }

      try {
        console.log("Fetching service areas from API...");
        const apiRoot = this.getApiRoot();
        const url = `${apiRoot}/api/regional-centers/service_areas/`;

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
      // Toggle the state first
      this.showServiceAreas = !this.showServiceAreas;
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
          // MapCanvas handles marker updates automatically
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
        // MapCanvas handles marker updates automatically
      }
    },

    // Toggle LA Regional Centers overlay
    async toggleLARegionalCenters() {
      this.showLARegionalCenters = !this.showLARegionalCenters;
      console.log(
        "toggleLARegionalCenters called, showLARegionalCenters:",
        this.showLARegionalCenters
      );

      if (!this.map) {
        console.error("Map not initialized yet");
        return;
      }

      if (this.showLARegionalCenters) {
        console.log("Showing LA Regional Centers overlay...");

        // Don't automatically switch to regional centers tab
        // Users can manually switch if they want to see regional centers
        // Make sure we have regional center data so the list and markers appear immediately
        try {
          if (!Array.isArray(this.regionalCenters) || this.regionalCenters.length === 0) {
            await this.regionalCenterData.fetchRegionalCenters();
          }
        } catch (e) {
          console.warn("Failed to pre-load regional centers:", e);
        }

        // Initialize all regional centers as visible
        this.laRegionalCentersList.forEach((center) => {
          if (!(center.name in this.selectedRegionalCenters)) {
            this.$set
              ? this.$set(this.selectedRegionalCenters, center.name, true)
              : (this.selectedRegionalCenters[center.name] = true);
          }
        });

        // Fetch data first if we don't have it
        if (!this.laRegionalCentersData) {
          try {
            console.log("Fetching service area data from API...");
            const response = await fetch(
              `${this.getApiRoot()}/api/regional-centers/service_area_boundaries/`
            );
            if (response.ok) {
              this.laRegionalCentersData = await response.json();
              console.log(
                "Service area data fetched successfully:",
                this.laRegionalCentersData
              );
            } else {
              console.error("Failed to fetch service area data:", response.status);
            }
          } catch (error) {
            console.error("Error fetching service area data:", error);
          }
        } else {
          console.log("Using cached service area data:", this.laRegionalCentersData);
        }
        await this.addLARegionalCentersToMap();
        // Ensure markers/list reflect current state without needing a second click
        try {
          // MapCanvas handles marker updates automatically
        } catch (_) {}
        // While showing ZIP-section overlay, fade county choropleth to avoid confusion
        try {
          if (this.map.getLayer("california-counties-fill")) {
            // Hide LA County fill completely; keep neighbors softly visible
            this.map.setPaintProperty("california-counties-fill", "fill-opacity", [
              "case",
              ["==", ["get", "name"], "Los Angeles"],
              0,
              0.15,
            ]);
          }
          if (this.map.getLayer("california-counties-outline")) {
            this.map.setPaintProperty("california-counties-outline", "line-opacity", 0.3);
          }
        } catch (e) {}
      } else {
        console.log("Hiding LA Regional Centers overlay...");
        this.removeLARegionalCentersFromMap();
        // Restore county layers to default style
        try {
          if (this.map.getLayer("california-counties-fill")) {
            this.map.setPaintProperty("california-counties-fill", "fill-opacity", [
              "case",
              ["boolean", ["feature-state", "hover"], false],
              0.8,
              0.6,
            ]);
          }
          if (this.map.getLayer("california-counties-outline")) {
            this.map.setPaintProperty("california-counties-outline", "line-opacity", 0.8);
          }
        } catch (e) {}
        // Restore ZIP layers visibility when overlay is hidden
        try {
          if (this.map.getLayer("la-zip-codes-fill")) {
            this.map.setLayoutProperty("la-zip-codes-fill", "visibility", "visible");
            this.map.setPaintProperty("la-zip-codes-fill", "fill-opacity", 0.15);
          }
          if (this.map.getLayer("la-zip-codes-outline")) {
            this.map.setLayoutProperty("la-zip-codes-outline", "visibility", "visible");
          }
        } catch (e) {}
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

        // Add California counties source using our production API
        const countiesResponse = await fetch(
          `${this.getApiRoot()}/api/california-counties/`
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

        // County click handler disabled - no county popups needed
        this.map.on("click", "california-counties-fill", (e) => {
          // County popups disabled per user request
          return;
        });

        // Hover effects for California counties
        let hoveredCountyId = null;

        this.map.on("mouseenter", "california-counties-fill", (e) => {
          // No cursor change - counties are not clickable

          if (e.features.length > 0 && e.features[0].id) {
            if (hoveredCountyId !== null) {
              try {
                this.map.setFeatureState(
                  {
                    source: "counties",
                    id: hoveredCountyId,
                  },
                  { hover: false }
                );
              } catch (error) {
                console.warn("Could not reset feature state:", error);
              }
            }
            hoveredCountyId = e.features[0].id;
            try {
              this.map.setFeatureState(
                {
                  source: "counties",
                  id: hoveredCountyId,
                },
                { hover: true }
              );
            } catch (error) {
              console.warn("Could not set feature state:", error);
            }
          }
        });

        this.map.on("mouseleave", "california-counties-fill", () => {
          // No cursor change needed

          if (hoveredCountyId !== null) {
            try {
              this.map.setFeatureState(
                {
                  source: "counties",
                  id: hoveredCountyId,
                },
                { hover: false }
              );
            } catch (error) {
              console.warn("Could not reset feature state on leave:", error);
            }
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

    // Add LA ZIP codes overlay to map
    async addLAZipCodesToMap() {
      try {
      if (!this.map) {
          console.warn("Map not available for LA ZIP codes overlay");
        return;
      }

        // Ensure map is loaded first
      if (!this.map.loaded()) {
        this.map.once("load", () => this.addLAZipCodesToMap());
        return;
      }

        // Clean up any existing layers/sources first
        this.removeLAZipCodesFromMap();

        // Add source from static asset URL
        this.map.addSource("la-zip-codes", {
          type: "geojson",
          data: LA_ZIP_CODES_URL,
        });

        // Filled polygons
        this.map.addLayer({
          id: "la-zip-codes-fill",
          type: "fill",
          source: "la-zip-codes",
          paint: {
            "fill-color": "#8e44ad",
            "fill-opacity": 0.15,
          },
        });

        // Outlines
        this.map.addLayer({
          id: "la-zip-codes-outline",
          type: "line",
          source: "la-zip-codes",
          paint: {
            "line-color": "#8e44ad",
            "line-width": 1.2,
            "line-opacity": 0.7,
          },
        });

        // Basic interactivity
        this.map.on("mouseenter", "la-zip-codes-fill", () => {
          this.map.getCanvas().style.cursor = "pointer";
        });
        this.map.on("mouseleave", "la-zip-codes-fill", () => {
          this.map.getCanvas().style.cursor = "";
        });
        this.map.on("click", "la-zip-codes-fill", (e) => {
          const feature = e.features && e.features[0];
          if (!feature) return;
          const props = feature.properties || {};
          const possibleKeys = [
            "zip",
            "zipcode",
            "ZIP",
            "ZIP_CODE",
            "ZCTA5CE10",
            "ZCTA5CE",
            "name",
            "Name",
          ];
          const zipValue = possibleKeys.find((k) => props[k] !== undefined);
          const zipText = zipValue ? props[zipValue] : "ZIP code";
          new mapboxgl.Popup({ closeOnClick: true })
            .setLngLat(e.lngLat)
            .setHTML(`<div style="font-size:13px"><strong>ZIP:</strong> ${zipText}</div>`)
            .addTo(this.map);
        });

        console.log("LA ZIP codes overlay added");
      } catch (error) {
        console.error("Error adding LA ZIP codes overlay:", error);
      }
    },

    // Remove LA ZIP codes overlay from map
    removeLAZipCodesFromMap() {
      if (!this.map) return;
      try {
        // Remove events
        try {
          this.map.off("click", "la-zip-codes-fill");
        } catch {}
        try {
          this.map.off("mouseenter", "la-zip-codes-fill");
        } catch {}
        try {
          this.map.off("mouseleave", "la-zip-codes-fill");
        } catch {}

        // Remove layers
        if (this.map.getLayer("la-zip-codes-outline")) {
          this.map.removeLayer("la-zip-codes-outline");
        }
        if (this.map.getLayer("la-zip-codes-fill")) {
          this.map.removeLayer("la-zip-codes-fill");
        }
        // Remove source
        if (this.map.getSource("la-zip-codes")) {
          this.map.removeSource("la-zip-codes");
        }
      } catch (e) {
        console.warn("Issue removing LA ZIP codes overlay:", e);
      }
    },

    // Add LA Regional Centers overlay to map (static GeoJSON)
    async addLARegionalCentersToMap() {
      console.log("addLARegionalCentersToMap called (static)");
      if (!this.map) {
        console.error("Map not available");
        return;
      }

      try {
        // Clean up any existing RC layers/sources from previous runs
        const staleLayers = [
          "rc-static-fill",
          "rc-static-outline",
          "la-regional-centers-fill",
          "la-regional-centers-outline",
          "la-county-boundary",
          "la-county-outline",
        ];
        for (const lid of staleLayers) {
          if (this.map.getLayer(lid)) {
            try {
              this.map.removeLayer(lid);
            } catch (e) {}
          }
        }
        if (this.map.getSource("rc")) {
          try {
            this.map.removeSource("rc");
          } catch (e) {}
        }
        if (this.map.getSource("la-regional-centers")) {
          try {
            this.map.removeSource("la-regional-centers");
          } catch (e) {}
        }

        // Fetch static dissolved 7-region GeoJSON and use object directly
        const respStatic = await fetch("/assets/geo/la_rc_7.geojson");
        if (!respStatic.ok) {
          console.error("Failed to load static RC geojson:", respStatic.status);
          return;
        }
        const rcGeoJson = await respStatic.json();
        this.map.addSource("rc", {
          type: "geojson",
          data: rcGeoJson,
        });

        // Build a match expression for fill-color using imported constants
        const centerNames = getRegionalCentersList();
        const colorMatch = [
          "match",
          ["get", "REGIONALCENTER"],
          ...centerNames.flatMap(rc => [rc.name, rc.color]),
          "#9e9e9e", // Default color for unknown centers
        ];

        // Build opacity expression based on user's RC
        const userRCName = this.userRegionalCenterName;
        const opacityExpression = userRCName
          ? [
              "case",
              ["==", ["get", "REGIONALCENTER"], userRCName],
              0.8, // Full opacity for user's RC
              0.3, // Dimmed opacity for other RCs
            ]
          : 0.8; // Default opacity if no user RC

        // Fill layer
        this.map.addLayer({
          id: "rc-static-fill",
          type: "fill",
          source: "rc",
          paint: {
            "fill-color": colorMatch,
            "fill-opacity": opacityExpression,
            "fill-outline-color": "#ffffff",
          },
        });

        // Outline layer
        this.map.addLayer({
          id: "rc-static-outline",
          type: "line",
          source: "rc",
          paint: {
            "line-color": "#ffffff",
            "line-width": 2.5,
          },
        });

        // If base ZIP layer exists and is obscuring, fade it
        try {
          if (this.map.getLayer("la-zip-codes-fill")) {
            this.map.setPaintProperty("la-zip-codes-fill", "fill-opacity", 0.1);
          }
        } catch (e) {}

        // Bring RC layers to the top
        try {
          this.map.moveLayer("rc-static-fill");
          this.map.moveLayer("rc-static-outline");
        } catch (e) {}

        // Interactivity
        this.map.on("mouseenter", "rc-static-fill", () => {
          this.map.getCanvas().style.cursor = "pointer";
        });
        this.map.on("mouseleave", "rc-static-fill", () => {
          this.map.getCanvas().style.cursor = "";
        });

        // Regional center polygon popup disabled per user request
        // this.map.on("click", "rc-static-fill", (e) => {
        //   // Check if the click was on a marker (not on the polygon)
        //   const target = e.originalEvent.target;
        //   if (target.closest('.mapboxgl-marker')) {
        //     // Click was on a marker, don't show regional center popup
        //     return;
        //   }

        //   // Always show polygon popups when clicked on colored regions
        //   const feature = e.features && e.features[0];
        //   const centerName = feature?.properties?.REGIONALCENTER || "Regional Center";

        //   console.log("Clicked on regional center polygon:", centerName);

        //   // Use the rich regional center popup
        //   const html = this.createRegionalCenterPopup(centerName);

        //   new mapboxgl.Popup({
        //     closeOnClick: true,
        //     offset: 25,
        //     maxWidth: "360px",
        //     className: "provider-popup-container",
        //   })
        //     .setLngLat(e.lngLat)
        //     .setHTML(html)
        //     .addTo(this.map);
        // });

        // Hide ZIP layers entirely to avoid visual conflict
        try {
          if (this.map.getLayer("la-zip-codes-fill")) {
            this.map.setLayoutProperty("la-zip-codes-fill", "visibility", "none");
          }
          if (this.map.getLayer("la-zip-codes-outline")) {
            this.map.setLayoutProperty("la-zip-codes-outline", "visibility", "none");
          }
          const layers = this.map.getStyle().layers || [];
          for (const layer of layers) {
            if (
              layer.id &&
              layer.id.startsWith("la-zip-center-") &&
              this.map.getLayer(layer.id)
            ) {
              this.map.setLayoutProperty(layer.id, "visibility", "none");
            }
          }
        } catch (e) {}

        // Fit to bounds of the static overlay so it's visible
        try {
          const bounds = new mapboxgl.LngLatBounds();
          const addCoords = (coords) => {
            for (const c of coords) {
              if (Array.isArray(c[0])) addCoords(c);
              else bounds.extend(c);
            }
          };
          for (const f of rcGeoJson.features || []) {
            const geom = f.geometry;
            if (!geom) continue;
            if (geom.type === "Polygon") addCoords(geom.coordinates);
            else if (geom.type === "MultiPolygon") addCoords(geom.coordinates);
          }
          if (!bounds.isEmpty()) {
            this.map.fitBounds(bounds, { padding: 40, maxZoom: 10, duration: 400 });
          }
        } catch (e) {}

        // Populate legend data from static file
        try {
          const featuresForLegend = (rcGeoJson.features || [])
            .map((f) => {
              const name = f?.properties?.REGIONALCENTER;
              const fillColor = name ? centerColors[name] || "#607d8b" : "#607d8b";
              return {
                type: "Feature",
                properties: { name, fillColor },
                geometry: f.geometry,
              };
            })
            .filter((f) => f.properties.name);
          this.laRegionalCentersData = {
            type: "FeatureCollection",
            features: featuresForLegend,
          };
          // initialize checkbox state if needed
          for (const f of featuresForLegend) {
            const n = f.properties.name;
            if (!(n in this.selectedRegionalCenters)) {
              this.$set
                ? this.$set(this.selectedRegionalCenters, n, true)
                : (this.selectedRegionalCenters[n] = true);
            }
          }
        } catch (_) {}

        console.log("Static Regional Centers overlay added");
      } catch (error) {
        console.error("Error adding static LA Regional Centers overlay:", error);
      }
    },

    /**
     * Update Regional Center polygon highlighting based on user's RC
     * Highlights user's RC at full opacity, dims others
     */
    updateRegionalCenterHighlighting() {
      if (!this.map || !this.map.getLayer("rc-static-fill")) {
        console.log("[MapView] Cannot update RC highlighting - layer not ready");
        return;
      }

      const userRCName = this.userRegionalCenterName;
      console.log("[MapView] Updating RC highlighting for:", userRCName);

      const opacityExpression = userRCName
        ? [
            "case",
            ["==", ["get", "REGIONALCENTER"], userRCName],
            0.8, // Full opacity for user's RC
            0.3, // Dimmed opacity for other RCs
          ]
        : 0.8; // Default opacity if no user RC

      try {
        this.map.setPaintProperty("rc-static-fill", "fill-opacity", opacityExpression);
        console.log("[MapView] RC highlighting updated successfully");
      } catch (error) {
        console.error("[MapView] Error updating RC highlighting:", error);
      }
    },

    // Remove LA Regional Centers overlay from map
    removeLARegionalCentersFromMap() {
      console.log("removeLARegionalCentersFromMap called");

      if (!this.map) {
        console.log("Map not available for removal");
        return;
      }

      try {
        // Remove event listeners first
        this.map.off("click", "la-regional-centers-fill");
        this.map.off("mouseenter", "la-regional-centers-fill");
        this.map.off("mouseleave", "la-regional-centers-fill");
        this.map.off("click", "la-county-boundary");
        this.map.off("mouseenter", "la-county-boundary");
        this.map.off("mouseleave", "la-county-boundary");

        // Remove static and dynamic RC layers
        const rcLayers = [
          "rc-static-fill",
          "rc-static-outline",
          "la-county-boundary",
          "la-county-outline",
          "la-regional-centers-fill",
          "la-regional-centers-outline",
        ];
        for (const lid of rcLayers) {
          if (this.map.getLayer(lid)) {
            try {
              this.map.removeLayer(lid);
              console.log(`Removed ${lid} layer`);
            } catch (e) {}
          }
        }

        // Remove per-center ZIP highlight layers
        try {
          const layers = this.map.getStyle().layers || [];
          for (const layer of layers) {
            if (
              layer.id &&
              layer.id.startsWith("la-zip-center-") &&
              this.map.getLayer(layer.id)
            ) {
              this.map.removeLayer(layer.id);
            }
          }
        } catch (e) {}

        // Restore base ZIP fill opacity if present
        if (this.map.getLayer("la-zip-codes-fill")) {
          try {
            this.map.setPaintProperty("la-zip-codes-fill", "fill-opacity", 0.15);
          } catch (e) {}
        }

        // Remove sources
        if (this.map.getSource("rc")) {
          try {
            this.map.removeSource("rc");
          } catch (e) {}
        }
        if (this.map.getSource("la-regional-centers")) {
          try {
            this.map.removeSource("la-regional-centers");
            console.log("Removed la-regional-centers source");
          } catch (e) {}
        }

        console.log("LA Regional Centers overlay successfully removed from map");
      } catch (error) {
        console.error("Error removing LA Regional Centers overlay from map:", error);
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
    // DEPRECATED: Marker management is now handled by MapCanvas component
    // This method is kept as a no-op stub for backward compatibility
    updateMarkers() {
      // All marker updates are handled automatically by MapCanvas
      return;

      // IMPROVED APPROACH: Create markers all at once without delays to prevent animation artifacts
      console.log("🎯 Creating all markers simultaneously to avoid animation issues");

      let markersCreated = 0;
      let markersSkipped = 0;

      // Create all markers at once
      for (let index = 0; index < items.length; index++) {
        const item = items[index];
        const result = this.createSingleMarker(item, index, items.length);
        if (result.created) {
          markersCreated++;
        } else {
          markersSkipped++;
        }
      }

      console.log(`📊 Marker Creation Summary:`);
      console.log(`   - Total items processed: ${items.length}`);
      console.log(`   - Markers created: ${markersCreated}`);
      console.log(`   - Markers skipped: ${markersSkipped}`);
      console.log(
        `   - Success rate: ${((markersCreated / items.length) * 100).toFixed(1)}%`
      );

      // Fit map to markers if we have any, but avoid zooming out to entire state
      if (this.markers.length > 0) {
        // For regional centers, prefer centering around the user's location if known
        if (
          this.displayType === "regionalCenters" &&
          this.userLocation &&
          this.userLocation.latitude &&
          this.userLocation.longitude
        ) {
          try {
            const currentZoom = this.map.getZoom() || 0;
            const targetZoom = Math.max(currentZoom, 10);
            this.map.flyTo({
              center: [this.userLocation.longitude, this.userLocation.latitude],
              zoom: targetZoom,
              duration: 400,
            });
          } catch (error) {
            console.error("Error centering to user location:", error);
          }
        } else {
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
                maxZoom: 13,
              });
            } catch (error) {
              console.error("Error fitting bounds:", error);
            }
          }
        }
      }

      // Release the lock
      this._updatingMarkers = false;
    },

    // Create a single marker with enhanced error handling
    createSingleMarker(item, index, totalItems) {
      try {
        const itemName =
          item && (item.name || item.regional_center || item.title || "Location");
        console.log(`🔄 Processing item ${index + 1}/${totalItems}: ${itemName}`);

        // Skip items marked as having invalid coordinates
        if (item._coordinatesInvalid) {
          console.log(`⏭️ Skipping ${itemName} due to invalid coordinates`);
          return { created: false, reason: "invalid_coordinates_flag" };
        }

        // Ensure coordinates are numbers and valid
        let lat = parseFloat(item.latitude);
        let lng = parseFloat(item.longitude);

        // Enhanced Debug logging
        console.log(`🔍 Item ${index + 1} coordinates debug for ${itemName}:`, {
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
          console.warn(`⚠️ Invalid coordinates for ${itemName}, skipping marker:`, {
            lat,
            lng,
            original_lat: item.latitude,
            original_lng: item.longitude,
          });
          return { created: false, reason: "invalid_coordinates" };
        }

        // Validate coordinates are within reasonable bounds for CA
        if (lat < 32 || lat > 42 || lng > -114 || lng < -125) {
          console.warn(`⚠️ Coordinates out of CA bounds for ${itemName}: ${lat}, ${lng}`);
          return { created: false, reason: "out_of_bounds" };
        }

        // Additional safety check for coordinates that would place markers at origin (0,0) or other invalid positions
        if (Math.abs(lat) < 0.01 || Math.abs(lng) < 0.01) {
          console.error(
            `🚨 BLOCKED INVALID COORDINATES: ${itemName} has coordinates too close to origin: ${lat}, ${lng}`
          );
          return { created: false, reason: "coordinates_too_close_to_origin" };
        }

        console.log(`✅ Creating marker for ${itemName} at lat=${lat}, lng=${lng}`);

        // Double-check coordinates before creating marker
        const finalLng = Number(lng);
        const finalLat = Number(lat);

        if (isNaN(finalLng) || isNaN(finalLat)) {
          console.error(`🚨 Final coordinate check failed for ${itemName}:`, {
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

        // Use default Mapbox marker to avoid CSS interference
        const markerColor = this.displayType === "providers" ? "#007bff" : "#28a745";

        // Create popup - use rich format for regional centers, simple for providers
        let popupHTML;
        let popupClass;

        // For markers, always use simple popups
        // The polygon clicks already show the detailed regional center info
        const isRegionalCenter = false;

        if (isRegionalCenter) {
          // Use rich popup for regional centers
          popupHTML = this.createRegionalCenterPopup(item.regional_center || item.name);
          popupClass = "provider-popup-container";
        } else {
          // Use simple popup for providers and other locations
          popupHTML = this.createSimplePopup(item);
          popupClass = "simple-popup";
        }

        const popup = new mapboxgl.Popup({
          offset: 25, // Simple offset to keep popup away from marker
          maxWidth: this.displayType === "regionalCenters" ? "360px" : "320px",
          closeOnClick: true,
          closeButton: true,
          className: popupClass,
        }).setHTML(popupHTML);

        // Create and add marker to map
        console.log(`🎯 CREATING MARKER: ${item.name} at [${finalLng}, ${finalLat}]`);
        const marker = new mapboxgl.Marker({
          color: markerColor,
          scale: 0.7, // Slightly smaller
          anchor: "center", // Ensure marker is anchored at center
        })
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

        console.log(
          `📌 Marker successfully created for ${item.name}. Total markers: ${this.markers.length}`
        );
        return { created: true, marker };
      } catch (error) {
        console.error(`🚨 Error creating marker for ${item.name}:`, error);
        return { created: false, reason: "exception", error };
      }
    },

    // Format hours data for display
    formatHours(hours) {
      if (!hours) return "Hours not available";
      
      // If it's already a string, return it
      if (typeof hours === 'string') {
        return hours;
      }
      
      // If it's an object, try to format it nicely
      if (typeof hours === 'object') {
        try {
          // Try to parse as JSON if it's a stringified object
          if (typeof hours === 'string') {
            const parsed = JSON.parse(hours);
            return this.formatHoursObject(parsed);
          }
          
          // If it's already an object, format it
          return this.formatHoursObject(hours);
        } catch (e) {
          console.warn("Could not parse hours object:", hours);
          return "Hours not available";
        }
      }
      
      return "Hours not available";
    },

    // Format hours object into readable text
    formatHoursObject(hoursObj) {
      if (!hoursObj || typeof hoursObj !== 'object') {
        return "Hours not available";
      }
      
      const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
      const formattedHours = [];
      
      days.forEach(day => {
        const dayHours = hoursObj[day.toLowerCase()] || hoursObj[day];
        if (dayHours && dayHours !== 'Closed' && dayHours !== '') {
          formattedHours.push(`${day}: ${dayHours}`);
        }
      });
      
      if (formattedHours.length === 0) {
        return "Hours not available";
      }
      
      return formattedHours.join('\n');
    },

    // Create simple popup content
    createSimplePopup(item) {
      console.log("Creating simple popup for item:", item);
      
      const title = item.name || item.regional_center || "Location";
      const phone = item.phone || item.telephone;
      const mapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${item.latitude},${item.longitude}`;

      // Handle address formatting
      let fullAddress = "";
      if (item.address || item.city || item.state || item.zip_code) {
        try {
          if (item.address && typeof item.address === "string" && item.address.startsWith("{")) {
            const addressData = JSON.parse(item.address);
            if (typeof addressData === "object") {
              fullAddress = [addressData.street, addressData.city, addressData.state, addressData.zip]
                .filter(Boolean)
                .join(", ");
            }
          } else {
            fullAddress = [item.address, item.city, item.state, item.zip_code]
              .filter(Boolean)
              .join(", ");
          }
        } catch (e) {
          fullAddress = [item.address, item.city, item.state, item.zip_code]
            .filter(Boolean)
            .join(", ");
        }
      }

      // Helper function to check if data exists and is not empty/null
      const hasData = (value) => {
        return value && value !== "[]" && value !== "null" && value !== "" && value !== "{}";
      };

      return `
        <div class="provider-popup" style="
          padding: 24px;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
          max-width: 360px;
          background: white;
          border-radius: 8px;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
          overflow: visible;
        ">
          <!-- Header -->
          <div style="
            border-bottom: 1px solid #dee2e6;
            padding-bottom: 16px;
            margin-bottom: 20px;
          ">
            <h5 style="
              margin: 0 0 8px 0;
              color: #212529;
              font-size: 18px;
              font-weight: 600;
              line-height: 1.3;
            ">${title}</h5>
            ${item.type && String(item.type).toLowerCase() !== "main" ? `
              <span style="
                background: #f8f9fa;
                color: #6c757d;
                padding: 3px 8px;
                border-radius: 4px;
                font-size: 11px;
                font-weight: 500;
                text-transform: uppercase;
                letter-spacing: 0.3px;
              ">${item.type}</span>
            ` : ""}
          </div>

          <!-- Content -->
          <div style="margin-bottom: 20px;">
            ${fullAddress ? `
              <div style="
                display: flex;
                align-items: flex-start;
                gap: 16px;
                margin-bottom: 16px;
                padding: 0;
              ">
                <span style="
                  color: #6c757d;
                  font-size: 13px;
                  font-weight: 500;
                  min-width: 70px;
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">Address</span>
                <div style="color: #212529; font-size: 14px; line-height: 1.4;">${fullAddress}</div>
              </div>
            ` : ""}

            ${phone ? `
              <div style="
                display: flex;
                align-items: center;
                gap: 16px;
                margin-bottom: 16px;
                padding: 0;
              ">
                <span style="
                  color: #6c757d;
                  font-size: 13px;
                  font-weight: 500;
                  min-width: 70px;
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">Phone</span>
                <a href="tel:${String(phone).replace(/[^\d+]/g, "")}" style="
                  color: #004877;
                  text-decoration: none;
                  font-size: 14px;
                  font-weight: 500;
                ">${phone}</a>
              </div>
            ` : ""}

            ${hasData(item.website) ? `
              <div style="
                display: flex;
                align-items: center;
                gap: 16px;
                margin-bottom: 16px;
                padding: 0;
              ">
                <span style="
                  color: #6c757d;
                  font-size: 13px;
                  font-weight: 500;
                  min-width: 70px;
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">Website</span>
                <a href="${item.website.startsWith("http") ? item.website : "https://" + item.website}" 
                   target="_blank" 
                   rel="noopener" 
                   style="
                     color: #004877;
                     text-decoration: none;
                     font-size: 14px;
                     font-weight: 500;
                   ">${item.website.replace(/^https?:\/\//, "").replace(/^www\./, "")}</a>
              </div>
            ` : ""}

            ${hasData(item.hours) ? `
              <div style="
                display: flex;
                align-items: flex-start;
                gap: 16px;
                margin-bottom: 16px;
                padding: 0;
              ">
                <span style="
                  color: #6c757d;
                  font-size: 13px;
                  font-weight: 500;
                  min-width: 70px;
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">Hours</span>
                <div style="color: #212529; font-size: 14px; line-height: 1.4; white-space: pre-wrap;">${this.formatHours(item.hours)}</div>
              </div>
            ` : ""}

            ${hasData(item.description) ? `
              <div style="
                display: flex;
                align-items: flex-start;
                gap: 16px;
                margin-bottom: 16px;
                padding: 0;
              ">
                <span style="
                  color: #6c757d;
                  font-size: 13px;
                  font-weight: 500;
                  min-width: 70px;
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">Services</span>
                <div style="color: #212529; font-size: 14px; line-height: 1.4;">${this.formatDescription(item.description)}</div>
              </div>
            ` : ""}

            ${hasData(item.insurance_accepted) ? `
              <div style="
                display: flex;
                align-items: flex-start;
                gap: 16px;
                margin-bottom: 16px;
                padding: 0;
              ">
                <span style="
                  color: #6c757d;
                  font-size: 13px;
                  font-weight: 500;
                  min-width: 70px;
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">Insurance</span>
                <div style="color: #212529; font-size: 14px; line-height: 1.4;">${this.formatInsurance(item.insurance_accepted)}</div>
              </div>
            ` : ""}

            ${hasData(item.languages_spoken) ? `
              <div style="
                display: flex;
                align-items: flex-start;
                gap: 16px;
                margin-bottom: 16px;
                padding: 0;
              ">
                <span style="
                  color: #6c757d;
                  font-size: 13px;
                  font-weight: 500;
                  min-width: 70px;
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">Languages</span>
                <div style="color: #212529; font-size: 14px; line-height: 1.4;">${this.formatLanguages(item.languages_spoken)}</div>
              </div>
            ` : ""}

            ${item.distance ? `
              <div style="
                display: flex;
                align-items: center;
                gap: 16px;
                margin-bottom: 16px;
                padding: 0;
              ">
                <span style="
                  color: #6c757d;
                  font-size: 13px;
                  font-weight: 500;
                  min-width: 70px;
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">Distance</span>
                <div style="color: #212529; font-size: 14px; font-weight: 500;">${item.distance.toFixed(1)} miles</div>
              </div>
            ` : ""}
          </div>

          <!-- Actions -->
          <div style="
            display: flex;
            gap: 8px;
            margin-top: 20px;
            border-top: 1px solid #dee2e6;
            padding-top: 16px;
          ">
            <a href="${mapsUrl}" target="_blank" style="
              background: #004877;
              color: white;
              padding: 8px 16px;
              border-radius: 4px;
              text-decoration: none;
              font-size: 13px;
              font-weight: 500;
              flex: 1;
              text-align: center;
              transition: background-color 0.2s;
            " onmouseover="this.style.background='#003861'" onmouseout="this.style.background='#004877'">
              Directions
            </a>

            ${phone ? `
              <a href="tel:${phone}" style="
                background: #6c757d;
                color: white;
                padding: 8px 16px;
                border-radius: 4px;
                text-decoration: none;
                font-size: 13px;
                font-weight: 500;
                flex: 1;
                text-align: center;
                transition: background-color 0.2s;
              " onmouseover="this.style.background='#5a6268'" onmouseout="this.style.background='#6c757d'">
                Call
              </a>
            ` : ""}

            ${hasData(item.website) ? `
              <a href="${item.website.startsWith("http") ? item.website : "https://" + item.website}" target="_blank" style="
                background: #6c757d;
                color: white;
                padding: 8px 16px;
                border-radius: 4px;
                text-decoration: none;
                font-size: 13px;
                font-weight: 500;
                flex: 1;
                text-align: center;
                transition: background-color 0.2s;
              " onmouseover="this.style.background='#5a6268'" onmouseout="this.style.background='#6c757d'">
                Website
              </a>
            ` : ""}
          </div>
        </div>
      `;
    },

    // Create Regional Center popup with professional styling and richer data
    createRegionalCenterPopup(name) {
      console.log("Creating popup for regional center:", name);

      // Hardcoded data for LA Regional Centers
      const regionalCenterData = {
        "North Los Angeles County Regional Center": {
          address: "15400 Sherman Way, Suite 170",
          city: "Van Nuys",
          state: "CA",
          zip_code: "91406",
          telephone: "(818) 778-1900",
          website: "https://www.nlacrc.org",
        },
        "San Gabriel/Pomona Regional Center": {
          address: "75 Rancho Camino Drive",
          city: "Pomona",
          state: "CA",
          zip_code: "91766",
          telephone: "(909) 620-7722",
          website: "https://www.sgprc.org",
        },
        "Eastern Los Angeles Regional Center": {
          address: "1000 S. Fremont Ave",
          city: "Alhambra",
          state: "CA",
          zip_code: "91803",
          telephone: "(626) 299-4700",
          website: "https://www.elarc.org",
        },
        "Westside Regional Center": {
          address: "5901 Green Valley Circle, Suite 320",
          city: "Culver City",
          state: "CA",
          zip_code: "90230",
          telephone: "(310) 258-4000",
          website: "https://www.westsiderc.org",
        },
        "Frank D. Lanterman Regional Center": {
          address: "3303 Wilshire Blvd., Suite 700",
          city: "Los Angeles",
          state: "CA",
          zip_code: "90010",
          telephone: "(213) 383-1300",
          website: "https://www.lanterman.org",
        },
        "South Central Los Angeles Regional Center": {
          address: "2500 S. Western Avenue",
          city: "Los Angeles",
          state: "CA",
          zip_code: "90018",
          telephone: "(213) 744-7000",
          website: "https://www.sclarc.org",
        },
        "Harbor Regional Center": {
          address: "21231 Hawthorne Boulevard",
          city: "Torrance",
          state: "CA",
          zip_code: "90503",
          telephone: "(310) 540-1711",
          website: "https://www.harborrc.org",
        },
      };

      // Get the hardcoded data for this regional center
      const hardcodedData = regionalCenterData[name] || {};

      // Attempt to find matching regional center from serviceAreas for richer metadata
      let rc = null;
      try {
        const features = this.serviceAreas?.features || [];
        rc = features.find(
          (f) => f?.properties?.regional_center?.toLowerCase() === name?.toLowerCase()
        )?.properties;
      } catch {}

      // Also check regionalCenters array for more complete data
      const rcData = this.regionalCenters?.find(
        (r) => r.regional_center?.toLowerCase() === name?.toLowerCase()
      );

      // Merge data sources, preferring rcData, then rc, then hardcodedData
      const phone = rcData?.telephone || rc?.telephone || hardcodedData.telephone || "";
      const address = rcData?.address || rc?.address || hardcodedData.address || "";
      const city = rcData?.city || rc?.city || hardcodedData.city || "";
      const state = rcData?.state || rc?.state || hardcodedData.state || "";
      const zip = rcData?.zip_code || rc?.zip_code || hardcodedData.zip_code || "";
      const fullAddress = [address, city, state, zip].filter(Boolean).join(", ");

      let website = rcData?.website || rc?.website || hardcodedData.website || "";
      if (website && !website.startsWith("http")) {
        website = `https://${website}`;
      }

      const lat = rcData?.latitude || rc?.latitude;
      const lng = rcData?.longitude || rc?.longitude;

      // Clean phone for tel link
      const phoneClean = phone ? phone.replace(/[^\d+]/g, "") : "";

      // Get website hostname for display
      let websiteDisplay = "";
      if (website) {
        try {
          const u = new URL(website);
          websiteDisplay = u.hostname.replace(/^www\./, "");
        } catch (_) {
          websiteDisplay = website.replace(/^https?:\/\//, "").replace(/^www\./, "");
        }
      }

      console.log("Regional center data found:", { name, phone, address, website });

      return `
        <div class="provider-popup" style="
          padding: 16px;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
          max-width: 320px;
          background: white;
          border-radius: 12px;
          box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
        ">
          <!-- Header -->
          <div style="
            border-bottom: 2px solid #f8f9fa;
            padding-bottom: 12px;
            margin-bottom: 16px;
          ">
            <h5 style="
              margin: 0 0 4px 0;
              color: #2c3e50;
              font-size: 18px;
              font-weight: 700;
              line-height: 1.3;
            ">${name}</h5>
          </div>

          <!-- Content -->
          <div style="margin-bottom: 16px;">
            ${
              fullAddress
                ? `
              <div style="
                margin-bottom: 12px;
                padding: 8px 12px;
                background: #f8f9fa;
                border-radius: 8px;
                border-left: 3px solid #007bff;
              ">
                <div style="
                  color: #495057;
                  font-size: 13px;
                  font-weight: 500;
                  margin-bottom: 2px;
                ">📍 Address</div>
                <div style="color: #6c757d; font-size: 14px;">${fullAddress}</div>
              </div>
            `
                : ""
            }

            ${
              phone
                ? `
              <div style="
                margin-bottom: 12px;
                padding: 8px 12px;
                background: #f8f9fa;
                border-radius: 8px;
                border-left: 3px solid #28a745;
              ">
                <div style="
                  color: #495057;
                  font-size: 13px;
                  font-weight: 500;
                  margin-bottom: 2px;
                ">📞 Phone</div>
                <div style="color: #6c757d; font-size: 14px;">
                  <a href="tel:${phoneClean}" style="color:#0d6efd; text-decoration:none;">${phone}</a>
                </div>
              </div>
            `
                : ""
            }

            ${
              website
                ? `
              <div style="
                margin-bottom: 12px;
                padding: 8px 12px;
                background: #f8f9fa;
                border-radius: 8px;
                border-left: 3px solid #0d6efd;
              ">
                <div style="
                  color: #495057;
                  font-size: 13px;
                  font-weight: 500;
                  margin-bottom: 2px;
                ">🌐 Website</div>
                <div style="color: #6c757d; font-size: 14px;">
                  <a href="${website}" target="_blank" rel="noopener" style="color:#0d6efd; text-decoration:none;">${websiteDisplay}</a>
                </div>
              </div>
            `
                : ""
            }
          </div>

          <!-- Actions -->
          <div style="
            display: flex;
            gap: 8px;
            margin-top: 16px;
            border-top: 1px solid #f8f9fa;
            padding-top: 16px;
          ">
            ${
              fullAddress || (lat && lng)
                ? `
              <a href="https://www.google.com/maps/dir/?api=1&destination=${
                lat && lng ? `${lat},${lng}` : encodeURIComponent(fullAddress)
              }" target="_blank" style="
                background: #007bff;
                color: white;
                padding: 10px 16px;
                border-radius: 8px;
                text-decoration: none;
                font-size: 13px;
                font-weight: 600;
                flex: 1;
                text-align: center;
                transition: background-color 0.2s;
              " onmouseover="this.style.background='#0056b3'" onmouseout="this.style.background='#007bff'">
                🗺️ Directions
              </a>
            `
                : ""
            }

            ${
              phone
                ? `
              <a href="tel:${phoneClean}" style="
                background: #28a745;
                color: white;
                padding: 10px 16px;
                border-radius: 8px;
                text-decoration: none;
                font-size: 13px;
                font-weight: 600;
                flex: 1;
                text-align: center;
                transition: background-color 0.2s;
              " onmouseover="this.style.background='#1e7e34'" onmouseout="this.style.background='#28a745'">
                📞 Call
              </a>
            `
                : ""
            }

            ${
              website
                ? `
              <a href="${website}" target="_blank" style="
                background: #6f42c1;
                color: white;
                padding: 10px 16px;
                border-radius: 8px;
                text-decoration: none;
                font-size: 13px;
                font-weight: 600;
                flex: 1;
                text-align: center;
                transition: background-color 0.2s;
              " onmouseover="this.style.background='#5a2d91'" onmouseout="this.style.background='#6f42c1'">
                🌐 Website
              </a>
            `
                : ""
            }
          </div>
        </div>
      `;
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

    // Onboarding Methods
    checkOnboardingStatus() {
      // Check if user has completed onboarding
      const onboardingComplete = localStorage.getItem("chla-onboarding-complete");
      const hasProfile = localStorage.getItem("chla-user-profile");

      // Show onboarding if not completed and no profile exists
      this.showOnboarding = !onboardingComplete && !hasProfile;
    },

    async handleOnboardingComplete(data) {
      console.log("🎉 ONBOARDING COMPLETED! Data:", data);
      console.log("🎉 Current userData before update:", this.userData);

      // Update user data with onboarding results
      this.userData = { ...this.userData, ...data.userProfile };
      console.log("🎉 Updated userData:", this.userData);

      // Sync user data to filterStore
      if (this.filterStore) {
        this.filterStore.updateUserData({
          age: this.userData.age,
          diagnosis: this.userData.diagnosis,
          therapy: this.userData.therapies ? this.userData.therapies[0] : undefined,
          insurance: this.userData.hasInsurance ? 'insurance' : (this.userData.hasRegionalCenter ? 'regional center' : undefined)
        });
        console.log("🎉 Synced userData to filterStore");
      }

      // Set location if provided
      if (data.userLocation) {
        this.userData.address = data.userLocation;
        console.log("🎉 Set address to:", data.userLocation);
        
        // Geocode ZIP to enable radius filtering
        const zipMatch = data.userLocation.match(/\d{5}/);
        if (zipMatch) {
          await this.ensureZipCodeCoordinates(zipMatch[0]);
        }
      }

      // Use filtered providers from onboarding instead of fetching all providers
      if (data.filteredProviders && data.filteredProviders.length > 0) {
        console.log("🎉 Using filtered providers from onboarding:", data.filteredProviders.length);

        // Update providerStore for new component architecture
        if (this.providerStore) {
          this.providerStore.providers = data.filteredProviders;
          if (data.matchedRegionalCenter) {
            this.providerStore.regionalCenterInfo = data.matchedRegionalCenter;
          }
          console.log("🎉 Updated providerStore with", data.filteredProviders.length, "providers");
        }

        // providerStore is now the source of truth, no need for separate assignment
        // MapCanvas handles marker updates automatically // Update markers with filtered providers
      }

      // Set regional center if provided
      if (data.matchedRegionalCenter) {
        this.matchedRegionalCenter = data.matchedRegionalCenter;
        this.userRegionalCenter = data.matchedRegionalCenter; // Also set for UI
        console.log("🎉 Set regional center:", data.matchedRegionalCenter);
      } else {
        // Find regional center for user's ZIP code
        await this.findUserRegionalCenter();
      }

      // Hide onboarding
      this.showOnboarding = false;
      console.log("🎉 Onboarding hidden, showOnboarding =", this.showOnboarding);

      // Initialize map (but don't fetch providers since we already have them)
      console.log("🎉 Calling initializeAfterOnboarding...");
      this.initializeAfterOnboarding();
    },

    handleOnboardingSkipped() {
      console.log("Onboarding skipped");

      // Mark onboarding as complete to prevent showing again
      localStorage.setItem("chla-onboarding-complete", "true");

      // Hide onboarding
      this.showOnboarding = false;

      // Initialize with default behavior
      this.initializeAfterOnboarding();
    },

    handleEditProfile() {
      console.log("Editing profile - reopening onboarding");
      // Reopen the onboarding flow to allow users to update their profile
      this.showOnboarding = true;
    },

    async handleLocationDetected(locationData) {
      console.log("Location detected from onboarding:", locationData);

      // Update user location
      this.userLocation = {
        latitude: locationData.latitude,
        longitude: locationData.longitude,
        accuracy: null,
        detected: true,
        error: null,
      };

      // Update address
      this.userData.address = locationData.address;

      // Find regional center for user's ZIP code
      await this.findUserRegionalCenter();
    },

    async handleLocationManual(address) {
      console.log("Manual location from onboarding:", address);

      // Update address
      this.userData.address = address;

      // Find regional center for user's ZIP code
      await this.findUserRegionalCenter();

      // Try to geocode the address
      this.geocodeAddress(address);
    },

    async geocodeAddress(address) {
      try {
        // This would use your geocoding service
        // For now, we'll use a basic implementation
        const coordinates = await this.basicGeocode(address);
        if (coordinates) {
          this.userLocation = {
            latitude: coordinates.lat,
            longitude: coordinates.lng,
            accuracy: null,
            detected: true,
            error: null,
          };
        }
      } catch (error) {
        console.error("Geocoding failed:", error);
      }
    },

    async basicGeocode(address) {
      // Enhanced ZIP code geocoding for LA area
      const zipCodes = {
        91361: { lat: 34.1678, lng: -118.5946 }, // Westlake Village
        91362: { lat: 34.1678, lng: -118.5946 }, // Westlake Village
        91377: { lat: 34.1678, lng: -118.5946 }, // Westlake Village
        90210: { lat: 34.103, lng: -118.4104 }, // Beverly Hills
        90211: { lat: 34.0901, lng: -118.4065 }, // Beverly Hills
        90028: { lat: 34.1016, lng: -118.3267 }, // Hollywood
        90046: { lat: 34.1056, lng: -118.3632 }, // West Hollywood
        91436: { lat: 34.1559, lng: -118.4818 }, // Encino
        91301: { lat: 34.2209, lng: -118.601 }, // Agoura Hills
        90405: { lat: 34.0195, lng: -118.4912 }, // Santa Monica
        90401: { lat: 34.0194, lng: -118.4912 }, // Santa Monica
        91505: { lat: 34.1808, lng: -118.309 }, // Burbank
        91304: { lat: 34.2703, lng: -118.737 }, // Canoga Park
        91302: { lat: 34.1678, lng: -118.5946 }, // Calabasas
        91307: { lat: 34.1984, lng: -118.612 }, // West Hills
        91316: { lat: 34.161, lng: -118.5079 }, // Encino
        91324: { lat: 34.2386, lng: -118.5645 }, // Northridge
        91325: { lat: 34.2386, lng: -118.5645 }, // Northridge
        91326: { lat: 34.2386, lng: -118.5645 }, // Northridge
        91356: { lat: 34.1713, lng: -118.5358 }, // Tarzana
        91357: { lat: 34.1713, lng: -118.5358 }, // Tarzana
        91364: { lat: 34.1678, lng: -118.5946 }, // Woodland Hills
        91365: { lat: 34.1678, lng: -118.5946 }, // Woodland Hills
        91367: { lat: 34.1699, lng: -118.6078 }, // Woodland Hills
        91401: { lat: 34.1716, lng: -118.4192 }, // Van Nuys
        91403: { lat: 34.1611, lng: -118.4678 }, // Sherman Oaks
        91406: { lat: 34.2008, lng: -118.503 }, // Van Nuys
        91423: { lat: 34.1869, lng: -118.4456 }, // Sherman Oaks
        91601: { lat: 34.1808, lng: -118.309 }, // North Hollywood
        91604: { lat: 34.1446, lng: -118.4112 }, // Studio City
        91607: { lat: 34.1508, lng: -118.3912 }, // Valley Village
        92660: { lat: 33.6189, lng: -117.9298 }, // Newport Beach
        92661: { lat: 33.6189, lng: -117.9298 }, // Newport Beach
        92625: { lat: 33.6, lng: -117.672 }, // Mission Viejo
        92688: { lat: 33.5225, lng: -117.7075 }, // Laguna Niguel
        92630: { lat: 33.5225, lng: -117.7075 }, // Lake Forest
        92614: { lat: 33.6846, lng: -117.8265 }, // Irvine
        92602: { lat: 33.6846, lng: -117.8265 }, // Irvine
        92603: { lat: 33.6846, lng: -117.8265 }, // Irvine
        92604: { lat: 33.6846, lng: -117.8265 }, // Irvine
        92606: { lat: 33.6846, lng: -117.8265 }, // Irvine
        92612: { lat: 33.6846, lng: -117.8265 }, // Irvine
        92618: { lat: 33.6846, lng: -117.8265 }, // Irvine
        92620: { lat: 33.6846, lng: -117.8265 }, // Irvine
        90019: { lat: 34.0522, lng: -118.2437 }, // Los Angeles
        90020: { lat: 34.0522, lng: -118.2437 }, // Los Angeles
        90036: { lat: 34.0522, lng: -118.2437 }, // Los Angeles
        90048: { lat: 34.0522, lng: -118.2437 }, // Los Angeles
        // San Diego area ZIP codes
        92101: { lat: 32.7157, lng: -117.1611 }, // Downtown San Diego
        92102: { lat: 32.7066, lng: -117.1364 }, // Golden Hill
        92103: { lat: 32.7284, lng: -117.1712 }, // Balboa Park
        92104: { lat: 32.7023, lng: -117.1364 }, // North Park
        92105: { lat: 32.7081, lng: -117.1059 }, // City Heights
        92106: { lat: 32.7461, lng: -117.2186 }, // Point Loma
        92107: { lat: 32.7572, lng: -117.2316 }, // Ocean Beach
        92108: { lat: 32.7672, lng: -117.1978 }, // Mission Bay
        92109: { lat: 32.7959, lng: -117.2348 }, // Pacific Beach
        92110: { lat: 32.7831, lng: -117.1994 }, // Bay Park
        92111: { lat: 32.8067, lng: -117.1661 }, // Clairemont
        92113: { lat: 32.6958, lng: -117.1059 }, // Southeast San Diego
        92114: { lat: 32.7031, lng: -117.0364 }, // Encanto
        92115: { lat: 32.7267, lng: -117.0364 }, // College Area
        92116: { lat: 32.7592, lng: -117.135 }, // Normal Heights (AUTISM RESEARCH INSTITUTE)
        92117: { lat: 32.8256, lng: -117.1661 }, // Clairemont Mesa
        92119: { lat: 32.7892, lng: -117.0442 }, // San Carlos
        92120: { lat: 32.7831, lng: -117.0981 }, // Del Cerro
        92121: { lat: 32.9, lng: -117.25 }, // Sorrento Valley (CORTICA)
        92122: { lat: 32.8667, lng: -117.2081 }, // University City
        92123: { lat: 32.8172, lng: -117.1428 }, // Serra Mesa
        92124: { lat: 32.8172, lng: -117.0817 }, // Tierrasanta
        92126: { lat: 32.9067, lng: -117.1383 }, // Mira Mesa
        92127: { lat: 32.9714, lng: -117.0795 }, // Rancho Bernardo
        92128: { lat: 32.9481, lng: -117.0242 }, // Rancho Peñasquitos
      };

      // Basic geocoding for common CA locations
      const locations = {
        "los angeles": { lat: 34.0522, lng: -118.2437 },
        "san francisco": { lat: 37.7749, lng: -122.4194 },
        "san diego": { lat: 32.7157, lng: -117.1611 },
        sacramento: { lat: 38.5816, lng: -121.4944 },
        fresno: { lat: 36.7468, lng: -119.7725 },
        oakland: { lat: 37.8044, lng: -122.2712 },
        "long beach": { lat: 33.7701, lng: -118.1937 },
        "santa monica": { lat: 34.0195, lng: -118.4912 },
        "beverly hills": { lat: 34.0736, lng: -118.4004 },
        pasadena: { lat: 34.1478, lng: -118.1445 },
        "westlake village": { lat: 34.1678, lng: -118.5946 },
        "thousand oaks": { lat: 34.1706, lng: -118.8376 },
        "agoura hills": { lat: 34.2209, lng: -118.601 },
        calabasas: { lat: 34.1678, lng: -118.5946 },
        "woodland hills": { lat: 34.1678, lng: -118.5946 },
        encino: { lat: 34.1559, lng: -118.4818 },
        tarzana: { lat: 34.1713, lng: -118.5358 },
        "van nuys": { lat: 34.2008, lng: -118.503 },
        "sherman oaks": { lat: 34.1869, lng: -118.4456 },
        "studio city": { lat: 34.1446, lng: -118.4112 },
        "north hollywood": { lat: 34.1808, lng: -118.309 },
        burbank: { lat: 34.1808, lng: -118.309 },
        "west hills": { lat: 34.1984, lng: -118.612 },
        "canoga park": { lat: 34.2703, lng: -118.737 },
        northridge: { lat: 34.2386, lng: -118.5645 },
        irvine: { lat: 33.6846, lng: -117.8265 },
        "newport beach": { lat: 33.6189, lng: -117.9298 },
        "mission viejo": { lat: 33.6, lng: -117.672 },
        "laguna niguel": { lat: 33.5225, lng: -117.7075 },
      };

      const addressLower = address.toLowerCase();

      // Check for exact ZIP code match first - use service area boundaries endpoint
      const zipMatch = address.match(/\b\d{5}\b/);
      if (zipMatch) {
        const zipCode = zipMatch[0];
        console.log(`🔍 Looking up ZIP code: ${zipCode}`);
        
        try {
          // Use the service area boundaries endpoint to find regional center and get coordinates
          const apiRoot = this.getApiRoot();
          const url = `${apiRoot}/api/regional-centers/service_area_boundaries/`;
          
          const response = await fetch(url, { headers: { Accept: "application/json" } });
          if (response.ok) {
            const data = await response.json();
            
            if (data && data.features && Array.isArray(data.features)) {
              // Find regional center by ZIP code
              const matchingCenter = data.features.find(feature => 
                feature.properties.zip_codes && 
                feature.properties.zip_codes.includes(zipCode)
              );
              
              if (matchingCenter) {
                console.log(`✅ Found ZIP code ${zipCode} in: ${matchingCenter.properties.name}`);
                
                // Extract coordinates from the geometry or use center coordinates
                const geometry = matchingCenter.geometry;
                if (geometry && geometry.coordinates) {
                  // For polygon geometry, calculate centroid
                  if (geometry.type === 'Polygon' && geometry.coordinates[0]) {
                    const coords = geometry.coordinates[0];
                    let sumLat = 0, sumLng = 0;
                    for (const coord of coords) {
                      sumLng += coord[0];
                      sumLat += coord[1];
                    }
                    const avgLat = sumLat / coords.length;
                    const avgLng = sumLng / coords.length;
                    console.log(`✅ Calculated coordinates for ZIP ${zipCode}:`, { lat: avgLat, lng: avgLng });
                    return { lat: avgLat, lng: avgLng };
                  }
                }
                
                // Fallback: use approximate coordinates based on regional center
                const centerName = matchingCenter.properties.name.toLowerCase();
                if (centerName.includes('lanterman')) {
                  return { lat: 34.0522, lng: -118.2437 }; // Central LA
                } else if (centerName.includes('north')) {
                  return { lat: 34.2386, lng: -118.5645 }; // Northridge area
                } else if (centerName.includes('eastern')) {
                  return { lat: 34.0522, lng: -118.2437 }; // Eastern LA
                } else if (centerName.includes('harbor')) {
                  return { lat: 33.7701, lng: -118.1937 }; // Harbor area
                } else if (centerName.includes('westside')) {
                  return { lat: 34.0522, lng: -118.2437 }; // Westside LA
                } else if (centerName.includes('south central')) {
                  return { lat: 34.0522, lng: -118.2437 }; // South Central LA
                } else if (centerName.includes('san gabriel')) {
                  return { lat: 34.0522, lng: -118.2437 }; // San Gabriel area
                }
              } else {
                console.log(`❌ ZIP code ${zipCode} not found in service area boundaries`);
              }
            }
          }
        } catch (error) {
          console.error('Error looking up ZIP code in service area boundaries:', error);
        }

        // If ZIP code not found in service areas, try to infer from first digits
        const firstTwo = zipCode.substring(0, 2);
        if (firstTwo === "91" || firstTwo === "90") {
          // LA area ZIP codes
          return { lat: 34.0522, lng: -118.2437 };
        } else if (firstTwo === "92") {
          // Orange County ZIP codes
          return { lat: 33.6846, lng: -117.8265 };
        } else if (firstTwo === "93") {
          // Central Valley ZIP codes
          return { lat: 36.7468, lng: -119.7725 };
        } else if (firstTwo === "94" || firstTwo === "95") {
          // Bay Area ZIP codes
          return { lat: 37.7749, lng: -122.4194 };
        }
      }

      // Check for city names
      for (const [city, coords] of Object.entries(locations)) {
        if (addressLower.includes(city)) {
          console.log(`Found city ${city} coordinates:`, coords);
          return coords;
        }
      }

      return null;
    },

    // Get coordinates for a search location (simpler version of basicGeocode for search)
    async getLocationFromSearch(searchText) {
      // First try the basic geocode
      let result = await this.basicGeocode(searchText);
      
      // If that fails and it looks like a ZIP code, try to geocode it
      if (!result && /^\d{5}(-\d{4})?$/.test(searchText.trim())) {
        console.log(`🔍 ZIP code detected: ${searchText}, attempting geocoding...`);
        // Return a promise-like object that will be handled by the caller
        return { needsGeocoding: true, zipCode: searchText.trim() };
      }
      
      return result;
    },

    async initializeAfterOnboarding() {
      console.log("🚀 Initializing after onboarding...");

      // Note: MapCanvas component handles map initialization
      // this.initMap() is deprecated
      // The map is already initialized by MapCanvas component
      
      // Note: We don't automatically fly to user location to avoid jarring map movement
      // Users can manually navigate to their location if needed

      // Ensure we're showing providers (not regional centers) by default
      this.displayType = "providers";

      // Clear any restrictive filters to show all providers initially
      console.log("🔧 Clearing filters to show all providers initially...");
      this.filterOptions = {
        acceptsInsurance: false,
        acceptsRegionalCenter: false,
        acceptsPrivatePay: false,
        matchesDiagnosis: false,
        matchesAge: false,
        diagnoses: [],
        therapies: [],
      };
      this.searchText = ""; // Clear search text

      // Load regional centers if not already loaded
      if (!Array.isArray(this.regionalCenters) || this.regionalCenters.length === 0) {
        console.log("🏢 Loading regional centers...");
        await this.regionalCenterData.fetchRegionalCenters();
      }

      // Only fetch providers if we don't already have them from onboarding
      if (!this.providers || this.providers.length === 0) {
        console.log("📊 Loading providers without filters...");
        await this.searchProviders();
      } else {
        console.log("📊 Using providers from onboarding:", this.providers.length);
      }
      
      // Force marker update after data is loaded
      console.log("🎯 Updating markers after onboarding...");
      this.$nextTick(() => {
        // MapCanvas handles marker updates automatically
        console.log("✅ Markers updated after onboarding");
      });
    },

    // Close all popups on the map
    closeAllPopups() {
      if (this.map) {
        // Close all popups
        this.map.getStyle().layers.forEach(layer => {
          if (layer.type === 'fill' || layer.type === 'line') {
            // This will close any popups associated with polygon layers
            const popups = document.querySelectorAll('.mapboxgl-popup');
            popups.forEach(popup => {
              if (popup.querySelector('.provider-popup-container')) {
                // This is a regional center popup, close it
                popup.remove();
              }
            });
          }
        });
      }
    },

    // Calculate bounds from provider locations
    calculateProviderBounds() {
      if (!this.providers || this.providers.length === 0) {
        return null;
      }

      const validCoords = this.providers
        .filter(provider => provider.latitude && provider.longitude)
        .map(provider => [parseFloat(provider.longitude), parseFloat(provider.latitude)]);

      if (validCoords.length === 0) {
        return null;
      }

      // Calculate bounding box
      const lngs = validCoords.map(coord => coord[0]);
      const lats = validCoords.map(coord => coord[1]);
      
      let bounds = [
        [Math.min(...lngs), Math.min(...lats)], // Southwest corner
        [Math.max(...lngs), Math.max(...lats)]  // Northeast corner
      ];

      // Always constrain to LA County area as the baseline
      // This ensures the map never zooms out beyond the relevant service area
      const laCountyBounds = [
        [-118.7, 33.7], // Southwest corner of LA County
        [-118.0, 34.4]  // Northeast corner of LA County
      ];

      // If calculated bounds are within LA County, use them
      // Otherwise, use LA County bounds as the maximum extent
      const lngSpan = bounds[1][0] - bounds[0][0];
      const latSpan = bounds[1][1] - bounds[0][1];
      
      if (lngSpan > 1.5 || latSpan > 1.5) {
        console.log("🔍 Using LA County bounds as baseline (calculated bounds too large)");
        bounds = laCountyBounds;
      } else {
        // Ensure bounds don't exceed LA County limits
        bounds[0][0] = Math.max(bounds[0][0], laCountyBounds[0][0]); // West limit
        bounds[0][1] = Math.max(bounds[0][1], laCountyBounds[0][1]); // South limit
        bounds[1][0] = Math.min(bounds[1][0], laCountyBounds[1][0]); // East limit
        bounds[1][1] = Math.min(bounds[1][1], laCountyBounds[1][1]); // North limit
        console.log("🔍 Constrained calculated bounds to LA County limits");
      }

      console.log(`🔍 Final bounds from ${validCoords.length} providers:`, bounds);
      return bounds;
    },

    // Format description text for better readability
    formatDescription(description) {
      if (!description) return "";

      // Clean up the description text
      let cleanDescription = description;

      // Clean up formatting without removing directional words
      cleanDescription = cleanDescription
        .replace(/,/g, ", ") // Add space after commas
        .replace(/\s+/g, " ") // Normalize whitespace
        .trim();

      // Convert to proper title case (capitalize first letter of each word)
      cleanDescription = cleanDescription
        .toLowerCase()
        .replace(/\b\w/g, (l) => l.toUpperCase());

      return cleanDescription;
    },

    // Format insurance information for better readability
    formatInsurance(insurance) {
      if (!insurance) return "";

      try {
        // Try to parse as JSON first
        const parsed = JSON.parse(insurance);
        if (Array.isArray(parsed)) {
          return parsed.join(", ");
        } else if (typeof parsed === "object") {
          return Object.values(parsed).join(", ");
        }
      } catch (e) {
        // If not JSON, treat as string
      }

      // Clean up insurance text
      let cleanInsurance = insurance;

      // Remove all brackets, braces and quotes
      cleanInsurance = cleanInsurance
        .replace(/[\[\]{}]/g, "") // Remove all brackets and braces
        .replace(/['"]/g, "") // Remove all quotes
        .replace(/\s*,\s*/g, ", ") // Normalize comma spacing
        .replace(/\s+/g, " ") // Normalize whitespace
        .trim();

      return cleanInsurance;
    },

    // Format languages information for better readability
    formatLanguages(languages) {
      if (!languages) return "";

      try {
        // Try to parse as JSON first
        const parsed = JSON.parse(languages);
        if (Array.isArray(parsed)) {
          return parsed.join(", ");
        } else if (typeof parsed === "object") {
          return Object.values(parsed).join(", ");
        }
      } catch (e) {
        // If not JSON, treat as string
      }

      // Clean up languages text
      let cleanLanguages = languages;

      // Remove all brackets, braces and quotes
      cleanLanguages = cleanLanguages
        .replace(/[\[\]{}]/g, "") // Remove all brackets and braces
        .replace(/['"]/g, "") // Remove all quotes
        .replace(/\s*,\s*/g, ", ") // Normalize comma spacing
        .replace(/\s+/g, " ") // Normalize whitespace
        .trim();

      return cleanLanguages;
    },
  },
};
</script>

<style>
/* Typography */
* {
  font-family: "Arial", "Helvetica", sans-serif !important;
}

/* Top Navigation Bar */
.top-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  display: flex;
  align-items: center;
}

.navbar-content {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.mobile-menu-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #333;
  padding: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mobile-menu-btn:hover {
  background: #f5f5f5;
  border-radius: 8px;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kindd-text-logo {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(90deg, #0066cc 0%, #ff3366 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-separator {
  color: #e0e0e0;
  font-size: 20px;
}

.brand-subtitle {
  font-size: 16px;
  color: #666;
  font-weight: 500;
}

/* Regional Center Legend in Navbar */
.navbar-legend {
  flex: 1;
  display: flex;
  justify-content: center;
  max-width: 600px;
  margin: 0 auto;
}

.legend-compact {
  position: relative;
}

.legend-toggle-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  transition: all 0.2s;
}

.legend-toggle-btn:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.legend-toggle-btn i:first-child {
  color: #6b7280;
  font-size: 16px;
}

.legend-toggle-btn i:last-child {
  font-size: 12px;
  color: #9ca3af;
}

.legend-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 12px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  min-width: 400px;
  z-index: 2000;
}

.legend-item-compact {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 13px;
  white-space: nowrap;
}

.legend-item-compact:hover {
  background: #f9fafb;
}

.legend-item-compact.is-active {
  background: #eff6ff;
  border: 1px solid #3b82f6;
  font-weight: 600;
  color: #1e40af;
}

.color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 2px solid rgba(0, 0, 0, 0.1);
}

.rc-name-short {
  font-size: 12px;
  line-height: 1.2;
}

.navbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 20px;
  color: #333;
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: background 0.2s;
}

.btn-icon:hover {
  background: #f5f5f5;
}

/* Mobile Search Bar */
.mobile-search-bar {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  background: #ffffff;
  padding: 12px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 999;
  border-bottom: 1px solid #e0e0e0;
  height: 140px; /* Fixed height for consistent layout */
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.search-input {
  flex: 1;
  height: 44px;
  border: 1px solid #e0e0e0;
  border-radius: 22px;
  padding: 0 20px;
  font-size: 16px;
  background: #f8f9fa;
}

.search-input:focus {
  outline: none;
  border-color: #004877;
  background: #ffffff;
}

.search-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #004877;
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.search-btn:hover {
  background: #003861;
}

/* Mobile Filter Pills */
.mobile-filters {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  -webkit-overflow-scrolling: touch;
}

.mobile-filters::-webkit-scrollbar {
  height: 0;
}

.filter-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  background: #ffffff;
  color: #666;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-pill:hover {
  border-color: #004877;
  color: #004877;
}

.filter-pill.active {
  background: #004877;
  color: white;
  border-color: #004877;
}

.filter-pill i {
  font-size: 16px;
}

/* Mobile-specific navbar adjustments */
@media (max-width: 767.98px) {
  .navbar-brand {
    margin-left: 8px;
  }

  .kindd-text-logo {
    font-size: 20px;
  }

  .navbar-actions {
    gap: 4px;
  }

  .btn-icon {
    padding: 8px;
    font-size: 18px;
  }

  /* Mobile popup styling */
  .mapboxgl-popup-content {
    padding: 12px !important;
    max-width: 280px !important;
  }

  .provider-popup {
    padding: 12px !important;
  }

  /* Mobile map controls */
  .mapboxgl-ctrl-bottom-left,
  .mapboxgl-ctrl-bottom-right {
    bottom: 20px !important;
  }

  .mapboxgl-ctrl-top-right {
    top: 80px !important;
    right: 10px !important;
  }
}

.map-app {
  display: flex;
  height: 100vh;
  width: 100%;
  position: relative;
  background: #f8f9fa;
}

.sidebar-container {
  position: fixed;
  left: 0;
  top: 60px; /* Below navbar */
  bottom: 0;
  width: 380px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 5;
  background: #ffffff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  border-right: 1px solid #dee2e6;
  transition: transform 0.3s ease;
}

/* Layout is now consistent regardless of authentication */

/* Desktop sidebar positioning */
@media (min-width: 768px) {
  .sidebar-container {
    transform: translateX(0) !important; /* Always visible on desktop */
  }
}

/* Mobile responsive sidebar - HIDDEN BY DEFAULT */
@media (max-width: 767.98px) {
  .sidebar-container {
    transform: translateX(-100%); /* Hidden off-screen */
    width: 320px;
    max-width: 85vw;
    z-index: 950; /* Below navbar */
  }

  .sidebar-container.mobile-open {
    transform: translateX(0); /* Slide in when toggled */
  }

  /* Map takes full width on mobile when sidebar is hidden */
  .map-container {
    flex: 1;
    width: 100%;
  }

  /* Mobile map container fills screen */
  .map-container-wrapper {
    position: fixed;
    left: 0 !important; /* Full width on mobile */
    top: 60px;
    right: 0;
    bottom: 0;
  }

  /* Adjust map position when search bar is visible */
  .map-container-wrapper.with-search {
    top: 200px !important; /* Navbar (60px) + search bar (140px) */
  }
}

/* Old mobile toggle button styles removed - now using navbar button */

/* Mobile backdrop */
.mobile-backdrop {
  position: fixed;
  top: 60px; /* Below navbar */
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 940; /* Below sidebar */
  animation: fadeIn 0.3s ease;
}

/* LA Regional Centers Legend */
.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* Unified Sidebar Content - Single Scroll */
.sidebar-content {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
}

/* Smooth scrolling */
.sidebar-content::-webkit-scrollbar {
  width: 6px;
}

.sidebar-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.sidebar-content::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 3px;
}

.sidebar-content::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}

/* CHLA Header Styling */
.chla-header {
  background: white;
  color: #004877;
  padding: 1.25rem;
  margin: 0;
  position: relative;
  border-bottom: 1px solid #e5e7eb;
}

/* Sidebar content sections spacing */
.sidebar-content > * {
  padding: 0 1rem;
}

.sidebar-content > .chla-header {
  padding: 1.25rem;
  margin-bottom: 0;
}

.sidebar-content .mb-3 {
  margin-bottom: 0.75rem !important;
}


/* Collapsible Section Styling */
.collapsible-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.collapsible-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: white;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.collapsible-header:hover {
  background: #f9fafb;
}

.collapsible-header .toggle-icon {
  font-size: 1rem;
  color: #6b7280;
  transition: transform 0.2s;
}

.collapsible-content {
  padding: 0.75rem 1rem;
  border-top: 1px solid #e5e7eb;
}

/* Results section with sticky header */
.results-section {
  display: flex;
  flex-direction: column;
  background: #f9fafb;
}

.results-sticky-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #10b981;
  padding: 0.75rem 1rem;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.results-sticky-header .info-card-header {
  display: flex;
  align-items: center;
  color: white;
  margin: 0;
}

.results-sticky-header .info-card-header i {
  color: white !important;
}

.results-sticky-header .badge {
  background: rgba(255, 255, 255, 0.3) !important;
}

.results-content {
  padding: 0;
}

/* Override ProviderList padding to match profile cards */
.results-content :deep(.provider-items) {
  padding: 1rem;
}

.chla-logo-container {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.chla-logo {
  height: 60px;
  max-width: 100%;
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
  color: #004877;
}

.chla-tagline {
  font-size: 14px;
  margin: 0;
  color: #0d9ddb;
  font-style: italic;
  font-weight: 300;
}

.chla-info-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background: #004877;
  border: 1px solid #004877;
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
  background: #0d9ddb;
  border-color: #0d9ddb;
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
  border: 1px solid #dee2e6;
}

.chla-btn {
  font-weight: 400;
  font-size: 14px;
  letter-spacing: 0.5px;
  border: none !important;
  transition: all 0.3s ease;
  border-radius: 0 !important;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
  overflow: hidden;
}

.chla-btn:not(:last-child) {
  border-right: 1px solid #dee2e6 !important;
}

.btn-chla-primary:not(:last-child) {
  border-right: 1px solid rgba(255, 255, 255, 0.3) !important;
}

.chla-btn i {
  font-size: 16px;
  font-weight: 400;
}

.chla-btn span {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-weight: inherit;
  letter-spacing: inherit;
}

.btn-chla-primary {
  background: #004877 !important;
  color: white !important;
  font-weight: 500;
  border: none !important;
}

.btn-chla-primary:hover {
  background: #0d9ddb !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(0, 72, 119, 0.3);
}

.btn-chla-outline {
  background: white !important;
  color: #004877 !important;
  font-weight: 400;
  border: none !important;
}

.btn-chla-outline:hover {
  background: #f8f9fa !important;
  color: #004877 !important;
  box-shadow: 0 2px 8px rgba(0, 72, 119, 0.2);
}

/* CHLA Alert Styling */
.alert {
  border-radius: 6px;
  border: 1px solid;
  font-weight: 400;
}

.alert-success {
  background: #d4edda;
  color: #155724;
  border-color: #4daa50;
}

.alert-warning {
  background: #fff3cd;
  color: #856404;
  border-color: #ffc923;
}

.alert-info {
  background: #d1ecf1;
  color: #0c5460;
  border-color: #0d9ddb;
}

/* Form Controls */
.form-control {
  border-radius: 4px;
  border: 1px solid #ced4da;
  transition: all 0.2s ease;
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

/* Markers - let Mapbox handle positioning via transforms */
/* Don't interfere with Mapbox's transform system */
/* Mapbox uses translate3d transforms to position markers */

/* Simple popup styling */
.mapboxgl-popup.simple-popup .mapboxgl-popup-content {
  padding: 0;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: none;
  overflow: hidden;
  max-height: 70vh;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #004877 #f1f1f1;
}

.mapboxgl-popup.simple-popup .mapboxgl-popup-content::-webkit-scrollbar {
  width: 8px;
}

.mapboxgl-popup.simple-popup .mapboxgl-popup-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.mapboxgl-popup.simple-popup .mapboxgl-popup-content::-webkit-scrollbar-thumb {
  background: #004877;
  border-radius: 4px;
}

.mapboxgl-popup.simple-popup .mapboxgl-popup-content::-webkit-scrollbar-thumb:hover {
  background: #003861;
}

.mapboxgl-popup.simple-popup .mapboxgl-popup-close-button {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  width: 24px;
  height: 24px;
  font-size: 16px;
  line-height: 1;
  color: #666;
  border: 1px solid #ddd;
  top: 8px;
  right: 8px;
  transition: all 0.2s ease;
}

.mapboxgl-popup.simple-popup .mapboxgl-popup-close-button:hover {
  background: white;
  color: #333;
  border-color: #999;
  transform: scale(1.1);
}

.btn-link {
  display: inline-block;
  padding: 6px 10px;
  background: #0d6efd;
  color: #fff;
  border-radius: 6px;
  text-decoration: none;
  font-size: 12px;
}
.btn-link:hover {
  background: #0b5ed7;
}

/* Sidebar action buttons */
.sidebar-action-btn {
  display: flex;
  align-items: center;
  text-align: left;
  white-space: nowrap;
}
.sidebar-action-btn i {
  display: inline-flex;
  width: 1.25rem;
  justify-content: center;
  flex-shrink: 0;
}
/* Responsive adjustments for action buttons */
@media (max-width: 1200px) {
  .action-buttons .sidebar-action-btn {
    padding: 0.6rem 0.8rem;
    font-size: 0.9rem;
  }
}
@media (max-width: 768px) {
  .action-buttons .sidebar-action-btn {
    padding: 0.5rem 0.7rem;
    font-size: 0.85rem;
  }
  .action-buttons .sidebar-action-btn i {
    width: 18px;
  }
}
/* Professional action button styling */
.sidebar .action-buttons {
  max-width: 100%;
  overflow: hidden;
}
.action-buttons .btn-group-vertical {
  gap: 0.5rem;
}
.action-buttons .sidebar-action-btn {
  text-align: left;
  padding: 0.75rem 1rem;
  font-weight: 500;
  transition: all 0.3s ease;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}
.action-buttons .sidebar-action-btn:hover {
  transform: translateX(2px);
  box-shadow: 0 2px 8px rgba(0, 72, 119, 0.15);
}
.action-buttons .sidebar-action-btn.btn-chla-primary {
  background: linear-gradient(135deg, #004877 0%, #0d9ddb 100%);
  border-color: #004877;
  color: white;
}
.action-buttons .sidebar-action-btn.btn-chla-primary:hover {
  background: linear-gradient(135deg, #003861 0%, #0b89c2 100%);
}
.action-buttons .sidebar-action-btn i {
  width: 20px;
  display: inline-block;
  text-align: center;
}
/* Provider and Regional Center popup classes */
.provider-popup-container .mapboxgl-popup-content {
  padding: 0;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: none;
  overflow: hidden;
  background: white;
  max-height: 70vh;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #004877 #f1f1f1;
}

.provider-popup-container .mapboxgl-popup-content::-webkit-scrollbar {
  width: 8px;
}

.provider-popup-container .mapboxgl-popup-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.provider-popup-container .mapboxgl-popup-content::-webkit-scrollbar-thumb {
  background: #004877;
  border-radius: 4px;
}

.provider-popup-container .mapboxgl-popup-content::-webkit-scrollbar-thumb:hover {
  background: #003861;
}
.provider-popup-container .mapboxgl-popup-tip {
  border-top-color: white;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}
.provider-popup-container .mapboxgl-popup-close-button {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  font-size: 18px;
  right: 10px;
  top: 10px;
  color: #6c757d;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}
.provider-popup-container .mapboxgl-popup-close-button:hover {
  background: #f8f9fa;
  color: #333;
  transform: scale(1.05);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.15);
}
.rc-header {
  display: grid;
  grid-template-columns: 44px 1fr;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.rc-badge {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #0d6efd10;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0d6efd;
  font-weight: 700;
}
.rc-headings {
  display: flex;
  flex-direction: column;
}
.rc-title {
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
}
.rc-sub {
  font-size: 12px;
  color: #6c757d;
}
.rc-body {
  display: grid;
  grid-template-columns: 20px 1fr;
  row-gap: 6px;
  column-gap: 8px;
  font-size: 13px;
  color: #34495e;
  align-items: center;
  margin-top: 6px;
}
.rc-row {
  display: contents;
}
.rc-ico {
  text-align: center;
}
.rc-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rc-link {
  color: #0d6efd;
  text-decoration: none;
}
.rc-link:hover {
  text-decoration: underline;
}
.rc-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

/* Enhanced popup tip styling */
.mapboxgl-popup.simple-popup .mapboxgl-popup-tip {
  border-top-color: white;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

/* Simple popup container styling */
.mapboxgl-popup.simple-popup .mapboxgl-popup-content {
  padding: 0;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: none;
  overflow: visible;
  background: white;
  max-height: none;
  overflow-y: visible;
}

.mapboxgl-popup.simple-popup .mapboxgl-popup-close-button {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  font-size: 18px;
  right: 10px;
  top: 10px;
  color: #6c757d;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.mapboxgl-popup.simple-popup .mapboxgl-popup-close-button:hover {
  background: #f8f9fa;
  color: #333;
  transform: scale(1.05);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.15);
}

.bg-primary {
  background: #004877 !important;
}


/* Reset Button */
.btn-secondary {
  background: #6c757d;
  border: none;
  border-radius: 8px;
  font-weight: 600;
}

.btn-secondary:hover {
  background: #5a6268;
}

/* Section Headers */
h5,
h6 {
  color: #004877;
  font-weight: 700;
}

.results-title {
  color: #004877;
  border-bottom: 2px solid #ffc923;
  padding-bottom: 8px;
  margin-bottom: 16px;
}

.map-container-wrapper {
  position: fixed;
  left: 380px; /* Width of sidebar */
  top: 60px; /* Below navbar */
  right: 0;
  bottom: 0;
}

.map-container {
  width: 100%;
  height: 100%;
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

/* Map marker pulse animation - more subtle to avoid interference */
@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
  100% {
    opacity: 1;
  }
}

.pulse-marker {
  animation: pulse 2s infinite;
}

/* Popup styling - let Mapbox handle positioning naturally */
:global(.mapboxgl-popup-content) {
  box-shadow: 0 4px 20px rgba(0, 72, 119, 0.2) !important;
  border: 2px solid #004877 !important;
  border-radius: 8px !important;
}

:global(.mapboxgl-popup-tip) {
  border-top-color: #004877 !important;
}

:global(.mapboxgl-popup-close-button) {
  background: #004877 !important;
  color: white !important;
  border: none !important;
  border-radius: 4px !important;
  width: 24px !important;
  height: 24px !important;
  font-size: 14px !important;
  font-weight: bold !important;
}

:global(.mapboxgl-popup-close-button):hover {
  background: #0d9ddb !important;
}

/* Additional popup styling to ensure visibility */
:global(.mapboxgl-popup-anchor-top .mapboxgl-popup-tip),
:global(.mapboxgl-popup-anchor-top-left .mapboxgl-popup-tip),
:global(.mapboxgl-popup-anchor-top-right .mapboxgl-popup-tip) {
  border-bottom-color: #004877 !important;
}

:global(.mapboxgl-popup-anchor-bottom .mapboxgl-popup-tip),
:global(.mapboxgl-popup-anchor-bottom-left .mapboxgl-popup-tip),
:global(.mapboxgl-popup-anchor-bottom-right .mapboxgl-popup-tip) {
  border-top-color: #004877 !important;
}

/* Marker improvements for dense areas */
/* Removed - let Mapbox handle all marker positioning and z-index */

/* Regional Centers Toggle List */
.regional-centers-list .card {
  border: 1px solid #e9ecef;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.regional-centers-list .card-title {
  color: #004877;
  font-weight: 600;
  font-size: 0.95rem;
  margin-bottom: 1rem;
}

.regional-center-toggles {
  max-height: 300px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #004877 #f1f1f1;
}

.regional-center-toggles::-webkit-scrollbar {
  width: 8px;
}

.regional-center-toggles::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.regional-center-toggles::-webkit-scrollbar-thumb {
  background: #004877;
  border-radius: 4px;
}

.regional-center-toggles::-webkit-scrollbar-thumb:hover {
  background: #003861;
}

/* Nearest centers list styling */
.nearest-centers-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nearest-center-item {
  padding: 6px 8px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 0.875rem;
}

.nearest-center-item:hover {
  background: #e9ecef;
}

.regional-center-toggles .form-check {
  padding: 0.5rem;
  border-radius: 6px;
  transition: background-color 0.2s ease;
}

.regional-center-toggles .form-check:hover {
  background-color: #f8f9fa;
}

.regional-center-toggles .form-check-label {
  cursor: pointer;
  font-size: 0.9rem;
  color: #495057;
}

.regional-center-toggles .color-indicator {
  box-shadow: 0 0 0 2px white, 0 0 0 3px rgba(0, 0, 0, 0.1);
}

/* Ensure popups don't block marker interactions */
:global(.mapboxgl-popup) {
  pointer-events: none;
}

:global(.mapboxgl-popup-content),
:global(.mapboxgl-popup-close-button) {
  pointer-events: auto;
}

/* Add subtle animation when popup opens */
:global(.mapboxgl-popup-content) {
  animation: popupFadeIn 0.2s ease-out;
}

@keyframes popupFadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* Unified Info Card Styling */
.info-card-section {
  width: 100%;
}

.info-card {
  padding: 12px 16px !important;
  border-width: 1px;
  border-style: solid;
  transition: all 0.2s ease;
}

.info-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.info-card-header {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.info-card-header i {
  font-size: 16px;
}

.info-card-content {
  font-size: 13px;
  color: #495057;
}

.info-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.info-card-item {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  gap: 2px;
}

.info-card-item:last-child {
  margin-bottom: 0;
}

.info-card-item i {
  font-size: 14px;
  width: 20px;
  flex-shrink: 0;
}

.info-card-item a {
  color: #0066cc;
  font-weight: 500;
}

.info-card-item a:hover {
  color: #0052a3;
  text-decoration: underline !important;
}

/* Specific color adjustments */
.border-primary.bg-primary.bg-opacity-10 {
  background-color: rgba(13, 110, 253, 0.08) !important;
}

.border-info.bg-info.bg-opacity-10 {
  background-color: rgba(13, 202, 240, 0.08) !important;
}

.border-info.bg-info.bg-opacity-5 {
  background-color: rgba(13, 202, 240, 0.05) !important;
  border-color: #0dcaf0 !important;
}

.border-secondary.bg-secondary.bg-opacity-5 {
  background-color: rgba(108, 117, 125, 0.03) !important;
  border-color: #dee2e6 !important;
}

.border-dark.bg-dark.bg-opacity-5 {
  background-color: rgba(33, 37, 41, 0.05) !important;
  border-color: #495057 !important;
}

.border-warning.bg-warning.bg-opacity-5 {
  background-color: rgba(255, 193, 7, 0.05) !important;
  border-color: #ffc107 !important;
}

.border-success.bg-success.bg-opacity-5 {
  background-color: rgba(25, 135, 84, 0.05) !important;
  border-color: #198754 !important;
}

/* Search input adjustments */
.info-card-content .input-group {
  margin-top: 8px;
}

.info-card-content .input-group .form-control {
  font-size: 13px;
}

.info-card-content .input-group .btn {
  font-size: 13px;
}

/* Button group in info cards */
.info-card-content .btn-group-vertical {
  gap: 0.25rem;
}

/* Subtitle styling */
.info-card-subtitle {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
}
</style>
