package com.chla.kindd.ui.survey

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.R
import com.chla.kindd.data.survey.DiagnosisStatus
import com.chla.kindd.data.survey.SurveyRatingQuestion
import com.chla.kindd.data.survey.SurveyTextQuestion
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPrimaryGradientCapsule
import com.chla.kindd.ui.theme.KiNDDShapeTokens
import com.chla.kindd.ui.theme.KiNDDSpacingTokens
import com.chla.kindd.ui.theme.kinddTopAppBarColorContract

@Composable
fun SurveyScreen(
    onBack: () -> Unit,
    viewModel: SurveyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    SurveyContent(
        state = state,
        onTextChange = viewModel::updateText,
        onRatingToggle = viewModel::toggleRating,
        onDiagnosisStatusToggle = viewModel::toggleDiagnosisStatus,
        onSubmit = { viewModel.submit(languageTag) },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyContent(
    state: SurveyUiState,
    onTextChange: (SurveyTextQuestion, String) -> Unit,
    onRatingToggle: (SurveyRatingQuestion, Int) -> Unit,
    onDiagnosisStatusToggle: (DiagnosisStatus) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = kinddTopAppBarColorContract()
    Scaffold(
        modifier = modifier.testTag("survey_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.survey_entry_label)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("survey_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.containerColor,
                    titleContentColor = colors.titleContentColor,
                    navigationIconContentColor = colors.navigationIconContentColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KiNDDSpacingTokens.PageInset, vertical = 20.dp)
                .testTag("survey_form"),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.survey_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            if (state.status == SurveySubmissionStatus.SUCCESS) {
                SurveySuccess(onDone = onBack)
            } else {
                SurveyForm(
                    state = state,
                    onTextChange = onTextChange,
                    onRatingToggle = onRatingToggle,
                    onDiagnosisStatusToggle = onDiagnosisStatusToggle,
                    onSubmit = onSubmit
                )
            }
        }
    }
}

@Composable
private fun SurveyForm(
    state: SurveyUiState,
    onTextChange: (SurveyTextQuestion, String) -> Unit,
    onRatingToggle: (SurveyRatingQuestion, Int) -> Unit,
    onDiagnosisStatusToggle: (DiagnosisStatus) -> Unit,
    onSubmit: () -> Unit
) {
    Text(
        text = stringResource(R.string.survey_lead),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    SurveyTextField(SurveyTextQuestion.DIAGNOSIS, state, onTextChange, singleLine = true)
    SurveyChoiceQuestion(
        question = stringResource(R.string.survey_q_diagnosis_status),
        testTag = "survey_diagnosis_status"
    ) {
        DiagnosisStatus.entries.forEach { status ->
            SurveyOption(
                label = stringResource(status.labelRes),
                selected = state.answers.diagnosisStatus == status,
                enabled = state.isEditable,
                onClick = { onDiagnosisStatusToggle(status) },
                testTag = "survey_diagnosis_status_${status.apiValue}"
            )
        }
    }
    SurveyRatingQuestion.entries.forEach { question ->
        SurveyChoiceQuestion(
            question = stringResource(question.promptRes),
            testTag = "survey_rating_${question.name.lowercase()}"
        ) {
            question.optionLabels.forEachIndexed { index, labelRes ->
                val rating = index + 1
                SurveyOption(
                    label = stringResource(labelRes),
                    selected = state.answers.ratings[question] == rating,
                    enabled = state.isEditable,
                    onClick = { onRatingToggle(question, rating) },
                    testTag = "survey_rating_${question.name.lowercase()}_$rating"
                )
            }
        }
    }
    listOf(
        SurveyTextQuestion.EXPERIENCE_BEFORE,
        SurveyTextQuestion.UNMET_NEEDS,
        SurveyTextQuestion.IMPROVEMENTS,
        SurveyTextQuestion.ADDITIONAL_COMMENTS
    ).forEach { question ->
        SurveyTextField(question, state, onTextChange, singleLine = false)
    }
    SurveyStatusMessage(state.status)
    SurveySubmitButton(
        submitting = state.status == SurveySubmissionStatus.SUBMITTING,
        onSubmit = onSubmit
    )
}

@Composable
private fun SurveyTextField(
    question: SurveyTextQuestion,
    state: SurveyUiState,
    onTextChange: (SurveyTextQuestion, String) -> Unit,
    singleLine: Boolean
) {
    val prompt = stringResource(question.promptRes)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SurveyQuestionText(prompt)
        OutlinedTextField(
            value = state.answers.text(question),
            onValueChange = { onTextChange(question, it) },
            enabled = state.isEditable,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("survey_text_${question.name.lowercase()}")
                .semantics { contentDescription = prompt }
        )
    }
}

@Composable
private fun SurveyChoiceQuestion(
    question: String,
    testTag: String,
    options: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SurveyQuestionText(question)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(KiNDDShapeTokens.Selection)
                )
                .selectableGroup()
                .testTag(testTag)
        ) {
            options()
        }
    }
}

