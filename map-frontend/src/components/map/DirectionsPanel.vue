<template>
  <div v-if="visible" class="directions-panel">
    <!-- Header -->
    <div class="directions-header">
      <div class="directions-title">
        <i class="bi bi-signpost-2-fill"></i>
        <h3>Directions</h3>
      </div>
      <button class="btn-close-directions" @click="$emit('close')" aria-label="Close directions">
        <i class="bi bi-x-lg"></i>
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="directions-loading">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading directions...</span>
      </div>
      <p>Calculating route...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="directions-error">
      <i class="bi bi-exclamation-triangle-fill"></i>
      <p>{{ error }}</p>
      <button class="btn-retry" @click="$emit('retry')">
        <i class="bi bi-arrow-clockwise"></i>
        Try Again
      </button>
    </div>

    <!-- Directions Content -->
    <div v-else-if="directions" class="directions-content">
      <!-- Route Summary -->
      <div class="route-summary">
        <div class="summary-item">
          <i class="bi bi-signpost-2"></i>
          <span class="summary-label">Distance:</span>
          <span class="summary-value">{{ formatDistance(directions.distance) }}</span>
        </div>
        <div class="summary-item">
          <i class="bi bi-clock"></i>
          <span class="summary-label">Time:</span>
          <span class="summary-value">{{ formatDuration(directions.duration) }}</span>
        </div>
      </div>

      <!-- Destination Info -->
      <div v-if="destination" class="destination-info">
        <div class="destination-name">
          <i class="bi bi-geo-alt-fill"></i>
          <strong>{{ destination.name }}</strong>
        </div>
        <div v-if="destination.address" class="destination-address">
          {{ formattedAddress }}
        </div>
      </div>

      <!-- Turn-by-Turn Steps -->
      <div class="directions-steps">
        <div class="steps-header">
          <h4>Turn-by-Turn Directions</h4>
          <span class="steps-count">{{ directions.steps.length }} steps</span>
        </div>

        <div class="steps-list">
          <div
            v-for="(step, index) in directions.steps"
            :key="index"
            class="direction-step"
            :class="{ 'is-destination': step.maneuver.type === 'arrive' }"
          >
            <div class="step-number">
              <i :class="getManeuverIcon(step.maneuver)"></i>
            </div>
            <div class="step-content">
              <div class="step-instruction">{{ step.instruction }}</div>
              <div class="step-details">
                <span v-if="step.name" class="step-name">{{ step.name }}</span>
                <span class="step-distance">{{ formatStepDistance(step.distance) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import {
  formatDistance,
  formatDuration,
  getManeuverIcon
} from '@/services/mapboxDirections';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  directions: {
    type: Object,
    default: null
  },
  destination: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: null
  }
});

defineEmits(['close', 'retry']);

/**
 * Format the destination address
 */
const formattedAddress = computed(() => {
  if (!props.destination?.address) return '';

  let addressObj = props.destination.address;

  // If address is a JSON string, parse it
  if (typeof addressObj === 'string') {
    try {
      if (addressObj.trim().startsWith('{')) {
        addressObj = JSON.parse(addressObj);
      } else {
        // Already a formatted string
        return addressObj;
      }
    } catch (e) {
      // Not valid JSON, return as is
      return addressObj;
    }
  }

  // If it's an object, format it as "street, city, state zip"
  if (typeof addressObj === 'object' && addressObj !== null) {
    const parts = [];
    if (addressObj.street) parts.push(addressObj.street);
    if (addressObj.city) parts.push(addressObj.city);
    if (addressObj.state) parts.push(addressObj.state);
    if (addressObj.zip) parts.push(addressObj.zip);
    return parts.join(', ');
  }

  return String(addressObj);
});

/**
 * Format step distance
 */
const formatStepDistance = (meters) => {
  const feet = meters * 3.28084;

  if (feet < 528) { // Less than 0.1 miles
    return `${Math.round(feet)} ft`;
  }

  const miles = feet / 5280;
  return `${miles.toFixed(1)} mi`;
};
</script>

<style scoped>
.directions-panel {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 380px;
  max-height: calc(100vh - 180px);
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  z-index: 1000;
  overflow: hidden;
}

/* Header */
.directions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
}

.directions-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.directions-title i {
  font-size: 20px;
  color: #2563eb;
}

.directions-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.btn-close-directions {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: #6b7280;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.btn-close-directions:hover {
  background-color: #f3f4f6;
  color: #1f2937;
}

.btn-close-directions i {
  font-size: 16px;
}

/* Loading State */
.directions-loading {
  padding: 60px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.directions-loading p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

/* Error State */
.directions-error {
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.directions-error i {
  font-size: 48px;
  color: #ef4444;
}

.directions-error p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.btn-retry {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-retry:hover {
  background-color: #1d4ed8;
}

/* Content */
.directions-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* Route Summary */
.route-summary {
  padding: 16px 20px;
  background: #eff6ff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  gap: 24px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.summary-item i {
  color: #2563eb;
  font-size: 16px;
}

.summary-label {
  color: #6b7280;
  font-weight: 500;
}

.summary-value {
  color: #1f2937;
  font-weight: 700;
}

/* Destination Info */
.destination-info {
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.destination-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 15px;
  color: #1f2937;
}

.destination-name i {
  color: #ef4444;
  font-size: 18px;
}

.destination-address {
  font-size: 13px;
  color: #6b7280;
  padding-left: 26px;
}

/* Steps */
.directions-steps {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.steps-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  background: white;
  z-index: 1;
}

.steps-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.steps-count {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.steps-list {
  padding: 12px 0;
}

/* Individual Step */
.direction-step {
  display: flex;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid #f3f4f6;
  transition: background-color 0.2s ease;
}

.direction-step:hover {
  background-color: #f9fafb;
}

.direction-step.is-destination {
  background-color: #fef3c7;
  border-left: 3px solid #f59e0b;
  padding-left: 17px;
}

.step-number {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  background-color: #eff6ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  font-size: 16px;
}

.direction-step.is-destination .step-number {
  background-color: #f59e0b;
  color: white;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-instruction {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
  margin-bottom: 4px;
  line-height: 1.4;
}

.step-details {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
}

.step-name {
  color: #6b7280;
  font-weight: 500;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-distance {
  color: #9ca3af;
  flex-shrink: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .directions-panel {
    top: auto;
    bottom: 0;
    right: 0;
    left: 0;
    width: 100%;
    max-height: 70vh;
    border-radius: 12px 12px 0 0;
  }

  .route-summary {
    flex-direction: column;
    gap: 12px;
  }
}

/* Scrollbar Styling */
.directions-steps::-webkit-scrollbar {
  width: 6px;
}

.directions-steps::-webkit-scrollbar-track {
  background: #f3f4f6;
}

.directions-steps::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.directions-steps::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
