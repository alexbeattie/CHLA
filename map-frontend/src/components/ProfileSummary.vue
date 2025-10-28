<template>
  <div class="profile-summary">
    <!-- Your Profile Card -->
    <div class="profile-card" v-if="hasProfile">
      <div class="card-header">
        <div class="header-left">
          <i class="bi bi-person-circle"></i>
          <h3>Your Profile</h3>
        </div>
        <button class="btn-edit" @click="$emit('edit-profile')" title="Edit Profile">
          <i class="bi bi-pencil"></i>
        </button>
      </div>
      
      <div class="profile-details">
        <div class="detail-item" v-if="profile.age">
          <i class="bi bi-calendar3"></i>
          <span>{{ formatAge(profile.age) }}</span>
        </div>
        
        <div class="detail-item" v-if="profile.diagnosis">
          <i class="bi bi-heart-pulse"></i>
          <span>{{ formatDiagnosis(profile.diagnosis) }}</span>
        </div>
        
        <div class="detail-item funding-sources" v-if="hasFunding">
          <i class="bi bi-wallet2"></i>
          <div class="funding-badges">
            <span class="badge badge-insurance" v-if="profile.hasInsurance">
              Insurance
            </span>
            <span class="badge badge-regional-center" v-if="profile.hasRegionalCenter">
              Regional Center
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Regional Center Card -->
    <div class="regional-center-card" v-if="regionalCenter">
      <div class="card-header">
        <div class="header-left">
          <i class="bi bi-building"></i>
          <h3>Your Regional Center</h3>
        </div>
      </div>
      
      <div class="regional-center-details">
        <div class="rc-name">{{ regionalCenter.name }}</div>
        
        <div class="rc-contact" v-if="regionalCenter.phone">
          <i class="bi bi-telephone"></i>
          <a :href="'tel:' + regionalCenter.phone" class="contact-link">
            {{ formatPhone(regionalCenter.phone) }}
          </a>
        </div>
        
        <div class="rc-website" v-if="regionalCenter.website">
          <i class="bi bi-globe"></i>
          <a :href="regionalCenter.website" target="_blank" rel="noopener" class="contact-link">
            Visit Website
            <i class="bi bi-box-arrow-up-right"></i>
          </a>
        </div>
      </div>
    </div>

    <!-- No Profile State -->
    <div class="no-profile-card" v-if="!hasProfile">
      <div class="no-profile-content">
        <i class="bi bi-person-plus"></i>
        <h3>Get Personalized Results</h3>
        <p>Set up your profile to see providers that match your needs</p>
        <button class="btn-setup" @click="$emit('edit-profile')">
          <i class="bi bi-pencil"></i>
          Set Up Profile
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProfileSummary',
  
  props: {
    profile: {
      type: Object,
      default: () => ({
        age: null,
        diagnosis: null,
        hasInsurance: false,
        hasRegionalCenter: false
      })
    },
    regionalCenter: {
      type: Object,
      default: null
    }
  },
  
  emits: ['edit-profile'],
  
  computed: {
    hasProfile() {
      return this.profile.age || this.profile.diagnosis || this.hasFunding;
    },
    
    hasFunding() {
      return this.profile.hasInsurance || this.profile.hasRegionalCenter;
    }
  },
  
  methods: {
    formatAge(age) {
      // Handle various age formats
      if (age.includes('-') || age.includes('+')) {
        return `${age} years`;
      }
      return age;
    },
    
    formatDiagnosis(diagnosis) {
      // Shorten common diagnoses for display
      const shortForms = {
        'Autism Spectrum Disorder': 'Autism',
        'Global Development Delay': 'Development Delay',
        'Intellectual Disability': 'Intellectual Disability',
        'Speech and Language Disorder': 'Speech/Language',
        'ADHD': 'ADHD'
      };
      return shortForms[diagnosis] || diagnosis;
    },
    
    formatPhone(phone) {
      // Format phone number for display
      const cleaned = phone.replace(/\D/g, '');
      if (cleaned.length === 10) {
        return `(${cleaned.slice(0,3)}) ${cleaned.slice(3,6)}-${cleaned.slice(6)}`;
      }
      return phone;
    }
  }
};
</script>

<style scoped>
.profile-summary {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Card Base Styles */
.profile-card,
.regional-center-card,
.no-profile-card {
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.profile-card:hover,
.regional-center-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-left i {
  font-size: 1.25rem;
  color: #004877;
}

.card-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
}

/* Edit Button */
.btn-edit {
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 0.375rem 0.625rem;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #6b7280;
}

.btn-edit:hover {
  background: #f9fafb;
  border-color: #004877;
  color: #004877;
}

.btn-edit i {
  font-size: 0.875rem;
}

/* Profile Details */
.profile-details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  font-size: 0.9375rem;
  color: #374151;
}

.detail-item i {
  font-size: 1.125rem;
  color: #6b7280;
  width: 1.25rem;
  text-align: center;
  flex-shrink: 0;
}

.detail-item.funding-sources {
  align-items: flex-start;
}

/* Funding Badges */
.funding-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.75rem;
  border-radius: 16px;
  font-size: 0.8125rem;
  font-weight: 500;
  line-height: 1.5;
}

.badge-insurance {
  background: #dbeafe;
  color: #1e40af;
}

.badge-regional-center {
  background: #d1fae5;
  color: #065f46;
}

/* Regional Center Details */
.regional-center-details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.rc-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
}

.rc-contact,
.rc-website {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  font-size: 0.875rem;
}

.rc-contact i,
.rc-website i {
  font-size: 1rem;
  color: #6b7280;
  width: 1.25rem;
  text-align: center;
  flex-shrink: 0;
}

.contact-link {
  color: #004877;
  text-decoration: none;
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.contact-link:hover {
  color: #0d9ddb;
  text-decoration: underline;
}

.contact-link .bi-box-arrow-up-right {
  font-size: 0.75rem;
}

/* No Profile State */
.no-profile-card {
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border: 2px dashed #d1d5db;
  text-align: center;
}

.no-profile-content {
  padding: 1rem 0;
}

.no-profile-content i {
  font-size: 2.5rem;
  color: #9ca3af;
  margin-bottom: 0.75rem;
}

.no-profile-content h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
}

.no-profile-content p {
  margin: 0 0 1.25rem 0;
  font-size: 0.875rem;
  color: #6b7280;
  line-height: 1.5;
}

.btn-setup {
  background: #004877;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.625rem 1.5rem;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-setup:hover {
  background: #003a5d;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 72, 119, 0.2);
}

.btn-setup:active {
  transform: translateY(0);
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .profile-card,
  .regional-center-card,
  .no-profile-card {
    padding: 1rem;
  }
  
  .card-header h3 {
    font-size: 0.9375rem;
  }
  
  .detail-item {
    font-size: 0.875rem;
  }
  
  .rc-name {
    font-size: 0.875rem;
  }
}
</style>

