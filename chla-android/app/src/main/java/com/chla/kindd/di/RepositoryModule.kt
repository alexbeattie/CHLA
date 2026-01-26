package com.chla.kindd.di

import android.content.Context
import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.repository.ProviderRepository
import com.chla.kindd.data.repository.RegionalCenterRepository
import com.chla.kindd.services.LocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationService {
        return LocationService(context, fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideProviderRepository(api: KINDDApi): ProviderRepository {
        return ProviderRepository(api)
    }

    @Provides
    @Singleton
    fun provideRegionalCenterRepository(api: KINDDApi): RegionalCenterRepository {
        return RegionalCenterRepository(api)
    }
}
