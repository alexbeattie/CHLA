package com.chla.kindd.ui.screens

import com.chla.kindd.data.discovery.DiscoveryController
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.discovery.DiscoveryError
import com.chla.kindd.data.discovery.DiscoveryOrigin
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.source.FakeUserLocationSource
import com.chla.kindd.data.source.UserCoordinates
import com.chla.kindd.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Map and List expose one controller state without loading during construction`() {
        val controller = FakeDiscoveryController(
            DiscoveryState(
                criteria = DiscoveryCriteria(
                    query = "speech",
                    therapyTypes = setOf(TherapyType.SPEECH),
                    diagnosis = "Autism Spectrum Disorder"
                ),
                providers = listOf(
                    provider("mapped", latitude = 34.0, longitude = -118.0),
                    provider("list-only")
                ),
                isLoading = true,
                error = DiscoveryError.NETWORK,
                hasLoadedOnce = true
            )
        )

        val map = MapViewModel(controller, FakeUserLocationSource())
        val list = ProviderListViewModel(controller)

        assertSame(controller.state, map.state)
        assertSame(controller.state, list.state)
        assertEquals("speech", map.state.value.criteria.query)
        assertEquals(map.state.value.criteria, list.state.value.criteria)
        assertEquals(map.state.value.isLoading, list.state.value.isLoading)
        assertEquals(map.state.value.error, list.state.value.error)
        assertEquals(setOf("mapped", "list-only"), list.providers.map(Provider::id).toSet())
        assertEquals(listOf("mapped"), map.mapProviders.map(Provider::id))
        assertTrue(controller.calls.isEmpty())
    }

    @Test
    fun `first appearance ensures shared state and typing from either presenter is shared`() {
        val controller = FakeDiscoveryController()
        val map = MapViewModel(controller, FakeUserLocationSource())
        val list = ProviderListViewModel(controller)

        map.onFirstAppearance()
        list.onFirstAppearance()
        map.setQuery("occupational")
        list.setQuery("speech")

        assertEquals(2, controller.calls.count { it == PresenterCall.EnsureLoaded })
        assertEquals(
            listOf(
                PresenterCall.SetQuery("occupational"),
                PresenterCall.SetQuery("speech")
            ),
            controller.calls.filterIsInstance<PresenterCall.SetQuery>()
        )
        assertEquals("speech", map.state.value.criteria.query)
        assertEquals("speech", list.state.value.criteria.query)
    }

    @Test
    fun `map refresh delegates to the shared discovery refresh operation`() {
        val controller = FakeDiscoveryController(stateWithResults())
        val map = MapViewModel(controller, FakeUserLocationSource())

        map.refresh()

        assertEquals(listOf(PresenterCall.Refresh), controller.calls)
    }

    @Test
    fun `location success changes discovery origin to device coordinates`() = runTest {
        val controller = FakeDiscoveryController(stateWithResults())
        val location = FakeUserLocationSource(
            permissionGranted = true,
            coordinates = UserCoordinates(34.0522, -118.2437)
        )
        val viewModel = MapViewModel(controller, location)

        viewModel.onLocationPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals(
            DiscoveryOrigin.DeviceLocation(34.0522, -118.2437),
            controller.state.value.criteria.origin
        )
        assertEquals(
            PresenterCall.UseDeviceLocation(34.0522, -118.2437),
            controller.calls.last()
        )
        assertEquals(MapLocationStatus.IDLE, viewModel.locationState.value.status)
        assertTrue(viewModel.locationState.value.hasPermission)
    }

    @Test
    fun `location denial preserves results and exposes localized denial category`() {
        val initial = stateWithResults()
        val controller = FakeDiscoveryController(initial)
        val viewModel = MapViewModel(controller, FakeUserLocationSource())

        viewModel.onLocationPermissionResult(granted = false)

        assertEquals(initial.providers, controller.state.value.providers)
        assertEquals(MapLocationStatus.PERMISSION_DENIED, viewModel.locationState.value.status)
        assertFalse(viewModel.locationState.value.hasPermission)
        assertTrue(controller.calls.isEmpty())
    }

    @Test
    fun `location lookup failure preserves results and exposes localized failure category`() =
        runTest {
            val initial = stateWithResults()
            val controller = FakeDiscoveryController(initial)
            val location = FakeUserLocationSource(permissionGranted = true).apply {
                coordinatesFailure = IllegalStateException("private location detail")
            }
            val viewModel = MapViewModel(controller, location)

            viewModel.onLocationPermissionResult(granted = true)
            advanceUntilIdle()

            assertEquals(initial.providers, controller.state.value.providers)
            assertEquals(MapLocationStatus.FAILED, viewModel.locationState.value.status)
            assertTrue(controller.calls.isEmpty())
        }

    @Test
    fun `new location request cancels the previous lookup`() = runTest {
        val first = CompletableDeferred<UserCoordinates?>()
        val location = SequencedUserLocationSource(
            first = first,
            second = UserCoordinates(34.0522, -118.2437),
            ignoreFirstCancellation = false
        )
        val controller = FakeDiscoveryController(stateWithResults())
        val viewModel = MapViewModel(controller, location)

        viewModel.onLocationPermissionResult(granted = true)
        runCurrent()
        viewModel.onLocationPermissionResult(granted = true)
        runCurrent()

        assertTrue(location.firstWasCancelled)
        assertEquals(
            listOf(PresenterCall.UseDeviceLocation(34.0522, -118.2437)),
            controller.calls
        )
        assertEquals(MapLocationStatus.IDLE, viewModel.locationState.value.status)
    }

    @Test
    fun `stale cancellation ignoring location cannot update discovery or status`() = runTest {
        val first = CompletableDeferred<UserCoordinates?>()
        val location = SequencedUserLocationSource(
            first = first,
            second = UserCoordinates(34.0522, -118.2437),
            ignoreFirstCancellation = true
        )
        val controller = FakeDiscoveryController(stateWithResults())
        val viewModel = MapViewModel(controller, location)

        viewModel.onLocationPermissionResult(granted = true)
        runCurrent()
        viewModel.onLocationPermissionResult(granted = true)
        runCurrent()
        first.complete(UserCoordinates(33.9425, -118.4081))
        runCurrent()

        assertEquals(
            listOf(PresenterCall.UseDeviceLocation(34.0522, -118.2437)),
            controller.calls
        )
        assertEquals(MapLocationStatus.IDLE, viewModel.locationState.value.status)
    }

    @Test
    fun `marker models use the same valid coordinate contract as discovery state`() {
        val providers = listOf(
            provider("null", latitude = null, longitude = -118.0),
            provider("nan", latitude = Double.NaN, longitude = -118.0),
            provider("infinite", latitude = 34.0, longitude = Double.NEGATIVE_INFINITY),
            provider("latitude-range", latitude = 91.0, longitude = -118.0),
            provider("longitude-range", latitude = 34.0, longitude = -181.0),
            provider("zero", latitude = 0.0, longitude = 0.0),
            provider("valid", latitude = 34.0522, longitude = -118.2437)
        )

        assertEquals(
            DiscoveryState(providers = providers).mapProviders.map(Provider::id),
            providerMarkerModels(providers).map(MapMarkerModel::providerId)
        )
        assertEquals(listOf("valid"), providerMarkerModels(providers).map(MapMarkerModel::providerId))
    }

    @Test
    fun `marker models preserve canonical therapy role with provider type fallback`() {
        val providers = listOf(
            Provider(
                id = "aba",
                name = "ABA",
                type = "Speech therapy resource",
                latitude = 34.0,
                longitude = -118.0,
                therapyTypes = listOf("ABA therapy")
            ),
            Provider(
                id = "speech",
                name = "Speech",
                latitude = 34.1,
                longitude = -118.1,
                therapyTypes = listOf("Speech and language therapy")
            ),
            Provider(
                id = "occupational",
                name = "Occupational",
                type = "Occupational Therapy Resource",
                latitude = 34.2,
                longitude = -118.2
            ),
            Provider(
                id = "physical",
                name = "Physical",
                latitude = 34.3,
                longitude = -118.3,
                therapyTypes = listOf("Physical therapy")
            ),
            Provider(
                id = "other",
                name = "Other",
                type = "Family support",
                latitude = 34.4,
                longitude = -118.4
            )
        )

        assertEquals(
            listOf(
                ProviderMarkerRole.ABA,
                ProviderMarkerRole.SPEECH,
                ProviderMarkerRole.OCCUPATIONAL,
                ProviderMarkerRole.PHYSICAL,
                ProviderMarkerRole.OTHER
            ),
            providerMarkerModels(providers).map(MapMarkerModel::role)
        )
    }

    private fun stateWithResults() = DiscoveryState(
        criteria = DiscoveryCriteria(origin = DiscoveryOrigin.ProfileZip("90001")),
        providers = listOf(provider("kept", latitude = 34.0, longitude = -118.0)),
        hasLoadedOnce = true
    )
}

private class SequencedUserLocationSource(
    private val first: CompletableDeferred<UserCoordinates?>,
    private val second: UserCoordinates,
    private val ignoreFirstCancellation: Boolean
) : com.chla.kindd.data.source.UserLocationSource {
    private var calls = 0
    var firstWasCancelled: Boolean = false
        private set

    override fun hasLocationPermission(): Boolean = true

    override suspend fun currentCoordinates(): UserCoordinates? = when (++calls) {
        1 -> if (ignoreFirstCancellation) {
            withContext(NonCancellable) { first.await() }
        } else {
            try {
                first.await()
            } catch (cancellation: java.util.concurrent.CancellationException) {
                firstWasCancelled = true
                throw cancellation
            }
        }
        2 -> second
        else -> error("Unexpected location call")
    }

    override suspend fun zipCodeFor(coordinates: UserCoordinates): String? = null
}

internal sealed interface PresenterCall {
    data object EnsureLoaded : PresenterCall
    data class SetQuery(val query: String) : PresenterCall
    data class ApplyFilters(
        val therapyTypes: Set<TherapyType>,
        val ageGroup: AgeGroup?,
        val diagnosis: String?,
        val insurance: String?,
        val radiusMiles: Int
    ) : PresenterCall
    data class UseDeviceLocation(val latitude: Double, val longitude: Double) : PresenterCall
    data object UseLosAngelesCatalog : PresenterCall
    data object Refresh : PresenterCall
    data object Retry : PresenterCall
    data object ClearAllFilters : PresenterCall
}

internal class FakeDiscoveryController(
    initialState: DiscoveryState = DiscoveryState()
) : DiscoveryController {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<DiscoveryState> = mutableState
    val calls = mutableListOf<PresenterCall>()

    override fun ensureLoaded() {
        calls += PresenterCall.EnsureLoaded
    }

    override fun setQuery(query: String) {
        calls += PresenterCall.SetQuery(query)
        mutableState.value = mutableState.value.copy(
            criteria = mutableState.value.criteria.copy(query = query)
        )
    }

    override fun applyFilters(
        therapyTypes: Set<TherapyType>,
        ageGroup: AgeGroup?,
        diagnosis: String?,
        insurance: String?,
        radiusMiles: Int
    ) {
        calls += PresenterCall.ApplyFilters(
            therapyTypes,
            ageGroup,
            diagnosis,
            insurance,
            radiusMiles
        )
        mutableState.value = mutableState.value.copy(
            criteria = mutableState.value.criteria.copy(
                therapyTypes = therapyTypes,
                ageGroup = ageGroup,
                diagnosis = diagnosis,
                insurance = insurance,
                radiusMiles = radiusMiles
            )
        )
    }

    override fun setSingleTherapyAndRefresh(therapyType: TherapyType) = Unit

    override fun useDeviceLocation(latitude: Double, longitude: Double) {
        calls += PresenterCall.UseDeviceLocation(latitude, longitude)
        mutableState.value = mutableState.value.copy(
            criteria = mutableState.value.criteria.copy(
                origin = DiscoveryOrigin.DeviceLocation(latitude, longitude)
            )
        )
    }

    override fun useLosAngelesCatalog() {
        calls += PresenterCall.UseLosAngelesCatalog
        mutableState.value = mutableState.value.copy(
            criteria = mutableState.value.criteria.copy(
                origin = DiscoveryOrigin.LosAngelesCatalog
            )
        )
    }

    override fun refresh() {
        calls += PresenterCall.Refresh
    }

    override fun retry() {
        calls += PresenterCall.Retry
    }

    override fun clearAllFilters() {
        calls += PresenterCall.ClearAllFilters
    }
}

internal fun provider(
    id: String,
    name: String = id,
    latitude: Double? = null,
    longitude: Double? = null,
    distance: Double? = null
) = Provider(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    distance = distance
)
