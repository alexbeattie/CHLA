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
        <router-link to="/" class="btn survey-submit">{{
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
            class="form-control"
            maxlength="1000"
          />
        </div>

        <fieldset class="survey-question">
          <legend>{{ t("survey.questions.diagnosisStatus") }}</legend>
          <div
            v-for="value in DIAGNOSIS_STATUSES"
            :key="value"
            class="form-check"
          >
            <input
              :id="`survey-status-${value}`"
              v-model="answers.diagnosis_status"
              class="form-check-input"
              type="radio"
              name="diagnosis_status"
              :value="value"
            />
            <label class="form-check-label" :for="`survey-status-${value}`">
              {{ t(`survey.diagnosisStatus.${value}`) }}
            </label>
          </div>
          <button
            v-if="answers.diagnosis_status"
            type="button"
            class="btn btn-link survey-clear"
            @click="answers.diagnosis_status = ''"
          >
            {{ t("survey.clear") }}
          </button>
        </fieldset>

        <fieldset
          v-for="question in RATING_QUESTIONS"
          :key="question.field"
          class="survey-question"
        >
          <legend>{{ t(`survey.questions.${question.key}`) }}</legend>
          <div
            v-for="(label, index) in tm(`survey.scales.${question.scale}`)"
            :key="index"
            class="form-check"
          >
            <input
              :id="`survey-${question.field}-${index + 1}`"
              v-model="answers[question.field]"
              class="form-check-input"
              type="radio"
              :name="question.field"
              :value="index + 1"
            />
            <label
              class="form-check-label"
              :for="`survey-${question.field}-${index + 1}`"
            >
              {{ rt(label) }}
            </label>
          </div>
          <button
            v-if="answers[question.field] !== null"
            type="button"
            class="btn btn-link survey-clear"
            @click="answers[question.field] = null"
          >
            {{ t("survey.clear") }}
          </button>
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
            class="form-control"
            rows="4"
            maxlength="4000"
          ></textarea>
        </div>

        <p v-if="statusMessage" class="survey-alert" role="alert">
          {{ statusMessage }}
        </p>

        <button
          type="submit"
          class="btn survey-submit"
          :disabled="isSubmitting"
        >
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
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 2rem 1rem;
  font-family: var(--kindd-font-family);
}

.survey-card {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  padding: 2rem;
}

.survey-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.survey-header h1 {
  color: #004877;
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
}

.survey-lead {
  color: #475569;
  margin: 0;
}

.survey-question {
  border: 0;
  border-top: 1px solid #e2e8f0;
  padding: 1.25rem 0 0;
  margin: 0 0 1.25rem;
}

.survey-question legend,
.survey-question > label {
  display: block;
  float: none;
  width: auto;
  font-size: 1rem;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 0.75rem;
  line-height: 1.45;
}

.form-check {
  padding-top: 0.2rem;
  padding-bottom: 0.2rem;
}

.survey-clear {
  padding: 0.25rem 0;
  font-size: 0.9rem;
}

.survey-alert {
  background: #fff4e5;
  border-left: 4px solid #ed6c02;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  color: #663c00;
}

.survey-submit {
  background: #004877;
  color: white;
  font-weight: 600;
  padding: 0.65rem 1.5rem;
}

.survey-submit:hover,
.survey-submit:focus-visible {
  background: #0d9ddb;
  color: white;
}

.survey-success {
  text-align: center;
  padding: 2rem 0;
}

.survey-success .bi {
  font-size: 2.5rem;
  color: #2e7d32;
}

.survey-success p {
  font-size: 1.1rem;
  margin: 1rem 0 1.5rem;
}

@media (max-width: 640px) {
  .survey-card {
    padding: 1.25rem;
  }

  .survey-header {
    flex-direction: column-reverse;
  }

  .survey-submit {
    width: 100%;
  }
}
</style>
