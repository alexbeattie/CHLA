<template>
  <main class="clinicians-view">
    <section class="hero">
      <div>
        <p class="eyebrow">Clinician workspace</p>
        <h1>Regional Center and provider referral support</h1>
        <p class="lede">
          Quickly orient a family by ZIP code, confirm their Regional Center,
          and jump back into provider search with a referral-ready context.
        </p>
      </div>
      <router-link class="primary-action" to="/">
        Open map
        <i class="bi bi-arrow-right"></i>
      </router-link>
    </section>

    <section class="lookup-card">
      <div class="lookup-copy">
        <h2>Find the family's Regional Center</h2>
        <p>
          Enter a Los Angeles County ZIP code to show the matching Regional
          Center color and referral path.
        </p>
      </div>
      <form class="zip-form" novalidate @submit.prevent="lookupZip">
        <input
          v-model.trim="zipCode"
          inputmode="numeric"
          maxlength="5"
          pattern="[0-9]{5}"
          placeholder="ZIP code"
          aria-label="ZIP code"
          @input="normalizeZipInput"
        />
        <button type="submit" :disabled="lookupLoading">
          <span v-if="lookupLoading">Looking up...</span>
          <span v-else>Lookup</span>
        </button>
      </form>

      <div
        v-if="matchedCenter"
        class="match-result"
        :style="{ '--rc-color': matchedCenter.color }"
      >
        <div class="rc-badge">{{ matchedCenter.abbreviation }}</div>
        <div>
          <p class="match-label">Regional Center</p>
          <h3>{{ matchedCenter.name }}</h3>
          <p class="match-meta" v-if="matchedCenter.phone">
            {{ matchedCenter.phone }}
          </p>
        </div>
        <a
          v-if="matchedCenter.website"
          class="tertiary-action"
          :href="formatWebsite(matchedCenter.website)"
          target="_blank"
          rel="noopener noreferrer"
        >
          Visit website
        </a>
        <router-link
          class="secondary-action"
          :to="{
            path: '/',
            query: {
              q: zipCode,
              regionalCenter: matchedCenter.name,
            },
          }"
        >
          Search providers for this ZIP
        </router-link>
      </div>

      <p v-else-if="lookupError" class="no-match">
        {{ lookupError }}
      </p>

      <p v-else-if="lookupAttempted" class="no-match">
        We could not match that ZIP to a configured LA County Regional Center.
      </p>
    </section>

    <section class="workflow-grid">
      <article
        v-for="item in workflowItems"
        :key="item.title"
        class="workflow-card"
      >
        <i :class="item.icon"></i>
        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { getRegionalCentersList } from "../constants/regionalCenters";
import { useSeo } from "../composables/useSeo";
import { getApiRoot } from "../utils/api";

useSeo({ path: "/clinicians" });

const API_BASE_URL = getApiRoot() || "http://127.0.0.1:8002";

interface ClinicianRegionalCenterMatch {
  name: string;
  abbreviation: string;
  color: string;
  phone?: string;
  website?: string;
}

const zipCode = ref("");
const lookupAttempted = ref(false);
const lookupLoading = ref(false);
const lookupError = ref("");
const matchedApiCenter = ref<any | null>(null);

const matchedCenter = computed<ClinicianRegionalCenterMatch | null>(() => {
  if (!matchedApiCenter.value?.regional_center) {
    return null;
  }

  const centerName = matchedApiCenter.value.regional_center;
  const localCenter = getRegionalCentersList().find(
    (center) => center.name === centerName
  );

  return {
    name: centerName,
    abbreviation: localCenter?.abbreviation || "RC",
    color: localCenter?.color || "#0d9ddb",
    phone: matchedApiCenter.value.telephone || matchedApiCenter.value.phone,
    website: matchedApiCenter.value.website,
  };
});

