package com.navigator.kindd.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.navigator.kindd.data.profile.PROFILE_DATASTORE_NAME
import com.navigator.kindd.data.profile.createUserProfileDataStore
import com.navigator.kindd.data.profile.DataStoreUserProfileRepository
import com.navigator.kindd.data.profile.UserProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

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
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<Preferences> = createUserProfileDataStore(
        produceFile = { context.preferencesDataStoreFile(PROFILE_DATASTORE_NAME) },
        scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    )

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        @UserProfileStore store: DataStore<Preferences>
    ): UserProfileRepository = DataStoreUserProfileRepository(store)
}
