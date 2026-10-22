<template>
  <div class="ca-rc-page">
    <header class="ca-rc-header">
      <div>
        <h1>{{ t("caRegionalCenters.title") }}</h1>
        <p class="ca-rc-lead">{{ t("caRegionalCenters.lead") }}</p>
      </div>
      <LanguageSwitcher class="dark" />
    </header>

    <div class="ca-rc-layout">
      <div class="ca-rc-map-wrap">
        <div ref="mapContainer" class="ca-rc-map" role="region" :aria-label="t('caRegionalCenters.mapLabel')"></div>
        <p v-if="loadError" class="ca-rc-map-error" role="alert">
          {{ t("caRegionalCenters.mapError") }}
        </p>
      </div>

      <aside class="ca-rc-panel">
        <section v-if="selected" class="ca-rc-detail" :style="{ borderLeftColor: selected.color }" aria-live="polite">
          <button type="button" class="ca-rc-back" @click="select(null)">
            <i class="bi bi-arrow-left" aria-hidden="true"></i>
            {{ t("caRegionalCenters.allCenters") }}
          </button>
          <h2>{{ selected.name }}</h2>
          <p class="ca-rc-counties">
            <strong>{{ t("caRegionalCenters.serves") }}</strong>
            {{ countiesLabel(selected) }}
          </p>
          <p class="ca-rc-office">
            <strong>{{ selected.officeType === "Main" ? t("caRegionalCenters.mainOffice") : t("caRegionalCenters.office") }}</strong>
            {{ selected.address }}, {{ selected.city }} {{ selected.zip }}
          </p>
          <div class="ca-rc-actions">
            <a :href="`tel:${selected.phone.replace(/[^\d]/g, '')}`" class="ca-rc-action">
              <i class="bi bi-telephone" aria-hidden="true"></i> {{ selected.phone }}
            </a>
            <a :href="selected.website" target="_blank" rel="noopener" class="ca-rc-action">
              <i class="bi bi-box-arrow-up-right" aria-hidden="true"></i>
              {{ t("caRegionalCenters.website") }}
            </a>
            <router-link v-if="selected.slug" :to="`/regional-centers/${selected.slug}`" class="ca-rc-action">
              <i class="bi bi-info-circle" aria-hidden="true"></i>
              {{ t("caRegionalCenters.laDetails") }}
            </router-link>
          </div>
        </section>

        <template v-else>
          <label for="ca-rc-search" class="ca-rc-search-label">{{ t("caRegionalCenters.searchLabel") }}</label>
          <input
            id="ca-rc-search"
            v-model="query"
            type="search"
            class="ca-rc-search"
            :placeholder="t('caRegionalCenters.searchPlaceholder')"
          />
          <ul class="ca-rc-list">
            <li v-for="rc in filtered" :key="rc.acronym">
              <button type="button" class="ca-rc-item" @click="select(rc.acronym)">
                <span class="ca-rc-swatch" :style="{ background: rc.color }" aria-hidden="true"></span>
                <span class="ca-rc-item-text">
                  <span class="ca-rc-item-name">{{ rc.name }}</span>
                  <span class="ca-rc-item-counties">{{ countiesLabel(rc) }}</span>
                </span>
              </button>
            </li>
            <li v-if="centers.length && !filtered.length" class="ca-rc-empty">
              {{ t("caRegionalCenters.noMatch") }}
            </li>
          </ul>
        </template>
      </aside>
    </div>

    <p class="ca-rc-note">{{ t("caRegionalCenters.note") }}</p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import LanguageSwitcher from "@/components/LanguageSwitcher.vue";
import { useCaliforniaRegionalCentersMap } from "@/composables/useCaliforniaRegionalCentersMap";
import { useSeo } from "@/composables/useSeo";

useSeo({ path: "/california-regional-centers" });

const { t } = useI18n();
const mapContainer = ref(null);
const query = ref("");
const { centers, selected, loadError, init, select } = useCaliforniaRegionalCentersMap(mapContainer);

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase();
  if (!q) return centers.value;
  return centers.value.filter(
    (rc) =>
      rc.name.toLowerCase().includes(q) ||
      rc.acronym.toLowerCase().includes(q) ||
      rc.counties.some((c) => c.toLowerCase().includes(q))
  );
});

function countiesLabel(rc) {
  if (rc.slug) return t("caRegionalCenters.partOfLa");
  return rc.counties.join(", ");
}

onMounted(() => init(import.meta.env.VITE_MAPBOX_TOKEN || ""));
</script>

<style scoped>
.ca-rc-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 20px 40px;
  color: #1f2937;
}

.ca-rc-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.ca-rc-header h1 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 6px;
}

.ca-rc-lead {
  margin: 0;
  color: #4b5563;
  max-width: 60ch;
}

.ca-rc-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  height: min(720px, 78vh);
}

.ca-rc-map-wrap {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f3f4f6;
}

.ca-rc-map {
  position: absolute;
  inset: 0;
}

.ca-rc-map-error {
  position: absolute;
  inset: auto 16px 16px;
  margin: 0;
  padding: 10px 14px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.ca-rc-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
  background: #fff;
}

.ca-rc-search-label {
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 6px;
}

.ca-rc-search {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  margin-bottom: 12px;
}

.ca-rc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  flex: 1;
}

.ca-rc-item {
  display: flex;
  gap: 10px;
  width: 100%;
  text-align: left;
  padding: 9px 8px;
  border: 0;
  border-radius: 10px;
  background: none;
  cursor: pointer;
}

.ca-rc-item:hover,
.ca-rc-item:focus-visible {
  background: #f3f4f6;
}

.ca-rc-swatch {
  flex: none;
  width: 14px;
  height: 14px;
  margin-top: 3px;
  border-radius: 4px;
}

.ca-rc-item-text {
  display: flex;
  flex-direction: column;
}

.ca-rc-item-name {
  font-weight: 600;
  font-size: 0.95rem;
}

.ca-rc-item-counties {
  font-size: 0.82rem;
  color: #6b7280;
}

.ca-rc-empty {
  padding: 8px;
  color: #6b7280;
}

.ca-rc-detail {
  border-left: 5px solid;
  padding-left: 14px;
  overflow-y: auto;
}

.ca-rc-back {
  border: 0;
  background: none;
  padding: 0;
  margin-bottom: 12px;
  color: #4f46e5;
  font-weight: 600;
  cursor: pointer;
}

.ca-rc-detail h2 {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0 0 10px;
}

.ca-rc-detail p {
  margin: 0 0 10px;
  font-size: 0.95rem;
}

.ca-rc-detail strong {
  display: block;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #6b7280;
}

.ca-rc-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.ca-rc-action {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  color: #1f2937;
  text-decoration: none;
  font-weight: 600;
}

.ca-rc-action:hover {
  background: #f9fafb;
}

.ca-rc-note {
  margin: 14px 0 0;
  font-size: 0.85rem;
  color: #6b7280;
}

@media (max-width: 900px) {
  .ca-rc-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .ca-rc-map-wrap {
    height: 60vh;
  }

  .ca-rc-panel {
    max-height: 70vh;
  }
}
</style>
