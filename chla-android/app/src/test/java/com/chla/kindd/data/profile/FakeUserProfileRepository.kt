package com.chla.kindd.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserProfileRepository(
    initialProfile: UserProfile = UserProfile()
) : UserProfileRepository {
    private val profiles = MutableStateFlow(initialProfile)

    override val profile: Flow<UserProfile> = profiles

    override suspend fun replaceProfile(profile: UserProfile) {
        profiles.value = profile
    }

    override suspend fun clearProfile() {
        profiles.value = UserProfile()
    }
}
