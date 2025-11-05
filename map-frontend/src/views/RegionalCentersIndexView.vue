<template>
  <div class="rc-index-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="container">
        <h1 class="page-title">Los Angeles County Regional Centers</h1>
        <p class="page-subtitle">
          Find your Regional Center and explore ABA therapy providers in your service area
        </p>
      </div>
    </section>

    <!-- Quick Finder -->
    <section class="quick-finder-section">
      <div class="container">
        <div class="finder-card">
          <h2>Find Your Regional Center</h2>
          <p>Enter your ZIP code to discover which Regional Center serves your area</p>
          <div class="zip-finder">
            <input 
              type="text" 
              v-model="zipCode" 
              placeholder="Enter ZIP code"
              @keyup.enter="findRegionalCenter"
              maxlength="5"
              class="zip-input"
              :disabled="detectingLocation"
            />
            <button @click="findRegionalCenter" class="btn btn-primary" :disabled="loading || detectingLocation">
              <i class="bi bi-search me-2"></i>
              Find My RC
            </button>
            <button @click="useMyLocation" class="btn btn-outline-primary" :disabled="loading || detectingLocation">
              <i :class="detectingLocation ? 'bi bi-arrow-clockwise spin' : 'bi bi-geo-alt-fill'" class="me-2"></i>
              {{ detectingLocation ? 'Detecting...' : 'Use My Location' }}
            </button>
          </div>
          <div v-if="foundRC" class="found-result">
            <i class="bi bi-check-circle-fill"></i>
            <span>Your ZIP code is served by <strong>{{ foundRC.shortName }}</strong></span>
            <router-link :to="`/regional-centers/${foundRC.slug}`" class="btn btn-sm btn-outline-primary ms-3">
              View Details →
            </router-link>
          </div>
          <div v-if="notFound" class="not-found-result">
            <i class="bi bi-x-circle-fill"></i>
            <span>ZIP code not found in LA County. Please verify and try again.</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Main Content -->
    <section class="content-section">
      <div class="container">
        
        <!-- What are Regional Centers -->
        <div class="content-block intro-block">
          <h2>What are Regional Centers?</h2>
          <p class="lead">
            Regional Centers are nonprofit agencies funded by the State of California to provide services and support to individuals with developmental disabilities and their families.
          </p>
          <div class="info-grid">
            <div class="info-card">
              <i class="bi bi-geo-alt-fill"></i>
              <h3>Geographic Assignment</h3>
              <p>Your Regional Center is determined by your ZIP code, not your choice. Each area is served by one specific Regional Center.</p>
            </div>
            <div class="info-card">
              <i class="bi bi-currency-dollar"></i>
              <h3>Free Services</h3>
              <p>Services through Regional Centers are free for eligible individuals with developmental disabilities.</p>
            </div>
            <div class="info-card">
              <i class="bi bi-people-fill"></i>
              <h3>Comprehensive Support</h3>
              <p>Regional Centers coordinate access to ABA therapy, respite care, support services, and more.</p>
            </div>
          </div>
        </div>

        <!-- 7 Regional Centers -->
        <div class="content-block">
          <h2>LA County's 7 Regional Centers</h2>
          <p>Los Angeles County is served by 7 Regional Centers, each covering specific geographic areas:</p>
          
          <div class="rc-grid">
            <router-link 
              v-for="rc in regionalCenters" 
              :key="rc.slug"
              :to="`/regional-centers/${rc.slug}`"
              class="rc-card"
              :style="{ borderLeftColor: rc.color }"
            >
              <div class="rc-header">
                <h3>{{ rc.name }}</h3>
                <span class="view-link">View Details →</span>
              </div>
              <p class="rc-tagline">{{ rc.tagline }}</p>
              <div class="rc-stats">
                <div class="stat">
                  <i class="bi bi-hospital"></i>
                  <span>{{ rc.providerCount }}+ Providers</span>
                </div>
                <div class="stat">
                  <i class="bi bi-geo-alt"></i>
                  <span>{{ rc.citiesCount }} Cities</span>
                </div>
                <div class="stat">
                  <i class="bi bi-people"></i>
                  <span>{{ rc.population }} Residents</span>
                </div>
              </div>
              <div class="rc-cities">
                <strong>Major Cities:</strong> {{ rc.cities.slice(0, 5).join(', ') }}
                <span v-if="rc.cities.length > 5"> +{{ rc.cities.length - 5 }} more</span>
              </div>
            </router-link>
          </div>
        </div>

        <!-- Map Visualization -->
        <div class="content-block highlight-block">
          <h2>Interactive Service Area Map</h2>
          <p>See all provider locations and Regional Center boundaries on our interactive map.</p>
          <router-link to="/" class="btn btn-primary btn-lg">
            <i class="bi bi-map me-2"></i>
            Explore the Map
          </router-link>
        </div>

        <!-- How to Get Services -->
        <div class="content-block">
          <h2>How to Access Regional Center Services</h2>
          <div class="steps">
            <div class="step">
              <div class="step-number">1</div>
              <div class="step-content">
                <h3>Determine Your Regional Center</h3>
                <p>Use the ZIP code finder above or check the list to find your Regional Center.</p>
              </div>
            </div>
            <div class="step">
              <div class="step-number">2</div>
              <div class="step-content">
                <h3>Contact Your Regional Center</h3>
                <p>Call your Regional Center to schedule an intake appointment and discuss eligibility.</p>
              </div>
            </div>
            <div class="step">
              <div class="step-number">3</div>
              <div class="step-content">
                <h3>Eligibility Assessment</h3>
                <p>Complete an assessment to determine if you qualify for services (developmental disability must begin before age 18).</p>
              </div>
            </div>
            <div class="step">
              <div class="step-number">4</div>
              <div class="step-content">
                <h3>Develop Service Plan</h3>
                <p>Work with your service coordinator to create an Individualized Program Plan (IPP).</p>
              </div>
            </div>
            <div class="step">
              <div class="step-number">5</div>
              <div class="step-content">
                <h3>Choose Providers</h3>
                <p>Select providers from our map that accept your Regional Center's funding.</p>
              </div>
            </div>
          </div>
        </div>

        <!-- CTA Section -->
        <div class="cta-section">
          <h2>Ready to Find ABA Therapy Providers?</h2>
          <p>Search our map to find providers in your Regional Center's service area.</p>
          <div class="cta-buttons">
            <router-link to="/" class="btn btn-primary btn-lg">
              <i class="bi bi-map me-2"></i>
              Search Providers
            </router-link>
            <router-link to="/faq" class="btn btn-outline-light btn-lg">
              <i class="bi bi-question-circle me-2"></i>
              Read FAQ
            </router-link>
          </div>
        </div>

      </div>
    </section>
  </div>
