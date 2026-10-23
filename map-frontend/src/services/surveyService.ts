const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000'

export type Rating = 1 | 2 | 3 | 4 | 5
export type DiagnosisStatus = '' | 'under_evaluation' | 'not_yet_diagnosed'

export interface SurveyPayload {
  diagnosis: string
  diagnosis_status: DiagnosisStatus
  ease_of_use: Rating | null
  helpfulness: Rating | null
  difficulty_before: Rating | null
  confidence_after: Rating | null
  needs_addressed: Rating | null
  experience_before: string
  unmet_needs: string
  improvements: string
  additional_comments: string
  locale: string
  platform: 'web'
  app_version: string
}

export class SurveySubmitError extends Error {
  constructor(public readonly status: number) {
    super(`Survey submission failed with status ${status}`)
  }
}

export async function submitSurvey(payload: SurveyPayload): Promise<void> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}/api/surveys/responses/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
  } catch {
    throw new SurveySubmitError(0)
  }
  if (!response.ok) {
    throw new SurveySubmitError(response.status)
  }
}
