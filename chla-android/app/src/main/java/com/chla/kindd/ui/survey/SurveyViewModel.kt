package com.chla.kindd.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.BuildConfig
import com.chla.kindd.data.survey.DiagnosisStatus
import com.chla.kindd.data.survey.SurveyAnswers
import com.chla.kindd.data.survey.SurveyRatingQuestion
import com.chla.kindd.data.survey.SurveyRepository
import com.chla.kindd.data.survey.SurveySubmitResult
import com.chla.kindd.data.survey.SurveyTextQuestion
import com.chla.kindd.data.survey.surveyLocale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SurveySubmissionStatus {
    IDLE,
    SUBMITTING,
    SUCCESS,
    EMPTY,
    RATE_LIMITED,
    ERROR
}

data class SurveyUiState(
    val answers: SurveyAnswers = SurveyAnswers(),
    val status: SurveySubmissionStatus = SurveySubmissionStatus.IDLE
) {
    val isEditable: Boolean
        get() = status != SurveySubmissionStatus.SUBMITTING &&
            status != SurveySubmissionStatus.SUCCESS
}

@HiltViewModel
class SurveyViewModel @Inject constructor(
    private val repository: SurveyRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SurveyUiState())
    val uiState: StateFlow<SurveyUiState> = mutableUiState.asStateFlow()

    fun updateText(question: SurveyTextQuestion, value: String) {
        updateAnswers { answers ->
            answers.copy(texts = answers.texts + (question to value.take(question.maxLength)))
        }
    }

    fun toggleRating(question: SurveyRatingQuestion, rating: Int) {
        if (rating !in SurveyRatingQuestion.ratingRange) return
        updateAnswers { answers ->
            val ratings = if (answers.ratings[question] == rating) {
                answers.ratings - question
            } else {
                answers.ratings + (question to rating)
            }
            answers.copy(ratings = ratings)
        }
    }

    fun toggleDiagnosisStatus(status: DiagnosisStatus) {
        updateAnswers { answers ->
            answers.copy(
                diagnosisStatus = status.takeUnless { it == answers.diagnosisStatus }
            )
        }
    }

    fun submit(languageTag: String) {
        val state = mutableUiState.value
        if (!state.isEditable) return
        if (state.answers.isEmpty) {
            mutableUiState.update { it.copy(status = SurveySubmissionStatus.EMPTY) }
            return
        }
        val request = state.answers.toRequest(
            locale = surveyLocale(languageTag),
            appVersion = BuildConfig.VERSION_NAME
        )
        mutableUiState.update { it.copy(status = SurveySubmissionStatus.SUBMITTING) }
        viewModelScope.launch {
            val status = when (repository.submit(request)) {
                SurveySubmitResult.SUCCESS -> SurveySubmissionStatus.SUCCESS
                SurveySubmitResult.RATE_LIMITED -> SurveySubmissionStatus.RATE_LIMITED
                SurveySubmitResult.FAILED -> SurveySubmissionStatus.ERROR
            }
            mutableUiState.update { it.copy(status = status) }
        }
    }

    private fun updateAnswers(transform: (SurveyAnswers) -> SurveyAnswers) {
        mutableUiState.update { state ->
            if (!state.isEditable) {
                state
            } else {
                SurveyUiState(answers = transform(state.answers))
            }
        }
    }
}
