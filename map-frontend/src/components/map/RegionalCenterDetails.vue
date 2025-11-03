<template>
  <transition name="slide">
    <div
      v-if="isVisible && regionalCenter"
      class="regional-center-details-panel"
      @click.stop
    >
      <!-- Header -->
      <div class="panel-header">
        <div class="header-title">
          <i class="bi bi-building"></i>
          <h2>Regional Center</h2>
        </div>
        <button class="btn-close" @click="$emit('close')" aria-label="Close">
          <i class="bi bi-x-lg"></i>
        </button>
      </div>

      <!-- Content -->
      <div class="panel-content">
        <!-- Name -->
        <div class="rc-header">
          <h3 class="rc-name">{{ regionalCenter.regional_center || regionalCenter.name }}</h3>
        </div>

        <!-- Distance -->
        <div v-if="distance !== null" class="distance-section">
          <i class="bi bi-geo-alt-fill"></i>
          <span class="distance-text">{{ formattedDistance }} away</span>
        </div>

        <!-- Address Section -->
        <div class="detail-section">
          <div class="section-header">
            <i class="bi bi-map"></i>
            <h4>Address</h4>
          </div>
          <div class="section-content">
            <div class="address-lines">
              <div v-if="regionalCenter.address" class="address-line">
                {{ regionalCenter.address }}
              </div>
              <div v-if="regionalCenter.suite" class="address-line">
                Suite {{ regionalCenter.suite }}
              </div>
              <div class="address-line">
                <span v-if="regionalCenter.city">{{ regionalCenter.city }}, </span>
                <span v-if="regionalCenter.state">{{ regionalCenter.state }} </span>
                <span v-if="regionalCenter.zip_code">{{ regionalCenter.zip_code }}</span>
              </div>
            </div>
            <button
              v-if="hasCoordinates"
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
            <div v-if="regionalCenter.telephone" class="contact-item">
              <i class="bi bi-telephone-fill"></i>
              <a :href="`tel:${regionalCenter.telephone}`" class="contact-link">
                {{ formatPhone(regionalCenter.telephone) }}
              </a>
            </div>
            <div v-if="regionalCenter.website" class="contact-item">
              <i class="bi bi-globe"></i>
              <a
                :href="regionalCenter.website"
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

        <!-- Service Area -->
        <div v-if="regionalCenter.county_served" class="detail-section">
          <div class="section-header">
            <i class="bi bi-geo"></i>
            <h4>Service Area</h4>
          </div>
          <div class="section-content">
            <p>{{ regionalCenter.county_served }}</p>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  regionalCenter: {
    type: Object,
    default: null
  },
  isVisible: {
    type: Boolean,
    default: false
  },
  distance: {
    type: Number,
    default: null
  }
});

const emit = defineEmits(['close', 'get-directions']);

/**
 * Check if regional center has coordinates
 */
const hasCoordinates = computed(() => {
  return props.regionalCenter?.latitude && props.regionalCenter?.longitude;
});


/**
 * Format distance
 */
const formattedDistance = computed(() => {
  if (props.distance === null) return '';
  return `${props.distance.toFixed(1)} mi`;
});

/**
 * Format phone number
 */
const formatPhone = (phone) => {
  if (!phone) return '';
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.length === 10) {
    return `(${cleaned.slice(0, 3)}) ${cleaned.slice(3, 6)}-${cleaned.slice(6)}`;
  }
  return phone;
};

/**
 * Handle get directions click
 */
const handleGetDirections = () => {
  emit('get-directions', {
    provider: props.regionalCenter,
    coordinates: {
      lat: props.regionalCenter.latitude,
      lng: props.regionalCenter.longitude
    }
  });
  // Close the panel after getting directions
  emit('close');
};
</script>

<style scoped>
/* Slide transition */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from {
  transform: translateX(-100%);
}

.slide-leave-to {
  transform: translateX(-100%);
}

/* Panel styling */
.regional-center-details-panel {
  position: fixed;
  top: 60px;
  left: 380px; /* Next to sidebar */
  bottom: 0;
  width: 400px;
  background: white;
  box-shadow: 4px 0 12px rgba(0, 0, 0, 0.15);
  z-index: 1000; /* Above map markers */
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Header */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title i {
  font-size: 20px;
  color: #10b981;
}

.header-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.btn-close {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: #6b7280;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  color: #1f2937;
}

/* Content */
.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* Regional Center Header */
.rc-header {
  margin-bottom: 16px;
  text-align: center;
  display: block; /* Override any grid layout from parent */
  grid-template-columns: unset; /* Remove grid columns if inherited */
}

.rc-name {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
  text-align: center;
}

.office-type-badge {
  display: inline-block;
  padding: 4px 12px;
  background: #dbeafe;
  color: #1e40af;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  margin-top: 8px;
}

/* Distance */
.distance-section {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f0fdf4;
  border-radius: 8px;
  margin-bottom: 20px;
}

.distance-section i {
  color: #10b981;
  font-size: 18px;
}

.distance-text {
  font-weight: 600;
  color: #059669;
}

/* Detail Sections */
.detail-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-header i {
  color: #6b7280;
  font-size: 16px;
}

.section-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-content {
  padding-left: 24px;
}

/* Address */
.address-lines {
  margin-bottom: 16px;
}

.address-line {
  color: #374151;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
}

.address-text {
  color: #4b5563;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

/* Directions Button */
.btn-directions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  justify-content: center;
}

.btn-directions:hover {
  background: #059669;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(16, 185, 129, 0.3);
}

.btn-directions i {
  font-size: 16px;
}

/* Contact */
.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.contact-item i {
  color: #10b981;
  font-size: 14px;
  width: 16px;
}

.contact-link {
  color: #2563eb;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 6px;
}

.contact-link:hover {
  text-decoration: underline;
}

.contact-link i {
  font-size: 12px;
}

/* Responsive */
@media (max-width: 1200px) {
  .regional-center-details-panel {
    left: 0;
    width: 100%;
    max-width: 400px;
  }
}

@media (max-width: 768px) {
  .regional-center-details-panel {
    top: 60px;
    width: 100%;
    max-width: none;
  }
}

/* Scrollbar */
.panel-content::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.panel-content::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 3px;
}

.panel-content::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}
</style>
