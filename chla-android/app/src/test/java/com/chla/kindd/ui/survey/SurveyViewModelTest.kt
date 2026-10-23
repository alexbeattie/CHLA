package com.chla.kindd.ui.survey

import com.chla.kindd.BuildConfig
import com.chla.kindd.data.api.SurveyResponseRequest
import com.chla.kindd.data.survey.DiagnosisStatus
import com.chla.kindd.data.survey.SurveyRatingQuestion
import com.chla.kindd.data.survey.SurveyRepository
import com.chla.kindd.data.survey.SurveySubmitResult
import com.chla.kindd.data.survey.SurveyTextQuestion
import com.chla.kindd.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SurveyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun emptySubmit_showsEmptyMessageWithoutCallingTheApi() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeSurveyRepository()
            val viewModel = SurveyViewModel(repository)
            viewModel.updateText(SurveyTextQuestion.IMPROVEMENTS, "   \n ")

            viewModel.submit("en-US")
            runCurrent()

            assertEquals(SurveySubmissionStatus.EMPTY, viewModel.uiState.value.status)
            assertTrue(repository.requests.isEmpty())
        }

    @Test
    fun clearedRatingAndStatus_countAsUnanswered() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSurveyRepository()
        val viewModel = SurveyViewModel(repository)
        viewModel.toggleRating(SurveyRatingQuestion.HELPFULNESS, 4)
        viewModel.toggleRating(SurveyRatingQuestion.HELPFULNESS, 4)
        viewModel.toggleDiagnosisStatus(DiagnosisStatus.UNDER_EVALUATION)
        viewModel.toggleDiagnosisStatus(DiagnosisStatus.UNDER_EVALUATION)

        viewModel.submit("en")
        runCurrent()

        assertTrue(viewModel.uiState.value.answers.ratings.isEmpty())
        assertNull(viewModel.uiState.value.answers.diagnosisStatus)
        assertEquals(SurveySubmissionStatus.EMPTY, viewModel.uiState.value.status)
        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun submit_mapsAnswersToTheContractWithNullRatingsAndAndroidMetadata() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeSurveyRepository()
            val viewModel = SurveyViewModel(repository)
            viewModel.updateText(SurveyTextQuestion.DIAGNOSIS, "  Autism  ")
            viewModel.toggleDiagnosisStatus(DiagnosisStatus.NOT_YET_DIAGNOSED)
            viewModel.toggleRating(SurveyRatingQuestion.EASE_OF_USE, 2)
            viewModel.toggleRating(SurveyRatingQuestion.EASE_OF_USE, 5)
            viewModel.toggleRating(SurveyRatingQuestion.NEEDS_ADDRESSED, 3)
            viewModel.updateText(SurveyTextQuestion.ADDITIONAL_COMMENTS, "Gracias")

            viewModel.submit("es-US")
            runCurrent()

            assertEquals(
                SurveyResponseRequest(
                    diagnosis = "Autism",
                    diagnosisStatus = "not_yet_diagnosed",
                    easeOfUse = 5,
                    helpfulness = null,
                    difficultyBefore = null,
                    confidenceAfter = null,
                    needsAddressed = 3,
                    experienceBefore = null,
                    unmetNeeds = null,
                    improvements = null,
                    additionalComments = "Gracias",
                    locale = "es",
                    platform = "android",
                    appVersion = BuildConfig.VERSION_NAME
                ),
                repository.requests.single()
            )
        }

    @Test
    fun submit_sendsEnglishForAnyNonSpanishAppLanguage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeSurveyRepository()
            val viewModel = SurveyViewModel(repository)
            viewModel.toggleRating(SurveyRatingQuestion.CONFIDENCE_AFTER, 1)

            viewModel.submit("fr-CA")
            runCurrent()

            assertEquals("en", repository.requests.single().locale)
        }

    @Test
    fun textAnswers_areCappedAtTheContractMaximums() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = SurveyViewModel(FakeSurveyRepository())

            viewModel.updateText(SurveyTextQuestion.DIAGNOSIS, "a".repeat(1200))
            viewModel.updateText(SurveyTextQuestion.UNMET_NEEDS, "b".repeat(4100))

            assertEquals(1000, viewModel.uiState.value.answers.text(SurveyTextQuestion.DIAGNOSIS).length)
            assertEquals(4000, viewModel.uiState.value.answers.text(SurveyTextQuestion.UNMET_NEEDS).length)
        }

    @Test
    fun created_movesThroughSubmittingToSuccessAndLocksTheForm() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository = FakeSurveyRepository(gate = gate)
            val viewModel = SurveyViewModel(repository)
            viewModel.toggleRating(SurveyRatingQuestion.DIFFICULTY_BEFORE, 4)

            viewModel.submit("en")
            runCurrent()
            assertEquals(SurveySubmissionStatus.SUBMITTING, viewModel.uiState.value.status)
            viewModel.submit("en")
            viewModel.toggleRating(SurveyRatingQuestion.DIFFICULTY_BEFORE, 1)
            assertEquals(4, viewModel.uiState.value.answers.ratings[SurveyRatingQuestion.DIFFICULTY_BEFORE])

            gate.complete(Unit)
            runCurrent()

            assertEquals(SurveySubmissionStatus.SUCCESS, viewModel.uiState.value.status)
            assertEquals(1, repository.requests.size)
            viewModel.updateText(SurveyTextQuestion.IMPROVEMENTS, "late edit")
            assertEquals("", viewModel.uiState.value.answers.text(SurveyTextQuestion.IMPROVEMENTS))
        }

    @Test
    fun rateLimited_showsRateLimitMessageAndKeepsAnswers() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = SurveyViewModel(
                FakeSurveyRepository(result = SurveySubmitResult.RATE_LIMITED)
            )
            viewModel.updateText(SurveyTextQuestion.IMPROVEMENTS, "More providers")

            viewModel.submit("en")
            runCurrent()

            assertEquals(SurveySubmissionStatus.RATE_LIMITED, viewModel.uiState.value.status)
            assertEquals(
                "More providers",
                viewModel.uiState.value.answers.text(SurveyTextQuestion.IMPROVEMENTS)
            )
        }

    @Test
    fun failure_showsGenericErrorAndRetryCanSucceed() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeSurveyRepository(result = SurveySubmitResult.FAILED)
            val viewModel = SurveyViewModel(repository)
            viewModel.toggleRating(SurveyRatingQuestion.EASE_OF_USE, 3)

            viewModel.submit("en")
            runCurrent()
            assertEquals(SurveySubmissionStatus.ERROR, viewModel.uiState.value.status)

            repository.result = SurveySubmitResult.SUCCESS
            viewModel.submit("en")
            runCurrent()

            assertEquals(SurveySubmissionStatus.SUCCESS, viewModel.uiState.value.status)
            assertEquals(2, repository.requests.size)
        }

    @Test
    fun editingAfterAMessage_clearsTheMessage() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = SurveyViewModel(FakeSurveyRepository())
        viewModel.submit("en")
        assertEquals(SurveySubmissionStatus.EMPTY, viewModel.uiState.value.status)

        viewModel.toggleRating(SurveyRatingQuestion.HELPFULNESS, 2)

        assertEquals(SurveySubmissionStatus.IDLE, viewModel.uiState.value.status)
    }

    private class FakeSurveyRepository(
        var result: SurveySubmitResult = SurveySubmitResult.SUCCESS,
        private val gate: CompletableDeferred<Unit>? = null
    ) : SurveyRepository {
        val requests = mutableListOf<SurveyResponseRequest>()

        override suspend fun submit(request: SurveyResponseRequest): SurveySubmitResult {
            requests += request
            gate?.await()
            return result
        }
    }
}
