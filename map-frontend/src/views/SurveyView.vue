<template>
  <div class="survey-page">
    <div class="survey-card">
      <header class="survey-header">
        <div>
          <h1>{{ t("survey.title") }}</h1>
          <p class="survey-lead">{{ t("survey.lead") }}</p>
        </div>
        <LanguageSwitcher class="dark" />
      </header>

      <div v-if="status === 'success'" class="survey-success" role="status">
        <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
        <p>{{ t("survey.success") }}</p>
        <router-link to="/" class="survey-submit">{{
          t("survey.backToMap")
        }}</router-link>
      </div>

      <form
        v-else
        class="survey-form"
        novalidate
        @submit.prevent="submit(locale)"
      >
        <div class="survey-question">
          <label for="survey-diagnosis">{{
            t("survey.questions.diagnosis")
          }}</label>
          <input
            id="survey-diagnosis"
            v-model="answers.diagnosis"
            type="text"
            class="survey-field"
            maxlength="1000"
          />
        </div>

        <fieldset class="survey-question">
          <legend>{{ t("survey.questions.diagnosisStatus") }}</legend>
          <div class="choice-group">
            <label
              v-for="value in DIAGNOSIS_STATUSES"
              :key="value"
              class="choice"
            >
              <input
                :id="`survey-status-${value}`"
                v-model="answers.diagnosis_status"
                class="choice-input"
                type="radio"
                name="diagnosis_status"
                :value="value"
              />
              <span class="choice-mark" aria-hidden="true"></span>
              <span class="choice-label">{{
                t(`survey.diagnosisStatus.${value}`)
              }}</span>
            </label>
            <button
              v-if="answers.diagnosis_status"
              type="button"
              class="survey-clear"
              @click="answers.diagnosis_status = ''"
            >
              {{ t("survey.clear") }}
            </button>
          </div>
        </fieldset>

        <fieldset
          v-for="question in RATING_QUESTIONS"
          :key="question.field"
          class="survey-question"
        >
          <legend>{{ t(`survey.questions.${question.key}`) }}</legend>
          <div class="choice-group">
            <label
              v-for="(label, index) in tm(`survey.scales.${question.scale}`)"
              :key="index"
              class="choice"
            >
              <input
                :id="`survey-${question.field}-${index + 1}`"
                v-model="answers[question.field]"
                class="choice-input"
                type="radio"
                :name="question.field"
                :value="index + 1"
              />
              <span class="choice-mark" aria-hidden="true"></span>
              <span class="choice-label">{{ rt(label) }}</span>
            </label>
            <button
              v-if="answers[question.field] !== null"
              type="button"
              class="survey-clear"
              @click="answers[question.field] = null"
            >
              {{ t("survey.clear") }}
            </button>
          </div>
        </fieldset>

        <div
          v-for="question in OPEN_QUESTIONS"
          :key="question.field"
          class="survey-question"
        >
          <label :for="`survey-${question.field}`">{{
            t(`survey.questions.${question.key}`)
          }}</label>
          <textarea
            :id="`survey-${question.field}`"
            v-model="answers[question.field]"
            class="survey-field"
            rows="4"
            maxlength="4000"
          ></textarea>
        </div>

        <p v-if="statusMessage" class="survey-alert" role="alert">
          {{ statusMessage }}
        </p>

        <button type="submit" class="survey-submit" :disabled="isSubmitting">
          {{ isSubmitting ? t("survey.submitting") : t("survey.submit") }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import LanguageSwitcher from "@/components/LanguageSwitcher.vue";
import { useSeo } from "@/composables/useSeo";
import {
  OPEN_QUESTIONS,
  RATING_QUESTIONS,
  useSurvey,
} from "@/composables/useSurvey";

useSeo({ path: "/survey" });

const DIAGNOSIS_STATUSES = ["under_evaluation", "not_yet_diagnosed"];
const STATUS_MESSAGE_KEYS = {
  empty: "survey.empty",
  error: "survey.error",
  rateLimited: "survey.rateLimited",
};

const { t, tm, rt, locale } = useI18n();
const { answers, status, isSubmitting, submit } = useSurvey();

watch(status, (value) => {
  if (value === "success") window.scrollTo({ top: 0 });
});

const statusMessage = computed(() => {
  const key = STATUS_MESSAGE_KEYS[status.value];
  return key ? t(key) : "";
});
</script>

<style scoped>
.survey-page {
  min-height: 100vh;
  background: #f2f2f7;
  padding: 2.5rem 1.25rem 4rem;
  font-family: var(--kindd-font-family);
  color: #1d1d1f;
  -webkit-font-smoothing: antialiased;
}

.survey-card {
  max-width: 680px;
  margin: 0 auto;
  background: #fff;
  border-radius: 20px;
  box-shadow:
    0 1px 1px rgba(0, 0, 0, 0.04),
    0 12px 32px rgba(0, 0, 0, 0.06);
  padding: 1.75rem 1.5rem 1.5rem;
}

.survey-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.75rem;
}

.survey-header h1 {
  color: #004877;
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.022em;
  line-height: 1.15;
  margin: 0 0 0.45rem;
}

.survey-lead {
  color: #6e6e73;
  margin: 0;
  max-width: 38rem;
  font-size: 0.95rem;
  line-height: 1.45;
}

.survey-question {
  border: 0;
  margin: 0 0 1.75rem;
  padding: 0;
  min-inline-size: 0;
}

