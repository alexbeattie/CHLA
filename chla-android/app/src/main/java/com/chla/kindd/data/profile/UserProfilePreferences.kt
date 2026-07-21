package com.chla.kindd.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope

const val PROFILE_DATASTORE_NAME = "user_profile"

internal object UserProfilePreferences {
    val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
    val audienceType = stringPreferencesKey("audience_type")
    val zipCode = stringPreferencesKey("zip_code")
    val regionalCenterId = intPreferencesKey("regional_center_id")
    val regionalCenterName = stringPreferencesKey("regional_center_name")
    val regionalCenterShortName = stringPreferencesKey("regional_center_short_name")
    val journeyStage = stringPreferencesKey("journey_stage")
    val ageGroup = stringPreferencesKey("age_group")
}

private val userProfileCorruptionHandler =
    ReplaceFileCorruptionHandler { emptyPreferences() }

internal fun createUserProfileDataStore(
    produceFile: () -> File,
    scope: CoroutineScope
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = userProfileCorruptionHandler,
    scope = scope,
    produceFile = produceFile
)
