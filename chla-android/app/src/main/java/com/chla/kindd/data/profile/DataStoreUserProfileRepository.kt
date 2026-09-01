package com.chla.kindd.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
            preferences.writeProfile(profile)
        }
    }

    override suspend fun replaceProfileIfCurrent(
        expected: UserProfile,
        replacement: UserProfile
    ): Boolean {
        var replaced = false
        store.edit { preferences ->
            if (decodeProfile(preferences) == expected) {
                preferences.writeProfile(replacement)
                replaced = true
            }
        }
        return replaced
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
            ),
            ageGroups = decodeAgeGroups(preferences[UserProfilePreferences.ageGroups]),
            hasMultipleChildren =
                preferences[UserProfilePreferences.hasMultipleChildren] ?: false
        )
    }

    private fun decodeAgeGroups(stored: String?): List<AgeGroup> {
        if (stored.isNullOrBlank()) return emptyList()
        return stored.split(',')
            .mapNotNull(AgeGroup::fromStorageValue)
            .filter { it in AgeGroup.childAges }
            .distinct()
    }

    private fun MutablePreferences.writeProfile(
        profile: UserProfile
    ) {
        clear()
        this[UserProfilePreferences.onboardingCompleted] = profile.onboardingCompleted
        profile.audienceType?.let {
            this[UserProfilePreferences.audienceType] = it.storageValue
        }
        profile.zipCode?.let {
            this[UserProfilePreferences.zipCode] = it
        }
        profile.regionalCenter?.let { center ->
            this[UserProfilePreferences.regionalCenterId] = center.id
            this[UserProfilePreferences.regionalCenterName] = center.name
            this[UserProfilePreferences.regionalCenterShortName] = center.shortName
        }
        profile.journeyStage?.let {
            this[UserProfilePreferences.journeyStage] = it.storageValue
        }
        profile.ageGroup?.let {
            this[UserProfilePreferences.ageGroup] = it.apiValue
        }
        if (profile.ageGroups.isNotEmpty()) {
            this[UserProfilePreferences.ageGroups] =
                profile.ageGroups.joinToString(",") { it.apiValue }
        }
        if (profile.hasMultipleChildren) {
            this[UserProfilePreferences.hasMultipleChildren] = true
        }
    }
}