.survey-question legend,
.survey-question > label {
  display: block;
  float: none;
  width: auto;
  padding: 0;
  font-size: 1.0625rem;
  font-weight: 600;
  letter-spacing: -0.014em;
  color: #1d1d1f;
  margin-bottom: 0.55rem;
  line-height: 1.35;
}

.survey-field {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 44px;
  padding: 0.7rem 0.9rem;
  border: 0;
  border-radius: 12px;
  background: #f5f5f7;
  color: #1d1d1f;
  font: inherit;
  font-size: 1rem;
  line-height: 1.4;
}

textarea.survey-field {
  min-height: 7.5rem;
  resize: vertical;
}

.survey-field:focus {
  outline: none;
  background: #fff;
  box-shadow:
    0 0 0 1px #004877,
    0 0 0 4px rgba(0, 72, 119, 0.16);
}

.choice-group {
  background: #f5f5f7;
  border-radius: 12px;
  overflow: hidden;
}

.choice {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-height: 44px;
  margin: 0;
  padding: 0.6rem 0.9rem;
  cursor: pointer;
}

.choice + .choice::before,
.survey-clear::before {
  content: "";
  position: absolute;
  top: 0;
  left: calc(0.9rem + 22px + 0.75rem);
  right: 0;
  height: 0.5px;
  background: rgba(60, 60, 67, 0.29);
}

.choice:active {
  background: rgba(0, 0, 0, 0.045);
}

.choice:has(.choice-input:checked) {
  background: #e8f3f8;
}

.choice:has(.choice-input:focus-visible) {
  outline: 2px solid #004877;
  outline-offset: -2px;
}

.choice-input {
  position: absolute;
  opacity: 0;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  cursor: pointer;
}

.choice-input:focus {
  outline: none;
}

.choice-mark {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 50%;
  border: 1.5px solid #c7c7cc;
  background: #fff;
  display: grid;
  place-items: center;
}

.choice-mark::after {
  content: "";
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
  transform: scale(0.4);
  transition: transform 120ms ease-out;
}

.choice:has(.choice-input:checked) .choice-mark {
  border-color: #004877;
  background: #004877;
}

.choice:has(.choice-input:checked) .choice-mark::after {
  transform: scale(1);
}

.choice-label {
  font-size: 1rem;
  font-weight: 400;
  line-height: 1.3;
  letter-spacing: -0.011em;
  color: #1d1d1f;
}

.survey-clear {
  position: relative;
  appearance: none;
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 44px;
  margin: 0;
  padding: 0.55rem 0.9rem 0.55rem calc(0.9rem + 22px + 0.75rem);
  border: 0;
  background: transparent;
  color: #004877;
  font: inherit;
  font-size: 1rem;
  font-weight: 500;
  letter-spacing: -0.011em;
  text-align: left;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.survey-clear:hover {
  color: #003a61;
}

.survey-clear:active {
  background: rgba(0, 0, 0, 0.045);
}

.survey-clear:focus-visible {
  outline: 2px solid #004877;
  outline-offset: -2px;
}

.survey-alert {
  background: #fff4e5;
  border-radius: 12px;
  padding: 0.85rem 1rem;
  color: #663c00;
  font-size: 0.95rem;
  line-height: 1.4;
  margin: 0 0 1rem;
}

.survey-submit {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 50px;
  margin-top: 0.25rem;
  border: 0;
  border-radius: 14px;
  background: #004877;
  color: #fff;
  font: inherit;
  font-size: 1.0625rem;
  font-weight: 600;
  letter-spacing: -0.015em;
  text-decoration: none;
  cursor: pointer;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
  transition:
    transform 100ms ease-out,
    background-color 100ms ease-out;
}

.survey-submit:hover {
  background: #003a61;
  color: #fff;
}

.survey-submit:active {
  transform: scale(0.985);
}

.survey-submit:focus-visible {
  outline: 2px solid #004877;
  outline-offset: 3px;
}

.survey-submit:disabled {
  opacity: 0.45;
  cursor: default;
  transform: none;
}

.survey-success {
  text-align: center;
  padding: 2.5rem 0.5rem 1rem;
}

.survey-success .bi {
  display: inline-grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #e8f6ee;
  font-size: 2rem;
  color: #1f7a3a;
}

.survey-success p {
  font-size: 1.15rem;
  font-weight: 600;
  letter-spacing: -0.016em;
  line-height: 1.35;
  margin: 1.15rem auto 1.5rem;
  max-width: 28rem;
}

.survey-success .survey-submit {
  width: auto;
  min-width: 12.5rem;
  margin-left: auto;
  margin-right: auto;
  padding: 0 1.5rem;
}

@media (max-width: 640px) {
  .survey-page {
    padding: 0 0 2.5rem;
    background: #fff;
  }

  .survey-card {
    max-width: none;
    border-radius: 0;
    box-shadow: none;
    padding: 1.25rem 1.15rem 1.5rem;
  }

  .survey-header {
    flex-direction: column-reverse;
  }
}

@media (prefers-reduced-motion: reduce) {
  .choice-mark::after,
  .survey-submit {
    transition: none;
  }

  .survey-submit:active {
    transform: none;
    opacity: 0.82;
  }
}

@media (prefers-contrast: more) {
  .choice-group,
  .survey-field {
    background: #fff;
    box-shadow: inset 0 0 0 1px #1d1d1f;
  }

  .choice-mark {
    border-color: #1d1d1f;
  }
}
</style>
