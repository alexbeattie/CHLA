<template>
  <div
    class="provider-card"
    :class="{
      selected: selected,
      'has-coordinates': hasCoordinates,
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

    <!-- Distance and Directions -->
    <div v-if="distance !== null" class="provider-distance-section">
      <div class="provider-distance">
        <i class="bi bi-signpost-2"></i>
        <span>{{ formattedDistance }} {{ $t("providerCard.away") }}</span>
      </div>
      <button
        class="btn-get-directions"
        @click.stop="handleGetDirections"
        :aria-label="
          $t('providerCard.getDirectionsTo', { name: provider.name })
        "
        :title="$t('providerCard.directionsTitle')"
      >
        <i class="bi bi-pin-map-fill"></i>
        <span>{{ $t("providerCard.directions") }}</span>
      </button>
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
          {{ $t("providerCard.website") }}
        </a>
      </div>
    </div>

    <!-- Insurance Badges -->
    <div
      v-if="showInsurance && insuranceTypes.length > 0"
      class="provider-insurance"
    >
      <div class="insurance-label">
        <i class="bi bi-credit-card"></i>
        <span>{{ $t("providerCard.accepts") }}</span>
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
    <div
      v-if="
        showTherapies &&
        provider.therapy_types &&
        provider.therapy_types.length > 0
      "
      class="provider-therapies"
    >
      <div class="therapies-label">
        <i class="bi bi-clipboard2-pulse"></i>
        <span>{{ $t("providerCard.services") }}</span>
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
          +{{ provider.therapy_types.length - maxTherapiesToShow }}
          {{ $t("providerCard.more") }}
        </span>
      </div>
    </div>

    <!-- Age Groups -->
    <div
      v-if="
        showAgeGroups && provider.age_groups && provider.age_groups.length > 0
      "
      class="provider-ages"
    >
      <div class="ages-label">
        <i class="bi bi-people"></i>
        <span>{{ $t("providerCard.ages") }}</span>
      </div>
      <div class="ages-list">
        {{ provider.age_groups.join(", ") }}
      </div>
    </div>

    <!-- No Coordinates Warning -->
    <div v-if="!hasCoordinates" class="no-coordinates-warning">
      <i class="bi bi-exclamation-triangle"></i>
      <span>{{ $t("providerCard.locationNotAvailable") }}</span>
    </div>
  </div>
</template>

<script>
import { computed } from "vue";

/**
 * ProviderCard Component
 * Displays individual provider information in a card format
 * Week 4: Component Extraction
 */
export default {
  name: "ProviderCard",

  props: {
    // Provider data object
    provider: {
      type: Object,
      required: true,
    },
    // Whether this provider is selected
    selected: {
      type: Boolean,
      default: false,
    },
    // Distance to provider in miles (optional)
    distance: {
      type: Number,
      default: null,
    },
    // Show insurance information
    showInsurance: {
      type: Boolean,
      default: true,
    },
    // Show therapy types
    showTherapies: {
      type: Boolean,
      default: true,
    },
    // Show age groups
    showAgeGroups: {
      type: Boolean,
      default: true,
    },
    // Maximum therapies to display
    maxTherapiesToShow: {
      type: Number,
      default: 3,
    },
  },

  emits: ["click", "select", "get-directions"],

  setup(props, { emit }) {
    /**
     * Check if provider has valid coordinates
     */
    const hasCoordinates = computed(() => {
      return (
        props.provider.latitude !== null &&
        props.provider.longitude !== null &&
        !isNaN(props.provider.latitude) &&
        !isNaN(props.provider.longitude)
      );
    });

    /**
     * Format distance for display
     */
    const formattedDistance = computed(() => {
      if (props.distance === null) return "";

      if (props.distance < 0.1) {
        return "Less than 0.1 mi";
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
      if (typeof props.provider.address === "object") {
        const addr = props.provider.address;
        const parts = [];

        if (addr.street) parts.push(addr.street);
        if (addr.city) parts.push(addr.city);
        if (addr.state) parts.push(addr.state);
        if (addr.zip) parts.push(addr.zip);

        return parts.join(", ");
      }

      // Handle string address (might be JSON string)
      if (typeof props.provider.address === "string") {
        try {
          const addr = JSON.parse(props.provider.address);
          const parts = [];

          if (addr.street) parts.push(addr.street);
          if (addr.city) parts.push(addr.city);
          if (addr.state) parts.push(addr.state);
          if (addr.zip) parts.push(addr.zip);

          return parts.join(", ");
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
      if (typeof insuranceData === "object") {
        if (Array.isArray(insuranceData)) {
          return insuranceData.filter((type) => type && type.length > 0);
        }
        // Empty object {} means no insurance
        if (Object.keys(insuranceData).length === 0) {
          return [];
        }
        return [];
      }

      // Handle string (might be JSON string like "{Aetna,Cigna}")
      if (typeof insuranceData === "string") {
        // Remove curly braces if present
        insuranceData = insuranceData.replace(/^{|}$/g, "").trim();

        if (!insuranceData) return [];

        // Split by comma
        return insuranceData
          .split(",")
          .map((type) => type.trim().replace(/^"|"$/g, "")) // Remove quotes
          .filter((type) => type.length > 0);
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
      if (!phone) return "";

      // Remove all non-digit characters
      const digits = phone.replace(/\D/g, "");

      // Format as (XXX) XXX-XXXX if 10 digits
      if (digits.length === 10) {
        return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(
          6
        )}`;
      }

      // Return original if not 10 digits
      return phone;
    };

    /**
     * Format website URL
     */
    const formatWebsite = (website) => {
      if (!website) return "";

      // Add https:// if no protocol
      if (!website.startsWith("http://") && !website.startsWith("https://")) {
        return `https://${website}`;
      }

      return website;
    };

    /**
     * Handle card click
     */
    const handleClick = () => {
      console.log(
        `🔘 ProviderCard: Clicked on provider ${props.provider.id} - ${props.provider.name}`
      );

      emit("click", props.provider);
      emit("select", props.provider.id);
    };

    /**
     * Handle get directions button click
     */
    const handleGetDirections = () => {
      console.log(
        `🗺️ ProviderCard: Get directions to provider ${props.provider.id} - ${props.provider.name}`
      );
      emit("get-directions", props.provider);
    };

    return {
      hasCoordinates,
      formattedDistance,
      formattedAddress,
      insuranceTypes,
      displayedTherapies,
      formatPhone,
      formatWebsite,
      handleClick,
      handleGetDirections,
    };
  },
};
</script>

<style scoped>
.provider-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  position: relative;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.provider-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.1), 0 1px 4px rgba(0, 0, 0, 0.04);
}

.provider-card.selected {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #ffffff 0%, #eff6ff 100%);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.15), 0 1px 4px rgba(0, 0, 0, 0.04);
}

.provider-card:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

/* Header */
.provider-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
}

.provider-icon {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #3b82f6;
  transition: background 0.2s ease, color 0.2s ease;
}

.provider-card.selected .provider-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}

