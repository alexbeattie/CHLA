<template>
  <div class="faq-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="container">
        <h1 class="page-title">Frequently Asked Questions</h1>
        <p class="page-subtitle">
          Everything you need to know about ABA therapy, Regional Centers, and finding providers in Los Angeles County
        </p>
      </div>
    </section>

    <!-- FAQ Content -->
    <section class="faq-content">
      <div class="container">
        
        <!-- ABA Therapy Questions -->
        <div class="faq-category">
          <h2 class="category-title">ABA Therapy Basics</h2>
          
          <div class="faq-item" v-for="faq in abaTherapyFaqs" :key="faq.id">
            <button 
              class="faq-question" 
              @click="toggleFaq(faq.id)"
              :aria-expanded="activeFaq === faq.id"
            >
              <span>{{ faq.question }}</span>
              <i class="bi" :class="activeFaq === faq.id ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
            </button>
            <div class="faq-answer" v-show="activeFaq === faq.id">
              <div v-html="faq.answer"></div>
            </div>
          </div>
        </div>

        <!-- Regional Centers Questions -->
        <div class="faq-category">
          <h2 class="category-title">Regional Centers</h2>
          
          <div class="faq-item" v-for="faq in regionalCenterFaqs" :key="faq.id">
            <button 
              class="faq-question" 
              @click="toggleFaq(faq.id)"
              :aria-expanded="activeFaq === faq.id"
            >
              <span>{{ faq.question }}</span>
              <i class="bi" :class="activeFaq === faq.id ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
            </button>
            <div class="faq-answer" v-show="activeFaq === faq.id">
              <div v-html="faq.answer"></div>
            </div>
          </div>
        </div>

        <!-- Insurance & Funding Questions -->
        <div class="faq-category">
          <h2 class="category-title">Insurance & Funding</h2>
          
          <div class="faq-item" v-for="faq in insuranceFaqs" :key="faq.id">
            <button 
              class="faq-question" 
              @click="toggleFaq(faq.id)"
              :aria-expanded="activeFaq === faq.id"
            >
              <span>{{ faq.question }}</span>
              <i class="bi" :class="activeFaq === faq.id ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
            </button>
            <div class="faq-answer" v-show="activeFaq === faq.id">
              <div v-html="faq.answer"></div>
            </div>
          </div>
        </div>

        <!-- Using the Map Questions -->
        <div class="faq-category">
          <h2 class="category-title">Using the KINDD Map</h2>
          
          <div class="faq-item" v-for="faq in mapFaqs" :key="faq.id">
            <button 
              class="faq-question" 
              @click="toggleFaq(faq.id)"
              :aria-expanded="activeFaq === faq.id"
            >
              <span>{{ faq.question }}</span>
              <i class="bi" :class="activeFaq === faq.id ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
            </button>
            <div class="faq-answer" v-show="activeFaq === faq.id">
              <div v-html="faq.answer"></div>
            </div>
          </div>
        </div>

        <!-- CTA Section -->
        <div class="cta-section">
          <h2>Ready to Find a Provider?</h2>
          <p>Search our interactive map to find ABA therapy providers near you.</p>
          <router-link to="/" class="btn btn-primary btn-lg">
            <i class="bi bi-map me-2"></i>
            Explore the Map
          </router-link>
        </div>

      </div>
    </section>
  </div>
</template>

<script>
import { ref } from 'vue';

