package com.chla.kindd.data.profile

import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    val profile: Flow<UserProfile>

    suspend fun replaceProfile(profile: UserProfile)

    suspend fun clearProfile()
}
