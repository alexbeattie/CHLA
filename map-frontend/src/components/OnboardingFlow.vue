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
        </div>

        <!-- Location Step -->
        <div v-if="currentStep === 2" class="step location-step">
          <h3>Your Location</h3>
          <p>Help us find your Regional Center and Services</p>

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

          <!-- Regional Center Display -->
          <div v-if="matchedRegionalCenter" class="regional-center-info">
            <div class="alert alert-success">
              <i class="bi bi-building"></i>
              <strong>{{ matchedRegionalCenter.name }}</strong>
              <div class="rc-details">
                <div class="rc-service-area">{{ matchedRegionalCenter.service_area }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Profile Step -->
        <div v-if="currentStep === 3" class="step profile-step">
          <h3>Quick Setup</h3>
          <p>Just a few details to personalize your experience</p>

          <div class="profile-form">
            <div class="form-row">
              <label>Age Group</label>
              <select v-model="userProfile.age" class="form-control">
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
        </div>

        <!-- Services Step -->
        <div v-if="currentStep === 4" class="step services-step">
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
        </div>

        <!-- Results Step -->
        <div v-if="currentStep === 5" class="step results-step">
          <h3>You're all set!</h3>
          <p>We've found {{ resultsCount }} providers in your area based on your preferences.</p>

          <div class="results-summary">
            <div class="summary-item">
              <i class="bi bi-hospital"></i>
              <span>{{ resultsCount }} Services</span>
            </div>
            <div class="summary-item">
              <i class="bi bi-building"></i>
              <span>{{ regionalCentersCount }} Regional Centers</span>
            </div>
          </div>

          <div class="next-steps">
            <h4>What's next?</h4>
            <ul class="steps-list">
              <li>Explore services on the map</li>
              <li>View detailed service information</li>
              <li>Use filters to refine your search</li>
            </ul>
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
          v-if="currentStep < totalSteps && currentStep !== 2"
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
  },

  data() {
    return {
      currentStep: 1,
      totalSteps: 5,
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
      switch (this.currentStep) {
        case 1:
          return true;
        case 2:
          return this.userLocation || this.locationDetected;
        case 3:
          return this.userProfile.age; // Age is required
        case 4:
          return this.userProfile.therapies.length > 0;
        case 5:
          return true;
        default:
          return false;
      }
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
        if (this.currentStep === 5) {
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

        this.nextStep();
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
        const url = `${apiBaseUrl}/api/regional-centers/by_location/?location=${encodeURIComponent(
          locationText
        )}&radius=40&limit=5`;
        const res = await fetch(url, { headers: { Accept: "application/json" } });
        if (!res.ok) {
          console.error("Regional center API error:", res.status, res.statusText);
          return;
        }
        const centers = await res.json();
        if (Array.isArray(centers) && centers.length > 0) {
          this.matchedRegionalCenter = centers[0];
          this.$emit("regional-center-matched", centers[0]);
        } else {
          console.log("No regional centers found for location:", locationText);
          // Don't set an error here as the user might have entered a city name
          // The main validation happens in validateLocation()
        }
      } catch (e) {
        console.error("Regional center lookup failed:", e);
      }
    },

    async generateResults() {
      // Get counts based on user's location within 5-mile radius
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
          const cleanLocation = this.userLocation.trim().replace(/\s+/g, ' ').replace(/\+/g, ' ');
          providerParams.append('location', cleanLocation);
          providerParams.append('radius', '5'); // 5-mile radius
        }
        if (this.userProfile.age) {
          providerParams.append('age_group', this.userProfile.age);
        }
        if (this.userProfile.diagnosis) {
          providerParams.append('diagnosis', this.userProfile.diagnosis);
        }
        if (this.userProfile.therapies && this.userProfile.therapies.length > 0) {
          this.userProfile.therapies.forEach(therapy => {
            providerParams.append('therapy', therapy);
          });
        }
        
        // Add funding source preferences
        if (this.userProfile.hasInsurance) {
          providerParams.append('insurance', 'Health Insurance');
        }
        if (this.userProfile.hasRegionalCenter) {
          providerParams.append('insurance', 'Regional Center');
        }
        
        const providerUrl = `${apiBaseUrl}/api/providers-v2/comprehensive_search/?${providerParams.toString()}`;
        console.log('Provider API URL:', providerUrl);
        console.log('Provider params:', providerParams.toString());
        
        const providerResponse = await fetch(providerUrl, { 
          headers: { Accept: "application/json" } 
        });
        
        console.log('Provider response status:', providerResponse.status);
        
        if (providerResponse.ok) {
          const providerData = await providerResponse.json();
          console.log('Provider data received:', providerData);
          // Provider API returns an array directly, not an object with count
          if (Array.isArray(providerData)) {
            this.resultsCount = providerData.length;
            console.log('Provider count (array):', this.resultsCount);
          } else {
            this.resultsCount = providerData.count || 0;
            console.log('Provider count (object):', this.resultsCount);
          }
        } else {
          console.error('Provider API error:', providerResponse.status);
          this.resultsCount = 0;
        }
        
        // Get regional center count within 5-mile radius
        const regionalCenterParams = new URLSearchParams();
        if (this.userLocation) {
          // Clean up the location string - remove extra spaces and fix encoding
          const cleanLocation = this.userLocation.trim().replace(/\s+/g, ' ').replace(/\+/g, ' ');
          regionalCenterParams.append('location', cleanLocation);
          regionalCenterParams.append('radius', '5'); // 5-mile radius
        }
        
        const regionalCenterUrl = `${apiBaseUrl}/api/regional-centers/by_location/?${regionalCenterParams.toString()}`;
        console.log('Regional center API URL:', regionalCenterUrl);
        console.log('Regional center params:', regionalCenterParams.toString());
        
        const regionalCenterResponse = await fetch(regionalCenterUrl, { 
          headers: { Accept: "application/json" } 
        });
        
        console.log('Regional center response status:', regionalCenterResponse.status);
        
        if (regionalCenterResponse.ok) {
          const regionalCenterData = await regionalCenterResponse.json();
          console.log('Regional center data received:', regionalCenterData);
          // Count unique regional centers (in case there are multiple offices)
          const uniqueCenters = new Set();
          if (Array.isArray(regionalCenterData)) {
            regionalCenterData.forEach(center => {
              uniqueCenters.add(center.regional_center);
            });
            this.regionalCentersCount = uniqueCenters.size;
            console.log('Regional center count:', this.regionalCentersCount);
          } else {
            this.regionalCentersCount = 0;
            console.log('Regional center count (not array):', this.regionalCentersCount);
          }
        } else {
          console.error('Regional center API error:', regionalCenterResponse.status);
          this.regionalCentersCount = 0;
        }
        
        console.log(`Generated results within 5-mile radius: ${this.resultsCount} providers, ${this.regionalCentersCount} regional centers`);
        
      } catch (error) {
        console.error('Error generating results:', error);
        // Fallback to reasonable defaults
        this.resultsCount = 0;
        this.regionalCentersCount = 0;
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

/* Welcome Step */
.welcome-step .chla-logo-large {
  margin-bottom: 12px;
}

.welcome-step .logo {
  height: 56px;
}

.welcome-step h2 {
  color: #004877;
  margin-bottom: 10px;
  font-size: 24px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.welcome-text {
  font-size: 15px;
  color: #495057;
  margin-bottom: 24px;
  line-height: 1.5;
  max-width: 480px;
  margin-left: auto;
  margin-right: auto;
}

.welcome-features {
  display: flex;
  flex-direction: row;
  justify-content: center;
  gap: 20px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.feature {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #495057;
  font-size: 14px;
  font-weight: 500;
}

.feature i {
  font-size: 16px;
  color: #004877;
}

/* Location Step */
.location-step h3 {
  color: #004877;
  margin-bottom: 8px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-weight: 600;
}

.location-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}

.location-btn {
  padding: 12px 20px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.location-divider {
  text-align: center;
  position: relative;
  color: #6c757d;
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
  padding: 0 16px;
  position: relative;
  z-index: 2;
}

.manual-location {
  display: flex;
  gap: 12px;
}

.location-input {
  flex: 1;
  padding: 12px 16px;
  font-size: 16px;
}

/* Profile Step */
.profile-step {
  text-align: left;
}

.profile-form {
  margin-top: 20px;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.form-row label {
  min-width: 120px;
  font-size: 14px;
  font-weight: 500;
  color: #495057;
}

.form-row .form-control {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  font-size: 14px;
  background: white;
}

.form-row .form-control:focus {
  outline: none;
  border-color: #004877;
  box-shadow: 0 0 0 2px rgba(0, 72, 119, 0.1);
}

.funding-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.funding-row label {
  min-width: 120px;
  font-size: 14px;
  font-weight: 500;
  color: #495057;
}

.funding-options {
  display: flex;
  gap: 16px;
  flex: 1;
}

.funding-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #495057;
}

.funding-option input[type="checkbox"] {
  width: 14px;
  height: 14px;
  accent-color: #004877;
}

/* Results Step */
.results-step h3 {
  color: #004877;
  margin-bottom: 8px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-weight: 600;
}

.results-summary {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin: 24px 0;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #495057;
  font-size: 14px;
  font-weight: 500;
}

.summary-item i {
  font-size: 18px;
  color: #004877;
}

.next-steps {
  margin-top: 32px;
  text-align: left;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}

.next-steps h4 {
  color: #004877;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.steps-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.steps-list li {
  color: #495057;
  font-size: 14px;
  margin-bottom: 8px;
  padding-left: 16px;
  position: relative;
}

.steps-list li::before {
  content: "•";
  color: #004877;
  position: absolute;
  left: 0;
}

/* Services Step */
.services-step h3 {
  color: #004877;
  margin-bottom: 8px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-weight: 600;
}
.service-types {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.service-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.service-option:hover {
  border-color: #004877;
  background: #f8f9fa;
}

.service-option input {
  display: none;
}

.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid #dee2e6;
  border-radius: 4px;
  position: relative;
  transition: all 0.3s ease;
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
  font-size: 14px;
}

.service-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.service-content i {
  font-size: 24px;
  color: #0d9ddb;
}

.service-content strong {
  display: block;
  margin-bottom: 4px;
}

.service-content small {
  display: block;
  color: #6c757d;
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

/* Navigation */
.step-navigation {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
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

/* Button Styles */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
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
