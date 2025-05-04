<template>
  <div class="location-details">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h4 class="mb-0">{{ location.name }}</h4>
      <button @click="$emit('close')" class="btn-close"></button>
    </div>
    
    <div class="mb-2">
      <span v-if="location.category_name" class="badge bg-secondary me-2">
        {{ location.category_name }}
      </span>
      <div class="star-rating">
        {{ '★'.repeat(Math.round(location.rating)) }}{{ '☆'.repeat(5 - Math.round(location.rating)) }}
      </div>
    </div>
    
    <p class="mb-2">
      {{ location.address }}, {{ location.city }}, 
      {{ location.state }} {{ location.zip_code }}
    </p>
    
    <div v-if="location.description" class="mb-3">
      <h6>About</h6>
      <p>{{ location.description }}</p>
    </div>
    
    <div class="mb-3">
      <h6>Contact</h6>
      <p v-if="location.phone" class="mb-1">
        <i class="bi bi-telephone"></i> {{ location.phone }}
      </p>
      <p v-if="location.email" class="mb-1">
        <i class="bi bi-envelope"></i> {{ location.email }}
      </p>
      <p v-if="location.website" class="mb-1">
        <a :href="location.website" target="_blank" class="btn btn-sm btn-outline-primary">
          <i class="bi bi-box-arrow-up-right"></i> Visit Website
        </a>
      </p>
    </div>
    
    <div v-if="location.hours_of_operation" class="mb-3">
      <h6>Hours</h6>
      <pre class="mb-0 small">{{ location.hours_of_operation }}</pre>
    </div>
    
    <div class="d-grid gap-2">
      <a 
        :href="`https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}`" 
        class="btn btn-primary"
        target="_blank"
      >
        <i class="bi bi-map"></i> Get Directions
      </a>
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
  }
}
</script>

<style scoped>
.location-details {
  position: absolute;
  top: 20px;
  right: 20px;
  background: white;
  padding: 15px;
  border-radius: 8px;
  width: 300px;
  max-width: 90%;
  box-shadow: 0 0 15px rgba(0,0,0,0.2);
  z-index: 500; /* High enough to be above the map */
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  overflow-x: hidden;
  word-wrap: break-word;
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
    padding: 12px;
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
    padding: 10px;
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