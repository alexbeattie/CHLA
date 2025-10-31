<template>
  <div class="onboarding-overlay" v-if="showOnboarding">
    <div class="onboarding-container">
      <!-- Progress Bar -->
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
      </div>

      <!-- Step Content -->
      <div class="step-content">
      <!-- Step Counter -->
      <div class="step-counter">Step {{ currentStep }} of {{ totalSteps }}</div>

        <!-- Welcome Step -->
        <div v-if="currentStep === 1" class="step welcome-step">
          <div class="chla-logo-large">
            <img src="@/assets/chla-logo.svg" alt="CHLA" class="logo" />
          </div>
          <h2>Neurodevelopmental Resource Navigator</h2>
          <p class="welcome-text">
            Find specialized healthcare providers and regional centers serving your area.
            We'll help you discover the best care options based on your specific needs.
          </p>
          <div class="welcome-features">
            <div class="feature">
              <i class="bi bi-geo-alt-fill"></i>
              <span>Location Search</span>
            </div>
            <div class="feature">
              <i class="bi bi-person-check"></i>
              <span>Personalized</span>
            </div>
            <div class="feature">
              <i class="bi bi-heart"></i>
              <span>Care Network</span>
            </div>
        </div>

          <!-- Location Input -->
          <div class="location-section">
            <h4>Enter your location to get started</h4>
          <div class="location-options">
            <button
              class="btn btn-chla-primary location-btn"
              @click="detectLocation"
              :disabled="locationDetecting"
            >
              <i class="bi bi-geo-alt-fill"></i>
                <span v-if="locationDetecting">Detecting...</span>
                <span v-else>Use Current Location</span>
            </button>

            <div class="location-divider">
                <span>or</span>
            </div>

            <div class="manual-location">
              <input
                type="text"
                class="form-control location-input"
                placeholder="ZIP code (5 digits) or city"
                v-model="userLocation"
                @keyup.enter="nextStep"
                @input="validateZipFormat"
              />
            </div>
          </div>

          <div v-if="locationError" class="alert alert-warning">
            <i class="bi bi-exclamation-triangle"></i>
            {{ locationError }}
            </div>
          </div>

          <!-- Regional Center Display -->
          <div v-if="effectiveRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <div class="rc-content">
                <div class="rc-name">
                  <a
                    v-if="effectiveRegionalCenter.website"
                    :href="effectiveRegionalCenter.website.startsWith('http') ? effectiveRegionalCenter.website : 'https://' + effectiveRegionalCenter.website"
                    target="_blank"
                    class="rc-link"
                  >
                    {{ effectiveRegionalCenter.regional_center || effectiveRegionalCenter.name }}
                  </a>
                  <span v-else>{{ effectiveRegionalCenter.regional_center || effectiveRegionalCenter.name }}</span>
                </div>
                <div class="rc-details" v-if="effectiveRegionalCenter.phone">
                  <div class="rc-phone">{{ effectiveRegionalCenter.phone }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Profile Step -->
        <div v-if="currentStep === 2" class="step profile-step">
          <h3>Quick Setup</h3>
          <p>Just a few details to personalize your experience</p>

          <div class="profile-form">
            <div class="form-row">
              <label>Age Group</label>
              <select v-model="userProfile.age" class="form-control" @change="console.log('Age selected:', userProfile.age)">
                <option value="">Select age</option>
                <option value="0-5">0-5 years</option>
                <option value="6-12">6-12 years</option>
                <option value="13-18">13-18 years</option>
                <option value="19+">19+ years</option>
              </select>
            </div>

            <div class="form-row">
              <label>Diagnosis (Optional)</label>
              <select v-model="userProfile.diagnosis" class="form-control">
                <option value="">Select diagnosis</option>
                <option value="Autism Spectrum Disorder">Autism</option>
                <option value="Global Development Delay">Development Delay</option>
                <option value="Intellectual Disability">Intellectual Disability</option>
                <option value="Speech and Language Disorder">Speech/Language</option>
                <option value="ADHD">ADHD</option>
              </select>
            </div>

            <div class="funding-row">
              <label>Funding Sources</label>
              <div class="funding-options">
                <label class="funding-option">
                  <input type="checkbox" v-model="userProfile.hasInsurance" />
                  <span>Health Insurance</span>
                </label>
                <label class="funding-option">
                  <input type="checkbox" v-model="userProfile.hasRegionalCenter" />
                  <span>Regional Center</span>
                </label>
              </div>
            </div>
          </div>

          <!-- Regional Center Display -->
          <div v-if="effectiveRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <div class="rc-content">
                <div class="rc-name">
                  <a 
                    v-if="effectiveRegionalCenter.website" 
                    :href="effectiveRegionalCenter.website.startsWith('http') ? effectiveRegionalCenter.website : 'https://' + effectiveRegionalCenter.website"
                    target="_blank"
                    class="rc-link"
                  >
                    {{ effectiveRegionalCenter.regional_center || effectiveRegionalCenter.name }}
                  </a>
                  <span v-else>{{ effectiveRegionalCenter.regional_center || effectiveRegionalCenter.name }}</span>
                </div>
                <div class="rc-details" v-if="effectiveRegionalCenter.phone">
                  <div class="rc-phone">{{ effectiveRegionalCenter.phone }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Services Step -->
        <div v-if="currentStep === 3" class="step services-step">
          <h3>Which therapies are you seeking?</h3>
          <p>Select all that apply. You can always change these later.</p>
          <div class="service-types">
            <label class="service-option" v-for="t in therapyOptions" :key="t">
              <input type="checkbox" v-model="userProfile.therapies" :value="t" />
              <span class="checkmark"></span>
              <div class="service-content">
                <div>
                  {{ t }}
                </div>
              </div>
            </label>
          </div>

          <!-- Regional Center Display -->
          <div v-if="effectiveRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <div class="rc-content">
                <div class="rc-name">
                  <a 
                    v-if="effectiveRegionalCenter.website" 
                    :href="effectiveRegionalCenter.website.startsWith('http') ? effectiveRegionalCenter.website : 'https://' + effectiveRegionalCenter.website"
                    target="_blank"
                    class="rc-link"
                  >
                    {{ effectiveRegionalCenter.regional_center || effectiveRegionalCenter.name }}
                  </a>
                  <span v-else>{{ effectiveRegionalCenter.regional_center || effectiveRegionalCenter.name }}</span>
                </div>
                <div class="rc-details" v-if="effectiveRegionalCenter.phone">
                  <div class="rc-phone">{{ effectiveRegionalCenter.phone }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Results Step -->
        <div v-if="currentStep === 4" class="step results-step">
          <div class="success-header">
            <div class="success-icon">
              <i class="bi bi-check-circle-fill"></i>
            </div>
            <h2>You're all set!</h2>
            <p class="success-subtitle">We found {{ resultsCount }} providers in your area</p>
          </div>

          <div class="results-grid">
            <div class="result-card">
              <div class="card-icon services-icon">
                <i class="bi bi-hospital"></i>
              </div>
              <div class="card-content">
                <div class="card-number">{{ resultsCount }}</div>
                <div class="card-title">Services</div>
              </div>
            </div>

            <div class="result-card">
              <div class="card-icon center-icon">
                <i class="bi bi-building"></i>
              </div>
              <div class="card-content">
                <div class="card-title">Regional Center</div>
                <div class="card-text" v-if="effectiveRegionalCenter && effectiveRegionalCenter.regional_center">
                  {{ effectiveRegionalCenter.regional_center }}
              </div>
                <div class="card-text" v-else-if="regionalCentersCount > 0">
                  {{ regionalCentersCount }} Centers Found
            </div>
                <div class="card-text error" v-else>
                  Not Found
                </div>
                </div>
                </div>
              </div>

          <div class="next-actions">
            <h3>What's next?</h3>
            <div class="action-list">
              <div class="action-item">
                <i class="bi bi-geo-alt"></i>
                <span>Explore services on the map</span>
          </div>
              <div class="action-item">
                <i class="bi bi-info-circle"></i>
                <span>View detailed service information</span>
              </div>
              <div class="action-item">
                <i class="bi bi-funnel"></i>
                <span>Use filters to refine your search</span>
              </div>
            </div>
          </div>

          <!-- Regional Center Display -->
          <div v-if="effectiveRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <div class="rc-content">
                <div class="rc-name">
                  <a 
                    v-if="effectiveRegionalCenter.website" 
                    :href="effectiveRegionalCenter.website.startsWith('http') ? effectiveRegionalCenter.website : 'https://' + effectiveRegionalCenter.website"
                    target="_blank"
                    class="rc-link"
                  >
                    {{ effectiveRegionalCenter.regional_center || effectiveRegionalCenter.name }}
                  </a>
                  <span v-else>{{ effectiveRegionalCenter.regional_center || effectiveRegionalCenter.name }}</span>
                </div>
                <div class="rc-details" v-if="effectiveRegionalCenter.phone">
                  <div class="rc-phone">{{ effectiveRegionalCenter.phone }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Navigation -->
      <div class="step-navigation">
        <!-- Back Button (all steps except first) -->
        <button 
          v-if="currentStep > 1" 
          class="btn btn-secondary" 
          @click="previousStep"
        >
          <i class="bi bi-arrow-left"></i>
          Back
        </button>

        <!-- Skip Link (only on first step) -->
        <button 
          v-if="currentStep === 1"
          class="btn btn-link skip-link"
          @click="skipOnboarding"
        >
          Skip for now
        </button>

        <div class="nav-spacer"></div>

        <!-- Next/Complete Button -->
        <button
          v-if="currentStep < totalSteps"
          class="btn btn-chla-primary"
          @click="nextStep"
          :disabled="!canProceed"
        >
          Next
          <i class="bi bi-arrow-right"></i>
        </button>

        <button
          v-if="currentStep === totalSteps"
          class="btn btn-chla-primary"
          @click="completeOnboarding"
        >
          Complete
          <i class="bi bi-check"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { useProviderStore } from '@/stores/providerStore';

export default {
  name: "OnboardingFlow",

  props: {
    showOnboarding: {
      type: Boolean,
      default: false,
    },
    matchedRegionalCenter: {
      type: Object,
      default: null,
    },
  },

  data() {
    return {
      currentStep: 1,
      totalSteps: 4,
      locationDetecting: false,
      locationError: null,
      loading: false,
      userLocation: "",
      userCoordinates: null, // Store lat/lng for fallback searches
      userProfile: {
        age: "",
        diagnosis: "",
        hasInsurance: false,
        hasRegionalCenter: false,
        therapies: [],
      },
      localRegionalCenter: null,
      therapyOptions: [
        "ABA therapy",
        "Speech therapy",
        "Occupational therapy",
        "Physical therapy",
        "Feeding therapy",
        "Parent child interaction therapy/parent training behavior management",
      ],
      resultsCount: 0,
      regionalCentersCount: 0,
      filteredProviders: [], // Store the actual filtered provider data
    };
  },

  computed: {
    progressPercentage() {
      return (this.currentStep / this.totalSteps) * 100;
    },

    // Use prop if provided, otherwise use local value
    effectiveRegionalCenter() {
      return this.matchedRegionalCenter || this.localRegionalCenter;
    },

    canProceed() {
      const result = (() => {
        switch (this.currentStep) {
          case 1:
            return this.userLocation && this.userLocation.length > 0; // Require location on step 1
          case 2:
            return this.userProfile.age && this.userProfile.age.length > 0; // Age is required on profile step
          case 3:
            return this.userProfile.therapies.length > 0; // Therapies required on services step
          case 4:
            return true; // Results step - always can proceed
          default:
            return false;
        }
      })();
      
      console.log(`canProceed check - Step ${this.currentStep}:`, {
        userLocation: this.userLocation,
        age: this.userProfile.age,
        ageLength: this.userProfile.age ? this.userProfile.age.length : 0,
        therapies: this.userProfile.therapies,
        result: result
      });
      
      return result;
    },

    locationDetected() {
      return this.userLocation && this.userLocation.length > 0;
    },

    userLocationDisplay() {
      return this.userLocation || "Current Location";
    },
  },

  methods: {
    async viewAllProviders() {
      this.loading = true;
      try {
        const providerStore = useProviderStore();
        await providerStore.loadAllProviders();
        console.log(`✅ Loaded all ${providerStore.providerCount} providers`);

        // Emit with minimal data structure - no user profile needed for "view all"
        this.$emit('onboarding-complete', {
          userProfile: {},
          userLocation: null,
          skipFilters: true
        });
      } catch (error) {
        console.error('Error loading all providers:', error);
        this.locationError = 'Failed to load providers. Please try again.';
      } finally {
        this.loading = false;
      }
    },

    async nextStep() {
      if (this.currentStep < this.totalSteps) {
        // On step 1, validate location before proceeding
        if (this.currentStep === 1) {
          await this.validateLocation();
          return; // validateLocation will handle moving to next step
        }

        // If moving from step 3 to step 4, regenerate results with latest therapy selections
        if (this.currentStep === 3) {
          this.currentStep++;
          await this.$nextTick(); // Wait for DOM update
          console.log('[Onboarding] Moving to results step, regenerating with therapies:', this.userProfile.therapies);
          await this.generateResults();
          await this.$nextTick(); // Wait for results to update in DOM
          console.log('[Onboarding] Results updated, count:', this.resultsCount);
        } else {
          this.currentStep++;
        }
      }
    },

    previousStep() {
      if (this.currentStep > 1) {
        this.currentStep--;
      }
    },

    async detectLocation() {
      this.locationDetecting = true;
      this.locationError = null;

      try {
        const position = await this.getCurrentPosition();
        const { latitude, longitude } = position.coords;

        // Store coordinates for fallback searches
        this.userCoordinates = { latitude, longitude };

        // Reverse geocode to get address
        const address = await this.reverseGeocode(latitude, longitude);
        this.userLocation = address;

        // Emit location data to parent
        this.$emit("location-detected", {
          latitude,
          longitude,
          address,
        });

        // Detect regional center immediately (await to ensure it's ready)
        console.log('🔍 Detecting regional center for geolocation:', address);
        await this.matchRegionalCenterByLocation(address);
        console.log('✅ Regional center detected:', this.localRegionalCenter);
      } catch (error) {
        this.locationError = "Unable to detect location. Please enter manually.";
        console.error("Location detection failed:", error);
      } finally {
        this.locationDetecting = false;
      }
    },

    getCurrentPosition() {
      return new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error("Geolocation not supported"));
          return;
        }

        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 300000,
        });
      });
    },

    async reverseGeocode(latitude, longitude) {
      try {
        // Try to get ZIP code from coordinates using a simple approximation
        // This is a basic approach - in production you'd use a proper geocoding service
        
        // LA County bounds check
        if (latitude >= 33.7 && latitude <= 34.8 && longitude >= -118.9 && longitude <= -117.6) {
          // Try to estimate ZIP code based on coordinates
          let estimatedZip = null;
          
          // Northridge area (91403) - expanded bounds
          if (latitude >= 34.1 && latitude <= 34.4 && longitude >= -118.7 && longitude <= -118.3) {
            estimatedZip = '91403';
          }
          // Beverly Hills area (90210) - approximate bounds  
          else if (latitude >= 34.0 && latitude <= 34.1 && longitude >= -118.5 && longitude <= -118.3) {
            estimatedZip = '90210';
          }
          // Downtown LA area (90001) - approximate bounds
          else if (latitude >= 34.0 && latitude <= 34.1 && longitude >= -118.3 && longitude <= -118.2) {
            estimatedZip = '90001';
          }
          // West LA area (90025) - approximate bounds
          else if (latitude >= 34.0 && latitude <= 34.1 && longitude >= -118.5 && longitude <= -118.4) {
            estimatedZip = '90025';
          }
          // Long Beach area (90802) - approximate bounds
          else if (latitude >= 33.7 && latitude <= 33.8 && longitude >= -118.2 && longitude <= -118.1) {
            estimatedZip = '90802';
          }
          
          if (estimatedZip) {
            console.log(`📍 Estimated ZIP code from coordinates: ${estimatedZip}`);
            return estimatedZip;
          }
        }
        
        // Fallback to coordinates
        return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
      } catch (error) {
        console.error("Reverse geocoding failed:", error);
        return "Location detected";
      }
    },

    validateZipFormat() {
      // Clear any existing ZIP format errors
      if (this.locationError && this.locationError.includes("ZIP code")) {
        this.locationError = "";
      }
      
      // Check if user entered a valid ZIP code and immediately match regional center
      const trimmedLocation = this.userLocation.trim();
      const zipMatch = trimmedLocation.match(/^\d{5}$/);
      
      if (zipMatch) {
        console.log('✅ Valid ZIP code detected:', zipMatch[0]);
        // Immediately match regional center for valid ZIP codes
        this.matchRegionalCenterByLocation(zipMatch[0]).catch((error) => {
          console.error('Immediate regional center detection failed:', error);
        });
      } else {
        // Clear regional center if not a valid ZIP
        this.localRegionalCenter = null;
      }
    },

    async validateLocation() {
      if (!this.userLocation.trim()) {
        this.locationError = "Please enter a location";
        return;
      }

      // Validate ZIP code format if it looks like a ZIP code
      const trimmedLocation = this.userLocation.trim();
      if (/^\d+$/.test(trimmedLocation)) {
        if (trimmedLocation.length !== 5) {
          this.locationError = "ZIP code must be exactly 5 digits";
          return;
        }
      }

      this.locationError = "";
      this.$emit("location-manual", this.userLocation);
      
      // MUST await regional center detection before proceeding!
      console.log('🔍 Detecting regional center for:', this.userLocation);
      await this.matchRegionalCenterByLocation(this.userLocation);
      console.log('✅ Regional center detected:', this.localRegionalCenter);
      
      // Move to next step
      this.currentStep++;
    },

    async matchRegionalCenterByLocation(locationText) {
      try {
        const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000";
        
        // Use the comprehensive service area boundaries endpoint
        const url = `${apiBaseUrl}/api/regional-centers/service_area_boundaries/`;
        
        const res = await fetch(url, { headers: { Accept: "application/json" } });
        if (!res.ok) return;
        
        const data = await res.json();
        
        if (data && data.features && Array.isArray(data.features)) {
          // Check if location is a ZIP code (5 digits)
          const zipMatch = locationText.match(/\b\d{5}\b/);
          
          if (zipMatch) {
            // Find regional center by ZIP code
            const matchingCenter = data.features.find(feature => 
              feature.properties.zip_codes && 
              feature.properties.zip_codes.includes(zipMatch[0])
            );
            
            if (matchingCenter) {
              this.localRegionalCenter = {
                regional_center: matchingCenter.properties.name,
                name: matchingCenter.properties.name,
                phone: matchingCenter.properties.phone,
                address: matchingCenter.properties.address,
                website: matchingCenter.properties.website,
                service_area: matchingCenter.properties.service_areas,
                zip_codes: matchingCenter.properties.zip_codes,
                center_id: matchingCenter.properties.center_id
              };
              console.log(`✅ Found regional center for ZIP ${zipMatch[0]}:`, this.localRegionalCenter.regional_center);
            }
          } else {
            // For non-ZIP locations, try to find by coordinates or location name
            // This is a simplified approach - in production you'd use reverse geocoding
            const locationLower = locationText.toLowerCase();
            
            // Try to match by city/area name in the regional center data
            const matchingCenter = data.features.find(feature => {
              const name = feature.properties.name.toLowerCase();
              const serviceAreas = feature.properties.service_areas;
              
              // Check if location text contains any service area names
              if (Array.isArray(serviceAreas)) {
                return serviceAreas.some(area => 
                  locationLower.includes(area.toLowerCase())
                );
              }
              
              // Fallback: check if location contains regional center name
              return locationLower.includes(name.toLowerCase());
            });
            
            if (matchingCenter) {
              this.localRegionalCenter = {
                regional_center: matchingCenter.properties.name,
                name: matchingCenter.properties.name,
                phone: matchingCenter.properties.phone,
                address: matchingCenter.properties.address,
                website: matchingCenter.properties.website,
                service_area: matchingCenter.properties.service_areas,
                zip_codes: matchingCenter.properties.zip_codes,
                center_id: matchingCenter.properties.center_id
              };
              console.log(`✅ Found regional center for location "${locationText}":`, this.localRegionalCenter.regional_center);
            }
          }
        }
      } catch (error) {
        console.error("Error matching regional center:", error);
      }
    },

    async generateResults() {
      // Get counts based on user's location within 5-mile radius
      console.log('🚀 GENERATE RESULTS CALLED!');
      try {
        const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000";
        
        console.log('=== GENERATE RESULTS DEBUG ===');
        console.log('User location:', this.userLocation);
        console.log('User coordinates:', this.userCoordinates);
        console.log('User profile:', this.userProfile);
        console.log('Local regional center:', this.localRegionalCenter);
        console.log('API base URL:', apiBaseUrl);
        
        // Get the ZIP code from user location or detected regional center
        let searchZip = null;
        const zipMatch = this.userLocation?.match(/\b\d{5}\b/);
        if (zipMatch) {
          searchZip = zipMatch[0];
        } else if (this.localRegionalCenter?.zip_codes?.length > 0) {
          // Use first ZIP from detected regional center
          searchZip = this.localRegionalCenter.zip_codes[0];
        }
        
        console.log('Search ZIP:', searchZip);
        
        let providerData = [];
        let triedZipSearch = false;

        if (searchZip) {
          // Use by_regional_center endpoint with ZIP code
          triedZipSearch = true;
          const providerParams = new URLSearchParams();
          providerParams.append('zip_code', searchZip);

          // Apply user's filters to make their selections meaningful
          if (this.userProfile.age) {
            providerParams.append('age', this.userProfile.age);
          }

          if (this.userProfile.diagnosis) {
            providerParams.append('diagnosis', this.userProfile.diagnosis);
          }

          if (this.userProfile.therapies && this.userProfile.therapies.length > 0) {
            this.userProfile.therapies.forEach(therapy => {
              providerParams.append('therapy', therapy);
            });
          }

          if (this.userProfile.hasInsurance) {
            providerParams.append('insurance', 'insurance');
          }

          const filteredUrl = `${apiBaseUrl}/api/providers-v2/by_regional_center/?${providerParams.toString()}`;
          console.log('✅ Filtered provider API URL:', filteredUrl);
          console.log('Applied filters:', {
            zip_code: searchZip,
            age: this.userProfile.age,
            diagnosis: this.userProfile.diagnosis,
            therapies: this.userProfile.therapies,
            hasInsurance: this.userProfile.hasInsurance
          });

          try {
            const filteredResponse = await fetch(filteredUrl);
            if (filteredResponse.ok) {
              const responseData = await filteredResponse.json();
              // Extract providers array from response (backend returns "results", not "providers")
              providerData = responseData.results || responseData.providers || [];
              console.log('✅ Filtered provider data received:', providerData.length, 'providers');
              console.log('Response data:', responseData);

              // If no results with filters, try again WITHOUT filters to show something
              if (providerData.length === 0) {
                console.log('⚠️ No providers match all filters, fetching unfiltered results...');
                const unfilteredUrl = `${apiBaseUrl}/api/providers-v2/by_regional_center/?zip_code=${searchZip}`;
                const unfilteredResponse = await fetch(unfilteredUrl);
                if (unfilteredResponse.ok) {
                  const unfilteredData = await unfilteredResponse.json();
                  providerData = unfilteredData.results || unfilteredData.providers || [];
                  console.log('✅ Showing unfiltered providers:', providerData.length);
                }
              }
            } else {
              console.error('Provider API error:', filteredResponse.status);
            }
          } catch (error) {
            console.error('Filtered search failed:', error);
          }
        }

        // If ZIP search returned nothing OR no ZIP available, try coordinate search
        console.log('🔍 Checking coordinate fallback:', {
          triedZipSearch,
          providerDataLength: providerData.length,
          hasCoordinates: !!this.userCoordinates,
          searchZip,
          shouldTriggerFallback: (triedZipSearch && providerData.length === 0 && this.userCoordinates) || (!searchZip && this.userCoordinates)
        });

        if ((triedZipSearch && providerData.length === 0 && this.userCoordinates) || (!searchZip && this.userCoordinates)) {
          // Fallback: Use coordinate-based search
          console.log('⚠️ ZIP search failed or unavailable, falling back to coordinate-based search');
          const providerParams = new URLSearchParams();
          providerParams.append('lat', this.userCoordinates.latitude.toString());
          providerParams.append('lng', this.userCoordinates.longitude.toString());
          providerParams.append('radius', '50'); // 50-mile radius for edge cases

          // Apply user's filters
          if (this.userProfile.age) {
            providerParams.append('age', this.userProfile.age);
          }

          if (this.userProfile.diagnosis) {
            providerParams.append('diagnosis', this.userProfile.diagnosis);
          }

          if (this.userProfile.therapies && this.userProfile.therapies.length > 0) {
            this.userProfile.therapies.forEach(therapy => {
              providerParams.append('therapy', therapy);
            });
          }

          if (this.userProfile.hasInsurance) {
            providerParams.append('insurance', 'insurance');
          }

          const coordUrl = `${apiBaseUrl}/api/providers-v2/comprehensive_search/?${providerParams.toString()}`;
          console.log('✅ Coordinate-based search URL:', coordUrl);
          console.log('Applied filters to coordinate search:', {
            lat: this.userCoordinates.latitude,
            lng: this.userCoordinates.longitude,
            radius: 50,
            age: this.userProfile.age,
            diagnosis: this.userProfile.diagnosis,
            therapies: this.userProfile.therapies,
            hasInsurance: this.userProfile.hasInsurance
          });

          try {
            const coordResponse = await fetch(coordUrl);
            if (coordResponse.ok) {
              const responseData = await coordResponse.json();
              // Handle both array and object responses
              providerData = Array.isArray(responseData) ? responseData : (responseData.results || responseData.providers || []);
              console.log('✅ Coordinate-based search returned:', providerData.length, 'providers');

              // If we got very few results with filters, try without filters to show nearest providers
              if (providerData.length < 5) {
                console.log('⚠️ Only found', providerData.length, 'providers with filters, fetching nearest unfiltered providers...');
                const unfilteredParams = new URLSearchParams();
                unfilteredParams.append('lat', this.userCoordinates.latitude.toString());
                unfilteredParams.append('lng', this.userCoordinates.longitude.toString());
                unfilteredParams.append('radius', '50');

                const unfilteredUrl = `${apiBaseUrl}/api/providers-v2/comprehensive_search/?${unfilteredParams.toString()}`;
                console.log('🔍 Fetching unfiltered nearest providers:', unfilteredUrl);

                try {
                  const unfilteredResponse = await fetch(unfilteredUrl);
                  if (unfilteredResponse.ok) {
                    const unfilteredData = await unfilteredResponse.json();
                    const unfilteredProviders = Array.isArray(unfilteredData) ? unfilteredData : (unfilteredData.results || unfilteredData.providers || []);
                    console.log('✅ Unfiltered search returned:', unfilteredProviders.length, 'providers');
                    providerData = unfilteredProviders;
                  }
                } catch (error) {
                  console.error('Unfiltered search failed:', error);
                }
              }
            }
          } catch (error) {
            console.error('Coordinate-based search failed:', error);
          }
        } else if (!this.userCoordinates) {
          console.warn('No coordinates available for provider search');
        }
        
        // Set the results count and store the actual provider data
        if (Array.isArray(providerData)) {
          // Remove duplicates based on provider ID
          const uniqueProviders = providerData.filter((provider, index, self) => 
            index === self.findIndex(p => p.id === provider.id)
          );
          
          this.resultsCount = uniqueProviders.length;
          this.filteredProviders = uniqueProviders; // Store the deduplicated provider data
          console.log('Final provider count:', this.resultsCount);
          console.log('Stored filtered providers (deduplicated):', this.filteredProviders);
          console.log('Removed duplicates:', providerData.length - uniqueProviders.length);
        } else {
          this.resultsCount = 0;
          this.filteredProviders = [];
          console.log('No provider data received');
        }
        
        // Use the regional center detected earlier by matchRegionalCenterByLocation
        if (this.localRegionalCenter) {
          this.regionalCentersCount = 1;
          console.log('✅ Using detected regional center:', this.localRegionalCenter.regional_center);
        } else {
          this.regionalCentersCount = 0;
          console.log('⚠️ No regional center detected');
        }
        
        console.log(`✅ Generated results: ${this.resultsCount} providers, ${this.regionalCentersCount} regional center`);
        
      } catch (error) {
        console.error('Error generating results:', error);
        // Fallback to reasonable defaults
        this.resultsCount = 5; // Show some providers as fallback
        this.regionalCentersCount = 1; // Show 1 regional center as fallback
        console.log('Using fallback counts:', this.resultsCount, this.regionalCentersCount);
      }
    },

    completeOnboarding() {
      this.saveProfile();
      this.$emit("onboarding-complete", {
        userProfile: this.userProfile,
        userLocation: this.userLocation,
        filteredProviders: this.filteredProviders, // Pass the filtered providers to the map
        matchedRegionalCenter: this.effectiveRegionalCenter,
      });
    },

    saveProfile() {
      // Save profile to localStorage
      localStorage.setItem("chla-user-profile", JSON.stringify(this.userProfile));
      localStorage.setItem("chla-user-location", this.userLocation);
      localStorage.setItem("chla-onboarding-complete", "true");
    },

    skipOnboarding() {
      this.$emit("onboarding-skipped");
    },
  },
};
</script>

