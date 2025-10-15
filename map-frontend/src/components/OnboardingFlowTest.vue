<template>
  <div class="onboarding-test-container">
    <div class="test-controls">
      <h2>Onboarding Flow Test Controls</h2>
      
      <div class="control-group">
        <label>
          <input type="checkbox" v-model="showOnboarding" />
          Show Onboarding Flow
        </label>
      </div>

      <div class="control-group">
        <h3>Test Parameters</h3>
        <div class="param-controls">
          <div class="param-item">
            <label>Current Step:</label>
            <input 
              type="number" 
              v-model.number="testCurrentStep" 
              min="1" 
              max="5"
              @change="updateOnboardingStep"
            />
          </div>
          
          <div class="param-item">
            <label>Total Steps:</label>
            <input 
              type="number" 
              v-model.number="testTotalSteps" 
              min="1" 
              max="10"
            />
          </div>
        </div>
      </div>

      <div class="control-group">
        <h3>User Profile Test Data</h3>
        <div class="profile-controls">
          <div class="param-item">
            <label>Age:</label>
            <input type="text" v-model="testUserProfile.age" />
          </div>
          
          <div class="param-item">
            <label>Diagnosis:</label>
            <input type="text" v-model="testUserProfile.diagnosis" />
          </div>
          
          <div class="param-item">
            <label>
              <input type="checkbox" v-model="testUserProfile.hasInsurance" />
              Has Insurance
            </label>
          </div>
          
          <div class="param-item">
            <label>
              <input type="checkbox" v-model="testUserProfile.hasRegionalCenter" />
              Has Regional Center
            </label>
          </div>
        </div>
      </div>

      <div class="control-group">
        <h3>Location Test Data</h3>
        <div class="param-item">
          <label>User Location:</label>
          <input type="text" v-model="testUserLocation" />
        </div>
        
        <div class="param-item">
          <label>
            <input type="checkbox" v-model="testLocationDetecting" />
            Location Detecting
          </label>
        </div>
        
        <div class="param-item">
          <label>Location Error:</label>
          <input type="text" v-model="testLocationError" />
        </div>
      </div>

      <div class="control-group">
        <h3>Results Test Data</h3>
        <div class="param-item">
          <label>Results Count:</label>
          <input type="number" v-model.number="testResultsCount" />
        </div>
        
        <div class="param-item">
          <label>Regional Centers Count:</label>
          <input type="number" v-model.number="testRegionalCentersCount" />
        </div>
      </div>

      <div class="control-group">
        <button @click="resetToDefaults" class="btn btn-secondary">Reset to Defaults</button>
        <button @click="applyTestData" class="btn btn-primary">Apply Test Data</button>
      </div>
    </div>

    <div class="onboarding-preview">
      <h3>Onboarding Flow Preview</h3>
      <div class="preview-container">
        <OnboardingFlow 
          :showOnboarding="showOnboarding"
          :testMode="true"
          :testData="testData"
          @onboarding-complete="handleOnboardingComplete"
          @onboarding-skipped="handleOnboardingSkipped"
          @location-detected="handleLocationDetected"
        />
      </div>
    </div>
  </div>
</template>

<script>
import OnboardingFlow from './OnboardingFlow.vue'

export default {
  name: 'OnboardingFlowTest',
  components: {
    OnboardingFlow
  },
  data() {
    return {
      showOnboarding: true,
      testCurrentStep: 1,
      testTotalSteps: 5,
      testUserProfile: {
        age: '',
        diagnosis: '',
        hasInsurance: false,
        hasRegionalCenter: false,
        therapies: []
      },
      testUserLocation: '',
      testLocationDetecting: false,
      testLocationError: null,
      testResultsCount: 0,
      testRegionalCentersCount: 0
    }
  },
  computed: {
    testData() {
      return {
        currentStep: this.testCurrentStep,
        totalSteps: this.testTotalSteps,
        userProfile: this.testUserProfile,
        userLocation: this.testUserLocation,
        locationDetecting: this.testLocationDetecting,
        locationError: this.testLocationError,
        resultsCount: this.testResultsCount,
        regionalCentersCount: this.testRegionalCentersCount
      }
    }
  },
  methods: {
    updateOnboardingStep() {
      // This method can be used to programmatically change the onboarding step
      console.log('Onboarding step changed to:', this.testCurrentStep)
    },
    
    resetToDefaults() {
      this.testCurrentStep = 1
      this.testTotalSteps = 5
      this.testUserProfile = {
        age: '',
        diagnosis: '',
        hasInsurance: false,
        hasRegionalCenter: false,
        therapies: []
      }
      this.testUserLocation = ''
      this.testLocationDetecting = false
      this.testLocationError = null
      this.testResultsCount = 0
      this.testRegionalCentersCount = 0
    },
    
    applyTestData() {
      console.log('Applying test data:', this.testData)
      // This method can be used to apply test data to the onboarding flow
    },
    
    handleOnboardingComplete(data) {
      console.log('Onboarding completed with data:', data)
    },
    
    handleOnboardingSkipped() {
      console.log('Onboarding skipped')
    },
    
    handleLocationDetected(location) {
      console.log('Location detected:', location)
    }
  }
}
</script>

<style scoped>
.onboarding-test-container {
  display: flex;
  gap: 20px;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.test-controls {
  flex: 0 0 400px;
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  height: fit-content;
}

.control-group {
  margin-bottom: 20px;
}

.control-group h3 {
  margin-bottom: 10px;
  color: #333;
}

.param-controls, .profile-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.param-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.param-item label {
  min-width: 120px;
  font-weight: 500;
}

.param-item input[type="text"],
.param-item input[type="number"] {
  flex: 1;
  padding: 5px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.param-item input[type="checkbox"] {
  margin-right: 5px;
}

.onboarding-preview {
  flex: 1;
}

.preview-container {
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 20px;
  min-height: 500px;
  background: white;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 10px;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn:hover {
  opacity: 0.8;
}
</style>
