package com.navigator.kindd.data.discovery

import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.data.profile.UserProfileRepository
import com.navigator.kindd.data.source.ProviderDiscoveryDataSource
import com.navigator.kindd.di.ApplicationScope
import com.navigator.kindd.di.IoDispatcher
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Singleton
class DiscoveryStore @Inject constructor(
    private val dataSource: ProviderDiscoveryDataSource,
    private val profileRepository: UserProfileRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val planner: DiscoveryRequestPlanner
) : DiscoveryController {
    private val stateLock = Any()
    private val mutableState = MutableStateFlow(DiscoveryState())
    private val generation = AtomicLong(0)
    private var requestJob: Job? = null
    private var lastAttemptedRequestKey: String? = null

    override val state: StateFlow<DiscoveryState> = mutableState.asStateFlow()

    init {
        applicationScope.launch {
            profileRepository.profile.collect(::handleProfile)
        }
    }

    override fun ensureLoaded() {
        synchronized(stateLock) {
            scheduleRequestLocked(force = false)
        }
    }

    override fun setQuery(query: String) {
        synchronized(stateLock) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(query = query)
            )
            scheduleRequestLocked(
                debounceMillis = if (query.isNotBlank()) QUERY_DEBOUNCE_MILLIS else 0,
                force = true
            )
        }
    }

    override fun applyFilters(
        therapyTypes: Set<TherapyType>,
        ageGroup: AgeGroup?,
        diagnosis: String?,
        insurance: String?,
        radiusMiles: Int
    ) {
        synchronized(stateLock) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(
                    therapyTypes = therapyTypes,
                    ageGroup = ageGroup,
                    diagnosis = diagnosis,
                    insurance = insurance,
                    radiusMiles = radiusMiles
                )
            )
            scheduleRequestLocked(force = true)
        }
    }

    override fun setSingleTherapyAndRefresh(therapyType: TherapyType) {
        synchronized(stateLock) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(
                    therapyTypes = setOf(therapyType)
                )
            )
            scheduleRequestLocked(force = true)
        }
    }

    override fun useDeviceLocation(latitude: Double, longitude: Double) {
        synchronized(stateLock) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(
                    origin = DiscoveryOrigin.DeviceLocation(latitude, longitude)
                )
            )
            scheduleRequestLocked(force = true)
        }
    }

    override fun useLosAngelesCatalog() {
        synchronized(stateLock) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(
                    origin = DiscoveryOrigin.LosAngelesCatalog
                )
            )
            scheduleRequestLocked(force = true)
        }
    }

    override fun refresh() {
        synchronized(stateLock) {
            scheduleRequestLocked(force = true)
        }
    }

    override fun retry() {
        synchronized(stateLock) {
            scheduleRequestLocked(force = true)
        }
    }

    override fun clearAllFilters() {
        synchronized(stateLock) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(
                    therapyTypes = emptySet(),
                    ageGroup = null,
                    diagnosis = null,
                    insurance = null,
                    radiusMiles = DEFAULT_RADIUS_MILES,
                    origin = DiscoveryOrigin.LosAngelesCatalog
                )
            )
            scheduleRequestLocked(force = true)
        }
    }

    private fun handleProfile(profile: UserProfile) {
        synchronized(stateLock) {
            if (!profile.isComplete) {
                generation.incrementAndGet()
                requestJob?.cancel()
                requestJob = null
                lastAttemptedRequestKey = null
                mutableState.value = DiscoveryState(profile = profile)
                return
            }

            val current = mutableState.value
            val criteriaChanged = !current.profile.isComplete ||
                current.profile.zipCode != profile.zipCode ||
                current.profile.ageGroup != profile.ageGroup

            if (!criteriaChanged) {
                mutableState.value = current.copy(profile = profile)
                return
            }

            val criteria = if (current.profile.isComplete) {
                current.criteria.copy(
                    ageGroup = profile.ageGroup,
                    origin = DiscoveryOrigin.ProfileZip(requireNotNull(profile.zipCode))
                )
            } else {
                DiscoveryCriteria(
                    ageGroup = profile.ageGroup,
                    origin = DiscoveryOrigin.ProfileZip(requireNotNull(profile.zipCode))
                )
            }
            mutableState.value = current.copy(profile = profile, criteria = criteria)
            scheduleRequestLocked(force = true)
        }
    }

    private fun scheduleRequestLocked(
        debounceMillis: Long = 0,
        force: Boolean
    ) {
        if (!mutableState.value.profile.isComplete) return

        val plannedRequest = planner.plan(mutableState.value.criteria)
        val requestKey = planner.requestKey(plannedRequest)
        if (!force && lastAttemptedRequestKey == requestKey) return

        val requestGeneration = generation.incrementAndGet()
        requestJob?.cancel()
        lastAttemptedRequestKey = requestKey
        requestJob = applicationScope.launch {
            if (debounceMillis > 0) delay(debounceMillis)
            executeRequest(requestGeneration, plannedRequest, requestKey)
        }
    }

    private suspend fun executeRequest(
        requestGeneration: Long,
        plannedRequest: PlannedDiscoveryRequest,
        requestKey: String
    ) {
        mutateIfCurrent(requestGeneration) { current ->
            current.copy(isLoading = true, error = null)
        }

        try {
            val remoteProviders = withContext(ioDispatcher) {
                when (plannedRequest) {
                    PlannedDiscoveryRequest.Catalog ->
                        dataSource.getProviderCatalog(limit = RESULT_LIMIT).getOrThrow()

                    is PlannedDiscoveryRequest.Comprehensive ->
                        dataSource.searchProviders(
                            request = plannedRequest.remote,
                            limit = RESULT_LIMIT
                        ).getOrThrow()

                    is PlannedDiscoveryRequest.ProfileZip ->
                        dataSource.getProvidersByRegionalCenter(
                            request = plannedRequest.remote,
                            limit = UNFILTERED_PROFILE_ZIP_LIMIT
                        ).getOrThrow()
                }
            }
            val providers = if (plannedRequest is PlannedDiscoveryRequest.ProfileZip) {
                planner.applyLocalFilters(remoteProviders, plannedRequest).take(RESULT_LIMIT)
            } else {
                remoteProviders
            }

            mutateIfCurrent(requestGeneration) { current ->
                current.copy(
                    providers = providers,
                    isLoading = false,
                    error = null,
                    hasLoadedOnce = true,
                    lastSuccessfulRequestKey = requestKey
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            mutateIfCurrent(requestGeneration) { current ->
                current.copy(
                    isLoading = false,
                    error = sanitizeError(exception)
                )
            }
        } finally {
            mutateIfCurrent(requestGeneration) { current ->
                current.copy(isLoading = false)
            }
        }
    }

    private fun mutateIfCurrent(
        requestGeneration: Long,
        transform: (DiscoveryState) -> DiscoveryState
    ) {
        synchronized(stateLock) {
            if (generation.get() == requestGeneration) {
                mutableState.value = transform(mutableState.value)
            }
        }
    }

    private fun sanitizeError(exception: Exception): DiscoveryError = when (exception) {
        is SocketTimeoutException -> DiscoveryError.TIMEOUT
        is IOException -> DiscoveryError.NETWORK
        is HttpException -> DiscoveryError.SERVER
        else -> DiscoveryError.UNKNOWN
    }

    private companion object {
        const val QUERY_DEBOUNCE_MILLIS = 300L
        const val DEFAULT_RADIUS_MILES = 15
        const val RESULT_LIMIT = 50
        const val UNFILTERED_PROFILE_ZIP_LIMIT = Int.MAX_VALUE
    }
}
