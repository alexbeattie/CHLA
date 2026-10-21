import { computed, reactive, ref, watch } from 'vue'
import {
  submitSurvey,
  SurveySubmitError,
  type DiagnosisStatus,
  type Rating,
  type SurveyPayload,
} from '@/services/surveyService'

export const RATING_QUESTIONS = [
  { field: 'ease_of_use', key: 'easeOfUse', scale: 'ease' },
  { field: 'helpfulness', key: 'helpfulness', scale: 'helpfulness' },
  { field: 'difficulty_before', key: 'difficultyBefore', scale: 'ease' },
  { field: 'confidence_after', key: 'confidenceAfter', scale: 'confidence' },
  { field: 'needs_addressed', key: 'needsAddressed', scale: 'extent' },
] as const

export const OPEN_QUESTIONS = [
  { field: 'experience_before', key: 'experienceBefore' },
  { field: 'unmet_needs', key: 'unmetNeeds' },
  { field: 'improvements', key: 'improvements' },
  { field: 'additional_comments', key: 'additionalComments' },
] as const

type RatingField = (typeof RATING_QUESTIONS)[number]['field']
type OpenField = (typeof OPEN_QUESTIONS)[number]['field']

export type SurveyAnswers = {
  diagnosis: string
  diagnosis_status: DiagnosisStatus
} & Record<RatingField, Rating | null> &
  Record<OpenField, string>

export type SurveyStatus = 'idle' | 'submitting' | 'success' | 'empty' | 'error' | 'rateLimited'

export function emptyAnswers(): SurveyAnswers {
  return {
    diagnosis: '',
    diagnosis_status: '',
    ease_of_use: null,
    helpfulness: null,
    difficulty_before: null,
    confidence_after: null,
    needs_addressed: null,
    experience_before: '',
    unmet_needs: '',
    improvements: '',
    additional_comments: '',
  }
}

export function hasAnyAnswer(answers: SurveyAnswers): boolean {
  return Object.values(answers).some((value) =>
    typeof value === 'string' ? value.trim() !== '' : value !== null
  )
}

export function buildSurveyPayload(answers: SurveyAnswers, locale: string): SurveyPayload {
  return {
    ...answers,
    diagnosis: answers.diagnosis.trim(),
    experience_before: answers.experience_before.trim(),
    unmet_needs: answers.unmet_needs.trim(),
    improvements: answers.improvements.trim(),
    additional_comments: answers.additional_comments.trim(),
    locale,
    platform: 'web',
    app_version: '',
  }
}

export function useSurvey() {
  const answers = reactive<SurveyAnswers>(emptyAnswers())
  const status = ref<SurveyStatus>('idle')
  const isSubmitting = computed(() => status.value === 'submitting')

  watch(answers, () => {
    if (status.value === 'empty') status.value = 'idle'
  })

  async function submit(locale: string) {
    if (!hasAnyAnswer(answers)) {
      status.value = 'empty'
      return
    }
    status.value = 'submitting'
    try {
      await submitSurvey(buildSurveyPayload(answers, locale))
      Object.assign(answers, emptyAnswers())
      status.value = 'success'
    } catch (error) {
      const rateLimited = error instanceof SurveySubmitError && error.status === 429
      status.value = rateLimited ? 'rateLimited' : 'error'
    }
  }

  return { answers, status, isSubmitting, submit }
}
