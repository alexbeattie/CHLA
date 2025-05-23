<template>
  <div class="location-details">
    <!-- Recommendation Badge at top if applicable -->
    <div v-if="location.isHighlyRecommended || location.recommendationLevel > 0" class="recommendation-banner mb-2">
      <span v-if="location.isHighlyRecommended" class="badge bg-success">
        <i class="bi bi-star-fill me-1"></i> Highly Recommended
      </span>
      <span v-else-if="location.recommendationLevel > 0" class="badge bg-info">
        <i class="bi bi-check-circle me-1"></i> Recommended
      </span>
    </div>

    <div class="d-flex justify-content-between align-items-center mb-2">
      <h4 class="mb-0">{{ location.name || location.regional_center || "Unnamed Location" }}</h4>
      <button @click="$emit('close')" class="btn-close"></button>
    </div>
    
    <!-- Categories and Tags -->
    <div class="mb-3">
      <span v-if="location.category_name" class="badge bg-secondary me-1">
        {{ location.category_name }}
      </span>
      <span v-if="location.diagnosisMatch" class="badge bg-danger me-1">
        Matches Your Diagnosis
      </span>
      <span v-if="location.servesUserArea" class="badge bg-secondary me-1">
        Serves Your Area
      </span>
      <div v-if="location.rating" class="star-rating mt-1">
        {{ '★'.repeat(Math.round(location.rating)) }}{{ '☆'.repeat(5 - Math.round(location.rating)) }}
      </div>
      <div v-if="location.distance" class="distance-badge mt-1">
        <span class="badge bg-info">{{ location.distance.toFixed(1) }} miles away</span>
      </div>
    </div>
    
    <!-- Address with copy button -->
    <div class="address-container mb-3">
      <p class="mb-1">
        {{ location.address || "" }}{{ location.city ? ", " + location.city : "" }}{{ location.state ? ", " + location.state : "" }}{{ location.zip_code ? " " + location.zip_code : "" }}
      </p>
      <button class="btn btn-sm btn-outline-secondary copy-btn" @click="copyAddress" title="Copy address">
        <i class="bi bi-clipboard"></i>
      </button>
    </div>
    
    <!-- Provider/Regional Center specific info -->
    <div v-if="isProvider" class="mb-3">
      <div v-if="location.diagnoses_served">
        <h6>Diagnoses Served</h6>
        <p>{{ location.diagnoses_served }}</p>
      </div>
      <div v-if="location.age_groups_served" class="mt-2">
        <h6>Age Groups</h6>
        <p>{{ location.age_groups_served }}</p>
      </div>
      <div v-if="location.coverage_areas" class="mt-2">
        <h6>Coverage Areas</h6>
        <p>{{ location.coverage_areas }}</p>
      </div>
    </div>
    
    <div v-if="isRegionalCenter" class="mb-3">
      <div v-if="location.county_served" class="mt-2">
        <h6>Counties Served</h6>
        <p>{{ location.county_served }}</p>
      </div>
      <div v-if="location.service_area" class="mt-2">
        <h6>Service Area</h6>
        <p>{{ location.service_area }}</p>
      </div>
    </div>
    
    <!-- Description -->
    <div v-if="location.description" class="mb-3">
      <h6>About</h6>
      <p>{{ location.description }}</p>
    </div>
    
    <!-- Contact Information -->
    <div class="mb-3">
      <h6>Contact</h6>
      <p v-if="location.phone || location.telephone" class="mb-1">
        <i class="bi bi-telephone"></i> {{ location.phone || location.telephone }}
        <a :href="`tel:${location.phone || location.telephone}`" class="btn btn-sm btn-outline-secondary ms-2">
          <i class="bi bi-telephone-fill"></i> Call
        </a>
      </p>
      <p v-if="location.email" class="mb-1">
        <i class="bi bi-envelope"></i> {{ location.email }}
        <a :href="`mailto:${location.email}`" class="btn btn-sm btn-outline-secondary ms-2">
          <i class="bi bi-envelope-fill"></i> Email
        </a>
      </p>
      <p v-if="location.website" class="mb-1">
        <a :href="formatWebsite(location.website)" target="_blank" class="btn btn-sm btn-outline-primary">
          <i class="bi bi-box-arrow-up-right"></i> Visit Website
        </a>
      </p>
    </div>
    
    <!-- Hours of Operation -->
    <div v-if="location.hours_of_operation" class="mb-3">
      <h6>Hours</h6>
      <pre class="mb-0 small">{{ location.hours_of_operation }}</pre>
    </div>
    
    <!-- Action Buttons -->
    <div class="d-grid gap-2">
      <a 
        :href="`https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}`" 
        class="btn btn-primary"
        target="_blank"
      >
        <i class="bi bi-map"></i> Get Directions
      </a>
      <button @click="shareLocation" class="btn btn-outline-secondary">
        <i class="bi bi-share"></i> Share
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LocationDetail',
  
  props: {
    location: {
      type: Object,
      required: true
    }
  },

  computed: {
    isProvider() {
      return this.location.diagnoses_served || 
             this.location.age_groups_served || 
             this.location.coverage_areas;
    },
    
    isRegionalCenter() {
      return this.location.regional_center || 
             this.location.county_served || 
             this.location.service_area;
    }
  },
  
  methods: {
    copyAddress() {
      const address = `${this.location.address || ""}, ${this.location.city || ""}, ${this.location.state || ""} ${this.location.zip_code || ""}`;
      navigator.clipboard.writeText(address).then(() => {
        alert("Address copied to clipboard!");
      }).catch(err => {
        console.error('Could not copy address: ', err);
      });
    },
    
    shareLocation() {
      const title = this.location.name || this.location.regional_center || "Location";
      const text = `Check out ${title} at ${this.location.address}, ${this.location.city}`;
      const url = `https://www.google.com/maps/search/?api=1&query=${this.location.latitude},${this.location.longitude}`;
      
      if (navigator.share) {
        navigator.share({
          title: title,
          text: text,
          url: url
        }).then(() => {
          console.log('Location shared successfully');
        }).catch((error) => {
          console.log('Error sharing location:', error);
          this.fallbackShare(text, url);
        });
      } else {
        this.fallbackShare(text, url);
      }
    },
    
    fallbackShare(text, url) {
      // Create a fallback for browsers that don't support navigator.share
      const shareText = `${text} - ${url}`;
      const textarea = document.createElement('textarea');
      textarea.value = shareText;
      textarea.style.position = 'fixed';
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      
      try {
        document.execCommand('copy');
        alert('Location details copied to clipboard. You can now paste and share it!');
      } catch (err) {
        console.error('Fallback share failed:', err);
        alert('Could not share location. Please copy the link manually.');
      }
      
      document.body.removeChild(textarea);
    },
    
    formatWebsite(url) {
      // Ensure URL has http:// or https:// prefix
      if (url && !url.startsWith('http://') && !url.startsWith('https://')) {
        return 'https://' + url;
      }
      return url;
    }
  }
}
</script>

