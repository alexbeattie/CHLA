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
                @keyup.enter="validateLocation"
                  @input="validateZipFormat"
              />
              <button
                class="btn btn-chla-outline"
                @click="validateLocation"
                :disabled="!userLocation"
              >
                Continue
              </button>
            </div>
          </div>

          <div v-if="locationError" class="alert alert-warning">
            <i class="bi bi-exclamation-triangle"></i>
            {{ locationError }}
            </div>
          </div>

          <!-- Regional Center Display -->
          <div v-if="matchedRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <strong>{{ matchedRegionalCenter.regional_center || matchedRegionalCenter.name }}</strong>
              <div class="rc-details">
                <div class="rc-service-area">{{ matchedRegionalCenter.service_area }}</div>
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
          <div v-if="matchedRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <strong>{{ matchedRegionalCenter.regional_center || matchedRegionalCenter.name }}</strong>
              <div class="rc-details">
                <div class="rc-service-area">{{ matchedRegionalCenter.service_area }}</div>
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
          <div v-if="matchedRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <strong>{{ matchedRegionalCenter.regional_center || matchedRegionalCenter.name }}</strong>
              <div class="rc-details">
                <div class="rc-service-area">{{ matchedRegionalCenter.service_area }}</div>
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
                <div class="card-text" v-if="matchedRegionalCenter && matchedRegionalCenter.regional_center">
                  {{ matchedRegionalCenter.regional_center }}
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
          <div v-if="matchedRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <strong>{{ matchedRegionalCenter.regional_center || matchedRegionalCenter.name }}</strong>
              <div class="rc-details">
                <div class="rc-service-area">{{ matchedRegionalCenter.service_area }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Navigation -->
      <div class="step-navigation">
        <button v-if="currentStep > 1" class="btn btn-secondary" @click="previousStep">
          <i class="bi bi-arrow-left"></i>
          Back
        </button>

        <div class="nav-spacer"></div>

        <button
          v-if="currentStep < totalSteps"
          class="btn btn-chla-primary"
          @click="nextStep"
          :disabled="!canProceed"
        >
          <span v-if="currentStep === 1">Get Started</span>
          <span v-else>Continue</span>
          <i class="bi bi-arrow-right"></i>
        </button>

        <button
          v-if="currentStep === totalSteps"
          class="btn btn-chla-primary"
          @click="completeOnboarding"
        >
          Complete Setup
          <i class="bi bi-check"></i>
        </button>

        <button
          v-if="currentStep < totalSteps"
          class="btn btn-link skip-btn"
          @click="skipOnboarding"
        >
          Skip for now
        </button>
      </div>
    </div>
  </div>
</template>

<script>
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
      userLocation: "",
      userProfile: {
        age: "",
        diagnosis: "",
        hasInsurance: false,
        hasRegionalCenter: false,
        therapies: [],
      },
      matchedRegionalCenter: null,
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
    };
  },

  computed: {
    progressPercentage() {
      return (this.currentStep / this.totalSteps) * 100;
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
    async nextStep() {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
        if (this.currentStep === 4) {
          await this.generateResults();
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

        // Reverse geocode to get address
        const address = await this.reverseGeocode(latitude, longitude);
        this.userLocation = address;

        // Emit location data to parent
        this.$emit("location-detected", {
          latitude,
          longitude,
          address,
        });

        // Detect regional center immediately
        this.matchRegionalCenterByLocation(address).catch((error) => {
          console.error('Regional center detection failed:', error);
        });
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
        // This would use your geocoding service
        // For now, return a placeholder
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
      
        // Regional center will be detected by parent MapView component
    },

    validateLocation() {
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
      this.matchRegionalCenterByLocation(this.userLocation).catch(() => {});
      this.nextStep();
    },

    async matchRegionalCenterByLocation(locationText) {
      try {
        const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000";
        
        // Check if location is a ZIP code (5 digits)
        const zipMatch = locationText.match(/\b\d{5}\b/);
        let url;
        
        if (zipMatch) {
          url = `${apiBaseUrl}/api/regional-centers/by_zip_code/?zip_code=${zipMatch[0]}`;
        } else {
          url = `${apiBaseUrl}/api/regional-centers/by_location/?location=${encodeURIComponent(locationText)}&radius=40&limit=5`;
        }
        
        const res = await fetch(url, { headers: { Accept: "application/json" } });
        if (!res.ok) return;
        
        const data = await res.json();
        
        if (zipMatch) {
          if (data.regional_center) {
            this.matchedRegionalCenter = data;
          }
        } else {
          if (Array.isArray(data) && data.length > 0) {
            this.matchedRegionalCenter = data[0];
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
        console.log('User profile:', this.userProfile);
        console.log('API base URL:', apiBaseUrl);
        
        // Get provider count within 5-mile radius of user's location
        const providerParams = new URLSearchParams();
        if (this.userLocation) {
          // Clean up the location string - remove extra spaces and fix encoding
          const cleanLocation = this.userLocation.trim().replace(/\s+/g, ' ').replace(/\+/g, '');
          providerParams.append('location', cleanLocation);
          providerParams.append('radius', '5'); // 5-mile radius for focused results
        }
        
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
          providerParams.append('insurance', 'insurance'); // Backend expects lowercase "insurance"
        }
        
        const filteredUrl = `${apiBaseUrl}/api/providers-v2/comprehensive_search/?${providerParams.toString()}`;
        console.log('Filtered provider API URL:', filteredUrl);
        console.log('Applied filters:', {
          age: this.userProfile.age,
          diagnosis: this.userProfile.diagnosis,
          therapies: this.userProfile.therapies,
          hasInsurance: this.userProfile.hasInsurance
        });
        
        let providerData = [];
        try {
          const filteredResponse = await fetch(filteredUrl);
          if (filteredResponse.ok) {
            providerData = await filteredResponse.json();
            console.log('Filtered provider data received:', providerData);
          }
        } catch (error) {
          console.error('Filtered search failed:', error);
        }
        
        // Set the results count
        if (Array.isArray(providerData)) {
          this.resultsCount = providerData.length;
          console.log('Final provider count:', this.resultsCount);
        } else {
          this.resultsCount = 0;
          console.log('No provider data received');
        }
        
        // Get regional center count within 5-mile radius
        const regionalCenterParams = new URLSearchParams();
        if (this.userLocation) {
          // Clean up the location string - remove extra spaces and fix encoding
          const cleanLocation = this.userLocation.trim().replace(/\s+/g, ' ').replace(/\+/g, '');
          
          // Check if location is a ZIP code (5 digits)
          const zipMatch = cleanLocation.match(/\b\d{5}\b/);
          if (zipMatch) {
            // Use ZIP code API for better accuracy
            regionalCenterParams.append('zip_code', zipMatch[0]);
            console.log('Using ZIP code API for:', zipMatch[0]);
          } else {
            // Check if location is GPS coordinates (lat,lng format)
            console.log('About to test regex on:', JSON.stringify(cleanLocation));
            const coordMatch = cleanLocation.match(/^(-?\d+\.?\d*),\s*(-?\d+\.?\d*)$/);
            console.log('Original location:', this.userLocation);
            console.log('Cleaned location:', cleanLocation);
            console.log('coordMatch result:', coordMatch);
            if (coordMatch) {
              const lat = parseFloat(coordMatch[1]);
              const lng = parseFloat(coordMatch[2]);
              console.log('GPS coordinates detected:', lat, lng);
              console.log('Checking if coordinates are in LA County bounds...');
              
              // For LA County coordinates, try to determine ZIP code
              // This is a rough approximation - in a real app you'd use reverse geocoding
              if (lat >= 33.7 && lat <= 34.8 && lng >= -118.9 && lng <= -117.6) {
                console.log('✅ Coordinates are in LA County bounds');
                // This is LA County - try to find ZIP code from coordinates
                // For now, let's use a simple approximation based on common LA ZIP codes
                let estimatedZip = null;
                
                // Northridge area (91403) - expanded bounds
                if (lat >= 34.1 && lat <= 34.4 && lng >= -118.7 && lng <= -118.3) {
                  estimatedZip = '91403';
                  console.log('✅ Estimated ZIP code: 91403 (Northridge area)');
                }
                // Beverly Hills area (90210) - approximate bounds  
                else if (lat >= 34.0 && lat <= 34.1 && lng >= -118.5 && lng <= -118.3) {
                  estimatedZip = '90210';
                }
                // Downtown LA area (90001) - approximate bounds
                else if (lat >= 34.0 && lat <= 34.1 && lng >= -118.3 && lng <= -118.2) {
                  estimatedZip = '90001';
                }
                // West LA area (90025) - approximate bounds
                else if (lat >= 34.0 && lat <= 34.1 && lng >= -118.5 && lng <= -118.4) {
                  estimatedZip = '90025';
                }
                
                if (estimatedZip) {
                  regionalCenterParams.append('zip_code', estimatedZip);
                  console.log('Estimated ZIP code from GPS:', estimatedZip);
                } else {
                  // Fall back to location API
                  regionalCenterParams.append('location', cleanLocation);
                  regionalCenterParams.append('radius', '5');
                  console.log('Using location API for GPS coordinates:', cleanLocation);
                }
              } else {
                // Outside LA County - use location API
                regionalCenterParams.append('location', cleanLocation);
                regionalCenterParams.append('radius', '5');
                console.log('Outside LA County, using location API:', cleanLocation);
              }
            } else {
              // Use location API for addresses/other formats
              regionalCenterParams.append('location', cleanLocation);
              regionalCenterParams.append('radius', '5');
              console.log('Using location API for:', cleanLocation);
            }
          }
        }
        
        // Determine which API endpoint to use
        let regionalCenterUrl;
        if (regionalCenterParams.has('zip_code')) {
          regionalCenterUrl = `${apiBaseUrl}/api/regional-centers/by_zip_code/?${regionalCenterParams.toString()}`;
        } else {
          regionalCenterUrl = `${apiBaseUrl}/api/regional-centers/by_location/?${regionalCenterParams.toString()}`;
        }
        
        console.log('Regional center API URL:', regionalCenterUrl);
        console.log('Regional center params:', regionalCenterParams.toString());
        
        const regionalCenterResponse = await fetch(regionalCenterUrl, { 
          headers: { Accept: "application/json" } 
        });
        
        console.log('Regional center response status:', regionalCenterResponse.status);
        
        if (regionalCenterResponse.ok) {
          const regionalCenterData = await regionalCenterResponse.json();
          console.log('Regional center data received:', regionalCenterData);
          
          // Handle different response formats
          if (regionalCenterParams.has('zip_code')) {
            // ZIP code API returns a single object or error
            if (regionalCenterData.regional_center) {
              this.regionalCentersCount = 1;
              this.matchedRegionalCenter = regionalCenterData; // Store the regional center data
              console.log('Regional center count (ZIP code):', this.regionalCentersCount);
              console.log('Regional center name:', regionalCenterData.regional_center);
            } else {
              this.regionalCentersCount = 0;
              this.matchedRegionalCenter = null;
              console.log('Regional center count (ZIP code, not found):', this.regionalCentersCount);
            }
          } else {
            // Location API returns an array
            if (Array.isArray(regionalCenterData)) {
              this.regionalCentersCount = regionalCenterData.length;
              if (regionalCenterData.length > 0) {
                this.matchedRegionalCenter = regionalCenterData[0]; // Store the first regional center
                console.log('Regional center name:', regionalCenterData[0].regional_center);
              }
              console.log('Regional center count (location array):', this.regionalCentersCount);
            } else {
              this.regionalCentersCount = 0;
              this.matchedRegionalCenter = null;
              console.log('Regional center count (location, not array):', this.regionalCentersCount);
            }
          }
        } else {
          console.error('Regional center API error:', regionalCenterResponse.status);
          this.regionalCentersCount = 0;
        }
        
        console.log(`Generated results within 5-mile radius: ${this.resultsCount} providers, ${this.regionalCentersCount} regional centers`);
        
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

.skip-btn {
  color: #ffffff;
  font-size: 14px;
  text-decoration: none;
  margin-left: 16px;
}

.skip-btn:hover {
  color: #e9ecef;
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

.regional-center-info {
  margin-top: 20px;
}

.rc-details {
  margin-top: 12px;
  text-align: left;
}

.rc-name {
  font-weight: 600;
  font-size: 16px;
  color: #155724;
  margin-bottom: 4px;
}

.rc-address {
  font-size: 14px;
  color: #155724;
  margin-bottom: 4px;
}

.rc-phone {
  font-size: 14px;
  color: #155724;
  font-weight: 500;
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

/* Regional Center Info - Small & Subtle Design */
.regional-center-info {
  margin-top: 1rem;
}

.alert {
  padding: 0.75rem;
  border-radius: 4px;
  font-size: 0.75rem;
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.alert-success {
  background: #d1edff;
  border: 1px solid #b3d9ff;
  color: #004877;
}

.alert i {
  font-size: 0.875rem;
  margin-top: 0.125rem;
}

.rc-details {
  margin-top: 0.25rem;
  text-align: left;
}

.rc-service-area {
  font-size: 0.7rem;
  color: #6c757d;
  font-style: italic;
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
</style>
