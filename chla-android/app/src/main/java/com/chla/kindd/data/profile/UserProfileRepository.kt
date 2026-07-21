package com.chla.kindd.data.profile

import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    val profile: Flow<UserProfile>

    suspend fun replaceProfile(profile: UserProfile)

    /** Atomically replaces the stored profile only when it still equals [expected]. */
    suspend fun replaceProfileIfCurrent(
        expected: UserProfile,
        replacement: UserProfile
    ): Boolean

    suspend fun clearProfile()
}
