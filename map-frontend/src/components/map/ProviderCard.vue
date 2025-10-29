<template>
  <div
    class="provider-card"
    :class="{
      'selected': selected,
      'has-coordinates': hasCoordinates
    }"
    :data-provider-id="provider.id"
    @click="handleClick"
    role="button"
    :aria-label="`Provider: ${provider.name}`"
    :aria-pressed="selected"
    tabindex="0"
    @keyup.enter="handleClick"
    @keyup.space="handleClick"
  >
    <!-- Provider Header -->
    <div class="provider-header">
      <div class="provider-icon">
        <i class="bi bi-hospital"></i>
      </div>
      <div class="provider-title">
        <h3 class="provider-name">{{ provider.name }}</h3>
        <div v-if="provider.type" class="provider-type">
          {{ provider.type }}
        </div>
      </div>
      <div v-if="selected" class="selected-indicator">
        <i class="bi bi-check-circle-fill"></i>
      </div>
    </div>

    <!-- Distance (if provided) -->
    <div v-if="distance !== null" class="provider-distance">
      <i class="bi bi-geo-alt-fill"></i>
      <span>{{ formattedDistance }}</span>
    </div>

    <!-- Address -->
    <div v-if="formattedAddress" class="provider-address">
      <i class="bi bi-map"></i>
      <span>{{ formattedAddress }}</span>
    </div>

    <!-- Contact Info -->
    <div class="provider-contact">
      <div v-if="provider.phone" class="contact-item">
        <i class="bi bi-telephone"></i>
        <a :href="`tel:${provider.phone}`" @click.stop class="contact-link">
          {{ formatPhone(provider.phone) }}
        </a>
      </div>
      <div v-if="provider.website" class="contact-item">
        <i class="bi bi-globe"></i>
        <a
          :href="formatWebsite(provider.website)"
          target="_blank"
          rel="noopener noreferrer"
          @click.stop
          class="contact-link"
        >
          Website
        </a>
      </div>
    </div>

    <!-- Insurance Badges -->
    <div v-if="showInsurance && insuranceTypes.length > 0" class="provider-insurance">
      <div class="insurance-label">
        <i class="bi bi-credit-card"></i>
        <span>Accepts:</span>
      </div>
      <div class="insurance-badges">
        <span
          v-for="insurance in insuranceTypes"
          :key="insurance"
          class="insurance-badge"
          :class="`badge-${insurance.toLowerCase().replace(/\s+/g, '-')}`"
        >
          {{ insurance }}
        </span>
      </div>
    </div>

    <!-- Therapy Types -->
    <div v-if="showTherapies && provider.therapy_types && provider.therapy_types.length > 0" class="provider-therapies">
      <div class="therapies-label">
        <i class="bi bi-clipboard2-pulse"></i>
        <span>Services:</span>
      </div>
      <div class="therapies-list">
        <span
          v-for="therapy in displayedTherapies"
          :key="therapy"
          class="therapy-chip"
        >
          {{ therapy }}
        </span>
        <span
          v-if="provider.therapy_types.length > maxTherapiesToShow"
          class="therapy-chip more-chip"
        >
          +{{ provider.therapy_types.length - maxTherapiesToShow }} more
        </span>
      </div>
    </div>

    <!-- Age Groups -->
    <div v-if="showAgeGroups && provider.age_groups && provider.age_groups.length > 0" class="provider-ages">
      <div class="ages-label">
        <i class="bi bi-people"></i>
        <span>Ages:</span>
      </div>
      <div class="ages-list">
        {{ provider.age_groups.join(', ') }}
      </div>
    </div>

    <!-- No Coordinates Warning -->
    <div v-if="!hasCoordinates" class="no-coordinates-warning">
      <i class="bi bi-exclamation-triangle"></i>
      <span>Location not available on map</span>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue';

/**
 * ProviderCard Component
 * Displays individual provider information in a card format
 * Week 4: Component Extraction
 */
