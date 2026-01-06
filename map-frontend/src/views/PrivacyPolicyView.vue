<template>
  <div class="privacy-policy">
    <div class="container">
      <header class="policy-header">
        <h1>Privacy Policy</h1>
        <p class="last-updated">Last Updated: December 15, 2025</p>
      </header>

      <div class="policy-content">
        <section>
          <h2>Introduction</h2>
          <p>
            NDD Resource Map ("we," "our," or "us") is committed to protecting
            your privacy. This Privacy Policy explains how we collect, use, and
            safeguard your information when you use our mobile application and
            website (collectively, the "Service").
          </p>
        </section>

        <section>
          <h2>Information We Collect</h2>

          <h3>Location Information</h3>
          <p>
            With your permission, we collect your device's location to help you
            find healthcare resources and service providers near you. This
            includes:
          </p>
          <ul>
            <li>
              Your current geographic coordinates (latitude and longitude)
            </li>
            <li>Distance calculations to nearby providers</li>
          </ul>
          <p>
            <strong>You can deny location access</strong> and still use all
            features of the app. Without location access, you can search for
            providers by ZIP code, city, or address.
          </p>

          <h3>Usage Data</h3>
          <p>
            We may automatically collect certain information about how you
            interact with our Service, including:
          </p>
          <ul>
            <li>Pages and features you access</li>
            <li>Search queries you enter</li>
            <li>Time spent on the Service</li>
            <li>Device type and operating system</li>
          </ul>
        </section>

        <section>
          <h2>How We Use Your Information</h2>
          <p>We use the information we collect to:</p>
          <ul>
            <li>Show you healthcare resources near your location</li>
            <li>Provide directions to service providers</li>
            <li>Improve and optimize our Service</li>
            <li>Respond to your inquiries and support requests</li>
            <li>Analyze usage patterns to enhance user experience</li>
          </ul>
        </section>

        <section>
          <h2>Information Sharing</h2>
          <p>
            <strong>We do not sell your personal information.</strong> We may
            share information only in the following circumstances:
          </p>
          <ul>
            <li>
              With service providers who assist in operating our Service (e.g.,
              hosting, analytics)
            </li>
            <li>When required by law or to protect our legal rights</li>
            <li>In connection with a merger, acquisition, or sale of assets</li>
          </ul>
        </section>

        <section>
          <h2>Data Security</h2>
          <p>
            We implement appropriate technical and organizational measures to
            protect your information against unauthorized access, alteration,
            disclosure, or destruction. However, no method of transmission over
            the Internet is 100% secure.
          </p>
        </section>

        <section>
          <h2>Your Rights</h2>
          <p>You have the right to:</p>
          <ul>
            <li>Access the personal information we hold about you</li>
            <li>Request correction of inaccurate information</li>
            <li>Request deletion of your information</li>
            <li>
              Withdraw consent for location tracking at any time through your
              device settings
            </li>
          </ul>
        </section>

        <section>
          <h2>Third-Party Services</h2>
          <p>Our Service may use the following third-party services:</p>
          <ul>
            <li>
              <strong>Apple Maps / MapKit</strong> - For displaying maps and
              providing directions
            </li>
            <li><strong>Mapbox</strong> - For map visualization on the web</li>
          </ul>
          <p>
            These services have their own privacy policies governing the use of
            your information.
          </p>
        </section>

        <section>
          <h2>Children's Privacy</h2>
          <p>
            Our Service is not directed to children under the age of 13. We do
            not knowingly collect personal information from children under 13.
            If you believe we have collected information from a child under 13,
            please contact us.
          </p>
        </section>

        <section>
          <h2>Changes to This Policy</h2>
          <p>
            We may update this Privacy Policy from time to time. We will notify
            you of any changes by posting the new Privacy Policy on this page
            and updating the "Last Updated" date.
          </p>
        </section>

        <section>
          <h2>Contact Us</h2>
          <p>
            If you have questions about this Privacy Policy or our privacy
            practices, please contact us at:
          </p>
          <p class="contact-info">
            <strong>Email:</strong> privacy@kinddhelp.com<br />
            <strong>Website:</strong>
            <a href="https://kinddhelp.com">https://kinddhelp.com</a>
          </p>
        </section>
      </div>

      <footer class="policy-footer">
        <router-link to="/" class="back-link">← Back to Map</router-link>
      </footer>
    </div>
  </div>
</template>

<script setup>
// No reactive state needed for static content
</script>

<style scoped>
.privacy-policy {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 2rem 1rem;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.policy-header {
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  color: white;
  padding: 3rem 2rem;
  text-align: center;
}

.policy-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  font-family: "Inter", -apple-system, BlinkMacSystemFont, sans-serif;
}

.last-updated {
  opacity: 0.9;
  font-size: 0.95rem;
  margin: 0;
}

.policy-content {
  padding: 2rem;
}

section {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #e2e8f0;
}

section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 1rem 0;
  font-family: "Inter", -apple-system, BlinkMacSystemFont, sans-serif;
}

h3 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #334155;
  margin: 1.5rem 0 0.75rem 0;
}

p {
  color: #475569;
  line-height: 1.7;
  margin: 0 0 1rem 0;
}

ul {
  color: #475569;
  line-height: 1.7;
  margin: 0 0 1rem 0;
  padding-left: 1.5rem;
}

li {
  margin-bottom: 0.5rem;
}

strong {
  color: #0f172a;
}

a {
  color: #0ea5e9;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

.contact-info {
  background: #f8fafc;
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid #0ea5e9;
}

.policy-footer {
  padding: 1.5rem 2rem;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.back-link {
  color: #0ea5e9;
  font-weight: 500;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.back-link:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .privacy-policy {
    padding: 1rem 0.5rem;
  }

  .policy-header {
    padding: 2rem 1.5rem;
  }

  .policy-header h1 {
    font-size: 2rem;
  }

  .policy-content {
    padding: 1.5rem;
  }

  h2 {
    font-size: 1.3rem;
  }
}
</style>



