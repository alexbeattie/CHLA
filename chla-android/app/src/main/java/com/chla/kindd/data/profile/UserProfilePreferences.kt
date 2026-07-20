package com.chla.kindd.data.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

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

internal val Context.userProfileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PROFILE_DATASTORE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)
