// Main Vue Application
const { createApp, ref, reactive, computed, onMounted, watch } = Vue;

const MapApp = {
  setup() {
    // State
    const loading = ref(true);
    const error = ref(null);
    const map = ref(null);
    const userPosition = reactive({
      lat: null,
      lng: null,
      accuracy: null
    });
    const locations = ref([]);
    const filteredLocations = ref([]);
    const selectedLocation = ref(null);
    const categories = ref([]);
    const priceOptions = ref([]);
    const amenities = ref([]);
    
    // Filter state
    const filters = reactive({
      category: '',
      price: '',
      hasParking: false,
      isAccessible: false,
      searchText: '',
      radius: 5 // kilometers
    });
    
    // Map markers
    const markers = ref([]);
    const userMarker = ref(null);
    
    // Methods
    const initMap = () => {
      // Create Leaflet map
      map.value = L.map('map').setView([37.7749, -122.4194], 13); // Default to San Francisco
      
      // Add MapBox tile layer with API key (replace with your own MapBox API key)
      const MAPBOX_API_KEY = 'pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw'; // This is a public demo key
      L.tileLayer('https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token={accessToken}', {
        attribution: '© <a href="https://www.mapbox.com/about/maps/">Mapbox</a> © <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        id: 'mapbox/streets-v11',
        tileSize: 512,
        zoomOffset: -1,
        accessToken: MAPBOX_API_KEY
      }).addTo(map.value);
      
      // Get user location if permission granted
      getUserLocation();
    };
    
    const getUserLocation = () => {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            userPosition.lat = position.coords.latitude;
            userPosition.lng = position.coords.longitude;
            userPosition.accuracy = position.coords.accuracy;
            
            // Center map on user's location
            map.value.setView([userPosition.lat, userPosition.lng], 15);
            
            // Add marker for user's position
            if (userMarker.value) {
              userMarker.value.remove();
            }
            
            const userIcon = L.divIcon({
              html: '<div class="user-marker"></div>',
              className: 'user-marker-container',
              iconSize: [20, 20]
            });
            
            userMarker.value = L.marker([userPosition.lat, userPosition.lng], {
              icon: userIcon,
              title: 'Your Location'
            }).addTo(map.value);
            
            // Add accuracy circle
            L.circle([userPosition.lat, userPosition.lng], {
              radius: userPosition.accuracy,
              color: '#4285F4',
              fillColor: '#4285F4',
              fillOpacity: 0.2
            }).addTo(map.value);
            
            // Load nearby locations
            fetchNearbyLocations();
          },
          (error) => {
            console.error("Error getting user location:", error);
            // Fallback: load all locations
            fetchAllLocations();
          }
        );
      } else {
        console.error("Geolocation is not supported by this browser");
        // Fallback: load all locations
        fetchAllLocations();
      }
    };
    
    const fetchFilterOptions = async () => {
      try {
        const response = await axios.get('/api/locations/filters/');
        categories.value = response.data.categories;
        priceOptions.value = response.data.price_levels.map(level => ({
          id: level,
          name: '$'.repeat(level)
        }));
        amenities.value = response.data.amenities;
      } catch (err) {
        console.error("Error fetching filter options:", err);
        error.value = "Failed to load filter options";
      }
    };
    
    const fetchNearbyLocations = async () => {
      if (!userPosition.lat || !userPosition.lng) return;
      
      loading.value = true;
      try {
        const response = await axios.get('/api/locations/nearby/', {
          params: {
            lat: userPosition.lat,
            lng: userPosition.lng,
            radius: filters.radius
          }
        });
        
        locations.value = response.data;
        applyFilters();
        addMarkersToMap();
        loading.value = false;
      } catch (err) {
        console.error("Error fetching nearby locations:", err);
        error.value = "Failed to load nearby locations";
        loading.value = false;
      }
    };
    
    const fetchAllLocations = async () => {
      loading.value = true;
      try {
        const response = await axios.get('/api/locations/');
        locations.value = response.data;
        applyFilters();
        addMarkersToMap();
        loading.value = false;
      } catch (err) {
        console.error("Error fetching all locations:", err);
        error.value = "Failed to load locations";
        loading.value = false;
      }
    };
    
    const applyFilters = () => {
      if (!locations.value.length) return;
      
      filteredLocations.value = locations.value.filter(location => {
        // Category filter
        if (filters.category && location.category != filters.category) {
          return false;
        }
        
        // Price filter
        if (filters.price && location.price_level != filters.price) {
          return false;
        }
        
        // Amenity filters
        if (filters.hasParking && !location.has_parking) {
          return false;
        }
        
        if (filters.isAccessible && !location.is_accessible) {
          return false;
        }
        
        // Search text
        if (filters.searchText) {
          const searchLower = filters.searchText.toLowerCase();
          const nameMatch = location.name.toLowerCase().includes(searchLower);
          const descMatch = location.description && location.description.toLowerCase().includes(searchLower);
          const addressMatch = location.address.toLowerCase().includes(searchLower);
          const cityMatch = location.city.toLowerCase().includes(searchLower);
          
          if (!nameMatch && !descMatch && !addressMatch && !cityMatch) {
            return false;
          }
        }
        
        return true;
      });
      
      // Update map markers
      addMarkersToMap();
    };
    
    const addMarkersToMap = () => {
      if (!map.value) return;
      
      // Clear existing markers
      markers.value.forEach(marker => marker.remove());
      markers.value = [];
      
      // Add new markers for filtered locations
      filteredLocations.value.forEach(location => {
        const marker = L.marker([location.latitude, location.longitude], {
          title: location.name
        }).addTo(map.value);
        
        marker.bindPopup(`
          <div class="location-popup">
            <h5>${location.name}</h5>
            <p>${location.address}, ${location.city}</p>
            <p>${'★'.repeat(Math.round(location.rating))}${'☆'.repeat(5 - Math.round(location.rating))}</p>
            <button class="btn btn-sm btn-primary view-details">View Details</button>
          </div>
        `);
        
        marker.on('click', () => {
          selectedLocation.value = location;
        });
        
        markers.value.push(marker);
      });
      
      // Adjust map view to fit all markers if we have any
      if (markers.value.length > 0) {
        const group = L.featureGroup(markers.value);
        map.value.fitBounds(group.getBounds().pad(0.1));
      }
    };
    
    const resetFilters = () => {
      filters.category = '';
      filters.price = '';
      filters.hasParking = false;
      filters.isAccessible = false;
      filters.searchText = '';
      applyFilters();
    };
    
    const selectLocation = (location) => {
      selectedLocation.value = location;
      
      // Center map on selected location
      if (map.value && location) {
        map.value.setView([location.latitude, location.longitude], 17);
        
        // Find and open the popup for this location
        markers.value.forEach(marker => {
          if (marker.getLatLng().lat === parseFloat(location.latitude) && 
              marker.getLatLng().lng === parseFloat(location.longitude)) {
            marker.openPopup();
          }
        });
      }
    };
    
    const closeLocationDetails = () => {
      selectedLocation.value = null;
    };
    
    // Watchers
    watch(filters, () => {
      applyFilters();
    }, { deep: true });
    
    watch(() => filters.radius, () => {
      if (userPosition.lat && userPosition.lng) {
        fetchNearbyLocations();
      }
    });
    
    // Lifecycle hooks
    onMounted(() => {
      // Wait a bit for the DOM to be ready
      setTimeout(() => {
        // Initialize the map
        const mapWrapper = document.getElementById('map-wrapper');
        if (!mapWrapper) {
          console.error("Map wrapper element not found. Creating one.");
          const wrapper = document.createElement('div');
          wrapper.id = 'map-wrapper';
          wrapper.className = 'w-100 h-100';
          document.querySelector('.col-md-9').appendChild(wrapper);
        }
        
        if (document.getElementById('map')) {
          initMap();
        } else {
          console.log("Creating map element");
          // Create the map container dynamically if it doesn't exist
          const mapDiv = document.createElement('div');
          mapDiv.id = 'map';
          mapDiv.className = 'map-container';
          document.getElementById('map-wrapper') ? 
            document.getElementById('map-wrapper').appendChild(mapDiv) : 
            document.querySelector('.col-md-9').appendChild(mapDiv);
          initMap();
        }
      }, 100);
      
      // Fetch filter options
      fetchFilterOptions();
    });
    
    return {
      loading,
      error,
      locations,
      filteredLocations,
      selectedLocation,
      categories,
      priceOptions,
      amenities,
      filters,
      userPosition,
      resetFilters,
      selectLocation,
      closeLocationDetails
    };
  },
  template: `
    <div class="container-fluid h-100">
      <div class="row h-100">
        <!-- Sidebar - Search and Filters -->
        <div class="col-md-3 sidebar bg-light p-3 overflow-auto">
          <h2 class="text-center mb-3">Location Finder</h2>
          
          <!-- Search Input -->
          <div class="mb-3">
            <input 
              type="text" 
              v-model="filters.searchText" 
              class="form-control" 
              placeholder="Search locations..."
            >
          </div>
          
          <!-- Filters -->
          <div class="card mb-3">
            <div class="card-header">
              <h5 class="mb-0">Filters</h5>
            </div>
            <div class="card-body">
              <!-- Categories -->
              <div class="mb-3">
                <label class="form-label">Category</label>
                <select v-model="filters.category" class="form-select">
                  <option value="">All Categories</option>
                  <option v-for="category in categories" :value="category.id">
                    {{ category.name }}
                  </option>
                </select>
              </div>
              
              <!-- Price Level -->
              <div class="mb-3">
                <label class="form-label">Price Level</label>
                <select v-model="filters.price" class="form-select">
                  <option value="">Any Price</option>
                  <option v-for="price in priceOptions" :value="price.id">
                    {{ price.name }}
                  </option>
                </select>
              </div>
              
              <!-- Amenities -->
              <div class="mb-3">
                <label class="form-label">Amenities</label>
                <div class="form-check">
                  <input 
                    type="checkbox" 
                    v-model="filters.hasParking" 
                    class="form-check-input" 
                    id="has-parking"
                  >
                  <label class="form-check-label" for="has-parking">Parking Available</label>
                </div>
                <div class="form-check">
                  <input 
                    type="checkbox" 
                    v-model="filters.isAccessible" 
                    class="form-check-input" 
                    id="is-accessible"
                  >
                  <label class="form-check-label" for="is-accessible">Accessibility Features</label>
                </div>
              </div>
              
              <!-- Distance Radius -->
              <div class="mb-3" v-if="userPosition.lat && userPosition.lng">
                <label class="form-label">Distance (km): {{ filters.radius }}</label>
                <input 
                  type="range" 
                  class="form-range" 
                  min="1" 
                  max="50" 
                  step="1" 
                  v-model.number="filters.radius"
                >
              </div>
              
              <!-- Reset Button -->
              <button @click="resetFilters" class="btn btn-secondary w-100">
                Reset Filters
              </button>
            </div>
          </div>
          
          <!-- Results List -->
          <div class="card">
            <div class="card-header d-flex justify-content-between align-items-center">
              <h5 class="mb-0">Results</h5>
              <span class="badge bg-primary">{{ filteredLocations.length }}</span>
            </div>
            <div class="card-body p-0">
              <div v-if="loading" class="p-3 text-center">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">Loading...</span>
                </div>
              </div>
              <div v-else-if="error" class="p-3 text-center text-danger">
                {{ error }}
              </div>
              <div v-else-if="filteredLocations.length === 0" class="p-3 text-center">
                No locations found matching your criteria.
              </div>
              <ul v-else class="list-group list-group-flush">
                <li 
                  v-for="location in filteredLocations" 
                  :key="location.id" 
                  class="list-group-item list-group-item-action"
                  :class="{ active: selectedLocation && selectedLocation.id === location.id }"
                  @click="selectLocation(location)"
                >
                  <h6 class="mb-1">{{ location.name }}</h6>
                  <p class="mb-1 small">{{ location.address }}</p>
                  <div class="d-flex justify-content-between">
                    <small>{{ '$'.repeat(location.price_level) }}</small>
                    <small>{{ '★'.repeat(Math.round(location.rating)) }}{{ '☆'.repeat(5 - Math.round(location.rating)) }}</small>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
        
        <!-- Map and Details -->
        <div class="col-md-9 p-0 position-relative">
          <!-- Map Container -->
          <div id="map-wrapper" class="w-100 h-100">
            <div id="map" class="w-100 h-100"></div>
          </div>
          
          <!-- Selected Location Details -->
          <div 
            v-if="selectedLocation" 
            class="location-details position-absolute bg-white p-3 shadow-lg"
            style="top: 20px; right: 20px; max-width: 350px; z-index: 1000; border-radius: 8px;"
          >
            <div class="d-flex justify-content-between align-items-start mb-2">
              <h4>{{ selectedLocation.name }}</h4>
              <button 
                @click="closeLocationDetails" 
                class="btn-close" 
                aria-label="Close"
              ></button>
            </div>
            
            <div class="mb-2">
              <span class="badge bg-secondary me-1">{{ selectedLocation.category_name }}</span>
              <span>{{ '$'.repeat(selectedLocation.price_level) }}</span>
              <div>{{ '★'.repeat(Math.round(selectedLocation.rating)) }}{{ '☆'.repeat(5 - Math.round(selectedLocation.rating)) }}</div>
            </div>
            
            <p>{{ selectedLocation.address }}, {{ selectedLocation.city }}, {{ selectedLocation.state }} {{ selectedLocation.zip_code }}</p>
            
            <div v-if="selectedLocation.description" class="mb-3">
              <h6>About</h6>
              <p>{{ selectedLocation.description }}</p>
            </div>
            
            <div class="mb-3">
              <h6>Contact</h6>
              <p v-if="selectedLocation.phone" class="mb-1">
                <i class="bi bi-telephone"></i> {{ selectedLocation.phone }}
              </p>
              <p v-if="selectedLocation.email" class="mb-1">
                <i class="bi bi-envelope"></i> {{ selectedLocation.email }}
              </p>
              <p v-if="selectedLocation.website" class="mb-1">
                <a :href="selectedLocation.website" target="_blank">Visit Website</a>
              </p>
            </div>
            
            <div class="mb-3">
              <h6>Features</h6>
              <ul class="list-unstyled">
                <li v-if="selectedLocation.has_parking">
                  <i class="bi bi-p-square"></i> Parking Available
                </li>
                <li v-if="selectedLocation.is_accessible">
                  <i class="bi bi-universal-access"></i> Accessibility Features
                </li>
              </ul>
            </div>
            
            <div v-if="selectedLocation.hours_of_operation" class="mb-3">
              <h6>Hours</h6>
              <pre class="small">{{ selectedLocation.hours_of_operation }}</pre>
            </div>
            
            <div class="d-grid gap-2">
              <a 
                :href="'https://www.google.com/maps/dir/?api=1&destination=' + selectedLocation.latitude + ',' + selectedLocation.longitude" 
                class="btn btn-primary"
                target="_blank"
              >
                Get Directions
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
};

// Mount the Vue application
document.addEventListener('DOMContentLoaded', () => {
  createApp(MapApp).mount('#app');
});

// Add custom CSS
document.head.insertAdjacentHTML('beforeend', `
  <style>
    .user-marker-container {
      background: none !important;
      border: none !important;
    }
    
    .user-marker {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background-color: #4285F4;
      border: 3px solid white;
      box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
    }
    
    .sidebar {
      height: 100%;
      overflow-y: auto;
      background-color: #f8f9fa;
    }
    
    @media (max-width: 768px) {
      .sidebar {
        height: 50vh;
        overflow-y: auto;
      }
    }
  </style>
`);

// Add Bootstrap Icons
document.head.insertAdjacentHTML('beforeend', `
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
`);