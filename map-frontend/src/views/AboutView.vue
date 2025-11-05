<template>
  <div class="about-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="container">
        <h1 class="page-title">About KINDD Provider Map</h1>
        <p class="page-subtitle">
          Helping families in Los Angeles County find ABA therapy and developmental disability services
        </p>
      </div>
    </section>

    <!-- Main Content -->
    <section class="content-section">
      <div class="container">
        
        <!-- Mission -->
        <div class="content-block">
          <h2>Our Mission</h2>
          <p class="lead">
            We create hope and build healthier futures by connecting families with the autism and developmental disability services they need.
          </p>
          <p>
            KINDD (Kids in Need of Developmental Disabilities) is a free, interactive map developed by <strong>Children's Hospital Los Angeles</strong> to help families find ABA therapy providers and autism services across Los Angeles County.
          </p>
        </div>

        <!-- What We Do -->
        <div class="content-block">
          <h2>What We Do</h2>
          <div class="feature-grid">
            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-map-fill"></i>
              </div>
              <h3>Interactive Map</h3>
              <p>Visualize provider locations geographically and see your Regional Center boundaries.</p>
            </div>

            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-geo-alt-fill"></i>
              </div>
              <h3>Location-Based Search</h3>
              <p>Automatically find providers near you based on your ZIP code or current location.</p>
            </div>

            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-funnel-fill"></i>
              </div>
              <h3>Smart Filters</h3>
              <p>Search by insurance accepted, therapy types, age groups, and diagnoses treated.</p>
            </div>

            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-building"></i>
              </div>
              <h3>Regional Center Integration</h3>
              <p>Understand which Regional Center serves your area and find their providers.</p>
            </div>

            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-phone-fill"></i>
              </div>
              <h3>Quick Contact</h3>
              <p>Get phone numbers, websites, and directions with one click.</p>
            </div>

            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-phone"></i>
              </div>
              <h3>Mobile-Friendly</h3>
              <p>Works seamlessly on phones, tablets, and desktops for search on-the-go.</p>
            </div>
          </div>
        </div>

        <!-- Regional Centers Explained -->
        <div class="content-block highlight-block">
          <h2>Understanding Regional Centers</h2>
          <p>
            <strong>Regional Centers are nonprofit agencies</strong> funded by the State of California to provide services and support to individuals with developmental disabilities. They are the gateway to free or low-cost services for eligible families.
          </p>
          
          <h3>LA County's 7 Regional Centers</h3>
          <div class="rc-grid">
            <div class="rc-card">
              <h4>San Gabriel/Pomona</h4>
              <p>Serves eastern LA County and parts of the San Gabriel Valley</p>
            </div>
            <div class="rc-card">
              <h4>Harbor</h4>
              <p>Serves South Bay and coastal communities</p>
            </div>
            <div class="rc-card">
              <h4>North LA County</h4>
              <p>Serves Antelope Valley and northern regions</p>
            </div>
            <div class="rc-card">
              <h4>Eastern LA</h4>
              <p>Serves communities east of downtown LA</p>
            </div>
            <div class="rc-card">
              <h4>South Central LA</h4>
              <p>Serves south-central LA communities</p>
            </div>
            <div class="rc-card">
              <h4>Westside</h4>
              <p>Serves West LA, Santa Monica, and surrounding areas</p>
            </div>
            <div class="rc-card">
              <h4>Frank D. Lanterman</h4>
              <p>Serves San Fernando and San Gabriel Valleys</p>
            </div>
          </div>

          <div class="info-box">
            <i class="bi bi-info-circle-fill"></i>
            <div>
              <strong>Important:</strong> Your Regional Center is determined by your ZIP code, not your choice. Each ZIP code in California is assigned to exactly one Regional Center based on geographic boundaries.
            </div>
          </div>
        </div>

        <!-- How It Works -->
        <div class="content-block">
          <h2>How the Map Works</h2>
          <div class="steps">
            <div class="step">
              <div class="step-number">1</div>
              <div class="step-content">
                <h3>Enter Your Location</h3>
                <p>Allow location access or enter your ZIP code to find providers near you.</p>
              </div>
            </div>

            <div class="step">
              <div class="step-number">2</div>
              <div class="step-content">
                <h3>See Your Regional Center</h3>
                <p>The map automatically shows which Regional Center serves your area.</p>
              </div>
            </div>

            <div class="step">
              <div class="step-number">3</div>
              <div class="step-content">
                <h3>Filter Providers</h3>
                <p>Use filters to narrow results by insurance, therapy types, and more.</p>
              </div>
            </div>

            <div class="step">
              <div class="step-number">4</div>
              <div class="step-content">
                <h3>Connect with Providers</h3>
                <p>View provider details, get directions, and contact them directly.</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Who We Serve -->
        <div class="content-block">
          <h2>Who We Serve</h2>
          <p>KINDD helps families seeking services for children and adults with:</p>
          <div class="diagnosis-grid">
            <div class="diagnosis-item">
              <i class="bi bi-check-circle-fill"></i>
              <span>Autism Spectrum Disorder (ASD)</span>
            </div>
            <div class="diagnosis-item">
              <i class="bi bi-check-circle-fill"></i>
              <span>Developmental Delays</span>
            </div>
            <div class="diagnosis-item">
              <i class="bi bi-check-circle-fill"></i>
              <span>Intellectual Disabilities</span>
            </div>
            <div class="diagnosis-item">
              <i class="bi bi-check-circle-fill"></i>
              <span>Communication Disorders</span>
            </div>
            <div class="diagnosis-item">
              <i class="bi bi-check-circle-fill"></i>
              <span>Behavioral Challenges</span>
            </div>
            <div class="diagnosis-item">
              <i class="bi bi-check-circle-fill"></i>
              <span>Learning Disabilities</span>
            </div>
          </div>
        </div>

        <!-- About CHLA -->
        <div class="content-block chla-block">
          <div class="chla-content">
            <div class="chla-logo-section">
              <img src="@/assets/chla-logo.svg" alt="Children's Hospital Los Angeles" class="chla-logo" />
            </div>
            <div>
              <h2>About Children's Hospital Los Angeles</h2>
              <p>
                <strong>Children's Hospital Los Angeles (CHLA)</strong> has been a leader in pediatric healthcare for over 120 years. As one of the top children's hospitals in the nation, CHLA is committed to creating hope and building healthier futures for children and families.
              </p>
              <p>
                CHLA developed the KINDD Provider Map as part of our mission to improve access to care for all children, including those with autism and developmental disabilities. We believe every family deserves easy access to quality services.
              </p>
              <p>
                <a href="https://www.chla.org" target="_blank" rel="noopener noreferrer" class="btn btn-outline-primary">
                  Learn More About CHLA <i class="bi bi-arrow-right"></i>
                </a>
              </p>
            </div>
          </div>
        </div>

        <!-- Free to Use -->
        <div class="content-block highlight-block">
          <h2>Free to Use, No Registration Required</h2>
          <p>
            KINDD is completely free to use. There are no fees, subscriptions, or registration required. We don't sell your data or charge providers to be listed. Our goal is simply to help families find the services they need.
          </p>
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-number">370+</div>
              <div class="stat-label">Verified Providers</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">7</div>
              <div class="stat-label">Regional Centers Covered</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">100%</div>
              <div class="stat-label">Free to Use</div>
            </div>
          </div>
        </div>

        <!-- CTA Section -->
        <div class="cta-section">
          <h2>Ready to Find a Provider?</h2>
          <p>Start your search now - it only takes a few seconds.</p>
          <div class="cta-buttons">
            <router-link to="/" class="btn btn-primary btn-lg">
              <i class="bi bi-map me-2"></i>
              Explore the Map
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
export default {
  name: 'AboutView'
};
</script>

