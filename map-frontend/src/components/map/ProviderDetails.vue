<template>
  <div class="provider-details" :class="{ 'is-visible': isVisible }">
    <!-- Header -->
    <div class="details-header">
      <div class="header-title">
        <i class="bi bi-hospital"></i>
        <h2>Provider Details</h2>
      </div>
      <button
        class="close-btn"
        @click="handleClose"
        aria-label="Close details"
      >
        <i class="bi bi-x-lg"></i>
      </button>
    </div>

    <!-- Content -->
    <div v-if="provider" class="details-content">
      <!-- Provider Name & Type -->
      <div class="provider-header-section">
        <h3 class="provider-name">{{ provider.name }}</h3>
        <div v-if="provider.type" class="provider-type-badge">
          {{ provider.type }}
        </div>
      </div>

      <!-- Distance (if available) -->
      <div v-if="distance !== null" class="distance-section">
        <i class="bi bi-geo-alt-fill"></i>
        <span class="distance-text">{{ formattedDistance }} away</span>
      </div>

      <!-- Address Section -->
      <div v-if="provider.address" class="detail-section">
        <div class="section-header">
          <i class="bi bi-map"></i>
          <h4>Address</h4>
        </div>
        <div class="section-content">
          <p class="address-text">{{ provider.address }}</p>
          <button
            v-if="showDirections && hasCoordinates"
            class="btn-directions"
            @click="handleGetDirections"
          >
            <i class="bi bi-signpost-2"></i>
            <span>Get Directions</span>
          </button>
        </div>
      </div>

      <!-- Contact Section -->
      <div class="detail-section">
        <div class="section-header">
          <i class="bi bi-telephone"></i>
          <h4>Contact Information</h4>
        </div>
        <div class="section-content">
          <div v-if="provider.phone" class="contact-item">
            <i class="bi bi-telephone-fill"></i>
            <a :href="`tel:${provider.phone}`" class="contact-link">
              {{ formatPhone(provider.phone) }}
            </a>
          </div>
          <div v-if="provider.email" class="contact-item">
            <i class="bi bi-envelope-fill"></i>
            <a :href="`mailto:${provider.email}`" class="contact-link">
              {{ provider.email }}
            </a>
          </div>
          <div v-if="provider.website" class="contact-item">
            <i class="bi bi-globe"></i>
            <a
              :href="formatWebsite(provider.website)"
              target="_blank"
              rel="noopener noreferrer"
              class="contact-link"
            >
              Visit Website
              <i class="bi bi-box-arrow-up-right"></i>
            </a>
          </div>
        </div>
      </div>

      <!-- Insurance Section -->
      <div v-if="provider.insurance_accepted" class="detail-section">
        <div class="section-header">
          <i class="bi bi-credit-card"></i>
          <h4>Insurance Accepted</h4>
        </div>
        <div class="section-content">
          <div class="insurance-badges">
            <span
              v-for="insurance in insuranceTypes"
              :key="insurance"
              class="insurance-badge"
            >
              {{ insurance }}
            </span>
          </div>
        </div>
      </div>

      <!-- Therapy Types Section -->
      <div v-if="provider.therapy_types && provider.therapy_types.length > 0" class="detail-section">
        <div class="section-header">
          <i class="bi bi-clipboard2-pulse"></i>
          <h4>Services Offered</h4>
        </div>
        <div class="section-content">
          <ul class="therapy-list">
            <li v-for="therapy in provider.therapy_types" :key="therapy">
              <i class="bi bi-check-circle-fill"></i>
              <span>{{ therapy }}</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Age Groups Section -->
      <div v-if="provider.age_groups && provider.age_groups.length > 0" class="detail-section">
        <div class="section-header">
          <i class="bi bi-people"></i>
          <h4>Age Groups Served</h4>
        </div>
        <div class="section-content">
          <div class="age-chips">
            <span
              v-for="age in provider.age_groups"
              :key="age"
              class="age-chip"
            >
              {{ age }}
            </span>
          </div>
        </div>
      </div>

      <!-- Diagnoses Section -->
      <div v-if="provider.diagnoses_treated && provider.diagnoses_treated.length > 0" class="detail-section">
        <div class="section-header">
          <i class="bi bi-heart-pulse"></i>
          <h4>Diagnoses Treated</h4>
        </div>
        <div class="section-content">
          <div class="diagnoses-chips">
            <span
              v-for="diagnosis in provider.diagnoses_treated"
              :key="diagnosis"
              class="diagnosis-chip"
            >
              {{ diagnosis }}
            </span>
          </div>
        </div>
      </div>

      <!-- Description Section -->
      <div v-if="provider.description" class="detail-section">
        <div class="section-header">
          <i class="bi bi-info-circle"></i>
          <h4>About</h4>
        </div>
        <div class="section-content">
          <p class="description-text">{{ provider.description }}</p>
        </div>
      </div>

      <!-- No Coordinates Warning -->
      <div v-if="!hasCoordinates" class="warning-section">
        <i class="bi bi-exclamation-triangle"></i>
        <span>Location coordinates not available - cannot show on map</span>
      </div>

      <!-- Custom Footer Slot -->
      <div v-if="$slots.footer" class="details-footer">
        <slot name="footer"></slot>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-details">
      <div class="empty-icon">
        <i class="bi bi-info-circle"></i>
      </div>
      <p>No provider selected</p>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue';
