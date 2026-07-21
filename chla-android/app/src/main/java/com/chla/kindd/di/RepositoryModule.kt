package com.chla.kindd.di

import android.content.Context
import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.discovery.DiscoveryController
import com.chla.kindd.data.discovery.DiscoveryStore
import com.chla.kindd.data.repository.ProviderRepository
import com.chla.kindd.data.repository.RegionalCenterRepository
import com.chla.kindd.data.servicearea.BundledServiceAreaDataSource
import com.chla.kindd.data.servicearea.ServiceAreaDataSource
import com.chla.kindd.data.source.ProviderDiscoveryDataSource
import com.chla.kindd.data.source.RegionalCenterDataSource
import com.chla.kindd.data.source.UserLocationSource
import com.chla.kindd.services.LocationService
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
                com.chla.kindd.R.raw.la_regional_centers
            ).reader()
        },
        ioDispatcher = ioDispatcher
    )
}
