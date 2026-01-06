<template>
  <div class="terms-of-service">
    <div class="container">
      <header class="terms-header">
        <h1>Terms of Service</h1>
        <p class="last-updated">Last Updated: December 15, 2025</p>
      </header>

      <div class="terms-content">
        <section>
          <h2>Acceptance of Terms</h2>
          <p>
            By accessing or using NDD Resource Map ("the Service"), you agree to be bound 
            by these Terms of Service. If you do not agree to these terms, please do not 
            use the Service.
          </p>
        </section>

        <section>
          <h2>Description of Service</h2>
          <p>
            NDD Resource Map is a free service that helps users find healthcare resources 
            and service providers for individuals with neurodevelopmental disabilities 
            in Los Angeles County. The Service provides:
          </p>
          <ul>
            <li>An interactive map of verified healthcare resources</li>
            <li>Provider contact information and service details</li>
            <li>Regional Center service area information</li>
            <li>Directions to provider locations</li>
          </ul>
        </section>

        <section>
          <h2>Information Accuracy</h2>
          <p>
            While we strive to provide accurate and up-to-date information, we cannot 
            guarantee the accuracy, completeness, or timeliness of all provider information. 
            Provider details including addresses, phone numbers, and services offered may 
            change without notice.
          </p>
          <p>
            <strong>Always verify information directly with providers</strong> before 
            making healthcare decisions or traveling to a location.
          </p>
        </section>

        <section>
          <h2>Not Medical Advice</h2>
          <p>
            The Service is an informational resource only and does not provide medical 
            advice, diagnosis, or treatment recommendations. The inclusion of a provider 
            in our database does not constitute an endorsement or recommendation.
          </p>
          <p>
            Always consult with qualified healthcare professionals and your Regional 
            Center service coordinator for guidance on services appropriate for your needs.
          </p>
        </section>

        <section>
          <h2>User Responsibilities</h2>
          <p>You agree to:</p>
          <ul>
            <li>Use the Service only for lawful purposes</li>
            <li>Not attempt to interfere with the proper functioning of the Service</li>
            <li>Not use automated means to access the Service without permission</li>
            <li>Verify provider information before relying on it</li>
          </ul>
        </section>

        <section>
          <h2>Intellectual Property</h2>
          <p>
            The Service and its original content, features, and functionality are owned 
            by NDD Resource Map and are protected by applicable copyright, trademark, 
            and other intellectual property laws.
          </p>
        </section>

        <section>
          <h2>Third-Party Services</h2>
          <p>
            The Service integrates with third-party mapping services (Apple Maps, Mapbox) 
            to provide location and directions features. Your use of these features is 
            subject to the respective third-party terms of service.
          </p>
        </section>

        <section>
          <h2>Limitation of Liability</h2>
          <p>
            To the maximum extent permitted by law, NDD Resource Map shall not be liable 
            for any indirect, incidental, special, consequential, or punitive damages, 
            including but not limited to:
          </p>
          <ul>
            <li>Loss of data or information</li>
            <li>Personal injury or property damage</li>
            <li>Reliance on information provided through the Service</li>
            <li>Interruption or unavailability of the Service</li>
          </ul>
        </section>

        <section>
          <h2>Modifications to Service</h2>
          <p>
            We reserve the right to modify, suspend, or discontinue the Service at any 
            time without notice. We may also update these Terms of Service from time to 
            time. Continued use of the Service after changes constitutes acceptance of 
            the modified terms.
          </p>
        </section>

        <section>
          <h2>Governing Law</h2>
          <p>
            These Terms shall be governed by and construed in accordance with the laws 
            of the State of California, without regard to its conflict of law provisions.
          </p>
        </section>

        <section>
          <h2>Contact Us</h2>
          <p>
            If you have questions about these Terms of Service, please contact us at:
          </p>
          <p class="contact-info">
            <strong>Email:</strong> support@kinddhelp.com<br>
            <strong>Website:</strong> <a href="https://kinddhelp.com">https://kinddhelp.com</a>
          </p>
        </section>
      </div>

      <footer class="terms-footer">
        <router-link to="/" class="back-link">← Back to Map</router-link>
        <router-link to="/privacy" class="privacy-link">Privacy Policy</router-link>
      </footer>
    </div>
  </div>
</template>

<script setup>
// No reactive state needed for static content
</script>

<style scoped>
.terms-of-service {
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

.terms-header {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  padding: 3rem 2rem;
  text-align: center;
}

.terms-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.last-updated {
  opacity: 0.9;
  font-size: 0.95rem;
  margin: 0;
}

.terms-content {
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
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
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
  color: #6366f1;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

.contact-info {
  background: #f8fafc;
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid #6366f1;
}

.terms-footer {
  padding: 1.5rem 2rem;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.back-link,
.privacy-link {
  color: #6366f1;
  font-weight: 500;
  text-decoration: none;
}

.back-link:hover,
.privacy-link:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .terms-of-service {
    padding: 1rem 0.5rem;
  }

  .terms-header {
    padding: 2rem 1.5rem;
  }

  .terms-header h1 {
    font-size: 2rem;
  }

  .terms-content {
    padding: 1.5rem;
  }

  h2 {
    font-size: 1.3rem;
  }

  .terms-footer {
    flex-direction: column;
    gap: 1rem;
  }
}
</style>




