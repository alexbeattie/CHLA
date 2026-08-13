package com.chla.kindd.ui.providers

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.profile.Diagnosis
import com.chla.kindd.data.profile.UserProfile
import java.util.Locale

/** Diagnoses worth highlighting on provider surfaces: active filter first, then profile picks. */
fun providerHighlightDiagnoses(
    criteria: DiscoveryCriteria,
    profile: UserProfile
): List<Diagnosis> {
    val activeFilter = criteria.diagnosis?.let { value ->
        Diagnosis.entries.firstOrNull { it.apiValue.equals(value.trim(), ignoreCase = true) }
    }
    return (listOfNotNull(activeFilter) + profile.diagnoses).distinct()
}

fun Provider.matchedDiagnosis(highlighted: List<Diagnosis>): Diagnosis? {
    val treated = diagnosesTreated.orEmpty()
        .map { it.trim().lowercase(Locale.ROOT) }
        .toSet()
    return highlighted.firstOrNull { it.apiValue.lowercase(Locale.ROOT) in treated }
}

/** Contains-style match because catalog values and provider data phrase therapies differently. */
fun Provider.matchedTherapy(selected: Set<TherapyType>): TherapyType? {
    val offered = therapyTypes.orEmpty()
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter(String::isNotEmpty)
    return selected.sortedBy(TherapyType::ordinal).firstOrNull { therapy ->
        val filterValue = therapy.apiValue.lowercase(Locale.ROOT)
        offered.any { it.contains(filterValue) || filterValue.contains(it) }
    }
}

@Composable
fun diagnosisShortLabel(diagnosis: Diagnosis): String = stringResource(
    when (diagnosis) {
        Diagnosis.AUTISM -> R.string.diagnosis_short_autism
        Diagnosis.ADHD -> R.string.discovery_diagnosis_adhd
        Diagnosis.GLOBAL_DEVELOPMENT_DELAY -> R.string.diagnosis_short_dev_delay
        Diagnosis.SENSORY_PROCESSING -> R.string.diagnosis_short_sensory
        Diagnosis.SPEECH_LANGUAGE -> R.string.diagnosis_short_speech
        Diagnosis.OTHER -> R.string.discovery_other
    }
)

@Composable
fun therapyShortLabel(therapy: TherapyType): String = stringResource(
    when (therapy) {
        TherapyType.ABA -> R.string.therapy_short_aba
        TherapyType.SPEECH -> R.string.therapy_short_speech
        TherapyType.OCCUPATIONAL -> R.string.therapy_short_occupational
        TherapyType.PHYSICAL -> R.string.therapy_short_physical
        TherapyType.FEEDING -> R.string.therapy_short_feeding
        TherapyType.PARENT_TRAINING -> R.string.therapy_short_parent_training
    }
)