</template>

<script>
import { ref, computed } from 'vue';
import { getAllRegionalCenters } from '@/data/regionalCenters';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://api.kinddhelp.com';

export default {
  name: 'RegionalCentersIndexView',
  
  setup() {
    const zipCode = ref('');
    const foundRC = ref(null);
    const notFound = ref(false);
    const loading = ref(false);
    const detectingLocation = ref(false);
    
    const regionalCenters = computed(() => getAllRegionalCenters());
    
    const findRegionalCenter = async () => {
      foundRC.value = null;
      notFound.value = false;
      loading.value = true;
      
      if (!zipCode.value || zipCode.value.length !== 5) {
        notFound.value = true;
        loading.value = false;
        return;
      }
      
      try {
        // Query API for the regional center by ZIP code
        const response = await fetch(`${API_BASE_URL}/api/regional-centers/by_zip_code/?zip_code=${zipCode.value}`);
        
        if (response.ok) {
          const rcData = await response.json();
          console.log(`✅ API returned RC for ZIP ${zipCode.value}:`, rcData.regional_center);
          
          // Find matching RC in our local data for display
          const localRC = getAllRegionalCenters().find(rc => 
            rc.name === rcData.regional_center
          );
          
          if (localRC) {
            foundRC.value = localRC;
          } else {
            // If not found in local data, create a basic object
            foundRC.value = {
              name: rcData.regional_center,
              shortName: rcData.regional_center,
              slug: rcData.regional_center.toLowerCase().replace(/\s+/g, '-')
            };
          }
        } else {
          console.warn(`⚠️ No RC found for ZIP ${zipCode.value}`);
          notFound.value = true;
        }
      } catch (error) {
        console.error('Error finding regional center:', error);
        notFound.value = true;
      } finally {
        loading.value = false;
      }
    };
    
    const useMyLocation = async () => {
      foundRC.value = null;
      notFound.value = false;
      detectingLocation.value = true;
      
      if (!navigator.geolocation) {
        alert('Geolocation is not supported by your browser');
        detectingLocation.value = false;
        return;
      }
      
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          try {
            const { latitude, longitude } = position.coords;
            console.log(`📍 Location detected: ${latitude}, ${longitude}`);
            
            // Reverse geocode to get ZIP code using Mapbox
            const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN || 'pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiY200ZHcwaTc0MDJjcjJscTE3emxhM2xvZCJ9.VnoxlGaFkGT7qKSgJLU_mQ';
            const geocodeUrl = `https://api.mapbox.com/geocoding/v5/mapbox.places/${longitude},${latitude}.json?access_token=${mapboxToken}&types=postcode`;
            
            const response = await fetch(geocodeUrl);
            const data = await response.json();
            
            if (data.features && data.features.length > 0) {
              const zip = data.features[0].text;
              console.log(`✅ ZIP code detected: ${zip}`);
              zipCode.value = zip;
              
              // Automatically find the regional center
              await findRegionalCenter();
            } else {
              alert('Could not determine your ZIP code. Please enter it manually.');
              notFound.value = true;
            }
          } catch (error) {
            console.error('Error reverse geocoding:', error);
            alert('Error detecting your location. Please enter your ZIP code manually.');
            notFound.value = true;
          } finally {
            detectingLocation.value = false;
          }
        },
        (error) => {
          console.error('Geolocation error:', error);
          alert('Unable to detect your location. Please enter your ZIP code manually.');
          detectingLocation.value = false;
          notFound.value = true;
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0
        }
      );
    };
    
    return {
      zipCode,
      foundRC,
      notFound,
      loading,
      detectingLocation,
      regionalCenters,
      findRegionalCenter,
      useMyLocation
    };
  }
};
</script>