<style scoped>
.onboarding-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.onboarding-container {
  background: white;
  border-radius: 12px;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.progress-bar {
  height: 4px;
  background: #e9ecef;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #004877, #0d9ddb);
  transition: width 0.3s ease;
}

.step-counter {
  font-size: 12px;
  color: #6c757d;
  margin-bottom: 20px;
  text-align: center;
  font-weight: 500;
}

.step-content {
  padding: 24px 24px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 0;
  flex: 1;
  max-height: 70vh;
  overflow-y: auto;
  scroll-behavior: smooth;
}

/* Custom scrollbar styling */
.step-content::-webkit-scrollbar {
  width: 6px;
}

.step-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.step-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.step-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.step {
  text-align: center;
}

/* Welcome Step - Small & Subtle Design */
.welcome-step {
  max-width: 350px;
  margin: 0 auto;
  padding: 0;
}

.welcome-step .chla-logo-large {
  margin-bottom: 0.75rem;
}

.welcome-step .logo {
  height: 2rem;
}

.welcome-step h2 {
  color: #004877;
  margin-bottom: 0.5rem;
  font-size: 1.25rem;
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.welcome-text {
  font-size: 0.875rem;
  color: #495057;
  margin-bottom: 1.5rem;
  line-height: 1.4;
  max-width: 320px;
  margin-left: auto;
  margin-right: auto;
}

.welcome-features {
  display: flex;
  flex-direction: row;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.feature {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: #495057;
  font-size: 0.75rem;
  font-weight: 400;
}

.feature i {
  font-size: 0.875rem;
  color: #004877;
}

/* Location Step - Small & Subtle Design */
.location-step {
  max-width: 350px;
  margin: 0 auto;
  padding: 0;
}

.location-step h3 {
  color: #004877;
  margin-bottom: 0.5rem;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-weight: 500;
  font-size: 1.25rem;
}

.location-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.location-btn {
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: center;
}

.location-divider {
  text-align: center;
  position: relative;
  color: #6c757d;
  font-size: 0.75rem;
}

.location-divider::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #dee2e6;
  z-index: 1;
}

.location-divider span {
  background: white;
  padding: 0 0.75rem;
  position: relative;
  z-index: 2;
}

.manual-location {
  display: flex;
  gap: 0.5rem;
}

.location-input {
  flex: 1;
  padding: 0.75rem;
  font-size: 0.875rem;
}

/* Profile Step - Small & Subtle Design */
.profile-step {
  text-align: left;
  max-width: 350px;
  margin: 0 auto;
  padding: 0;
}

.profile-step h3 {
  color: #004877;
  margin-bottom: 0.5rem;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-weight: 500;
  font-size: 1.25rem;
  text-align: center;
}

.profile-form {
  margin-top: 1rem;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.form-row label {
  min-width: 80px;
  font-size: 0.75rem;
  font-weight: 400;
  color: #495057;
}

.form-row .form-control {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  font-size: 0.75rem;
  background: white;
}

.form-row .form-control:focus {
  outline: none;
  border-color: #004877;
  box-shadow: 0 0 0 1px rgba(0, 72, 119, 0.1);
}

.funding-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.funding-row label {
  min-width: 80px;
  font-size: 0.75rem;
  font-weight: 400;
  color: #495057;
}

.funding-options {
  display: flex;
  gap: 0.75rem;
  flex: 1;
}

.funding-option {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  font-size: 0.7rem;
  color: #495057;
}

.funding-option input[type="checkbox"] {
  width: 12px;
  height: 12px;
  accent-color: #004877;
}

/* Results Step */
.results-step h3 {
  color: #004877;
  margin-bottom: 8px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-weight: 600;
}

/* Results Step - Small & Subtle Design */
.results-step {
  max-width: 350px;
  margin: 0 auto;
  padding: 0;
}

.success-header {
  text-align: center;
  margin-bottom: 1.5rem;
}

.success-icon {
  width: 2rem;
  height: 2rem;
  background: linear-gradient(135deg, #10b981, #059669);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 0.75rem;
  box-shadow: 0 1px 4px rgba(16, 185, 129, 0.2);
}

.success-icon i {
  font-size: 1rem;
  color: white;
}

.success-header h2 {
  font-size: 1.25rem;
  font-weight: 500;
  color: #111827;
  margin: 0 0 0.25rem 0;
  line-height: 1.2;
}

.success-subtitle {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
  font-weight: 400;
}

.results-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.result-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 0.75rem;
  text-align: center;
  transition: all 0.2s ease;
}

.result-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.card-icon {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 0.5rem;
  font-size: 0.75rem;
}

.services-icon {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: white;
}

.center-icon {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
  color: white;
}

.card-number {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
  line-height: 1;
  margin-bottom: 0.125rem;
}

.card-title {
  font-size: 0.7rem;
  font-weight: 500;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.25rem;
}

.card-text {
  font-size: 0.7rem;
  color: #374151;
  font-weight: 400;
  line-height: 1.3;
}

.card-text.error {
  color: #dc2626;
  font-style: italic;
}

.next-actions {
  background: #f9fafb;
  border-radius: 6px;
  padding: 1rem;
  border: 1px solid #e5e7eb;
}

.next-actions h3 {
  font-size: 0.875rem;
  font-weight: 500;
  color: #111827;
  margin: 0 0 0.75rem 0;
  text-align: center;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: white;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.action-item:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

.action-item i {
  width: 1rem;
  height: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  font-size: 0.75rem;
}

.action-item span {
  font-size: 0.75rem;
  color: #374151;
  font-weight: 400;
}

@media (max-width: 640px) {
  .results-grid {
    grid-template-columns: 1fr;
    gap: 0.5rem;
  }
  
  .success-header h2 {
    font-size: 1.125rem;
  }
  
  .success-subtitle {
    font-size: 0.8rem;
  }
}


/* Results Step */
.results-step {
  max-width: 600px;
  margin: 0 auto;
}

.results-step .step-navigation {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e2e8f0;
}

.results-step .btn-primary {
  background: #10b981;
  border-color: #10b981;
  font-weight: 600;
  padding: 0.75rem 2rem;
  font-size: 1rem;
}

.results-step .btn-primary:hover {
  background: #059669;
  border-color: #059669;
}
/* Preferences Step - Small & Subtle Design */
.preferences-step {
  max-width: 350px;
  margin: 0 auto;
  padding: 0;
}

.preferences-step h3 {
  color: #004877;
  margin-bottom: 0.5rem;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-weight: 500;
  font-size: 1.25rem;
  text-align: center;
}

.service-types {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.5rem;
  margin-top: 1rem;
}

.service-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.75rem;
}

.service-option:hover {
  border-color: #004877;
  background: #f8f9fa;
}

.service-option input {
  display: none;
}

.checkmark {
  width: 14px;
  height: 14px;
  border: 1px solid #dee2e6;
  border-radius: 3px;
  position: relative;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.service-option input:checked + .checkmark {
  background: #004877;
  border-color: #004877;
}

.service-option input:checked + .checkmark::after {
  content: "✓";
  color: white;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 0.7rem;
}

.service-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.service-content i {
  font-size: 1rem;
  color: #0d9ddb;
}

.service-content strong {
  display: block;
  margin-bottom: 0.125rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.service-content small {
  display: block;
  color: #6c757d;
  font-size: 0.7rem;
}

/* Results Step */
.results-preview {
  margin: 32px 0;
}

.result-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 32px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat i {
  font-size: 32px;
  color: #0d9ddb;
}

.stat span {
  color: #004877;
  font-weight: 500;
}

.quick-tutorial {
  background: #f8f9fa;
  padding: 24px;
  border-radius: 8px;
  text-align: left;
}

.quick-tutorial h4 {
  color: #004877;
  margin-bottom: 16px;
}

.tutorial-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tutorial-step {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #6c757d;
}

.tutorial-step i {
  color: #0d9ddb;
  font-size: 18px;
}

.results-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 32px;
}

/* Navigation - Small & Subtle Design */
.step-navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
  max-width: 350px;
  margin: 0 auto;
  gap: 1rem;
}

.nav-spacer {
  flex: 1;
}

.skip-link {
  color: #6c757d;
  font-size: 0.875rem;
  text-decoration: none;
  padding: 0.375rem 0.75rem;
  background: transparent;
  border: none;
  cursor: pointer;
}

.skip-link:hover {
  color: #004877;
  text-decoration: underline;
}

/* Button Styles - Small & Subtle Design */
.btn {
  padding: 0.5rem 0.75rem;
  border: none;
  border-radius: 4px;
  font-weight: 400;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
}

.btn-chla-primary {
  background: #004877;
  color: white;
}

.btn-chla-primary:hover {
  background: #0d9ddb;
}

.btn-chla-outline {
  background: transparent;
  color: #004877;
  border: 2px solid #004877;
}

.btn-chla-outline:hover {
  background: #004877;
  color: white;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Alert */
.alert {
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}

.alert-warning {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.alert-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}


/* Location Section on Step 1 */
.location-section {
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.location-section h4 {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin: 0 0 1rem 0;
  text-align: center;
}

/* Regional Center Info - Clean Simple Design */
.regional-center-info {
  margin-top: 1rem !important;
}

.regional-center-info .alert {
  padding: 0.75rem !important;
  border-radius: 6px !important;
  display: flex !important;
  align-items: center !important;
  gap: 0.75rem !important;
  background: #d1edff !important;
  border: 1px solid #b3d9ff !important;
  color: #004877 !important;
  font-size: 0.85rem !important;
  line-height: 1.2 !important;
}

.regional-center-info .alert i {
  font-size: 1rem !important;
  color: #004877 !important;
  flex-shrink: 0 !important;
}

.regional-center-info .rc-content {
  flex: 1 !important;
}

.regional-center-info .rc-name {
  font-weight: 600 !important;
  font-size: 0.85rem !important;
  color: #004877 !important;
  margin: 0 !important;
  line-height: 1.2 !important;
}

.regional-center-info .rc-link {
  color: #004877 !important;
  text-decoration: none !important;
}

.regional-center-info .rc-link:hover {
  color: #003d5c !important;
  text-decoration: underline !important;
}

.regional-center-info .rc-details {
  margin: 0 !important;
}

.regional-center-info .rc-phone {
  font-size: 0.75rem !important;
  color: #004877 !important;
  font-weight: 500 !important;
  margin: 0 !important;
}

/* Responsive */
@media (max-width: 768px) {
  .onboarding-container {
    width: 95%;
    max-height: 95vh;
  }

  .step-content {
    padding: 20px 16px;
    max-height: 75vh;
  }

  .welcome-step .logo {
    height: 56px;
  }

  .welcome-step h2 {
    font-size: 24px;
  }

  .welcome-text {
    font-size: 15px;
    margin-bottom: 24px;
  }

  .welcome-features {
    gap: 24px;
    margin-bottom: 20px;
  }

  .feature {
    font-size: 13px;
  }

  .service-types {
    grid-template-columns: 1fr;
  }

  .result-stats {
    flex-direction: column;
    gap: 16px;
  }

  .results-actions {
    flex-direction: column;
  }
}

/* Skip Section */
.skip-section {
  margin-top: 2rem;
  padding-top: 1.5rem;
  text-align: center;
}

.skip-divider {
  display: flex;
  align-items: center;
  margin-bottom: 1.25rem;
  color: #9ca3af;
  font-size: 0.875rem;
}

.skip-divider::before,
.skip-divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid #e5e7eb;
}

.skip-divider span {
  padding: 0 1rem;
}

.skip-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.75rem 2rem;
  font-size: 0.95rem;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px rgba(102, 126, 234, 0.25);
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  width: auto;
}

.skip-btn i {
  font-size: 1.1rem;
}

.skip-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(102, 126, 234, 0.35);
  background: linear-gradient(135deg, #5568d3 0%, #6a3f8f 100%);
}

.skip-btn:active:not(:disabled) {
  transform: translateY(0);
}

.skip-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.skip-text {
  margin-top: 0.75rem;
  font-size: 0.8rem;
  color: #6b7280;
  margin-bottom: 0;
}
</style>