<style scoped>
.about-page {
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

/* Content Section */
.content-section {
  padding: 3rem 0;
}

.container {
  max-width: 1100px;
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

.content-block h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #004877;
  margin-top: 2rem;
  margin-bottom: 1rem;
}

.lead {
  font-size: 1.25rem;
  font-weight: 500;
  color: #495057;
  margin-bottom: 1.5rem;
}

.content-block p {
  line-height: 1.7;
  color: #495057;
  margin-bottom: 1rem;
}

/* Feature Grid */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-top: 2rem;
}

.feature-card {
  text-align: center;
  padding: 1.5rem;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.feature-card:hover {
  border-color: #FFC923;
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 3rem;
  color: #004877;
  margin-bottom: 1rem;
}

.feature-card h3 {
  font-size: 1.25rem;
  margin-top: 0;
  margin-bottom: 0.5rem;
}

.feature-card p {
  font-size: 0.95rem;
  margin-bottom: 0;
}

/* Regional Centers Grid */
.rc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
  margin: 2rem 0;
}

.rc-card {
  padding: 1.25rem;
  background: #f8f9fa;
  border-left: 4px solid #0D9DDB;
  border-radius: 4px;
}

.rc-card h4 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #004877;
  margin-bottom: 0.5rem;
}

.rc-card p {
  font-size: 0.9rem;
  margin-bottom: 0;
  color: #6c757d;
}

