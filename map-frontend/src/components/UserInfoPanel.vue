<template>
  <div class="card mb-3 user-info-panel">
    <div class="card-header d-flex justify-content-between align-items-center">
      <h5 class="mb-0">Your Information</h5>
      <button class="btn btn-sm btn-outline-secondary" @click="$emit('toggle')">
        {{ showPanel ? 'Hide' : 'Show' }}
      </button>
    </div>
    <div class="card-body" v-if="showPanel">
      <!-- Age -->
      <div class="mb-3">
        <label class="form-label">Age</label>
        <input type="number" v-model="formData.age" class="form-control" min="0" max="120" 
               inputmode="numeric" pattern="[0-9]*">
      </div>
      
      <!-- Address -->
      <div class="mb-3">
        <label class="form-label">Address</label>
        <input type="text" v-model="formData.address" class="form-control" 
               placeholder="Enter your address">
      </div>
      
      <!-- Diagnosis -->
      <div class="mb-3">
        <label class="form-label">Diagnosis</label>
        <select v-model="formData.diagnosis" class="form-select">
          <option value="">Select diagnosis...</option>
          <option value="Autism">Autism</option>
          <option value="dup15q">dup15q</option>
          <option value="TSC">TSC</option>
          <option value="ADHD">ADHD</option>
          <option value="Angelman Syndrome">Angelman Syndrome</option>
          <option value="Rhetts">Rhetts</option>
          <option value="Anxiety/Depression">Anxiety/Depression</option>
          <option value="Other">Other</option>
        </select>
      </div>
      
      <!-- Other Diagnosis -->
      <div class="mb-3" v-if="formData.diagnosis === 'Other'">
        <label class="form-label">Please specify</label>
        <input type="text" v-model="formData.otherDiagnosis" class="form-control" 
               placeholder="Enter diagnosis">
      </div>
      
      <!-- Save Button -->
      <button class="btn btn-primary w-100" @click="saveData">
        Save Information
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserInfoPanel',
  
  props: {
    userData: {
      type: Object,
      required: true
    },
    showPanel: {
      type: Boolean,
      default: true
    }
  },
  
  data() {
    return {
      formData: {
        age: '',
        address: '',
        diagnosis: '',
        otherDiagnosis: ''
      }
    };
  },
  
  watch: {
    userData: {
      handler(newData) {
        this.formData = { ...newData };
      },
      immediate: true,
      deep: true
    }
  },
  
  methods: {
    saveData() {
      // Validate user data
      if (!this.validateUserData()) {
        return;
      }
      
      // Emit the save event with the form data
      this.$emit('save', { ...this.formData });
    },
    
    validateUserData() {
      // Basic validation
      if (this.formData.diagnosis === '') {
        alert('Please select a diagnosis');
        return false;
      }
      
      if (this.formData.diagnosis === 'Other' && !this.formData.otherDiagnosis) {
        alert('Please specify your diagnosis');
        return false;
      }
      
      return true;
    }
  }
}
</script>

<style scoped>
.user-info-panel {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0,0,0,0.08);
  transition: all 0.3s ease;
}

.card-header {
  background-color: #f8f9fa;
  border-bottom: 1px solid rgba(0,0,0,0.08);
  padding: 12px 15px;
}

.card-body {
  background-color: white;
  padding: 15px;
}

.form-label {
  font-weight: 500;
  margin-bottom: 4px;
  font-size: 0.9rem;
}

.btn-primary {
  margin-top: 5px;
  transition: all 0.2s ease;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.btn-primary:active {
  transform: translateY(0);
}

/* Mobile adjustments */
@media (max-width: 768px) {
  .user-info-panel {
    margin-bottom: 12px !important;
  }
  
  .card-header {
    padding: 10px 12px;
  }
  
  .card-body {
    padding: 12px;
  }
  
  .form-label {
    font-size: 0.85rem;
  }
  
  .mb-3 {
    margin-bottom: 10px !important;
  }
}
</style>