const workflowItems = [
  {
    icon: "bi bi-signpost-split",
    title: "Confirm the referral region",
    description:
      "Use ZIP code context before suggesting Regional Center or provider next steps.",
  },
  {
    icon: "bi bi-funnel",
    title: "Narrow provider options",
    description:
      "Return to the map with location context and layer on therapy, age, and funding filters.",
  },
  {
    icon: "bi bi-chat-square-heart",
    title: "Explain the handoff",
    description:
      "Give families a clear Regional Center name and provider-search starting point.",
  },
];

async function lookupZip() {
  lookupAttempted.value = true;
  lookupError.value = "";
  matchedApiCenter.value = null;

  if (!/^\d{5}$/.test(zipCode.value)) {
    lookupError.value = "Enter a valid 5-digit ZIP code.";
    return;
  }

  lookupLoading.value = true;
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/regional-centers/by_zip_code/?zip_code=${zipCode.value}`,
      { headers: { Accept: "application/json" } }
    );

    if (response.status === 404) {
      return;
    }

    if (!response.ok) {
      throw new Error(`Regional Center lookup failed (${response.status})`);
    }

    matchedApiCenter.value = await response.json();
  } catch (error) {
    console.error("Clinician ZIP lookup failed:", error);
    lookupError.value =
      "We could not complete the ZIP lookup. Please try again.";
  } finally {
    lookupLoading.value = false;
  }
}

function formatWebsite(website?: string) {
  if (!website) return "";
  return website.startsWith("http") ? website : `https://${website}`;
}

function normalizeZipInput() {
  zipCode.value = zipCode.value.replace(/\D/g, "").slice(0, 5);
}
</script>

<style scoped>
.clinicians-view {
  min-height: 100vh;
  padding: 96px 24px 48px;
  background: #f8fafc;
  color: #1f2937;
}

.hero,
.lookup-card,
.workflow-card {
  max-width: 1080px;
  margin: 0 auto;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 40px;
}

.eyebrow,
.match-label {
  margin: 0 0 8px;
  color: #0d9ddb;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  max-width: 720px;
  font-size: clamp(2rem, 4vw, 3.4rem);
  line-height: 1.04;
}

.lede {
  max-width: 680px;
  color: #4b5563;
  font-size: 1.08rem;
}

.primary-action,
.secondary-action,
.tertiary-action,
.zip-form button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 999px;
  background: #004877;
  color: #ffffff;
  cursor: pointer;
  font-weight: 800;
  padding: 12px 18px;
  text-decoration: none;
}

.zip-form button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.tertiary-action {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #004877;
  font-weight: 700;
}

.lookup-card {
  margin-top: 24px;
  padding: 32px;
}

.lookup-copy p {
  color: #6b7280;
}

.zip-form {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.zip-form input {
  width: 180px;
  border: 1px solid #cbd5e1;
  border-radius: 14px;
  font-size: 1.3rem;
  font-weight: 800;
  letter-spacing: 0.16em;
  padding: 12px 14px;
  text-align: center;
}

.match-result {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 24px;
  padding: 18px;
  border: 2px solid var(--rc-color);
  border-radius: 18px;
  background: color-mix(in srgb, var(--rc-color) 10%, #ffffff);
}

.match-meta {
  margin: 6px 0 0;
  color: #4b5563;
  font-weight: 700;
}

.rc-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 999px;
  background: var(--rc-color);
  box-shadow: 0 0 0 4px #ffffff,
    0 0 0 8px color-mix(in srgb, var(--rc-color) 32%, transparent);
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 900;
}

.match-result .secondary-action {
  margin-left: auto;
}

.no-match {
  margin: 18px 0 0;
  color: #b45309;
  font-weight: 700;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  max-width: 1080px;
  margin: 24px auto 0;
}

.workflow-card {
  margin: 0;
  padding: 24px;
}

.workflow-card i {
  color: #0d9ddb;
  font-size: 1.6rem;
}

.workflow-card p {
  color: #6b7280;
}

@media (max-width: 800px) {
  .hero,
  .match-result,
  .zip-form {
    align-items: stretch;
    flex-direction: column;
  }

  .match-result .secondary-action {
    margin-left: 0;
  }

  .workflow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