<style scoped>
.rc-index-page {
  min-height: 100vh;
  background-color: #f8f9fa;
}

/* Hero Section */
.hero-section {
  background: linear-gradient(135deg, #004877 0%, #003355 100%);
  color: white;
  padding: 4rem 0;
  text-align: center;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.page-subtitle {
  font-size: 1.25rem;
  opacity: 0.9;
  max-width: 800px;
  margin: 0 auto;
}

/* Quick Finder */
.quick-finder-section {
  margin-top: -2rem;
  padding-bottom: 2rem;
}

.finder-card {
  background: white;
  padding: 2.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.finder-card h2 {
  font-size: 1.75rem;
  color: #004877;
  margin-bottom: 0.5rem;
}

.finder-card p {
  color: #6c757d;
  margin-bottom: 1.5rem;
}

.zip-finder {
  display: flex;
  gap: 1rem;
  max-width: 700px;
  margin: 0 auto;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .zip-finder {
    flex-direction: column;
  }
}

.zip-input {
  flex: 1;
  padding: 0.75rem 1rem;
  font-size: 1.1rem;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  transition: border-color 0.3s;
}

.zip-input:focus {
  outline: none;
  border-color: #004877;
}

.zip-input:disabled {
  background-color: #f8f9fa;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.found-result, .not-found-result {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: 8px;
  margin-top: 1rem;
}

.found-result {
  background: #d4edda;
  border: 1px solid #c3e6cb;
  color: #155724;
}

.found-result i {
  font-size: 1.5rem;
  color: #28a745;
}

.not-found-result {
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  color: #721c24;
}

.not-found-result i {
  font-size: 1.5rem;
  color: #dc3545;
}

/* Content Section */
.content-section {
  padding: 2rem 0 3rem 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.content-block {
  background: white;
  padding: 2.5rem;
  border-radius: 12px;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.content-block h2 {
  font-size: 2rem;
  font-weight: 700;
  color: #004877;
  margin-bottom: 1.5rem;
}

.lead {
  font-size: 1.25rem;
  font-weight: 500;
  color: #495057;
  margin-bottom: 2rem;
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-top: 2rem;
}

.info-card {
  text-align: center;
  padding: 1.5rem;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.info-card:hover {
  border-color: #FFC923;
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.info-card i {
  font-size: 3rem;
  color: #004877;
  margin-bottom: 1rem;
}

.info-card h3 {
  font-size: 1.25rem;
  color: #004877;
  margin-bottom: 0.5rem;
}

.info-card p {
  font-size: 0.95rem;
  color: #6c757d;
  margin: 0;
}

/* RC Grid */
.rc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
  margin-top: 2rem;
}

.rc-card {
  background: white;
  padding: 1.75rem;
  border-radius: 8px;
  border-left: 5px solid;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.rc-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.rc-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.rc-header h3 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #004877;
  margin: 0;
  flex: 1;
}

.view-link {
  color: #0D9DDB;
  font-weight: 600;
  font-size: 0.9rem;
  white-space: nowrap;
  transition: transform 0.3s ease;
}

.rc-card:hover .view-link {
  transform: translateX(5px);
}

.rc-tagline {
  color: #6c757d;
  font-size: 0.95rem;
  margin-bottom: 1rem;
}

.rc-stats {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.rc-stats .stat {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #495057;
}

.rc-stats .stat i {
  color: #004877;
}

.rc-cities {
  font-size: 0.85rem;
  color: #6c757d;
  line-height: 1.5;
  padding-top: 1rem;
  border-top: 1px solid #e9ecef;
}

.rc-cities strong {
  color: #004877;
}

/* Steps */
.steps {
  margin-top: 2rem;
}

.step {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 2rem;
  align-items: flex-start;
}

.step-number {
  flex-shrink: 0;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #004877, #0D9DDB);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
}

.step-content h3 {
  font-size: 1.25rem;
  color: #004877;
  margin-top: 0;
  margin-bottom: 0.5rem;
}

.step-content p {
  color: #495057;
  margin: 0;
}

/* Highlight Block */
.highlight-block {
  background: linear-gradient(135deg, #eff6ff 0%, #e0f2fe 100%);
  border-left: 6px solid #0D9DDB;
  text-align: center;
}

/* CTA Section */
.cta-section {
  background: linear-gradient(135deg, #004877 0%, #0D9DDB 100%);
  color: white;
  padding: 3rem;
  border-radius: 12px;
  text-align: center;
}

.cta-section h2 {
  font-size: 2rem;
  color: white;
  margin-bottom: 1rem;
}

.cta-section p {
  font-size: 1.25rem;
  margin-bottom: 2rem;
  opacity: 0.9;
}

.cta-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-primary {
  background-color: #FFC923;
  border-color: #FFC923;
  color: #004877;
  font-weight: 600;
  padding: 0.75rem 2rem;
  font-size: 1.1rem;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
  border-radius: 8px;
}

.btn-primary:hover {
  background-color: #ffb700;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 201, 35, 0.4);
}

.btn-outline-light {
  border: 2px solid white;
  background: transparent;
  color: white;
  font-weight: 600;
  padding: 0.75rem 2rem;
  font-size: 1.1rem;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
  border-radius: 8px;
}

.btn-outline-light:hover {
  background-color: white;
  color: #004877;
  transform: translateY(-2px);
}

.btn-outline-primary {
  border: 2px solid #004877;
  background: white;
  color: #004877;
  padding: 0.375rem 1rem;
  font-size: 0.9rem;
  text-decoration: none;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.btn-outline-primary:hover {
  background-color: #004877;
  color: white;
}

/* Responsive */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }

  .page-subtitle {
    font-size: 1.1rem;
  }

  .zip-finder {
    flex-direction: column;
  }

  .rc-grid {
    grid-template-columns: 1fr;
  }

  .step {
    gap: 1rem;
  }

  .step-number {
    width: 40px;
    height: 40px;
    font-size: 1.25rem;
  }

  .cta-buttons {
    flex-direction: column;
  }
}
</style>