export default {
  name: 'ProviderCard',

  props: {
    // Provider data object
    provider: {
      type: Object,
      required: true
    },
    // Whether this provider is selected
    selected: {
      type: Boolean,
      default: false
    },
    // Distance to provider in miles (optional)
    distance: {
      type: Number,
      default: null
    },
    // Show insurance information
    showInsurance: {
      type: Boolean,
      default: true
    },
    // Show therapy types
    showTherapies: {
      type: Boolean,
      default: true
    },
    // Show age groups
    showAgeGroups: {
      type: Boolean,
      default: false
    },
    // Maximum therapies to display
    maxTherapiesToShow: {
      type: Number,
      default: 3
    }
  },

  emits: ['click', 'select'],

  setup(props, { emit }) {
    /**
     * Check if provider has valid coordinates
     */
    const hasCoordinates = computed(() => {
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
     * Format address from JSON object to string
     */
    const formattedAddress = computed(() => {
      if (!props.provider.address) return null;

      // Handle JSON object address
      if (typeof props.provider.address === 'object') {
        const addr = props.provider.address;
        const parts = [];

        if (addr.street) parts.push(addr.street);
        if (addr.city) parts.push(addr.city);
        if (addr.state) parts.push(addr.state);
        if (addr.zip) parts.push(addr.zip);

        return parts.join(', ');
      }

      // Handle string address (might be JSON string)
      if (typeof props.provider.address === 'string') {
        try {
          const addr = JSON.parse(props.provider.address);
          const parts = [];

          if (addr.street) parts.push(addr.street);
          if (addr.city) parts.push(addr.city);
          if (addr.state) parts.push(addr.state);
          if (addr.zip) parts.push(addr.zip);

          return parts.join(', ');
        } catch (e) {
          // If not JSON, return as-is
          return props.provider.address;
        }
      }

      return null;
    });

    /**
     * Parse and display insurance types
     */
    const insuranceTypes = computed(() => {
      if (!props.provider.insurance_accepted) return [];

      let insuranceData = props.provider.insurance_accepted;

      // Handle JSON object or empty object
      if (typeof insuranceData === 'object') {
        if (Array.isArray(insuranceData)) {
          return insuranceData.filter(type => type && type.length > 0);
        }
        // Empty object {} means no insurance
        if (Object.keys(insuranceData).length === 0) {
          return [];
        }
        return [];
      }

      // Handle string (might be JSON string like "{Aetna,Cigna}")
      if (typeof insuranceData === 'string') {
        // Remove curly braces if present
        insuranceData = insuranceData.replace(/^{|}$/g, '').trim();

        if (!insuranceData) return [];

        // Split by comma
        return insuranceData
          .split(',')
          .map(type => type.trim().replace(/^"|"$/g, '')) // Remove quotes
          .filter(type => type.length > 0);
      }

      return [];
    });

    /**
     * Get therapies to display (limited by maxTherapiesToShow)
     */
    const displayedTherapies = computed(() => {
      if (!props.provider.therapy_types) return [];
      return props.provider.therapy_types.slice(0, props.maxTherapiesToShow);
    });

    /**
     * Format phone number for display
     */
    const formatPhone = (phone) => {
      if (!phone) return '';

      // Remove all non-digit characters
      const digits = phone.replace(/\D/g, '');

      // Format as (XXX) XXX-XXXX if 10 digits
      if (digits.length === 10) {
        return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`;
      }

      // Return original if not 10 digits
      return phone;
    };

    /**
     * Format website URL
     */
    const formatWebsite = (website) => {
      if (!website) return '';

      // Add https:// if no protocol
      if (!website.startsWith('http://') && !website.startsWith('https://')) {
        return `https://${website}`;
      }

      return website;
    };

    /**
     * Handle card click
     */
    const handleClick = () => {
      console.log(`🔘 ProviderCard: Clicked on provider ${props.provider.id} - ${props.provider.name}`);

      emit('click', props.provider);
      emit('select', props.provider.id);
    };

    return {
      hasCoordinates,
      formattedDistance,
      formattedAddress,
      insuranceTypes,
      displayedTherapies,
      formatPhone,
      formatWebsite,
      handleClick
    };
  }
};
</script>

<style scoped>
.provider-card {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.provider-card:hover {
  border-color: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
  transform: translateY(-2px);
}

.provider-card.selected {
  border-color: #2563eb;
  background-color: #eff6ff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.provider-card:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* Header */
.provider-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.provider-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  background-color: #eff6ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #2563eb;
}

.provider-card.selected .provider-icon {
  background-color: #2563eb;
  color: white;
}

.provider-title {
  flex: 1;
  min-width: 0;
}

.provider-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  margin-bottom: 4px;
  line-height: 1.3;
}

.provider-type {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.selected-indicator {
  flex-shrink: 0;
  color: #2563eb;
  font-size: 24px;
}

/* Distance */
.provider-distance {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #2563eb;
  margin-bottom: 8px;
}

.provider-distance i {
  font-size: 16px;
}

/* Address */
.provider-address {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  color: #4b5563;
  margin-bottom: 12px;
  line-height: 1.5;
}

.provider-address i {
  flex-shrink: 0;
  margin-top: 2px;
  color: #9ca3af;
}

/* Contact Info */
.provider-contact {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.contact-item i {
  color: #6b7280;
}

.contact-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

.contact-link:hover {
  text-decoration: underline;
}

/* Insurance Badges */
.provider-insurance {
  margin-bottom: 12px;
}

.insurance-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 6px;
}

.insurance-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.insurance-badge {
  display: inline-block;
  padding: 4px 10px;
  background-color: #f3f4f6;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
}

.insurance-badge.badge-insurance {
  background-color: #dbeafe;
  color: #1e40af;
}

.insurance-badge.badge-regional-center {
  background-color: #fef3c7;
  color: #92400e;
}

.insurance-badge.badge-private-pay {
  background-color: #f3e8ff;
  color: #6b21a8;
}

/* Therapy Types */
.provider-therapies {
  margin-bottom: 12px;
}

.therapies-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 6px;
}

.therapies-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.therapy-chip {
  display: inline-block;
  padding: 4px 10px;
  background-color: #eff6ff;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: #1e40af;
}

.therapy-chip.more-chip {
  background-color: #f3f4f6;
  color: #6b7280;
}

/* Age Groups */
.provider-ages {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 8px;
}

.ages-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #6b7280;
  flex-shrink: 0;
}

.ages-list {
  flex: 1;
}

/* Warning */
.no-coordinates-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background-color: #fef3c7;
  border-radius: 6px;
  font-size: 12px;
  color: #92400e;
  margin-top: 8px;
}

.no-coordinates-warning i {
  color: #f59e0b;
}

/* Responsive */
@media (max-width: 768px) {
  .provider-card {
    padding: 14px;
    margin-bottom: 10px;
  }

  .provider-name {
    font-size: 15px;
  }

  .provider-type,
  .provider-address,
  .contact-item {
    font-size: 13px;
  }
}
</style>