@Composable
private fun SurveyOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SurveyQuestionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SurveyStatusMessage(status: SurveySubmissionStatus) {
    val message = when (status) {
        SurveySubmissionStatus.EMPTY -> R.string.survey_empty
        SurveySubmissionStatus.RATE_LIMITED -> R.string.survey_rate_limited
        SurveySubmissionStatus.ERROR -> R.string.survey_error
        SurveySubmissionStatus.IDLE,
        SurveySubmissionStatus.SUBMITTING,
        SurveySubmissionStatus.SUCCESS -> null
    } ?: return
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("survey_status_message")
            .semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
private fun SurveySubmitButton(submitting: Boolean, onSubmit: () -> Unit) {
    KiNDDPrimaryGradientCapsule(
        onClick = { if (!submitting) onSubmit() },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("survey_submit")
            .alpha(if (submitting) 0.45f else 1f)
            .semantics { if (submitting) disabled() }
    ) {
        Text(
            text = stringResource(
                if (submitting) R.string.survey_submitting else R.string.survey_submit
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun SurveySuccess(onDone: () -> Unit) {
    Text(
        text = stringResource(R.string.survey_success),
        style = MaterialTheme.typography.bodyLarge,
        color = KiNDDIndigo,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("survey_success")
            .semantics { liveRegion = LiveRegionMode.Polite }
    )
    KiNDDPrimaryGradientCapsule(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().testTag("survey_done")
    ) {
        Text(
            text = stringResource(R.string.done),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@get:StringRes
private val SurveyTextQuestion.promptRes: Int
    get() = when (this) {
        SurveyTextQuestion.DIAGNOSIS -> R.string.survey_q_diagnosis
        SurveyTextQuestion.EXPERIENCE_BEFORE -> R.string.survey_q_experience_before
        SurveyTextQuestion.UNMET_NEEDS -> R.string.survey_q_unmet_needs
        SurveyTextQuestion.IMPROVEMENTS -> R.string.survey_q_improvements
        SurveyTextQuestion.ADDITIONAL_COMMENTS -> R.string.survey_q_additional_comments
    }

@get:StringRes
private val SurveyRatingQuestion.promptRes: Int
    get() = when (this) {
        SurveyRatingQuestion.EASE_OF_USE -> R.string.survey_q_ease_of_use
        SurveyRatingQuestion.HELPFULNESS -> R.string.survey_q_helpfulness
        SurveyRatingQuestion.DIFFICULTY_BEFORE -> R.string.survey_q_difficulty_before
        SurveyRatingQuestion.CONFIDENCE_AFTER -> R.string.survey_q_confidence_after
        SurveyRatingQuestion.NEEDS_ADDRESSED -> R.string.survey_q_needs_addressed
    }

private val SurveyRatingQuestion.optionLabels: List<Int>
    get() = when (this) {
        SurveyRatingQuestion.EASE_OF_USE,
        SurveyRatingQuestion.DIFFICULTY_BEFORE -> listOf(
            R.string.survey_ease_1,
            R.string.survey_ease_2,
            R.string.survey_ease_3,
            R.string.survey_ease_4,
            R.string.survey_ease_5
        )
        SurveyRatingQuestion.HELPFULNESS -> listOf(
            R.string.survey_helpful_1,
            R.string.survey_helpful_2,
            R.string.survey_helpful_3,
            R.string.survey_helpful_4,
            R.string.survey_helpful_5
        )
        SurveyRatingQuestion.CONFIDENCE_AFTER -> listOf(
            R.string.survey_confident_1,
            R.string.survey_confident_2,
            R.string.survey_confident_3,
            R.string.survey_confident_4,
            R.string.survey_confident_5
        )
        SurveyRatingQuestion.NEEDS_ADDRESSED -> listOf(
            R.string.survey_extent_1,
            R.string.survey_extent_2,
            R.string.survey_extent_3,
            R.string.survey_extent_4,
            R.string.survey_extent_5
        )
    }

@get:StringRes
private val DiagnosisStatus.labelRes: Int
    get() = when (this) {
        DiagnosisStatus.UNDER_EVALUATION -> R.string.survey_status_under_evaluation
        DiagnosisStatus.NOT_YET_DIAGNOSED -> R.string.survey_status_not_yet_diagnosed
    }
