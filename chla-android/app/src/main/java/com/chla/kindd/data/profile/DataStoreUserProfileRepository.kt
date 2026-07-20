package com.chla.kindd.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreUserProfileRepository(
    private val store: DataStore<Preferences>
) : UserProfileRepository {

    override val profile: Flow<UserProfile> = store.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::decodeProfile)

    override suspend fun replaceProfile(profile: UserProfile) {
        store.edit { preferences ->
            preferences.clear()
            preferences[UserProfilePreferences.onboardingCompleted] = profile.onboardingCompleted
            profile.audienceType?.let {
                preferences[UserProfilePreferences.audienceType] = it.storageValue
            }
            profile.zipCode?.let {
                preferences[UserProfilePreferences.zipCode] = it
            }
            profile.regionalCenter?.let { center ->
                preferences[UserProfilePreferences.regionalCenterId] = center.id
                preferences[UserProfilePreferences.regionalCenterName] = center.name
                preferences[UserProfilePreferences.regionalCenterShortName] = center.shortName
            }
            profile.journeyStage?.let {
                preferences[UserProfilePreferences.journeyStage] = it.storageValue
            }
            profile.ageGroup?.let {
                preferences[UserProfilePreferences.ageGroup] = it.apiValue
            }
        }
    }

    override suspend fun clearProfile() {
        store.edit { preferences ->
            preferences.clear()
        }
    }

    private fun decodeProfile(preferences: Preferences): UserProfile {
        val regionalCenterId = preferences[UserProfilePreferences.regionalCenterId]
        val regionalCenterName = preferences[UserProfilePreferences.regionalCenterName]
        val regionalCenterShortName =
            preferences[UserProfilePreferences.regionalCenterShortName]
        val regionalCenter = if (
            regionalCenterId != null &&
            regionalCenterName != null &&
            regionalCenterShortName != null
        ) {
            RegionalCenterIdentity(
                id = regionalCenterId,
                name = regionalCenterName,
                shortName = regionalCenterShortName
            )
        } else {
            null
        }

        return UserProfile(
            onboardingCompleted =
                preferences[UserProfilePreferences.onboardingCompleted] ?: false,
            audienceType = AudienceType.fromStorageValue(
                preferences[UserProfilePreferences.audienceType]
            ),
            zipCode = preferences[UserProfilePreferences.zipCode],
            regionalCenter = regionalCenter,
            journeyStage = JourneyStage.fromStorageValue(
                preferences[UserProfilePreferences.journeyStage]
            ),
            ageGroup = AgeGroup.fromStorageValue(
                preferences[UserProfilePreferences.ageGroup]
            )
        )
    }
}