export default {
  name: 'FaqView',
  
  setup() {
    const activeFaq = ref(null);

    const toggleFaq = (id) => {
      activeFaq.value = activeFaq.value === id ? null : id;
    };

    const abaTherapyFaqs = [
      {
        id: 'aba-1',
        question: 'What is ABA therapy?',
        answer: `<p><strong>Applied Behavior Analysis (ABA)</strong> is a scientific, evidence-based therapy that helps improve specific behaviors, such as social skills, communication, reading, and academics, as well as adaptive learning skills.</p>
        <p>ABA therapy is particularly effective for individuals with autism spectrum disorder (ASD) and developmental disabilities. It uses positive reinforcement and data-driven techniques to teach new skills and reduce challenging behaviors.</p>`
      },
      {
        id: 'aba-2',
        question: 'Who can benefit from ABA therapy?',
        answer: `<p>ABA therapy can benefit individuals of all ages with:</p>
        <ul>
          <li><strong>Autism Spectrum Disorder (ASD)</strong> - Most common use case</li>
          <li><strong>Developmental Disabilities</strong> - Cognitive or intellectual delays</li>
          <li><strong>Learning Disabilities</strong> - Academic challenges</li>
          <li><strong>Behavioral Disorders</strong> - ADHD, ODD, and others</li>
          <li><strong>Communication Disorders</strong> - Speech and language delays</li>
        </ul>
        <p>While ABA is most commonly used for children with autism, it can be beneficial for anyone needing structured behavioral intervention.</p>`
      },
      {
        id: 'aba-3',
        question: 'How long does ABA therapy take?',
        answer: `<p>ABA therapy duration varies based on individual needs:</p>
        <ul>
          <li><strong>Intensive programs:</strong> 20-40 hours per week (common for young children with autism)</li>
          <li><strong>Focused programs:</strong> 10-20 hours per week (specific skill development)</li>
          <li><strong>Consultation:</strong> 1-5 hours per week (maintenance or specific issues)</li>
        </ul>
        <p><strong>Timeline:</strong> Most children participate for 1-3 years, but some may need longer or shorter periods depending on their progress and goals. Your provider will create an individualized treatment plan.</p>`
      },
      {
        id: 'aba-4',
        question: 'What happens in an ABA therapy session?',
        answer: `<p>A typical ABA therapy session includes:</p>
        <ol>
          <li><strong>Structured Activities:</strong> Therapist works on specific goals using evidence-based techniques</li>
          <li><strong>Positive Reinforcement:</strong> Rewards for desired behaviors</li>
          <li><strong>Data Collection:</strong> Progress is measured and documented</li>
          <li><strong>Natural Environment Teaching:</strong> Skills practiced in real-world settings</li>
          <li><strong>Parent/Caregiver Training:</strong> Teaching strategies for home use</li>
        </ol>
        <p>Sessions can take place at home, in clinics, schools, or community settings.</p>`
      },
      {
        id: 'aba-5',
        question: 'How do I choose an ABA provider?',
        answer: `<p>When selecting an ABA provider, consider:</p>
        <ul>
          <li><strong>Certification:</strong> Board Certified Behavior Analyst (BCBA) oversight</li>
          <li><strong>Experience:</strong> Years working with your child's specific needs</li>
          <li><strong>Insurance:</strong> Accepts your insurance or Regional Center funding</li>
          <li><strong>Location:</strong> Convenient to your home or child's school</li>
          <li><strong>Approach:</strong> Treatment philosophy aligns with your values</li>
          <li><strong>Communication:</strong> Responsive and collaborative with families</li>
          <li><strong>Reviews:</strong> Positive feedback from other families</li>
        </ul>
        <p><strong>Use our map</strong> to find providers that match your criteria and compare options in your area.</p>`
      }
    ];

    const regionalCenterFaqs = [
      {
        id: 'rc-1',
        question: 'What is a Regional Center?',
        answer: `<p><strong>Regional Centers</strong> are nonprofit agencies that provide services and support to individuals with developmental disabilities in California. They are funded by the state and serve as the gateway to services, including ABA therapy.</p>
        <p><strong>LA County has 7 Regional Centers:</strong></p>
        <ul>
          <li>San Gabriel/Pomona Regional Center</li>
          <li>Harbor Regional Center</li>
          <li>North Los Angeles County Regional Center</li>
          <li>Eastern Los Angeles Regional Center</li>
          <li>South Central Los Angeles Regional Center</li>
          <li>Westside Regional Center</li>
          <li>Frank D. Lanterman Regional Center</li>
        </ul>
        <p>Your Regional Center is determined by your ZIP code, not your choice.</p>`
      },
      {
        id: 'rc-2',
        question: 'How do I know which Regional Center serves my area?',
        answer: `<p><strong>Your Regional Center is determined by your ZIP code.</strong> Each ZIP code in California is assigned to exactly one Regional Center.</p>
        <p><strong>To find your Regional Center:</strong></p>
        <ol>
          <li>Use our interactive map - it automatically detects your location</li>
          <li>Enter your ZIP code in the search bar</li>
          <li>The map will show your Regional Center boundary and providers in your area</li>
        </ol>
        <p>You cannot choose your Regional Center - it's based solely on where you live.</p>`
      },
      {
        id: 'rc-3',
        question: 'How do I get services from a Regional Center?',
        answer: `<p><strong>Steps to access Regional Center services:</strong></p>
        <ol>
          <li><strong>Contact your Regional Center:</strong> Call or visit their website to make an intake appointment</li>
          <li><strong>Intake Assessment:</strong> Meet with a service coordinator to discuss needs</li>
          <li><strong>Eligibility Determination:</strong> Regional Center determines if you qualify (usually within 120 days)</li>
          <li><strong>IPP Development:</strong> Create an Individualized Program Plan with services and supports</li>
          <li><strong>Choose Providers:</strong> Select from Regional Center-approved vendors</li>
          <li><strong>Services Begin:</strong> Start receiving authorized services</li>
        </ol>
        <p><strong>Note:</strong> To qualify, you must have a developmental disability that began before age 18 and will continue indefinitely.</p>`
      },
      {
        id: 'rc-4',
        question: 'Do I need to use a Regional Center to get ABA therapy?',
        answer: `<p><strong>No, there are multiple ways to access ABA therapy:</strong></p>
        <ul>
          <li><strong>Regional Center:</strong> Free services for eligible individuals with developmental disabilities</li>
          <li><strong>Private Insurance:</strong> Most health plans in California cover ABA therapy for autism (required by law)</li>
          <li><strong>Medi-Cal:</strong> Covers ABA therapy for eligible children</li>
          <li><strong>School District:</strong> IEP services through special education</li>
          <li><strong>Private Pay:</strong> Self-funded therapy</li>
        </ul>
        <p>Many families use a combination of funding sources. Our map shows which providers accept different insurance types and Regional Center funding.</p>`
      },
      {
        id: 'rc-5',
        question: 'Can I see providers outside my Regional Center area?',
        answer: `<p><strong>It depends on your funding source:</strong></p>
        <ul>
          <li><strong>Regional Center Funding:</strong> You should use providers that serve your Regional Center, but exceptions can be made</li>
          <li><strong>Private Insurance:</strong> You can see any in-network provider regardless of location</li>
          <li><strong>Medi-Cal:</strong> Depends on managed care plan and provider network</li>
        </ul>
        <p>Regional Centers typically prefer providers in their service area, but may approve out-of-area providers if there's a good reason (e.g., specialized services, long wait lists, family preference).</p>`
      }
    ];

    const insuranceFaqs = [
      {
        id: 'ins-1',
        question: 'Does insurance cover ABA therapy in California?',
        answer: `<p><strong>Yes!</strong> California law requires most health insurance plans to cover ABA therapy for autism spectrum disorder.</p>
        <p><strong>Coverage includes:</strong></p>
        <ul>
          <li><strong>Commercial Insurance:</strong> Required to cover ABA for autism (up to age 21 in most cases)</li>
          <li><strong>Medi-Cal:</strong> Covers ABA therapy for eligible children</li>
          <li><strong>Regional Center:</strong> Provides funding for eligible individuals</li>
          <li><strong>TRICARE:</strong> Covers ABA for military families</li>
        </ul>
        <p><strong>Important:</strong> Coverage varies by plan. Check with your insurance provider for specific benefits, copays, and authorization requirements.</p>`
      },
      {
        id: 'ins-2',
        question: 'What insurance companies are accepted by providers?',
        answer: `<p>Providers in our map accept a wide range of insurance, including:</p>
        <ul>
          <li><strong>Commercial Plans:</strong> Aetna, Anthem Blue Cross, Blue Shield of California, Cigna, Health Net, Kaiser Permanente, UnitedHealthcare</li>
          <li><strong>Medi-Cal Managed Care:</strong> Health Net, Kaiser, LA Care, Molina Healthcare</li>
          <li><strong>Other:</strong> TRICARE, TRIWEST, Magellan Health, MHN, Evernorth</li>
          <li><strong>Regional Center:</strong> Most providers accept Regional Center funding</li>
        </ul>
        <p><strong>Use our map filters</strong> to search for providers that accept your specific insurance.</p>`
      },
      {
        id: 'ins-3',
        question: 'How much does ABA therapy cost?',
        answer: `<p><strong>Costs vary widely depending on funding source:</strong></p>
        <ul>
          <li><strong>Regional Center:</strong> Free for eligible families</li>
          <li><strong>Insurance:</strong> Typically copay only ($0-$75 per session, depending on plan)</li>
          <li><strong>Medi-Cal:</strong> No cost for covered services</li>
          <li><strong>Private Pay:</strong> $50-$150+ per hour (varies by provider and intensity)</li>
        </ul>
        <p><strong>Intensive programs</strong> (20-40 hours/week) can cost $30,000-$70,000+ per year if paying out-of-pocket, which is why insurance or Regional Center funding is crucial.</p>`
      },
      {
        id: 'ins-4',
        question: 'Do I need a diagnosis to get ABA therapy?',
        answer: `<p><strong>Yes, typically you need a formal diagnosis, which varies by funding source:</strong></p>
        <ul>
          <li><strong>Insurance:</strong> Usually requires autism spectrum disorder (ASD) diagnosis from a licensed professional</li>
          <li><strong>Regional Center:</strong> Requires developmental disability diagnosis (autism, intellectual disability, cerebral palsy, epilepsy, or related condition)</li>
          <li><strong>School-Based:</strong> Requires IEP and educational need for ABA services</li>
        </ul>
        <p><strong>Who can diagnose?</strong> Psychologists, developmental pediatricians, psychiatrists, or neurologists can provide diagnoses. Regional Centers can also assess for developmental disabilities.</p>`
      },
      {
        id: 'ins-5',
        question: 'How do I get insurance authorization for ABA therapy?',
        answer: `<p><strong>Steps to get authorization:</strong></p>
        <ol>
          <li><strong>Get a Diagnosis:</strong> Obtain formal autism or developmental disability diagnosis</li>
          <li><strong>Choose a Provider:</strong> Select an in-network ABA provider</li>
          <li><strong>Assessment:</strong> Provider conducts initial assessment and creates treatment plan</li>
          <li><strong>Submit for Authorization:</strong> Provider submits treatment plan to insurance for approval</li>
          <li><strong>Wait for Approval:</strong> Typically 7-30 days (varies by insurer)</li>
          <li><strong>Begin Services:</strong> Once authorized, therapy can begin</li>
        </ol>
        <p><strong>Tip:</strong> Many providers handle the authorization process for you. Ask about this during your initial consultation.</p>`
      }
    ];

    const mapFaqs = [
      {
        id: 'map-1',
        question: 'How do I use the KINDD Provider Map?',
        answer: `<p><strong>It's easy! The map automatically finds providers near you:</strong></p>
        <ol>
          <li><strong>Allow Location Access:</strong> The map will request your location to show nearby providers</li>
          <li><strong>Or Enter ZIP Code:</strong> Type your ZIP code in the search bar</li>
          <li><strong>Browse Results:</strong> See providers listed by distance from your location</li>
          <li><strong>Filter:</strong> Use filters to narrow by insurance, therapy types, or Regional Center</li>
          <li><strong>Get Details:</strong> Click on a provider to see contact info, services, and directions</li>
        </ol>
        <p>The map shows your Regional Center boundary and all providers in your area.</p>`
      },
      {
        id: 'map-2',
        question: 'How is the map different from other provider directories?',
        answer: `<p><strong>KINDD offers unique features:</strong></p>
        <ul>
          <li><strong>Regional Center Integration:</strong> Shows which RC you're in based on ZIP code</li>
          <li><strong>Visual Map:</strong> See provider locations geographically, not just a list</li>
          <li><strong>Distance Calculation:</strong> Automatically calculates miles from your location</li>
          <li><strong>Comprehensive Filters:</strong> Search by insurance, therapy types, and more</li>
          <li><strong>Up-to-Date:</strong> Regularly updated provider information</li>
          <li><strong>Mobile-Friendly:</strong> Works great on phones and tablets</li>
          <li><strong>Free to Use:</strong> No registration or fees required</li>
        </ul>
        <p>Developed by Children's Hospital Los Angeles to help families find autism services quickly.</p>`
      },
      {
        id: 'map-3',
        question: 'Are all ABA providers in LA County listed on the map?',
        answer: `<p>We strive for comprehensive coverage, but the map may not include every provider. We currently have:</p>
        <ul>
          <li><strong>370+ verified providers</strong> across LA County</li>
          <li><strong>All 7 Regional Centers</strong> service areas covered</li>
          <li><strong>Major insurance networks</strong> represented</li>
          <li><strong>Clinic and in-home providers</strong> included</li>
        </ul>
        <p><strong>If you know a provider not listed:</strong> They can contact us to be added. We're continuously expanding our directory.</p>
        <p><strong>Note:</strong> Inclusion in the map does not constitute an endorsement. Families should verify credentials, licensing, and fit for their needs.</p>`
      },
      {
        id: 'map-4',
        question: 'How current is the provider information?',
        answer: `<p><strong>We work to keep information up-to-date:</strong></p>
        <ul>
          <li><strong>Regular Updates:</strong> Provider data is reviewed and updated periodically</li>
          <li><strong>Provider Submissions:</strong> Providers can update their own information</li>
          <li><strong>User Feedback:</strong> We incorporate corrections from families</li>
        </ul>
        <p><strong>Always verify</strong> key details (insurance acceptance, availability, services) directly with providers, as circumstances can change.</p>
        <p><strong>Found outdated information?</strong> Let us know so we can update our records.</p>`
      },
      {
        id: 'map-5',
        question: 'Is the KINDD map really free to use?',
        answer: `<p><strong>Yes, completely free!</strong></p>
        <ul>
          <li>✅ No registration required</li>
          <li>✅ No fees or subscriptions</li>
          <li>✅ No credit card needed</li>
          <li>✅ No hidden costs</li>
          <li>✅ Unlimited searches</li>
        </ul>
        <p>KINDD is a public service developed by Children's Hospital Los Angeles to help families in LA County find autism and developmental disability services.</p>
        <p><strong>Our mission:</strong> Make it easier for families to access the care their children need.</p>`
      }
    ];

    return {
      activeFaq,
      toggleFaq,
      abaTherapyFaqs,
      regionalCenterFaqs,
      insuranceFaqs,
      mapFaqs
    };
  }
};
</script>

