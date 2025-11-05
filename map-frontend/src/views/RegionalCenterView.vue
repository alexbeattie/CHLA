<template>
  <div class="regional-center-page">
    <!-- Hero Section -->
    <section class="hero-section" :style="{ background: `linear-gradient(135deg, ${rcData.color} 0%, ${rcData.colorDark} 100%)` }">
      <div class="container">
        <div class="breadcrumb">
          <router-link to="/">Home</router-link>
          <span class="separator">›</span>
          <router-link to="/regional-centers">Regional Centers</router-link>
          <span class="separator">›</span>
          <span>{{ rcData.name }}</span>
        </div>
        <h1 class="page-title">{{ rcData.name }}</h1>
        <p class="page-subtitle">{{ rcData.tagline }}</p>
        <div class="hero-stats">
          <div class="stat">
            <i class="bi bi-hospital"></i>
            <span>{{ rcData.providerCount }}+ Providers</span>
          </div>
          <div class="stat">
            <i class="bi bi-geo-alt"></i>
            <span>{{ rcData.citiesCount }} Cities Served</span>
          </div>
          <div class="stat">
            <i class="bi bi-people"></i>
            <span>{{ rcData.population }} Residents</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Quick Search -->
    <section class="quick-search-section">
      <div class="container">
        <div class="search-card">
          <h2>Find Providers in {{ rcData.shortName }}</h2>
          <p>Enter your ZIP code to see ABA therapy providers near you</p>
          <div class="search-form">
            <input 
              type="text" 
              v-model="zipCode" 
              placeholder="Enter ZIP code (e.g., 91789)"
              @keyup.enter="searchProviders"
              maxlength="5"
              class="zip-input"
            />
            <button @click="searchProviders" class="btn btn-primary">
              <i class="bi bi-search me-2"></i>
              Search Providers
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Main Content -->
    <section class="content-section">
      <div class="container">
        
        <!-- About This Regional Center -->
        <div class="content-block">
          <h2>About {{ rcData.name }}</h2>
          <div class="rc-info-grid">
            <div class="rc-description">
              <p v-html="rcData.description"></p>
              
              <div class="contact-info">
                <h3>Contact Information</h3>
                <div class="contact-item">
                  <i class="bi bi-telephone-fill"></i>
                  <a :href="`tel:${rcData.phone}`">{{ rcData.phoneFormatted }}</a>
                </div>
                <div class="contact-item">
                  <i class="bi bi-globe"></i>
                  <a :href="rcData.website" target="_blank" rel="noopener noreferrer">Visit Website</a>
                </div>
                <div class="contact-item">
                  <i class="bi bi-geo-alt-fill"></i>
                  <span>{{ rcData.address }}</span>
                </div>
              </div>
            </div>

            <div class="rc-highlights">
              <h3>Service Highlights</h3>
              <ul class="highlights-list">
                <li v-for="highlight in rcData.highlights" :key="highlight">
                  <i class="bi bi-check-circle-fill"></i>
                  <span>{{ highlight }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Cities & ZIP Codes Served -->
        <div class="content-block">
          <h2>Cities & Communities Served</h2>
          <p>{{ rcData.name }} serves the following areas in Los Angeles County:</p>
          <div class="cities-grid">
            <div v-for="city in displayedCities" :key="city" class="city-chip">
              <i class="bi bi-geo-alt"></i>
              <span>{{ city }}</span>
            </div>
          </div>
          <div v-if="rcData.cities.length > 15" class="text-center mt-3">
            <button @click="toggleCities" class="btn btn-outline-secondary btn-sm">
              <i :class="showAllCities ? 'bi bi-chevron-up' : 'bi bi-chevron-down'" class="me-1"></i>
              {{ showAllCities ? 'Show Less' : `Show All ${rcData.cities.length} Cities` }}
            </button>
          </div>
          
          <div class="zip-section">
            <h3>ZIP Codes in {{ rcData.shortName }}</h3>
            <p class="text-muted">The following ZIP codes are served by {{ rcData.name }}:</p>
            <div class="zip-codes">
              <span v-for="zip in displayedZips" :key="zip" class="zip-badge">{{ zip }}</span>
            </div>
            <div v-if="rcData.zipCodes.length > 30" class="text-center mt-3">
              <button @click="toggleZips" class="btn btn-outline-secondary btn-sm">
                <i :class="showAllZips ? 'bi bi-chevron-up' : 'bi bi-chevron-down'" class="me-1"></i>
                {{ showAllZips ? 'Show Less' : `Show All ${rcData.zipCodes.length} ZIP Codes` }}
              </button>
            </div>
          </div>
        </div>

        <!-- Services Available -->
        <div class="content-block highlight-block">
          <h2>Services Available Through {{ rcData.shortName }}</h2>
          <p>Individuals served by {{ rcData.name }} may be eligible for:</p>
          <div class="services-grid">
            <div class="service-card">
              <div class="service-icon">
                <i class="bi bi-clipboard2-pulse"></i>
              </div>
              <h3>ABA Therapy</h3>
              <p>Applied Behavior Analysis for autism and developmental disabilities</p>
            </div>
            <div class="service-card">
              <div class="service-icon">
                <i class="bi bi-chat-dots"></i>
              </div>
              <h3>Speech Therapy</h3>
              <p>Communication and language development services</p>
            </div>
            <div class="service-card">
              <div class="service-icon">
                <i class="bi bi-activity"></i>
              </div>
              <h3>Occupational Therapy</h3>
              <p>Daily living skills and sensory integration</p>
            </div>
            <div class="service-card">
              <div class="service-icon">
                <i class="bi bi-person-walking"></i>
              </div>
              <h3>Physical Therapy</h3>
              <p>Mobility and motor skill development</p>
            </div>
            <div class="service-card">
              <div class="service-icon">
                <i class="bi bi-people"></i>
              </div>
              <h3>Social Skills Groups</h3>
              <p>Peer interaction and social development</p>
            </div>
            <div class="service-card">
              <div class="service-icon">
                <i class="bi bi-house-heart"></i>
              </div>
              <h3>Respite Care</h3>
              <p>Temporary relief for families and caregivers</p>
            </div>
          </div>
        </div>

        <!-- How to Get Services -->
        <div class="content-block">
          <h2>How to Access Services</h2>
          <div class="steps">
            <div class="step">
              <div class="step-number">1</div>
              <div class="step-content">
                <h3>Contact {{ rcData.shortName }}</h3>
                <p>Call {{ rcData.phoneFormatted }} or visit their website to schedule an intake appointment.</p>
              </div>
            </div>
            <div class="step">
              <div class="step-number">2</div>
              <div class="step-content">
                <h3>Intake Assessment</h3>
                <p>Meet with a service coordinator to discuss your child's needs and determine eligibility.</p>
              </div>
            </div>
            <div class="step">
              <div class="step-number">3</div>
              <div class="step-content">
                <h3>Develop IPP</h3>
                <p>Work with your team to create an Individualized Program Plan outlining services and supports.</p>
              </div>
            </div>
            <div class="step">
              <div class="step-number">4</div>
              <div class="step-content">
                <h3>Choose Providers</h3>
                <p>Select providers from our map that accept {{ rcData.shortName }} funding and serve your area.</p>
              </div>
            </div>
            <div class="step">
              <div class="step-number">5</div>
              <div class="step-content">
                <h3>Begin Services</h3>
                <p>Start receiving authorized services from your chosen providers.</p>
              </div>
            </div>
          </div>
        </div>

        <!-- CTA Section -->
        <div class="cta-section">
          <h2>Find ABA Therapy Providers in {{ rcData.shortName }}</h2>
          <p>Search our interactive map to find providers near you that accept {{ rcData.shortName }} funding.</p>
          <div class="cta-buttons">
            <button @click="searchProviders" class="btn btn-primary btn-lg">
              <i class="bi bi-map me-2"></i>
              View {{ rcData.shortName }} Providers
            </button>
            <router-link to="/faq" class="btn btn-outline-light btn-lg">
              <i class="bi bi-question-circle me-2"></i>
              Learn More
            </router-link>
          </div>
        </div>

        <!-- Other Regional Centers -->
        <div class="content-block">
          <h2>Other LA County Regional Centers</h2>
          <p>Not in {{ rcData.shortName }}? Find your Regional Center:</p>
          <div class="rc-links-grid">
            <router-link 
              v-for="rc in otherRegionalCenters" 
              :key="rc.slug"
              :to="`/regional-centers/${rc.slug}`"
              class="rc-link-card"
            >
              <h4>{{ rc.name }}</h4>
              <p>{{ rc.tagline }}</p>
              <span class="link-arrow">→</span>
            </router-link>
          </div>
        </div>

      </div>
    </section>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { REGIONAL_CENTERS } from '@/data/regionalCenters';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://api.kinddhelp.com';

export default {
  name: 'RegionalCenterView',
  
  setup() {
    const route = useRoute();
    const router = useRouter();
    const zipCode = ref('');
    const actualZipCodes = ref([]);
    const actualCities = ref([]);
    const loading = ref(true);
    const showAllCities = ref(false);
    const showAllZips = ref(false);
    
    const rcSlug = computed(() => route.params.slug);
    const rcData = computed(() => {
      const baseData = REGIONAL_CENTERS[rcSlug.value] || {};
      // Replace hardcoded data with actual data from API
      return {
        ...baseData,
        zipCodes: actualZipCodes.value.length > 0 ? actualZipCodes.value : baseData.zipCodes || [],
        cities: actualCities.value.length > 0 ? actualCities.value : baseData.cities || []
      };
    });
    
    // Display limited items initially (approximately 3 rows)
    const displayedCities = computed(() => {
      const cities = rcData.value.cities || [];
      if (showAllCities.value || cities.length <= 15) return cities;
      return cities.slice(0, 15); // Show first 15 (approx 3 rows)
    });
    
    const displayedZips = computed(() => {
      const zips = rcData.value.zipCodes || [];
      if (showAllZips.value || zips.length <= 30) return zips;
      return zips.slice(0, 30); // Show first 30 (approx 3 rows)
    });
    
    const toggleCities = () => {
      showAllCities.value = !showAllCities.value;
    };
    
    const toggleZips = () => {
      showAllZips.value = !showAllZips.value;
    };
    
    const otherRegionalCenters = computed(() => {
      return Object.values(REGIONAL_CENTERS)
        .filter(rc => rc.slug !== rcSlug.value)
        .map(rc => ({
          name: rc.shortName,
          slug: rc.slug,
          tagline: rc.tagline
        }));
    });
    
    const searchProviders = () => {
      // Navigate to map with search text
      // Use ZIP code if provided, otherwise use regional center name for API query
      const searchQuery = zipCode.value || rcData.value.name;
      router.push({
        path: '/',
        query: { q: searchQuery }
      });
    };
    
    const fetchActualData = async () => {
      try {
        loading.value = true;
        const response = await fetch(`${API_BASE_URL}/api/regional-centers/`);
        const data = await response.json();
        
        // API returns paginated response with results array
        const regionalCenters = data.results || data;
        
        // Find the matching regional center by name
        const matchingRC = regionalCenters.find(rc => 
          rc.regional_center === rcData.value.name
        );
        
        if (matchingRC) {
          // Fetch ZIP codes from API
          if (matchingRC.zip_codes) {
            actualZipCodes.value = matchingRC.zip_codes;
            console.log(`✅ Loaded ${actualZipCodes.value.length} ZIP codes for ${matchingRC.regional_center} from API`);
          }
          
          // Fetch cities/service areas from API
          if (matchingRC.service_areas) {
            actualCities.value = matchingRC.service_areas;
            console.log(`✅ Loaded ${actualCities.value.length} cities for ${matchingRC.regional_center} from API`);
          }
        }
      } catch (error) {
        console.error('Error fetching data from API:', error);
      } finally {
        loading.value = false;
      }
    };
    
    onMounted(async () => {
      // Fetch actual ZIP codes and cities from API
      await fetchActualData();
      
      // Track page view
      if (window.gtag) {
        window.gtag('event', 'page_view', {
          page_title: `${rcData.value.name} | KINDD`,
          page_path: route.path
        });
      }
    });
    
    // Watch for route changes (when clicking other RC links)
    watch(rcSlug, async (newSlug, oldSlug) => {
      if (newSlug !== oldSlug) {
        // Reset UI state
        showAllCities.value = false;
        showAllZips.value = false;
        zipCode.value = '';
        
        // Scroll to top of page
        window.scrollTo({ top: 0, behavior: 'smooth' });
        
        // Refetch data for new regional center
        await fetchActualData();
        
        // Track page view
        if (window.gtag) {
          window.gtag('event', 'page_view', {
            page_title: `${rcData.value.name} | KINDD`,
            page_path: route.path
          });
        }
      }
    });
    
    return {
      rcData,
      otherRegionalCenters,
      zipCode,
      searchProviders,
      loading,
      displayedCities,
      displayedZips,
      showAllCities,
      showAllZips,
      toggleCities,
      toggleZips
    };
  }
};
</script>

<style scoped>
.regional-center-page {
  min-height: 100vh;
  background-color: #f8f9fa;
}

/* Hero Section */
.hero-section {
  color: white;
  padding: 3rem 0 4rem 0;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 50%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(255, 255, 255, 0.08) 0%, transparent 50%),
    linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.1) 100%);
  pointer-events: none;
}

