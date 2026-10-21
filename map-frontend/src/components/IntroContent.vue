<template>
  <div class="intro-content">
    <div class="intro-body">
      <div class="intro-header">
        <component :is="standalone ? 'h1' : 'h2'" class="intro-title">
          {{ t("intro.title") }}
        </component>
        <LanguageSwitcher class="dark" />
      </div>
      <p class="intro-lead">{{ t("intro.lead") }}</p>

      <section
        v-for="id in SECTION_IDS"
        :key="id"
        class="intro-section"
        :class="{ 'intro-section--urgent': id === 'urgent' }"
      >
        <component :is="standalone ? 'h2' : 'h3'" class="intro-section-title">
          <i v-if="id === 'urgent'" class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
          {{ t(`intro.sections.${id}.title`) }}
        </component>
        <i18n-t :keypath="`intro.sections.${id}.body`" tag="p" scope="global">
          <template #email>
            <a :href="`mailto:${SUPPORT_EMAIL}`" :aria-label="t('intro.emailLabel')">{{
              SUPPORT_EMAIL
            }}</a>
          </template>
          <template #privacyLink>
            <a href="/privacy" target="_blank" rel="noopener">{{ t("intro.privacyLink") }}</a>
          </template>
        </i18n-t>
      </section>
    </div>

    <div class="intro-footer">
      <button type="button" class="btn intro-continue" @click="emit('continue')">
        {{ buttonLabel }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from "vue-i18n";
import LanguageSwitcher from "@/components/LanguageSwitcher.vue";

defineProps({
  buttonLabel: { type: String, required: true },
  standalone: { type: Boolean, default: false },
});

const emit = defineEmits(["continue"]);
const { t } = useI18n();

const SUPPORT_EMAIL = "support@kinddhelp.org";
const SECTION_IDS = [
  "howItWorks",
  "services",
  "audience",
  "afterQuestions",
  "privacy",
  "language",
  "keepInMind",
  "providers",
  "urgent",
  "help",
];
</script>

<style scoped>
.intro-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  font-family: var(--kindd-font-family);
}

.intro-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px;
}

.intro-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.intro-title {
  color: #004877;
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 12px;
}

.intro-lead {
  font-size: 1.05rem;
  color: #333;
  margin-bottom: 20px;
}

.intro-section {
  margin-bottom: 16px;
}

.intro-section-title {
  font-size: 1rem;
  font-weight: 700;
  color: #004877;
  margin: 0 0 4px;
}

.intro-section p {
  margin: 0;
  color: #444;
  line-height: 1.55;
}

.intro-section--urgent {
  background: #fdecea;
  border-left: 4px solid #c62828;
  border-radius: 8px;
  padding: 12px 14px;
}

.intro-section--urgent .intro-section-title {
  color: #b71c1c;
}

.intro-section--urgent .bi {
  margin-right: 6px;
}

.intro-footer {
  padding: 16px 24px;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: flex-end;
}

.intro-continue {
  min-width: 160px;
  background: #004877;
  color: white;
  font-weight: 600;
}

.intro-continue:hover,
.intro-continue:focus-visible {
  background: #0d9ddb;
  color: white;
}

@media (max-width: 640px) {
  .intro-body {
    padding: 18px;
  }

  .intro-continue {
    width: 100%;
  }
}
</style>