/* Info Box */
.info-box {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  background: #e7f3ff;
  border-radius: 8px;
  border-left: 4px solid #004877;
  margin-top: 1.5rem;
}

.info-box i {
  font-size: 1.5rem;
  color: #004877;
  flex-shrink: 0;
}

.info-box strong {
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
  margin-top: 0;
  margin-bottom: 0.5rem;
}

.step-content p {
  margin-bottom: 0;
}

/* Diagnosis Grid */
.diagnosis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.diagnosis-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 6px;
}

.diagnosis-item i {
  color: #4DAA50;
  font-size: 1.25rem;
}

.diagnosis-item span {
  font-weight: 500;
}

/* CHLA Block */
.chla-block {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.chla-content {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 2rem;
  align-items: center;
}

.chla-logo {
  width: 100%;
  height: auto;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-top: 2rem;
}

.stat-card {
  text-align: center;
  padding: 2rem 1rem;
  background: white;
  border-radius: 8px;
  border: 2px solid #e9ecef;
}

.stat-number {
  font-size: 3rem;
  font-weight: 700;
  color: #004877;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-size: 1rem;
  color: #6c757d;
  font-weight: 500;
}

/* Highlight Block */
.highlight-block {
  background: linear-gradient(135deg, #eff6ff 0%, #e0f2fe 100%);
  border-left: 6px solid #0D9DDB;
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
}

.btn-primary:hover {
  background-color: #ffb700;
  border-color: #ffb700;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 201, 35, 0.4);
}

.btn-outline-light {
  border: 2px solid white;
  color: white;
  font-weight: 600;
  padding: 0.75rem 2rem;
  font-size: 1.1rem;
  transition: all 0.3s ease;
}

.btn-outline-light:hover {
  background-color: white;
  color: #004877;
  transform: translateY(-2px);
}

.btn-outline-primary {
  border: 2px solid #004877;
  color: #004877;
  font-weight: 600;
  padding: 0.75rem 2rem;
  text-decoration: none;
  display: inline-block;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.btn-outline-primary:hover {
  background-color: #004877;
  color: white;
  transform: translateY(-2px);
}

/* Responsive */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }

  .page-subtitle {
    font-size: 1.1rem;
  }

  .hero-section {
    padding: 3rem 0;
  }

  .content-section {
    padding: 2rem 0;
  }

  .content-block {
    padding: 1.5rem;
  }

  .content-block h2 {
    font-size: 1.75rem;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .chla-content {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .chla-logo {
    max-width: 200px;
    margin: 0 auto;
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

  .cta-section h2 {
    font-size: 1.5rem;
  }

  .cta-buttons {
    flex-direction: column;
  }

  .cta-buttons .btn {
    width: 100%;
  }
}
</style>

