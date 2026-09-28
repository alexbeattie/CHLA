<template>
  <section class="app-download" :aria-labelledby="headingId">
    <h2 :id="headingId">{{ $t("about.downloadTitle") }}</h2>
    <p class="app-download-text">{{ $t("about.downloadText") }}</p>
    <ul class="app-download-list">
      <li>
        <a
          :href="iosUrl"
          class="app-download-link"
          target="_blank"
          rel="noopener noreferrer"
          :aria-label="$t('about.downloadIphoneLabel')"
        >
          <i class="bi bi-apple" aria-hidden="true"></i>
          <span>{{ $t("about.downloadIphone") }}</span>
        </a>
      </li>
      <li>
        <a
          :href="androidUrl"
          class="app-download-link"
          target="_blank"
          rel="noopener noreferrer"
          :aria-label="$t('about.downloadAndroidLabel')"
        >
          <i class="bi bi-google-play" aria-hidden="true"></i>
          <span>{{ $t("about.downloadAndroid") }}</span>
        </a>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { computed, useAttrs } from "vue";
import {
  ANDROID_PLAY_STORE_URL,
  IOS_APP_STORE_URL,
} from "@/seo/siteConfig";

defineProps({
  iosUrl: { type: String, default: IOS_APP_STORE_URL },
  androidUrl: { type: String, default: ANDROID_PLAY_STORE_URL },
});

const attrs = useAttrs();
const headingId = computed(() => `${attrs.id || "app-download"}-heading`);
</script>

<style scoped>
.app-download {
  scroll-margin-top: 76px;
}

.app-download h2 {
  font-size: 2rem;
  font-weight: 700;
  color: #004877;
  margin-bottom: 1rem;
}

.app-download-text {
  line-height: 1.7;
  color: #495057;
  margin-bottom: 1.25rem;
}

@media (max-width: 768px) {
  .app-download h2 {
    font-size: 1.75rem;
  }
}

.app-download-list {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.app-download-list li {
  flex: 1 1 220px;
  display: flex;
}

.app-download-link {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  min-height: 52px;
  padding: 0.75rem 1.25rem;
  border-radius: 10px;
  font-weight: 600;
  margin: 0;
}

.app-download-link {
  background-color: #004877;
  color: #ffffff;
  text-decoration: none;
  transition: background-color 0.2s ease;
}

.app-download-link:hover {
  background-color: #003355;
  color: #ffffff;
}

.app-download-link:focus-visible {
  outline: 3px solid #ffc923;
  outline-offset: 2px;
}

.app-download-link i {
  font-size: 1.35rem;
}

</style>
