package com.chla.kindd.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chla.kindd.data.profile.DataStoreUserProfileRepository
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.profile.userProfileDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserProfileStore

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    @Provides
    @Singleton
    @UserProfileStore
    fun provideUserProfileStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.userProfileDataStore

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        @UserProfileStore store: DataStore<Preferences>
    ): UserProfileRepository = DataStoreUserProfileRepository(store)
}
