package com.navigator.kindd.di

import android.content.Context
import com.navigator.kindd.data.api.KINDDApi
import com.navigator.kindd.data.discovery.DiscoveryController
import com.navigator.kindd.data.discovery.DiscoveryStore
import com.navigator.kindd.data.repository.ProviderRepository
import com.navigator.kindd.data.repository.RegionalCenterRepository
import com.navigator.kindd.data.servicearea.BundledServiceAreaDataSource
import com.navigator.kindd.data.servicearea.ServiceAreaDataSource
import com.navigator.kindd.data.source.ProviderDiscoveryDataSource
import com.navigator.kindd.data.source.RegionalCenterDataSource
import com.navigator.kindd.data.source.UserLocationSource
import com.navigator.kindd.services.LocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineDispatcher
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
        fusedLocationProviderClient: FusedLocationProviderClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): LocationService {
        return LocationService(context, fusedLocationProviderClient, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUserLocationSource(
        locationService: LocationService
    ): UserLocationSource = locationService

    @Provides
    @Singleton
    fun provideProviderRepository(
        api: KINDDApi,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ProviderRepository {
        return ProviderRepository(api, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideProviderDiscoveryDataSource(
        repository: ProviderRepository
    ): ProviderDiscoveryDataSource = repository

    @Provides
    @Singleton
    fun provideDiscoveryController(
        store: DiscoveryStore
    ): DiscoveryController = store

    @Provides
    @Singleton
    fun provideRegionalCenterRepository(
        api: KINDDApi,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): RegionalCenterRepository {
        return RegionalCenterRepository(api, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideRegionalCenterDataSource(
        repository: RegionalCenterRepository
    ): RegionalCenterDataSource = repository

    @Provides
    @Singleton
    fun provideServiceAreaDataSource(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ServiceAreaDataSource = BundledServiceAreaDataSource(
        resourceReader = {
            context.resources.openRawResource(
                com.navigator.kindd.R.raw.la_regional_centers
            ).reader()
        },
        ioDispatcher = ioDispatcher
    )
}
