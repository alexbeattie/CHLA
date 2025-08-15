<template>
  <div class="onboarding-overlay" v-if="showOnboarding">
    <div class="onboarding-container">
      <!-- Progress Bar -->
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
      </div>
      
      <!-- Step Counter -->
      <div class="step-counter">
        Step {{ currentStep }} of {{ totalSteps }}
      </div>

      <!-- Step Content -->
      <div class="step-content">
        <!-- Welcome Step -->
        <div v-if="currentStep === 1" class="step welcome-step">
          <div class="chla-logo-large">
            <img src="@/assets/chla-logo.svg" alt="CHLA" class="logo" />
          </div>
          <h2>Welcome to CHLA Provider Network</h2>
          <p class="welcome-text">
            Find specialized healthcare providers and regional centers serving your area.
            We'll help you discover the best care options based on your specific needs.
          </p>
          <div class="welcome-features">
            <div class="feature">
              <i class="bi bi-geo-alt-fill"></i>
              <span>Location-based provider search</span>
            </div>
            <div class="feature">
              <i class="bi bi-person-check"></i>
              <span>Personalized recommendations</span>
            </div>
            <div class="feature">
              <i class="bi bi-heart"></i>
              <span>Comprehensive care network</span>
            </div>
          </div>
          <button class="btn btn-chla-primary btn-lg" @click="nextStep">
            Get Started
          </button>
        </div>

        <!-- Location Step -->
        <div v-if="currentStep === 2" class="step location-step">
          <h3>Let's find services near you</h3>
          <p>We'll use your location to show the most relevant providers in your area.</p>
          
          <div class="location-options">
            <button 
              class="btn btn-chla-primary location-btn"
              @click="detectLocation"
              :disabled="locationDetecting"
            >
              <i class="bi bi-geo-alt-fill"></i>
              <span v-if="locationDetecting">Detecting Location...</span>
              <span v-else>Use My Current Location</span>
            </button>
            
            <div class="location-divider">
              <span>OR</span>
            </div>
            
            <div class="manual-location">
              <input 
                type="text" 
                class="form-control location-input"
                placeholder="Enter your ZIP code or city"
                v-model="userLocation"
                @keyup.enter="validateLocation"
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

        <!-- Profile Step -->
        <div v-if="currentStep === 3" class="step profile-step">
          <h3>Tell us about your needs</h3>
          <p>This helps us show you the most relevant providers and services.</p>
          
          <form @submit.prevent="nextStep" class="profile-form">
            <div class="form-group">
              <label for="ageGroup">Age Group</label>
              <select id="ageGroup" v-model="userProfile.ageGroup" class="form-control">
                <option value="">Select age group</option>
                <option value="0-5">Early Childhood (0-5)</option>
                <option value="6-12">School Age (6-12)</option>
                <option value="13-18">Teen (13-18)</option>
                <option value="19+">Adult (19+)</option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="diagnosis">Primary Diagnosis (Optional)</label>
              <select id="diagnosis" v-model="userProfile.primaryDiagnosis" class="form-control">
                <option value="">Select primary diagnosis</option>
                <option value="Autism">Autism Spectrum Disorder</option>
                <option value="ADHD">ADHD</option>
                <option value="Developmental Delay">Developmental Delay</option>
                <option value="Learning Disabilities">Learning Disabilities</option>
                <option value="Speech Delay">Speech Delay</option>
                <option value="Other">Other</option>
              </select>
            </div>
            
            <div v-if="userProfile.primaryDiagnosis === 'Other'" class="form-group">
              <label for="otherDiagnosis">Please specify</label>
              <input 
                type="text" 
                id="otherDiagnosis"
                v-model="userProfile.otherDiagnosis"
                class="form-control"
                placeholder="Enter diagnosis"
              />
            </div>
            
            <div class="insurance-section">
              <h4>Insurance & Funding</h4>
              <p class="insurance-help">Select all that apply to see relevant providers:</p>
              
              <div class="insurance-options">
                <label class="insurance-option">
                  <input type="checkbox" v-model="userProfile.hasInsurance" />
                  <span class="checkmark"></span>
                  <div class="option-content">
                    <strong>I have health insurance</strong>
                    <small>Most major insurers accepted</small>
                  </div>
                </label>
                
                <label class="insurance-option">
                  <input type="checkbox" v-model="userProfile.hasRegionalCenter" />
                  <span class="checkmark"></span>
                  <div class="option-content">
                    <strong>I work with a regional center</strong>
                    <small>Regional center funded services</small>
                  </div>
                </label>
                
                <label class="insurance-option">
                  <input type="checkbox" v-model="userProfile.canPrivatePay" />
                  <span class="checkmark"></span>
                  <div class="option-content">
                    <strong>I can pay privately</strong>
                    <small>Self-pay and private funding</small>
                  </div>
                </label>
              </div>
            </div>
          </form>
        </div>

        <!-- Services Step -->
        <div v-if="currentStep === 4" class="step services-step">
          <h3>What type of services are you looking for?</h3>
          <p>Select the services you're interested in to get personalized recommendations.</p>
          
          <div class="service-types">
            <label class="service-option" v-for="service in serviceOptions" :key="service.id">
              <input type="checkbox" v-model="userProfile.interestedServices" :value="service.id" />
              <span class="checkmark"></span>
              <div class="service-content">
                <i :class="service.icon"></i>
                <div>
                  <strong>{{ service.name }}</strong>
                  <small>{{ service.description }}</small>
                </div>
              </div>
            </label>
          </div>
        </div>

        <!-- Results Step -->
        <div v-if="currentStep === 5" class="step results-step">
          <h3>Perfect! Here's what we found for you</h3>
          <p>Based on your preferences, we've found <strong>{{ resultsCount }}</strong> relevant providers in your area.</p>
          
          <div class="results-preview">
            <div class="result-stats">
              <div class="stat">
                <i class="bi bi-hospital"></i>
                <span>{{ resultsCount }} Providers</span>
              </div>
              <div class="stat">
                <i class="bi bi-building"></i>
                <span>{{ regionalCentersCount }} Regional Centers</span>
              </div>
              <div class="stat">
                <i class="bi bi-geo-alt"></i>
                <span>{{ userLocationDisplay }}</span>
              </div>
            </div>
            
            <div class="quick-tutorial">
              <h4>Quick Tutorial</h4>
              <div class="tutorial-steps">
                <div class="tutorial-step">
                  <i class="bi bi-1-circle"></i>
                  <span>Use the map to see providers in your area</span>
                </div>
                <div class="tutorial-step">
                  <i class="bi bi-2-circle"></i>
                  <span>Click on markers for provider details</span>
                </div>
                <div class="tutorial-step">
                  <i class="bi bi-3-circle"></i>
                  <span>Use filters to narrow your search</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="results-actions">
            <button class="btn btn-chla-primary btn-lg" @click="completeOnboarding">
              Explore Providers
            </button>
            <button class="btn btn-chla-outline" @click="saveProfile">
              Save My Profile
            </button>
          </div>
        </div>
      </div>

      <!-- Navigation -->
      <div class="step-navigation">
        <button 
          v-if="currentStep > 1" 
          class="btn btn-secondary"
          @click="previousStep"
        >
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
          Continue
          <i class="bi bi-arrow-right"></i>
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
  name: 'OnboardingFlow',
  
  props: {
    showOnboarding: {
      type: Boolean,
      default: false
    }
  },
  
  data() {
    return {
      currentStep: 1,
      totalSteps: 5,
      locationDetecting: false,
      locationError: null,
      userLocation: '',
      userProfile: {
        ageGroup: '',
        primaryDiagnosis: '',
        otherDiagnosis: '',
        hasInsurance: false,
        hasRegionalCenter: false,
        canPrivatePay: false,
        interestedServices: []
      },
      serviceOptions: [
        {
          id: 'therapy',
          name: 'Therapy Services',
          description: 'Speech, occupational, physical therapy',
          icon: 'bi bi-chat-heart'
        },
        {
          id: 'medical',
          name: 'Medical Services',
          description: 'Specialized medical care and treatment',
          icon: 'bi bi-heart-pulse'
        },
        {
          id: 'educational',
          name: 'Educational Support',
          description: 'Special education and learning support',
          icon: 'bi bi-mortarboard'
        },
        {
          id: 'behavioral',
          name: 'Behavioral Services',
          description: 'ABA therapy and behavioral interventions',
          icon: 'bi bi-person-check'
        },
        {
          id: 'diagnostic',
          name: 'Diagnostic Services',
          description: 'Assessments and evaluations',
          icon: 'bi bi-clipboard-data'
        },
        {
          id: 'support',
          name: 'Family Support',
          description: 'Counseling and family resources',
          icon: 'bi bi-people'
        }
      ],
      resultsCount: 0,
      regionalCentersCount: 0
    }
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
          return this.userProfile.ageGroup; // Age group is required
        case 4:
          return this.userProfile.interestedServices.length > 0;
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
      return this.userLocation || 'Current Location';
    }
  },
  
  methods: {
    nextStep() {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
        if (this.currentStep === 5) {
          this.generateResults();
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
        this.$emit('location-detected', {
          latitude,
          longitude,
          address
        });
        
        this.nextStep();
      } catch (error) {
        this.locationError = 'Unable to detect location. Please enter manually.';
        console.error('Location detection failed:', error);
      } finally {
        this.locationDetecting = false;
      }
    },
    
    getCurrentPosition() {
      return new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error('Geolocation not supported'));
          return;
        }
        
        navigator.geolocation.getCurrentPosition(
          resolve,
          reject,
          {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 300000
          }
        );
      });
    },
    
    async reverseGeocode(latitude, longitude) {
      try {
        // This would use your geocoding service
        // For now, return a placeholder
        return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
      } catch (error) {
        console.error('Reverse geocoding failed:', error);
        return 'Location detected';
      }
    },
    
    validateLocation() {
      if (this.userLocation.trim()) {
        this.$emit('location-manual', this.userLocation);
        this.nextStep();
      }
    },
    
    generateResults() {
      // This would query your API for actual results
      // For now, generate sample counts
      this.resultsCount = Math.floor(Math.random() * 20) + 10;
      this.regionalCentersCount = Math.floor(Math.random() * 5) + 2;
    },
    
    completeOnboarding() {
      this.saveProfile();
      this.$emit('onboarding-complete', {
        userProfile: this.userProfile,
        userLocation: this.userLocation
      });
    },
    
    saveProfile() {
      // Save profile to localStorage
      localStorage.setItem('chla-user-profile', JSON.stringify(this.userProfile));
      localStorage.setItem('chla-user-location', this.userLocation);
      localStorage.setItem('chla-onboarding-complete', 'true');
    },
    
    skipOnboarding() {
      this.$emit('onboarding-skipped');
    }
  }
}
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
  padding: 16px 24px;
  font-size: 14px;
  color: #6c757d;
  border-bottom: 1px solid #e9ecef;
}

