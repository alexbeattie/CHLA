<template>
  <div class="user-profile-manager">
    <!-- Profile Status Badge -->
    <div class="profile-status" :class="{ 'has-profile': hasProfile }">
      <div class="status-indicator">
        <i :class="hasProfile ? 'bi bi-person-check-fill' : 'bi bi-person-plus'"></i>
        <span v-if="hasProfile">Profile Complete</span>
        <span v-else>Setup Profile</span>
      </div>
      <button class="btn btn-sm btn-chla-outline" @click="toggleProfile">
        {{ showProfile ? 'Hide' : 'Edit' }}
      </button>
    </div>

    <!-- Profile Form -->
    <div class="profile-form" v-if="showProfile">
      <h4>Your Profile</h4>
      <p class="form-description">
        Update your information to get more personalized provider recommendations.
      </p>

      <form @submit.prevent="saveProfile">
        <!-- Location Section -->
        <div class="form-section">
          <h5>
            <i class="bi bi-geo-alt"></i>
            Location
          </h5>
          <div class="location-group">
            <div class="location-input-group">
              <input 
                type="text" 
                v-model="profile.address"
                class="form-control"
                placeholder="Enter your ZIP code or address"
                @blur="validateLocation"
              />
              <button 
                type="button" 
                class="btn btn-chla-outline location-detect-btn"
                @click="detectLocation"
                :disabled="locationDetecting"
              >
                <i class="bi bi-geo-alt-fill"></i>
                <span v-if="locationDetecting">Detecting...</span>
                <span v-else>Detect</span>
              </button>
            </div>
            <div v-if="locationStatus" class="location-status">
              <i :class="locationStatus.icon"></i>
              <span>{{ locationStatus.message }}</span>
            </div>
          </div>
        </div>

        <!-- Demographics Section -->
        <div class="form-section">
          <h5>
            <i class="bi bi-person"></i>
            Demographics
          </h5>
          <div class="form-row">
            <div class="form-group">
              <label for="ageGroup">Age Group</label>
              <select id="ageGroup" v-model="profile.ageGroup" class="form-control">
                <option value="">Select age group</option>
                <option value="0-5">Early Childhood (0-5)</option>
                <option value="6-12">School Age (6-12)</option>
                <option value="13-18">Teen (13-18)</option>
                <option value="19-25">Young Adult (19-25)</option>
                <option value="26-64">Adult (26-64)</option>
                <option value="65+">Senior (65+)</option>
              </select>
            </div>
            <div class="form-group">
              <label for="diagnosis">Primary Diagnosis</label>
              <select id="diagnosis" v-model="profile.primaryDiagnosis" class="form-control">
                <option value="">Select diagnosis (optional)</option>
                <option value="Autism">Autism Spectrum Disorder</option>
                <option value="ADHD">ADHD</option>
                <option value="Developmental Delay">Developmental Delay</option>
                <option value="Learning Disabilities">Learning Disabilities</option>
                <option value="Intellectual Disability">Intellectual Disability</option>
                <option value="Cerebral Palsy">Cerebral Palsy</option>
                <option value="Speech Delay">Speech/Language Delay</option>
                <option value="Sensory Processing">Sensory Processing Disorder</option>
                <option value="Mental Health">Mental Health Condition</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>
          <div v-if="profile.primaryDiagnosis === 'Other'" class="form-group">
            <label for="otherDiagnosis">Please specify diagnosis</label>
            <input 
              type="text" 
              id="otherDiagnosis"
              v-model="profile.otherDiagnosis"
              class="form-control"
              placeholder="Enter specific diagnosis"
            />
          </div>
        </div>

        <!-- Insurance & Funding Section -->
        <div class="form-section">
          <h5>
            <i class="bi bi-credit-card"></i>
            Insurance & Funding
          </h5>
          <p class="section-description">
            Select all funding sources you have access to:
          </p>
          <div class="funding-options">
            <label class="funding-option">
              <input type="checkbox" v-model="profile.hasInsurance" />
              <span class="checkmark"></span>
              <div class="option-content">
                <strong>Health Insurance</strong>
                <small>Private insurance, HMO, PPO</small>
              </div>
            </label>
            
            <label class="funding-option">
              <input type="checkbox" v-model="profile.hasMediCal" />
              <span class="checkmark"></span>
              <div class="option-content">
                <strong>Medi-Cal</strong>
                <small>California Medicaid program</small>
              </div>
            </label>
            
            <label class="funding-option">
              <input type="checkbox" v-model="profile.hasRegionalCenter" />
              <span class="checkmark"></span>
              <div class="option-content">
                <strong>Regional Center</strong>
                <small>Developmental disabilities services</small>
              </div>
            </label>
            
            <label class="funding-option">
              <input type="checkbox" v-model="profile.canPrivatePay" />
              <span class="checkmark"></span>
              <div class="option-content">
                <strong>Private Pay</strong>
                <small>Self-pay or private funding</small>
              </div>
            </label>
          </div>
        </div>

        <!-- Service Preferences Section -->
        <div class="form-section">
          <h5>
            <i class="bi bi-heart-pulse"></i>
            Service Preferences
          </h5>
          <p class="section-description">
            What types of services are you most interested in?
          </p>
          <div class="service-preferences">
            <label class="service-option" v-for="service in availableServices" :key="service.id">
              <input 
                type="checkbox" 
                v-model="profile.interestedServices" 
                :value="service.id" 
              />
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

        <!-- Transportation Section -->
        <div class="form-section">
          <h5>
            <i class="bi bi-car-front"></i>
            Transportation
          </h5>
          <div class="form-row">
            <div class="form-group">
              <label for="maxDistance">Maximum travel distance</label>
              <select id="maxDistance" v-model="profile.maxDistance" class="form-control">
                <option value="5">Within 5 miles</option>
                <option value="10">Within 10 miles</option>
                <option value="15">Within 15 miles</option>
                <option value="25">Within 25 miles</option>
                <option value="50">Within 50 miles</option>
                <option value="100">Statewide</option>
              </select>
            </div>
            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="profile.needsTransportation" />
                <span class="checkmark"></span>
                I need transportation assistance
              </label>
            </div>
          </div>
        </div>

        <!-- Form Actions -->
        <div class="form-actions">
          <button type="submit" class="btn btn-chla-primary">
            <i class="bi bi-check-circle"></i>
            Save Profile
          </button>
          <button type="button" class="btn btn-secondary" @click="resetProfile">
            <i class="bi bi-arrow-counterclockwise"></i>
            Reset
          </button>
          <button type="button" class="btn btn-outline-danger" @click="clearProfile">
            <i class="bi bi-trash"></i>
            Clear All
          </button>
        </div>
      </form>
    </div>

    <!-- Profile Summary -->
    <div class="profile-summary" v-if="hasProfile && !showProfile">
      <h5>Quick Profile Summary</h5>
      <div class="summary-items">
        <div class="summary-item" v-if="profile.ageGroup">
          <i class="bi bi-person"></i>
          <span>{{ profile.ageGroup }}</span>
        </div>
        <div class="summary-item" v-if="profile.primaryDiagnosis">
          <i class="bi bi-clipboard-data"></i>
          <span>{{ profile.primaryDiagnosis }}</span>
        </div>
        <div class="summary-item" v-if="profile.address">
          <i class="bi bi-geo-alt"></i>
          <span>{{ profile.address }}</span>
        </div>
        <div class="summary-item" v-if="fundingSourcesText">
          <i class="bi bi-credit-card"></i>
          <span>{{ fundingSourcesText }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserProfileManager',
  
  props: {
    initialProfile: {
      type: Object,
      default: () => ({})
    }
  },
  
  data() {
    return {
      showProfile: false,
      locationDetecting: false,
      locationStatus: null,
      profile: {
        address: '',
        ageGroup: '',
        primaryDiagnosis: '',
        otherDiagnosis: '',
        hasInsurance: false,
        hasMediCal: false,
        hasRegionalCenter: false,
        canPrivatePay: false,
        interestedServices: [],
        maxDistance: '15',
        needsTransportation: false
      },
      availableServices: [
        {
          id: 'therapy',
          name: 'Therapy Services',
          description: 'Speech, occupational, physical therapy',
          icon: 'bi bi-chat-heart'
        },
        {
          id: 'medical',
          name: 'Medical Services',
          description: 'Specialized medical care',
          icon: 'bi bi-heart-pulse'
        },
        {
          id: 'behavioral',
          name: 'Behavioral Services',
          description: 'ABA therapy, behavioral interventions',
          icon: 'bi bi-person-check'
        },
        {
          id: 'educational',
          name: 'Educational Support',
          description: 'Special education, tutoring',
          icon: 'bi bi-mortarboard'
        },
        {
          id: 'diagnostic',
          name: 'Diagnostic Services',
          description: 'Assessments, evaluations',
          icon: 'bi bi-clipboard-data'
        },
        {
          id: 'support',
          name: 'Support Services',
          description: 'Respite care, family support',
          icon: 'bi bi-people'
        }
      ]
    }
  },
  
  computed: {
    hasProfile() {
      return this.profile.ageGroup || this.profile.primaryDiagnosis || this.profile.address || 
             this.profile.hasInsurance || this.profile.hasRegionalCenter || this.profile.canPrivatePay;
    },
    
    fundingSourcesText() {
      const sources = [];
      if (this.profile.hasInsurance) sources.push('Insurance');
      if (this.profile.hasMediCal) sources.push('Medi-Cal');
      if (this.profile.hasRegionalCenter) sources.push('Regional Center');
      if (this.profile.canPrivatePay) sources.push('Private Pay');
      return sources.join(', ');
    }
  },
  
  mounted() {
    this.loadProfile();
  },
  
  methods: {
    loadProfile() {
      // Load from localStorage
      const saved = localStorage.getItem('chla-user-profile');
      if (saved) {
        try {
          this.profile = { ...this.profile, ...JSON.parse(saved) };
        } catch (error) {
          console.error('Error loading profile:', error);
        }
      }
      
      // Merge with initial profile prop
      this.profile = { ...this.profile, ...this.initialProfile };
    },
    
    saveProfile() {
      // Save to localStorage
      localStorage.setItem('chla-user-profile', JSON.stringify(this.profile));
      
      // Emit to parent component
      this.$emit('profile-saved', this.profile);
      
      // Show success message
      this.showSuccessMessage('Profile saved successfully!');
      
      // Hide form
      this.showProfile = false;
    },
    
    resetProfile() {
      this.loadProfile();
      this.showSuccessMessage('Profile reset to last saved version');
    },
    
    clearProfile() {
      if (confirm('Are you sure you want to clear all profile data?')) {
        this.profile = {
          address: '',
          ageGroup: '',
          primaryDiagnosis: '',
          otherDiagnosis: '',
          hasInsurance: false,
          hasMediCal: false,
          hasRegionalCenter: false,
          canPrivatePay: false,
          interestedServices: [],
          maxDistance: '15',
          needsTransportation: false
        };
        
        localStorage.removeItem('chla-user-profile');
        this.$emit('profile-cleared');
        this.showSuccessMessage('Profile cleared');
      }
    },
    
    toggleProfile() {
      this.showProfile = !this.showProfile;
    },
    
    async detectLocation() {
      this.locationDetecting = true;
      this.locationStatus = null;
      
      try {
        const position = await this.getCurrentPosition();
        const { latitude, longitude } = position.coords;
        
        // Reverse geocode to get address
        const address = await this.reverseGeocode(latitude, longitude);
        this.profile.address = address;
        
        this.locationStatus = {
          icon: 'bi bi-check-circle-fill text-success',
          message: 'Location detected successfully'
        };
        
        this.$emit('location-detected', { latitude, longitude, address });
        
      } catch (error) {
        this.locationStatus = {
          icon: 'bi bi-exclamation-triangle-fill text-warning',
          message: 'Could not detect location. Please enter manually.'
        };
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
        // This would integrate with your geocoding service
        // For now, return a formatted coordinate string
        return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
      } catch (error) {
        console.error('Reverse geocoding failed:', error);
        return 'Location detected';
      }
    },
    
    validateLocation() {
      // Basic validation for ZIP codes and addresses
      const address = this.profile.address.trim();
      if (!address) return;
      
      const zipRegex = /^\d{5}(-\d{4})?$/;
      if (zipRegex.test(address)) {
        this.locationStatus = {
          icon: 'bi bi-check-circle-fill text-success',
          message: 'Valid ZIP code'
        };
      } else if (address.length > 5) {
        this.locationStatus = {
          icon: 'bi bi-info-circle-fill text-info',
          message: 'Address entered'
        };
      }
    },
    
    showSuccessMessage(message) {
      // This would integrate with your notification system
      console.log(message);
    }
  }
}
</script>