<style scoped>
.location-details {
  position: absolute;
  top: 20px;
  right: 20px;
  background: white;
  padding: 16px;
  border-radius: 8px;
  width: 350px;
  max-width: 90%;
  box-shadow: 0 0 20px rgba(0,0,0,0.2);
  z-index: 500; /* High enough to be above the map */
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  overflow-x: hidden;
  word-wrap: break-word;
}

.recommendation-banner {
  position: relative;
  margin-top: -16px;
  margin-left: -16px;
  margin-right: -16px;
  padding: 6px 16px;
  background-color: #f8f9fa;
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
  text-align: center;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.address-container {
  position: relative;
  display: flex;
  align-items: flex-start;
}

.address-container p {
  flex: 1;
  margin-right: 10px;
}

.copy-btn {
  flex-shrink: 0;
  padding: 2px 6px;
  font-size: 0.8rem;
}

.star-rating {
  color: gold;
  letter-spacing: -1px; /* Make stars closer together */
  font-size: 1.1rem;
}

pre {
  white-space: pre-wrap;
  font-size: 0.9rem;
  overflow-x: hidden;
  max-width: 100%;
  background-color: #f8f9fa;
  padding: 8px;
  border-radius: 4px;
}

h6 {
  font-weight: 600;
  color: #495057;
  margin-bottom: 6px;
  font-size: 0.95rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

/* Fix for buttons */
.btn-close {
  opacity: 0.7;
  transition: opacity 0.2s;
}

.btn-close:hover {
  opacity: 1;
}

.btn-primary {
  transition: all 0.2s;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.btn-primary:active {
  transform: translateY(0);
}

/* Mobile layouts */
@media (max-width: 768px) {
  .location-details {
    top: 10px;
    right: 10px;
    max-width: calc(100% - 20px);
    padding: 14px;
    max-height: calc(50vh - 20px); /* Limit height on mobile */
  }
}

@media (max-width: 576px) {
  .location-details {
    max-width: calc(100% - 20px);
    width: calc(100% - 20px);
    right: 10px;
    left: 10px;
    transform: none; /* Remove centering transform for better performance */
    border-radius: 6px;
    padding: 12px;
  }
  
  h4 {
    font-size: 1.3rem;
  }
  
  h6 {
    font-size: 1rem;
  }
  
  /* More compact on small screens */
  .mb-2 {
    margin-bottom: 0.4rem !important;
  }
  
  .mb-3 {
    margin-bottom: 0.6rem !important;
  }
  
  .recommendation-banner {
    margin-top: -12px;
    margin-left: -12px;
    margin-right: -12px;
    padding: 5px 12px;
  }
}

/* Extra fix for iOS Safari */
@supports (-webkit-touch-callout: none) {
  .location-details {
    /* Prevent rubber-band scrolling effects */
    overscroll-behavior-y: contain;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