<style scoped>
.faq-page {
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

/* FAQ Content */
.faq-content {
  padding: 3rem 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.faq-category {
  margin-bottom: 3rem;
}

.category-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #004877;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 3px solid #FFC923;
}

.faq-item {
  background: white;
  border-radius: 8px;
  margin-bottom: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: box-shadow 0.3s ease;
}

.faq-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.faq-question {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  background: none;
  border: none;
  text-align: left;
  font-size: 1.1rem;
  font-weight: 600;
  color: #212529;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.faq-question:hover {
  background-color: #f8f9fa;
}

.faq-question i {
  font-size: 1.25rem;
  color: #004877;
  transition: transform 0.3s ease;
}

.faq-answer {
  padding: 0 1.5rem 1.5rem 1.5rem;
  color: #495057;
  line-height: 1.7;
}

.faq-answer :deep(p) {
  margin-bottom: 1rem;
}

.faq-answer :deep(ul),
.faq-answer :deep(ol) {
  margin-left: 1.5rem;
  margin-bottom: 1rem;
}

.faq-answer :deep(li) {
  margin-bottom: 0.5rem;
}

.faq-answer :deep(strong) {
  color: #004877;
  font-weight: 600;
}

/* CTA Section */
.cta-section {
  background: linear-gradient(135deg, #004877 0%, #0D9DDB 100%);
  color: white;
  padding: 3rem;
  border-radius: 12px;
  text-align: center;
  margin-top: 3rem;
}

.cta-section h2 {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.cta-section p {
  font-size: 1.25rem;
  margin-bottom: 1.5rem;
  opacity: 0.9;
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

  .faq-content {
    padding: 2rem 0;
  }

  .category-title {
    font-size: 1.5rem;
  }

  .faq-question {
    font-size: 1rem;
    padding: 1rem;
  }

  .faq-answer {
    padding: 0 1rem 1rem 1rem;
  }

  .cta-section {
    padding: 2rem 1.5rem;
  }

  .cta-section h2 {
    font-size: 1.5rem;
  }

  .cta-section p {
    font-size: 1.1rem;
  }
}
</style>