<style scoped>
.user-profile-manager {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.profile-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.profile-status.has-profile {
  background: #d4edda;
  border: 1px solid #c3e6cb;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6c757d;
}

.profile-status.has-profile .status-indicator {
  color: #155724;
}

.profile-status i {
  font-size: 18px;
}

.profile-form {
  border-top: 1px solid #dee2e6;
  padding-top: 20px;
}

.profile-form h4 {
  color: #004877;
  margin-bottom: 8px;
}

.form-description {
  color: #6c757d;
  margin-bottom: 24px;
}

.form-section {
  margin-bottom: 32px;
}

.form-section h5 {
  color: #004877;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #dee2e6;
}

.section-description {
  color: #6c757d;
  font-size: 14px;
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 4px;
  color: #004877;
  font-weight: 500;
  font-size: 14px;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.15s ease-in-out;
}

.form-control:focus {
  outline: none;
  border-color: #004877;
  box-shadow: 0 0 0 0.2rem rgba(0, 72, 119, 0.25);
}

.location-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.location-input-group {
  display: flex;
  gap: 8px;
}

.location-input-group .form-control {
  flex: 1;
}

.location-detect-btn {
  flex-shrink: 0;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.location-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.funding-options, .service-preferences {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.funding-option, .service-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.funding-option:hover, .service-option:hover {
  border-color: #004877;
  background: #f8f9fa;
}

.funding-option input, .service-option input {
  display: none;
}

.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid #ced4da;
  border-radius: 3px;
  position: relative;
  transition: all 0.15s ease;
}

.funding-option input:checked + .checkmark,
.service-option input:checked + .checkmark {
  background: #004877;
  border-color: #004877;
}

.funding-option input:checked + .checkmark::after,
.service-option input:checked + .checkmark::after {
  content: '✓';
  color: white;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
}

.option-content {
  text-align: left;
}

.option-content strong {
  display: block;
  color: #004877;
  font-size: 14px;
  margin-bottom: 2px;
}

.option-content small {
  color: #6c757d;
  font-size: 12px;
}

.service-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.service-content i {
  font-size: 20px;
  color: #0d9ddb;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #004877;
  font-weight: 500;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #dee2e6;
}

.profile-summary {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-top: 16px;
}

.profile-summary h5 {
  color: #004877;
  margin-bottom: 12px;
}

.summary-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6c757d;
  font-size: 14px;
}

.summary-item i {
  color: #0d9ddb;
  width: 16px;
}

/* Button Styles */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
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
  border: 1px solid #004877;
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

.btn-outline-danger {
  background: transparent;
  color: #dc3545;
  border: 1px solid #dc3545;
}

.btn-outline-danger:hover {
  background: #dc3545;
  color: white;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .location-input-group {
    flex-direction: column;
  }
  
  .location-detect-btn {
    align-self: flex-start;
  }
}
</style>