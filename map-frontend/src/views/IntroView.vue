<template>
  <div class="intro-page">
    <div class="intro-card">
      <IntroContent standalone :button-label="t('intro.done')" @continue="router.push('/')" />
    </div>
  </div>
</template>

<script setup>
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import IntroContent from "@/components/IntroContent.vue";
import { useSeo } from "@/composables/useSeo";

useSeo({ path: "/intro" });

const { t } = useI18n();
const router = useRouter();
</script>

<style scoped>
.intro-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 2rem 1rem;
}

.intro-card {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}
</style>
