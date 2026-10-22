package com.chla.kindd.data.survey

import com.chla.kindd.data.api.SurveyResponseRequest

enum class SurveyTextQuestion(val maxLength: Int) {
    DIAGNOSIS(1000),
    EXPERIENCE_BEFORE(4000),
    UNMET_NEEDS(4000),
    IMPROVEMENTS(4000),
    ADDITIONAL_COMMENTS(4000)
}

enum class SurveyRatingQuestion {
    EASE_OF_USE,
    HELPFULNESS,
    DIFFICULTY_BEFORE,
    CONFIDENCE_AFTER,
    NEEDS_ADDRESSED;

    companion object {
        val ratingRange = 1..5
    }
}

enum class DiagnosisStatus(val apiValue: String) {
    UNDER_EVALUATION("under_evaluation"),
    NOT_YET_DIAGNOSED("not_yet_diagnosed")
}

data class SurveyAnswers(
    val texts: Map<SurveyTextQuestion, String> = emptyMap(),
    val ratings: Map<SurveyRatingQuestion, Int> = emptyMap(),
    val diagnosisStatus: DiagnosisStatus? = null
) {
    val isEmpty: Boolean
        get() = texts.values.all(String::isBlank) && ratings.isEmpty() && diagnosisStatus == null

    fun text(question: SurveyTextQuestion): String = texts[question].orEmpty()

    fun toRequest(locale: String, appVersion: String): SurveyResponseRequest = SurveyResponseRequest(
        diagnosis = answerText(SurveyTextQuestion.DIAGNOSIS),
        diagnosisStatus = diagnosisStatus?.apiValue,
        easeOfUse = ratings[SurveyRatingQuestion.EASE_OF_USE],
        helpfulness = ratings[SurveyRatingQuestion.HELPFULNESS],
        difficultyBefore = ratings[SurveyRatingQuestion.DIFFICULTY_BEFORE],
        confidenceAfter = ratings[SurveyRatingQuestion.CONFIDENCE_AFTER],
        needsAddressed = ratings[SurveyRatingQuestion.NEEDS_ADDRESSED],
        experienceBefore = answerText(SurveyTextQuestion.EXPERIENCE_BEFORE),
        unmetNeeds = answerText(SurveyTextQuestion.UNMET_NEEDS),
        improvements = answerText(SurveyTextQuestion.IMPROVEMENTS),
        additionalComments = answerText(SurveyTextQuestion.ADDITIONAL_COMMENTS),
        locale = locale,
        appVersion = appVersion
    )

    private fun answerText(question: SurveyTextQuestion): String? =
        texts[question]?.trim()?.take(question.maxLength)?.takeIf(String::isNotEmpty)
}

enum class SurveySubmitResult {
    SUCCESS,
    RATE_LIMITED,
    FAILED
}

fun surveyLocale(languageTag: String): String =
    if (languageTag.lowercase().startsWith("es")) "es" else "en"
