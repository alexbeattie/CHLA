<template>
  <div class="faq-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="container">
        <h1 class="page-title">{{ $t("faq.title") }}</h1>
        <p class="page-subtitle">
          {{ $t("faq.subtitle") }}
        </p>
      </div>
    </section>

    <!-- FAQ Content -->
    <section class="faq-content">
      <div class="container">
        <!-- ABA Therapy Questions -->
        <div class="faq-category">
          <h2 class="category-title">{{ $t("faq.abaTherapyBasics") }}</h2>

          <div class="faq-item" v-for="faq in abaTherapyFaqs" :key="faq.id">
            <button
              class="faq-question"
              @click="toggleFaq(faq.id)"
              :aria-expanded="activeFaq === faq.id"
            >
              <span>{{ faq.question }}</span>
              <i
                class="bi"
                :class="
                  activeFaq === faq.id ? 'bi-chevron-up' : 'bi-chevron-down'
                "
              ></i>
            </button>
            <div class="faq-answer" v-show="activeFaq === faq.id">
              <div v-html="faq.answer"></div>
            </div>
          </div>
        </div>

        <!-- Regional Centers Questions -->
        <div class="faq-category">
          <h2 class="category-title">{{ $t("nav.regionalCenters") }}</h2>

          <div class="faq-item" v-for="faq in regionalCenterFaqs" :key="faq.id">
            <button
              class="faq-question"
              @click="toggleFaq(faq.id)"
              :aria-expanded="activeFaq === faq.id"
            >
              <span>{{ faq.question }}</span>
              <i
                class="bi"
                :class="
                  activeFaq === faq.id ? 'bi-chevron-up' : 'bi-chevron-down'
                "
              ></i>
            </button>
            <div class="faq-answer" v-show="activeFaq === faq.id">
              <div v-html="faq.answer"></div>
            </div>
          </div>
        </div>

        <!-- Insurance & Funding Questions -->
        <div class="faq-category">
          <h2 class="category-title">{{ $t("faq.insuranceFunding") }}</h2>

          <div class="faq-item" v-for="faq in insuranceFaqs" :key="faq.id">
            <button
              class="faq-question"
              @click="toggleFaq(faq.id)"
              :aria-expanded="activeFaq === faq.id"
            >
              <span>{{ faq.question }}</span>
              <i
                class="bi"
                :class="
                  activeFaq === faq.id ? 'bi-chevron-up' : 'bi-chevron-down'
                "
              ></i>
            </button>
            <div class="faq-answer" v-show="activeFaq === faq.id">
              <div v-html="faq.answer"></div>
            </div>
          </div>
        </div>

        <!-- Using the Map Questions -->
        <div class="faq-category">
          <h2 class="category-title">{{ $t("faq.usingTheMap") }}</h2>

          <div class="faq-item" v-for="faq in mapFaqs" :key="faq.id">
            <button
              class="faq-question"
              @click="toggleFaq(faq.id)"
              :aria-expanded="activeFaq === faq.id"
            >
              <span>{{ faq.question }}</span>
              <i
                class="bi"
                :class="
                  activeFaq === faq.id ? 'bi-chevron-up' : 'bi-chevron-down'
                "
              ></i>
            </button>
            <div class="faq-answer" v-show="activeFaq === faq.id">
              <div v-html="faq.answer"></div>
            </div>
          </div>
        </div>

        <!-- CTA Section -->
        <div class="cta-section">
          <h2>{{ $t("about.readyToFind") }}</h2>
          <p>{{ $t("about.startSearch") }}</p>
          <router-link to="/" class="btn btn-primary btn-lg">
            <i class="bi bi-map me-2"></i>
            {{ $t("about.exploreMap") }}
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useSeo } from "@/composables/useSeo";

export default {
  name: "FaqView",

  setup() {
    const { t } = useI18n();
    const activeFaq = ref(null);

    useSeo({ path: "/faq" });

    const toggleFaq = (id) => {
      activeFaq.value = activeFaq.value === id ? null : id;
    };

    const abaTherapyFaqs = computed(() => [
      { id: "aba-1", question: t("faq.aba1Q"), answer: t("faq.aba1A") },
      { id: "aba-2", question: t("faq.aba2Q"), answer: t("faq.aba2A") },
      { id: "aba-3", question: t("faq.aba3Q"), answer: t("faq.aba3A") },
    ]);

    const regionalCenterFaqs = computed(() => [
      { id: "rc-1", question: t("faq.rc1Q"), answer: t("faq.rc1A") },
      { id: "rc-2", question: t("faq.rc2Q"), answer: t("faq.rc2A") },
      { id: "rc-3", question: t("faq.rc3Q"), answer: t("faq.rc3A") },
    ]);

    const insuranceFaqs = computed(() => [
      { id: "ins-1", question: t("faq.ins1Q"), answer: t("faq.ins1A") },
      { id: "ins-2", question: t("faq.ins2Q"), answer: t("faq.ins2A") },
    ]);

    const mapFaqs = computed(() => [
      { id: "map-1", question: t("faq.map1Q"), answer: t("faq.map1A") },
      { id: "map-2", question: t("faq.map2Q"), answer: t("faq.map2A") },
    ]);

    return {
      activeFaq,
      toggleFaq,
      abaTherapyFaqs,
      regionalCenterFaqs,
      insuranceFaqs,
      mapFaqs,
    };
  },
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
  border-bottom: 3px solid #ffc923;
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
  gap: 1rem;
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

.faq-question span {
  flex: 1;
}

.faq-question i {
  flex-shrink: 0;
  font-size: 1.5rem;
  color: #004877;
  transition: transform 0.3s ease;
  padding: 0.5rem;
  margin: -0.5rem;
  border-radius: 50%;
}

.faq-question:hover i {
  background-color: rgba(0, 72, 119, 0.1);
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
  background: linear-gradient(135deg, #004877 0%, #0d9ddb 100%);
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
  background-color: #ffc923;
  border-color: #ffc923;
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
