<template>
  <div>
    <h5 class="d-flex justify-content-between align-items-center">
      Results
      <span class="badge bg-primary">{{ locations.length }}</span>
    </h5>

    <div v-if="loading" class="text-center my-4">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <p>Loading locations...</p>
    </div>

    <div v-else-if="error" class="alert alert-danger">
      {{ error }}
    </div>

    <div v-else-if="locations.length === 0" class="text-center my-4">
      <div class="alert alert-warning">
        <i class="bi bi-search"></i>
        <strong>No results found</strong>
        <p class="mb-2 small">Try adjusting your search criteria:</p>
        <ul class="list-unstyled small text-start">
          <li>• Increase the distance radius</li>
          <li>• Remove some filters</li>
          <li>• Try a different location or zip code</li>
          <li>• Check if you're searching for the right services</li>
        </ul>
      </div>
    </div>

    <div v-else class="location-list">
      <div
        v-for="location in locations"
        :key="location.id"
        class="location-item"
        :class="{
          recommended: location.recommendationLevel > 0,
          'highly-recommended': location.isHighlyRecommended,
        }"
        @click="centerOnLocation(location)"
      >
        <!-- Recommendation Badges -->
        <div
          v-if="
            location.isHighlyRecommended ||
            location.recommendationLevel > 0 ||
            location.diagnosisMatch ||
            location.servesUserArea
          "
          class="badges-container mb-1"
        >
          <span v-if="location.isHighlyRecommended" class="badge bg-success me-1"
            >Highly Recommended</span
          >
          <span v-else-if="location.recommendationLevel > 0" class="badge bg-info me-1"
            >Recommended</span
          >
          <span v-if="location.diagnosisMatch" class="badge bg-danger me-1"
            >Matches Diagnosis</span
          >
          <span v-if="location.servesUserArea" class="badge bg-secondary me-1"
            >Serves Your Area</span
          >
        </div>

        <!-- Location Name -->
        <h5 class="mb-1">
          {{ location.name || location.regional_center || "Unnamed Location" }}
        </h5>

        <!-- Location Address -->
        <p class="mb-1 small">
          {{ location.address || "" }}{{ location.city ? ", " + location.city : "" }}
        </p>

        <!-- Ratings (for locations only) -->
        <div v-if="location.rating" class="d-flex justify-content-between">
          <small class="star-rating">
            {{ "★".repeat(Math.round(location.rating))
            }}{{ "☆".repeat(5 - Math.round(location.rating)) }}
          </small>
        </div>

        <!-- Distance Badge -->
        <div v-if="location.distance" class="mt-1">
          <span class="badge bg-info text-white"
            >{{ location.distance.toFixed(1) }} miles</span
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "LocationList",

  props: {
    locations: {
      type: Array,
      required: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    error: {
      type: String,
      default: null,
    },
  },

  methods: {
    centerOnLocation(location) {
      // Emit an event to center the map on this location
      this.$emit("center-on-location", location);
    },
  },
};
</script>

<style scoped>
.location-list {
  overflow-y: auto;
  padding-right: 5px;
  margin-bottom: 15px;
  position: relative;
  height: 100%;
  max-height: calc(100vh - 350px); /* Dynamic height based on viewport */
  will-change: transform; /* Performance optimization for scrolling */
  -webkit-overflow-scrolling: touch; /* Smooth scrolling on iOS */
  overscroll-behavior: contain; /* Prevent page overscroll */
}

.location-item {
  cursor: pointer;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 10px;
  transition: all 0.2s ease-out;
  border: 1px solid #eee;
  background-color: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: relative;
  user-select: none; /* Prevent text selection when tapping */
  touch-action: manipulation; /* Optimize for touch */
  -webkit-tap-highlight-color: transparent; /* Remove tap highlight on mobile */
}

.location-item:hover {
  background-color: #f8f9fa;
  transform: translateY(-1px);
  box-shadow: 0 3px 5px rgba(0, 0, 0, 0.08);
}

.location-item:active {
  transform: translateY(0);
  background-color: #f1f3f5;
}

.location-item.active {
  background-color: #007bff;
  color: white;
  border-color: #0069d9;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

/* Recommended item styling */
.location-item.recommended {
  border-left: 3px solid #17a2b8;
  padding-left: 10px;
  background-color: #f8f9fa;
}

/* Highly recommended item styling */
.location-item.highly-recommended {
  border-left: 4px solid #28a745;
  padding-left: 9px;
  background-color: #f0fff4;
  box-shadow: 0 2px 8px rgba(40, 167, 69, 0.2);
}

/* Badges container */
.badges-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* If both recommended and active */
.location-item.active.recommended,
.location-item.active.highly-recommended {
  border-left-color: white;
}

.location-item h5 {
  font-size: 1rem;
  margin-bottom: 4px;
  line-height: 1.3;
  word-break: break-word;
  font-weight: 600;
}

.location-item p {
  margin-bottom: 4px;
  line-height: 1.4;
  word-break: break-word;
  opacity: 0.85;
}

.star-rating {
  color: gold;
  letter-spacing: -1px; /* Make stars closer together */
}

/* Make the scrollbar more subtle and modern */
.location-list::-webkit-scrollbar {
  width: 6px;
}

.location-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}

.location-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 10px;
}

.location-list::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* Improved mobile layout */
@media (max-width: 768px) {
  .location-list {
    max-height: 30vh;
    padding-right: 3px; /* Less padding on mobile */
    margin-bottom: 10px; /* Less margin on mobile */
  }

  .location-item {
    padding: 10px;
    margin-bottom: 8px;
    border-radius: 5px;
  }
}

/* Small screens */
@media (max-width: 576px) {
  .location-list {
    max-height: 25vh;
  }

  /* Slightly larger targets for touch on small screens */
  .location-item {
    padding: 10px;
    margin-bottom: 6px;
  }

  .location-item h5 {
    font-size: 0.95rem;
  }

  .location-item p {
    font-size: 0.85rem;
    margin-bottom: 3px;
  }
}
</style>
