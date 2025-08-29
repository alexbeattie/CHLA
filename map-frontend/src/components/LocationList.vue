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

        <!-- Provider Type Badge -->
        <div v-if="location.type" class="mb-2">
          <span class="badge bg-primary me-1">{{ location.type }}</span>
        </div>

        <!-- Location Address (cleaned) -->
        <p class="mb-2 small text-muted">
          {{ formatAddress(location) }}
        </p>

        <!-- Description -->
        <div v-if="location.description" class="mb-2">
          <div
            class="small"
            style="
              background: #fff3cd;
              padding: 8px 12px;
              border-radius: 6px;
              border-left: 3px solid #ffc107;
              margin: 0;
            "
          >
            <div
              style="
                color: #856404;
                font-size: 11px;
                font-weight: 600;
                margin-bottom: 4px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
              "
            >
              📍 Service Areas
            </div>
            <div style="color: #856404; font-size: 13px; line-height: 1.4">
              {{ formatDescription(location.description) }}
            </div>
          </div>
        </div>

        <!-- Insurance Information -->
        <div
          v-if="location.insurance_accepted && location.insurance_accepted !== '[]'"
          class="mb-2"
        >
          <div
            class="small"
            style="
              background: #d4edda;
              padding: 8px 12px;
              border-radius: 6px;
              border-left: 3px solid #28a745;
              color: #155724;
            "
          >
            <div
              style="
                font-size: 11px;
                font-weight: 600;
                margin-bottom: 4px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
              "
            >
              🏥 Insurance Accepted
            </div>
            <div style="font-size: 13px; line-height: 1.4">
              {{ formatInsurance(location.insurance_accepted) }}
            </div>
          </div>
        </div>

        <!-- Languages -->
        <div
          v-if="location.languages_spoken && location.languages_spoken !== '[]'"
          class="mb-2"
        >
          <div
            class="small"
            style="
              background: #e2e3e5;
              padding: 8px 12px;
              border-radius: 6px;
              border-left: 3px solid #6c757d;
              color: #383d41;
            "
          >
            <div
              style="
                font-size: 11px;
                font-weight: 600;
                margin-bottom: 4px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
              "
            >
              🗣️ Languages
            </div>
            <div style="font-size: 13px; line-height: 1.4">
              {{ formatLanguages(location.languages_spoken) }}
            </div>
          </div>
        </div>

        <!-- Website -->
        <div v-if="location.website" class="mb-2">
          <a
            :href="
              location.website.startsWith('http')
                ? location.website
                : 'https://' + location.website
            "
            target="_blank"
            class="small text-decoration-none"
            style="
              background: #f8f9fa;
              padding: 6px 8px;
              border-radius: 4px;
              border-left: 3px solid #6f42c1;
              color: #6f42c1;
              display: inline-block;
            "
          >
            🌐 Visit Website
          </a>
        </div>

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
    formatAddress(location) {
      const parts = [];

      // Handle address field which might be JSON string or object
      let addressData = null;
      if (location.address) {
        try {
          // Try to parse if it's a JSON string
          if (typeof location.address === "string") {
            addressData = JSON.parse(location.address);
          } else {
            addressData = location.address;
          }
        } catch (e) {
          // If parsing fails, treat as plain text
          addressData = null;
        }
      }

      // Build address from parsed data or fallback to individual fields
      if (addressData && typeof addressData === "object") {
        // Use parsed JSON data
        if (addressData.street) parts.push(addressData.street);
        if (addressData.city) parts.push(addressData.city);
        if (addressData.state) parts.push(addressData.state);
        if (addressData.zip) parts.push(addressData.zip);
      } else {
        // Fallback to individual fields (for backward compatibility)
        if (location.address && !addressData) {
          parts.push(String(location.address).replace(/,?\s*United States\b/i, ""));
        }
        if (location.city) parts.push(location.city);
        if (location.state) parts.push(location.state);
        if (location.zip_code) parts.push(location.zip_code);
      }

      // Clean up and filter out empty parts
      return parts.filter(Boolean).join(", ");
    },
    formatDescription(description) {
      if (!description) return "";

      // Clean up the description text
      let cleanDescription = description;

      // Clean up formatting without removing directional words
      cleanDescription = cleanDescription
        .replace(/,/g, ", ") // Add space after commas
        .replace(/\s+/g, " ") // Normalize whitespace
        .trim();

      // Convert to proper title case (capitalize first letter of each word)
      cleanDescription = cleanDescription
        .toLowerCase()
        .replace(/\b\w/g, (l) => l.toUpperCase());

      return cleanDescription;
    },

    formatInsurance(insurance) {
      if (!insurance) return "";

      try {
        // Try to parse as JSON first
        const parsed = JSON.parse(insurance);
        if (Array.isArray(parsed)) {
          return parsed.join(", ");
        } else if (typeof parsed === "object") {
          return Object.values(parsed).join(", ");
        }
      } catch (e) {
        // If not JSON, treat as string
      }

      // Clean up insurance text
      let cleanInsurance = insurance;

      // Remove all brackets, braces and quotes
      cleanInsurance = cleanInsurance
        .replace(/[\[\]{}]/g, "") // Remove all brackets and braces
        .replace(/['"]/g, "") // Remove all quotes
        .replace(/\s*,\s*/g, ", ") // Normalize comma spacing
        .replace(/\s+/g, " ") // Normalize whitespace
        .trim();

      return cleanInsurance;
    },
    formatLanguages(languages) {
      if (!languages) return "";

      try {
        const parsed = JSON.parse(languages);
        if (Array.isArray(parsed)) {
          return parsed.join(", ");
        } else if (typeof parsed === "object") {
          return Object.values(parsed).join(", ");
        }
      } catch (e) {
        // If not JSON, treat as string
      }

      // Clean up languages text
      let cleanLanguages = languages;

      // Remove all brackets, braces and quotes
      cleanLanguages = cleanLanguages
        .replace(/[\[\]{}]/g, "") // Remove all brackets and braces
        .replace(/['"]/g, "") // Remove all quotes
        .replace(/\s*,\s*/g, ", ") // Normalize comma spacing
        .replace(/\s+/g, " ") // Normalize whitespace
        .trim();

      return cleanLanguages;
    },
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
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s ease-out;
  border: 1px solid #e9ecef;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  user-select: none;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
}

.location-item:hover {
  background-color: #f8f9fa;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-1px);
}

.location-item:active {
  background-color: #e9ecef;
  transform: translateY(0);
}

.location-item.active {
  background-color: #007bff;
  color: white;
  border-color: #0056b3;
  box-shadow: 0 4px 16px rgba(0, 123, 255, 0.3);
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

/* Enhanced badge styling */
.badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* If both recommended and active */
.location-item.active.recommended,
.location-item.active.highly-recommended {
  border-left-color: white;
}

/* Improved typography */
.location-item h5 {
  font-size: 1.1rem;
  margin-bottom: 8px;
  line-height: 1.4;
  word-break: break-word;
  font-weight: 700;
  color: #2c3e50;
}

.location-item p {
  margin-bottom: 8px;
  line-height: 1.5;
  word-break: break-word;
  opacity: 0.9;
  color: #495057;
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