.step-content {
  padding: 32px 24px;
  min-height: 400px;
  max-height: 60vh;
  overflow-y: auto;
}

.step {
  text-align: center;
}

/* Welcome Step */
.welcome-step .chla-logo-large {
  margin-bottom: 24px;
}

.welcome-step .logo {
  height: 80px;
}

.welcome-step h2 {
  color: #004877;
  margin-bottom: 16px;
  font-size: 28px;
  font-weight: 600;
}

.welcome-text {
  font-size: 16px;
  color: #6c757d;
  margin-bottom: 32px;
  line-height: 1.6;
}

.welcome-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  color: #004877;
}

.feature i {
  font-size: 24px;
  color: #0d9ddb;
}

/* Location Step */
.location-step h3 {
  color: #004877;
  margin-bottom: 16px;
}

.location-options {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 32px;
}

.location-btn {
  padding: 16px 24px;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
}

.location-divider {
  text-align: center;
  position: relative;
  color: #6c757d;
}

.location-divider::before {
  content: '';
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
.profile-form {
  text-align: left;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #004877;
  font-weight: 500;
}

.form-control {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s ease;
}

.form-control:focus {
  outline: none;
  border-color: #004877;
}

.insurance-section {
  margin-top: 32px;
}

.insurance-section h4 {
  color: #004877;
  margin-bottom: 8px;
}

.insurance-help {
  color: #6c757d;
  font-size: 14px;
  margin-bottom: 20px;
}

.insurance-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.insurance-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.insurance-option:hover {
  border-color: #004877;
  background: #f8f9fa;
}

.insurance-option input {
  display: none;
}

.checkmark {
  width: 20px;
  height: 20px;
  border: 2px solid #dee2e6;
  border-radius: 4px;
  position: relative;
  transition: all 0.3s ease;
}

.insurance-option input:checked + .checkmark {
  background: #004877;
  border-color: #004877;
}

.insurance-option input:checked + .checkmark::after {
  content: '✓';
  color: white;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 14px;
}

.option-content {
  text-align: left;
}

.option-content strong {
  display: block;
  color: #004877;
  margin-bottom: 4px;
}

.option-content small {
  color: #6c757d;
  font-size: 12px;
}

/* Services Step */
.service-types {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.service-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.service-option:hover {
  border-color: #004877;
  background: #f8f9fa;
}

.service-option input {
  display: none;
}

.service-option input:checked + .checkmark {
  background: #004877;
  border-color: #004877;
}

.service-option input:checked + .checkmark::after {
  content: '✓';
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
  color: #6c757d;
  font-size: 14px;
  text-decoration: none;
  margin-left: 16px;
}

.skip-btn:hover {
  color: #495057;
  text-decoration: underline;
}

/* Button Styles */
.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
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

.btn-lg {
  padding: 16px 32px;
  font-size: 18px;
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

/* Responsive */
@media (max-width: 768px) {
  .onboarding-container {
    width: 95%;
    max-height: 95vh;
  }
  
  .step-content {
    padding: 24px 16px;
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