import { useMapStore } from '@/stores/mapStore';

/**
 * ProviderDetails Component
 * Displays detailed information about a selected provider
 * Week 4: Component Extraction
 */
export default {
  name: 'ProviderDetails',

  props: {
    // Provider object to display
    provider: {
      type: Object,
      default: null
    },
    // Whether the panel is visible
    isVisible: {
      type: Boolean,
      default: true
    },
    // Distance to provider in miles
    distance: {
      type: Number,
      default: null
    },
    // Show "Get Directions" button
    showDirections: {
      type: Boolean,
      default: true
    }
  },

  emits: [
    'close',          // When close button is clicked
    'get-directions'  // When get directions is clicked
  ],

  setup(props, { emit }) {
    const mapStore = useMapStore();

    /**
     * Check if provider has valid coordinates
     */
    const hasCoordinates = computed(() => {
      if (!props.provider) return false;
      return props.provider.latitude !== null &&
             props.provider.longitude !== null &&
             !isNaN(props.provider.latitude) &&
             !isNaN(props.provider.longitude);
    });

    /**
     * Format distance for display
     */
    const formattedDistance = computed(() => {
      if (props.distance === null) return '';

      if (props.distance < 0.1) {
        return 'Less than 0.1 mi';
      } else if (props.distance < 1) {
        return `${props.distance.toFixed(1)} mi`;
      } else {
        return `${props.distance.toFixed(1)} mi`;
      }
    });

    /**
     * Parse insurance types
     */
    const insuranceTypes = computed(() => {
      if (!props.provider?.insurance_accepted) return [];

      const insuranceStr = props.provider.insurance_accepted;
      const separator = insuranceStr.includes('|') ? '|' : ',';

      return insuranceStr
        .split(separator)
        .map(type => type.trim())
        .filter(type => type.length > 0);
    });

    /**
     * Format phone number
     */
    const formatPhone = (phone) => {
      if (!phone) return '';

      const digits = phone.replace(/\D/g, '');

      if (digits.length === 10) {
        return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`;
      }

      return phone;
    };

    /**
     * Format website URL
     */
    const formatWebsite = (website) => {
      if (!website) return '';

      if (!website.startsWith('http://') && !website.startsWith('https://')) {
        return `https://${website}`;
      }

      return website;
    };

    /**
     * Handle close button click
     */
    const handleClose = () => {
      console.log('📄 ProviderDetails: Close button clicked');
      emit('close');
    };

    /**
     * Handle get directions click
     */
    const handleGetDirections = async () => {
      console.log('📄 ProviderDetails: Get directions clicked');

      if (!props.provider || !hasCoordinates.value) {
        console.error('❌ ProviderDetails: Cannot get directions - no coordinates');
        return;
      }

      // Use map store to get directions
      try {
        await mapStore.getDirectionsTo({
          lat: props.provider.latitude,
          lng: props.provider.longitude
        });

        emit('get-directions', {
          providerId: props.provider.id,
          providerName: props.provider.name,
          coordinates: {
            lat: props.provider.latitude,
            lng: props.provider.longitude
          }
        });

        console.log('✅ ProviderDetails: Directions requested');
      } catch (error) {
        console.error('❌ ProviderDetails: Error getting directions:', error);
      }
    };

    return {
      hasCoordinates,
      formattedDistance,
      insuranceTypes,
      formatPhone,
      formatWebsite,
      handleClose,
      handleGetDirections
    };
  }
};
</script>