.provider-title {
  flex: 1;
  min-width: 0;
}

.provider-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  margin-bottom: 2px;
  line-height: 1.25;
  letter-spacing: -0.01em;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.provider-type {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  letter-spacing: 0.01em;
  line-height: 1.3;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.selected-indicator {
  flex-shrink: 0;
  color: #3b82f6;
  font-size: 22px;
  animation: fadeIn 0.3s ease;
  line-height: 1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* Distance and Directions Section */
.provider-distance-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f3f4f6;
}

.provider-distance {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #3b82f6;
  letter-spacing: -0.01em;
  line-height: 1;
}

.provider-distance i {
  font-size: 14px;
}

.btn-get-directions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 6px 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 7px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease;
  white-space: nowrap;
  box-shadow: 0 1px 4px rgba(59, 130, 246, 0.2);
  line-height: 1;
  height: 30px;
}

.btn-get-directions:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.btn-get-directions:active {
  box-shadow: 0 1px 3px rgba(59, 130, 246, 0.25);
}

.btn-get-directions i {
  font-size: 13px;
}

/* Address */
.provider-address {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 6px;
  line-height: 1.35;
}

.provider-address i {
  flex-shrink: 0;
  margin-top: 2px;
  color: #9ca3af;
  font-size: 13px;
}

/* Contact Info */
.provider-contact {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f3f4f6;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  line-height: 1.3;
}

.contact-item i {
  color: #6b7280;
  font-size: 13px;
}

.contact-link {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.contact-link:hover {
  color: #2563eb;
  text-decoration: underline;
}

/* Insurance Badges */
.provider-insurance {
  margin-bottom: 8px;
}

.insurance-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 5px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  line-height: 1;
}

.insurance-label i {
  font-size: 12px;
}

.insurance-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.insurance-badge {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 3px 10px;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  font-size: 11px;
  font-weight: 500;
  color: #374151;
  transition: background 0.15s ease, border-color 0.15s ease;
  line-height: 1.4;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.insurance-badge:hover {
  background-color: #f3f4f6;
  border-color: #d1d5db;
}

.insurance-badge.badge-insurance {
  background-color: #eff6ff;
  border-color: #bfdbfe;
  color: #1e40af;
}

.insurance-badge.badge-regional-center {
  background-color: #fffbeb;
  border-color: #fde68a;
  color: #92400e;
}

.insurance-badge.badge-private-pay {
  background-color: #faf5ff;
  border-color: #e9d5ff;
  color: #6b21a8;
}

/* Therapy Types */
.provider-therapies {
  margin-bottom: 8px;
}

.therapies-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 5px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  line-height: 1;
}

.therapies-label i {
  font-size: 12px;
}

.therapies-list {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.therapy-chip {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 3px 10px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  font-size: 11px;
  font-weight: 500;
  color: #1e40af;
  transition: background 0.15s ease, border-color 0.15s ease;
  line-height: 1.4;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.therapy-chip:hover {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.therapy-chip.more-chip {
  background: #f9fafb;
  border-color: #e5e7eb;
  color: #6b7280;
  font-weight: 600;
}

/* Age Groups */
.provider-ages {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #4b5563;
  margin-bottom: 0;
  line-height: 1.3;
}

.ages-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
  color: #6b7280;
  flex-shrink: 0;
}

.ages-list {
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
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
    padding: 16px;
    margin-bottom: 10px;
    border-radius: 14px;
  }

  .provider-icon {
    width: 44px;
    height: 44px;
    font-size: 20px;
  }

  .provider-name {
    font-size: 16px;
  }

  .provider-type,
  .provider-address,
  .contact-item {
    font-size: 13px;
  }

  .btn-get-directions {
    padding: 7px 14px;
    font-size: 12px;
  }

  .insurance-badge,
  .therapy-chip {
    padding: 4px 10px;
    font-size: 10px;
  }
}
</style>