.hero-section .container {
  position: relative;
  z-index: 1;
}

.breadcrumb {
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
  opacity: 0.9;
}

.breadcrumb a {
  color: white;
  text-decoration: none;
  transition: opacity 0.2s;
}

.breadcrumb a:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.breadcrumb .separator {
  margin: 0 0.5rem;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 1.25rem;
  opacity: 0.95;
  margin-bottom: 2rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
  font-weight: 400;
}

.hero-stats {
  display: flex;
  gap: 2rem;
  flex-wrap: wrap;
}

.hero-stats .stat {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.1rem;
  font-weight: 500;
  padding: 0.75rem 1.25rem;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.hero-stats .stat:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.hero-stats .stat i {
  font-size: 1.5rem;
  opacity: 0.9;
}

/* Quick Search */
.quick-search-section {
  margin-top: -2rem;
  padding-bottom: 2rem;
}

.search-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  padding: 2.5rem;
  border-radius: 16px;
  box-shadow: 
    0 10px 30px rgba(0, 0, 0, 0.1),
    0 1px 8px rgba(0, 0, 0, 0.06);
  text-align: center;
  border: 1px solid rgba(0, 72, 119, 0.1);
  position: relative;
  overflow: hidden;
}

.search-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #004877 0%, #0066aa 50%, #004877 100%);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.search-card h2 {
  font-size: 1.75rem;
  color: #004877;
  margin-bottom: 0.5rem;
  font-weight: 700;
}