<style scoped>
.provider-details {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: white;
  border-left: 1px solid #e5e7eb;
  overflow: hidden;
}

/* Header */
.details-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background-color: #f9fafb;
  border-bottom: 2px solid #e5e7eb;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title i {
  font-size: 24px;
  color: #2563eb;
}

.header-title h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.close-btn {
  width: 36px;
  height: 36px;
  background-color: transparent;
  border: none;
  border-radius: 6px;
  color: #6b7280;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background-color: #f3f4f6;
  color: #1f2937;
}

/* Content */
.details-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* Provider Header Section */
.provider-header-section {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.provider-name {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
  line-height: 1.3;
}

.provider-type-badge {
  display: inline-block;
  padding: 6px 12px;
  background-color: #eff6ff;
  color: #1e40af;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
}

/* Distance Section */
.distance-section {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: #eff6ff;
  border-radius: 8px;
  margin-bottom: 20px;
}

.distance-section i {
  font-size: 20px;
  color: #2563eb;
}

.distance-text {
  font-size: 16px;
  font-weight: 600;
  color: #1e40af;
}

/* Detail Section */
.detail-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.section-header i {
  font-size: 18px;
  color: #6b7280;
}

.section-header h4 {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin: 0;
}

.section-content {
  padding-left: 28px;
}

/* Address */
.address-text {
  color: #4b5563;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.btn-directions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-directions:hover {
  background-color: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.btn-directions i {
  font-size: 16px;
}

/* Contact Items */
.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.contact-item i {
  font-size: 16px;
  color: #6b7280;
  width: 20px;
}

.contact-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s ease;
}

.contact-link:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

/* Insurance Badges */
.insurance-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.insurance-badge {
  display: inline-block;
  padding: 8px 14px;
  background-color: #f3f4f6;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

/* Therapy List */
.therapy-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.therapy-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  color: #4b5563;
}

.therapy-list i {
  font-size: 16px;
  color: #10b981;
  flex-shrink: 0;
}

/* Age Chips */
.age-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.age-chip {
  display: inline-block;
  padding: 8px 14px;
  background-color: #fef3c7;
  color: #92400e;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}

/* Diagnoses Chips */
.diagnoses-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.diagnosis-chip {
  display: inline-block;
  padding: 8px 14px;
  background-color: #fce7f3;
  color: #9f1239;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}

/* Description */
.description-text {
  color: #4b5563;
  line-height: 1.7;
  margin: 0;
}

/* Warning Section */
.warning-section {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background-color: #fef3c7;
  border-radius: 8px;
  margin-top: 20px;
  color: #92400e;
  font-size: 14px;
}

.warning-section i {
  font-size: 20px;
  color: #f59e0b;
  flex-shrink: 0;
}

/* Footer */
.details-footer {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

/* Empty State */
.empty-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: #9ca3af;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background-color: #f3f4f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.empty-icon i {
  font-size: 36px;
}

.empty-details p {
  font-size: 16px;
  margin: 0;
}

/* Scrollbar */
.details-content::-webkit-scrollbar {
  width: 8px;
}

.details-content::-webkit-scrollbar-track {
  background: #f3f4f6;
}

.details-content::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}

.details-content::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* Responsive */
@media (max-width: 768px) {
  .details-header {
    padding: 16px;
  }

  .header-title h2 {
    font-size: 18px;
  }

  .details-content {
    padding: 16px;
  }

  .provider-name {
    font-size: 20px;
  }
}
</style>