.search-card p {
  color: #6c757d;
  margin-bottom: 1.5rem;
}

.search-form {
  display: flex;
  gap: 1rem;
  max-width: 500px;
  margin: 0 auto;
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

/* Content Section */
.content-section {
  padding: 2rem 0 3rem 0;
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.content-block {
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  padding: 2.5rem;
  border-radius: 12px;
  margin-bottom: 2rem;
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.06),
    0 1px 3px rgba(0, 0, 0, 0.04);
  border-left: 4px solid #004877;
  transition: all 0.3s ease;
}

.content-block:hover {
  box-shadow: 
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 2px 6px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.content-block h2 {
  font-size: 2rem;
  font-weight: 700;
  color: #004877;
  margin-bottom: 1.5rem;
  background: linear-gradient(135deg, #004877 0%, #0066aa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.content-block h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #004877;
  margin-top: 2rem;
  margin-bottom: 1rem;
}

.content-block p {
  line-height: 1.7;
  color: #495057;
  margin-bottom: 1rem;
}

/* RC Info Grid */
.rc-info-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
  margin-top: 1.5rem;
}

.contact-info {
  margin-top: 2rem;
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.contact-info h3 {
  margin-top: 0;
  font-size: 1.25rem;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.contact-item i {
  color: #004877;
  font-size: 1.25rem;
  width: 24px;
}

.contact-item a {
  color: #004877;
  text-decoration: none;
  font-weight: 500;
}

.contact-item a:hover {
  text-decoration: underline;
}

.highlights-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.highlights-list li {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 6px;
}

.highlights-list i {
  color: #4DAA50;
  font-size: 1.25rem;
  flex-shrink: 0;
  margin-top: 0.1rem;
}

/* Cities Grid */
.cities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.75rem;
  margin: 1.5rem 0;
}

.city-chip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #0D9DDB;
  font-weight: 500;
  color: #212529;
}

.city-chip i {
  color: #0D9DDB;
}

/* ZIP Codes */
.zip-section {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #e9ecef;
}

.zip-codes {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1rem;
}

.zip-badge {
  display: inline-block;
  padding: 0.5rem 1rem;
  background: #e7f3ff;
  border: 1px solid #0D9DDB;
  border-radius: 6px;
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: #004877;
  font-size: 0.9rem;
}

/* Services Grid */
.services-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-top: 2rem;
}

.service-card {
  text-align: center;
  padding: 1.5rem;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.service-card:hover {
  border-color: #FFC923;
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.service-icon {
  font-size: 3rem;
  color: #004877;
  margin-bottom: 1rem;
}

.service-card h3 {
  font-size: 1.25rem;
  margin-top: 0;
  margin-bottom: 0.5rem;
}

.service-card p {
  font-size: 0.95rem;
  margin-bottom: 0;
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
  margin-top: 0;
  margin-bottom: 0.5rem;
}

.step-content p {
  margin-bottom: 0;
}

/* CTA Section */
.cta-section {
  background: linear-gradient(135deg, #004877 0%, #0D9DDB 100%);
  color: white;
  padding: 3rem;
  border-radius: 12px;
  text-align: center;
  margin-top: 2rem;
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
  color: white;
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
  border: none;
  border-radius: 8px;
  cursor: pointer;
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

/* Other RC Links */
.rc-links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.rc-link-card {
  padding: 1.5rem;
  background: #f8f9fa;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.rc-link-card:hover {
  border-color: #0D9DDB;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.rc-link-card h4 {
  color: #004877;
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
}

.rc-link-card p {
  color: #6c757d;
  font-size: 0.9rem;
  margin-bottom: 0;
}

.link-arrow {
  position: absolute;
  top: 1.5rem;
  right: 1.5rem;
  font-size: 1.5rem;
  color: #0D9DDB;
  transition: transform 0.3s ease;
}

.rc-link-card:hover .link-arrow {
  transform: translateX(5px);
}

/* Highlight Block */
.highlight-block {
  background: linear-gradient(135deg, #eff6ff 0%, #e0f2fe 100%);
  border-left: 6px solid #0D9DDB;
}

/* Responsive */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }

  .page-subtitle {
    font-size: 1.1rem;
  }

  .hero-stats {
    flex-direction: column;
    gap: 1rem;
  }

  .search-form {
    flex-direction: column;
  }

  .rc-info-grid {
    grid-template-columns: 1fr;
  }

  .content-block {
    padding: 1.5rem;
  }

  .step {
    gap: 1rem;
  }

  .step-number {
    width: 40px;
    height: 40px;
    font-size: 1.25rem;
  }

  .cta-section {
    padding: 2rem 1.5rem;
  }

  .cta-buttons {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }
}
</style>

