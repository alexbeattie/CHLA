package com.chla.kindd.data.discovery

import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.FakeUserProfileRepository
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryStoreTest {

    @Test
    fun `completed persisted profile seeds ProfileZip and age then loads exactly once`() =
        runTest {
            val dataSource = FakeProviderDiscoveryDataSource().apply {
                enqueueProviders(provider("seed"))
            }
            val profile = completeProfile(zipCode = "90001", ageGroup = AgeGroup.SCHOOL_AGE)
            val fixture = fixture(profile, dataSource)

            runCurrent()

            assertEquals(profile, fixture.store.state.value.profile)
            assertEquals(
                DiscoveryOrigin.ProfileZip("90001"),
                fixture.store.state.value.criteria.origin
            )
            assertEquals(AgeGroup.SCHOOL_AGE, fixture.store.state.value.criteria.ageGroup)
            assertEquals(listOf("seed"), fixture.store.state.value.providers.map(Provider::id))
            assertEquals(1, dataSource.calls.size)
            val call = dataSource.calls.single() as ProviderDiscoveryCall.ProfileZip
            assertEquals("90001", call.request.zipCode)
            assertEquals("6-12", call.request.ageGroup)
        }

    @Test
    fun `incomplete profile updates state and performs no provider request`() = runTest {
        val incomplete = UserProfile(zipCode = "90001")
        val fixture = fixture(incomplete)

        runCurrent()

        assertEquals(incomplete, fixture.store.state.value.profile)
        assertEquals(DiscoveryCriteria(), fixture.store.state.value.criteria)
        assertTrue(fixture.dataSource.calls.isEmpty())
    }

    @Test
    fun `criteria relevant profile change updates origin and age and refreshes exactly once`() =
        runTest {
            val dataSource = FakeProviderDiscoveryDataSource().apply {
                enqueueProviders(provider("old"))
                enqueueProviders(provider("new"))
            }
            val fixture = fixture(completeProfile("90001", AgeGroup.EARLY_INTERVENTION), dataSource)
            runCurrent()

            fixture.profileRepository.replaceProfile(
                completeProfile("91101", AgeGroup.ADOLESCENT)
            )
            runCurrent()

            assertEquals(2, dataSource.calls.size)
            assertEquals(
                DiscoveryOrigin.ProfileZip("91101"),
                fixture.store.state.value.criteria.origin
            )
            assertEquals(AgeGroup.ADOLESCENT, fixture.store.state.value.criteria.ageGroup)
            assertEquals(listOf("new"), fixture.store.state.value.providers.map(Provider::id))
        }

    @Test
    fun `audience or journey only profile change updates state without duplicate refresh`() =
        runTest {
            val original = completeProfile("90001", AgeGroup.SCHOOL_AGE)
            val dataSource = FakeProviderDiscoveryDataSource().apply {
                enqueueProviders(provider("seed"))
            }
            val fixture = fixture(original, dataSource)
            runCurrent()

            val changed = original.copy(
                audienceType = AudienceType.CLINICIAN,
                journeyStage = JourneyStage.RECEIVING_SERVICES
            )
            fixture.profileRepository.replaceProfile(changed)
            runCurrent()

            assertEquals(changed, fixture.store.state.value.profile)
            assertEquals(1, dataSource.calls.size)
        }

    @Test
    fun `setQuery is synchronous supersedes immediately and debounces nonblank query 300ms`() =
        runTest {
            val dataSource = FakeProviderDiscoveryDataSource().apply {
                enqueueProviders(provider("seed"))
            }
            val fixture = fixture(completeProfile(), dataSource)
            runCurrent()

            val oldResponse = dataSource.enqueuePending(ignoreCancellation = true)
            fixture.store.setQuery("old")
            assertEquals("old", fixture.store.state.value.criteria.query)
            advanceTimeBy(299)
            runCurrent()
            assertEquals(1, dataSource.calls.size)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(2, dataSource.calls.size)

            dataSource.enqueueProviders(provider("new"))
            fixture.store.setQuery("new")
            assertEquals("new", fixture.store.state.value.criteria.query)
            oldResponse.succeed(listOf(provider("stale")))
            runCurrent()
            assertEquals(listOf("seed"), fixture.store.state.value.providers.map(Provider::id))
            advanceTimeBy(299)
            runCurrent()
            assertEquals(2, dataSource.calls.size)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(3, dataSource.calls.size)
            assertEquals(listOf("new"), fixture.store.state.value.providers.map(Provider::id))
        }

    @Test
    fun `clearing query requests immediately without debounce`() = runTest {
        val dataSource = FakeProviderDiscoveryDataSource().apply {
            enqueueProviders(provider("seed"))
        }
        val fixture = fixture(completeProfile(), dataSource)
        runCurrent()

        fixture.store.setQuery("waiting")
        assertEquals("waiting", fixture.store.state.value.criteria.query)
        dataSource.enqueueProviders(provider("cleared"))
        fixture.store.setQuery("")
        assertEquals("", fixture.store.state.value.criteria.query)
        runCurrent()

        assertEquals(2, dataSource.calls.size)
        assertEquals(listOf("cleared"), fixture.store.state.value.providers.map(Provider::id))
    }

    @Test
    fun `filters locations Home shortcut refresh retry and clear all request immediately`() =
        runTest {
            val dataSource = FakeProviderDiscoveryDataSource().apply {
                enqueueProviders(provider("seed"))
            }
            val fixture = fixture(completeProfile(), dataSource)
            runCurrent()

            dataSource.enqueueProviders(
                provider("matching", therapyTypes = listOf(TherapyType.ABA.apiValue)),
                provider("filtered", therapyTypes = listOf(TherapyType.SPEECH.apiValue))
            )
            fixture.store.applyFilters(
                therapyTypes = setOf(TherapyType.ABA),
                ageGroup = AgeGroup.ADULT,
                diagnosis = "Other",
                insurance = "Medi-Cal",
                radiusMiles = 25
            )
            assertEquals(setOf(TherapyType.ABA), fixture.store.state.value.criteria.therapyTypes)
            runCurrent()
            assertEquals(listOf("matching"), fixture.store.state.value.providers.map(Provider::id))

            dataSource.enqueueProviders(provider("device"))
            fixture.store.useDeviceLocation(34.0522, -118.2437)
            assertEquals(
                DiscoveryOrigin.DeviceLocation(34.0522, -118.2437),
                fixture.store.state.value.criteria.origin
            )
            runCurrent()

            dataSource.enqueueProviders(provider("catalog"))
            fixture.store.useLosAngelesCatalog()
            assertEquals(
                DiscoveryOrigin.LosAngelesCatalog,
                fixture.store.state.value.criteria.origin
            )
            runCurrent()

            dataSource.enqueueProviders(provider("home"))
            fixture.store.setSingleTherapyAndRefresh(TherapyType.SPEECH)
            assertEquals(
                setOf(TherapyType.SPEECH),
                fixture.store.state.value.criteria.therapyTypes
            )
            runCurrent()

            dataSource.enqueueProviders(provider("refresh"))
            fixture.store.refresh()
            runCurrent()

            dataSource.enqueueProviders(provider("retry"))
            fixture.store.retry()
            runCurrent()

            dataSource.enqueueProviders(provider("clear"))
            fixture.store.clearAllFilters()
            assertTrue(fixture.store.state.value.criteria.therapyTypes.isEmpty())
            assertNull(fixture.store.state.value.criteria.ageGroup)
            assertNull(fixture.store.state.value.criteria.diagnosis)
            assertNull(fixture.store.state.value.criteria.insurance)
            assertEquals(15, fixture.store.state.value.criteria.radiusMiles)
            runCurrent()

            assertEquals(8, dataSource.calls.size)
            assertEquals(listOf("clear"), fixture.store.state.value.providers.map(Provider::id))
        }

    @Test
    fun `ProfileZip filters the complete remote result before applying the final cap`() =
        runTest {
            val unrelated = (1..50).map { index -> provider("unrelated-$index") }
            val matching = (51..101).map { index ->
                Provider(id = "match-$index", name = "Matching Provider $index")
            }
            val dataSource = FakeProviderDiscoveryDataSource().apply {
                enqueueProviders(provider("seed"))
                enqueueProviders(*(unrelated + matching).toTypedArray())
            }
            val fixture = fixture(completeProfile(), dataSource)
            runCurrent()

            fixture.store.setQuery("matching")
            advanceTimeBy(300)
            runCurrent()

            val call = dataSource.calls.last() as ProviderDiscoveryCall.ProfileZip
            assertEquals(Int.MAX_VALUE, call.limit)
            assertEquals(50, fixture.store.state.value.providers.size)
            assertEquals(
                (51..100).map { index -> "match-$index" },
                fixture.store.state.value.providers.map(Provider::id)
            )
        }

    @Test
    fun `clear all removes device origin and every facet while preserving the query`() = runTest {
        val dataSource = FakeProviderDiscoveryDataSource().apply {
            enqueueProviders(provider("seed"))
            enqueueProviders(provider("located"))
            enqueueProviders(provider("cleared"))
        }
        val fixture = fixture(completeProfile(), dataSource)
        runCurrent()

        fixture.store.useDeviceLocation(34.0522, -118.2437)
        runCurrent()
        fixture.store.setQuery("support")
        fixture.store.applyFilters(
            therapyTypes = setOf(TherapyType.ABA),
            ageGroup = AgeGroup.ADULT,
            diagnosis = "Other",
            insurance = "Medi-Cal",
            radiusMiles = 50
        )
        fixture.store.clearAllFilters()

        assertEquals(
            DiscoveryCriteria(
                query = "support",
                radiusMiles = 15,
                origin = DiscoveryOrigin.LosAngelesCatalog
            ),
            fixture.store.state.value.criteria
        )
        runCurrent()
        assertEquals(
            ProviderDiscoveryCall.Comprehensive(
                request = ComprehensiveProviderRequest(query = "support"),
                limit = 50
            ),
            dataSource.calls.last()
        )
    }

    @Test
    fun `rapid requests cannot let slower cancellation ignoring result overwrite latest`() =
        runTest {
            val dataSource = FakeProviderDiscoveryDataSource().apply {
                enqueueProviders(provider("seed"))
            }
            val fixture = fixture(completeProfile(), dataSource)
            runCurrent()

            val oldResponse = dataSource.enqueuePending(ignoreCancellation = true)
            fixture.store.useDeviceLocation(34.0, -118.0)
            runCurrent()
            val newResponse = dataSource.enqueuePending(ignoreCancellation = true)
            fixture.store.useLosAngelesCatalog()
            runCurrent()

            newResponse.succeed(listOf(provider("latest")))
            runCurrent()
            oldResponse.succeed(listOf(provider("stale")))
            runCurrent()

            assertEquals(3, dataSource.calls.size)
            assertEquals(listOf("latest"), fixture.store.state.value.providers.map(Provider::id))
        }

    @Test
    fun `CancellationException never becomes DiscoveryError`() = runTest {
        val dataSource = FakeProviderDiscoveryDataSource().apply {
            enqueueFailure(CancellationException("do not expose"))
        }
        val fixture = fixture(completeProfile(), dataSource)

        runCurrent()

        assertNull(fixture.store.state.value.error)
        assertFalse(fixture.store.state.value.isLoading)
        assertFalse(fixture.store.state.value.hasLoadedOnce)
    }

    @Test
    fun `initial failures are full sanitized errors mapped only by type`() = runTest {
        val dataSource = FakeProviderDiscoveryDataSource().apply {
            enqueueFailure(IllegalStateException("secret profile text 90001"))
            enqueueFailure(SocketTimeoutException("secret timeout details"))
            enqueueFailure(IOException("secret network details"))
            enqueueFailure(
                HttpException(
                    Response.error<String>(
                        503,
                        "secret backend body".toResponseBody("text/plain".toMediaType())
                    )
                )
            )
        }
        val fixture = fixture(completeProfile(), dataSource)
        runCurrent()
        assertEquals(DiscoveryError.UNKNOWN, fixture.store.state.value.error)

        fixture.store.retry()
        runCurrent()
        assertEquals(DiscoveryError.TIMEOUT, fixture.store.state.value.error)

        fixture.store.retry()
        runCurrent()
        assertEquals(DiscoveryError.NETWORK, fixture.store.state.value.error)

        fixture.store.retry()
        runCurrent()
        assertEquals(DiscoveryError.SERVER, fixture.store.state.value.error)
        assertTrue(fixture.store.state.value.providers.isEmpty())
        assertFalse(fixture.store.state.value.hasLoadedOnce)
        assertFalse(fixture.store.state.value.isLoading)
        assertFalse(fixture.store.state.value.error.toString().contains("secret"))
    }

    @Test
    fun `refresh failure preserves providers and exposes retryable error`() = runTest {
        val dataSource = FakeProviderDiscoveryDataSource().apply {
            enqueueProviders(provider("existing"))
            enqueueFailure(IOException("private network detail"))
        }
        val fixture = fixture(completeProfile(), dataSource)
        runCurrent()

        fixture.store.refresh()
        runCurrent()

        assertEquals(listOf("existing"), fixture.store.state.value.providers.map(Provider::id))
        assertEquals(DiscoveryError.NETWORK, fixture.store.state.value.error)
        assertTrue(fixture.store.state.value.hasLoadedOnce)
        assertFalse(fixture.store.state.value.isLoading)
    }

    @Test
    fun `successful empty result sets loaded once and produces empty state`() = runTest {
        val dataSource = FakeProviderDiscoveryDataSource().apply { enqueueProviders() }
        val fixture = fixture(completeProfile(), dataSource)

        runCurrent()

        assertTrue(fixture.store.state.value.providers.isEmpty())
        assertTrue(fixture.store.state.value.hasLoadedOnce)
        assertFalse(fixture.store.state.value.isLoading)
        assertNull(fixture.store.state.value.error)
    }

    @Test
    fun `map IDs are coordinate subset of exact List provider IDs`() = runTest {
        val dataSource = FakeProviderDiscoveryDataSource().apply {
            enqueueProviders(
                provider("both", latitude = 34.0, longitude = -118.0),
                provider("latitude", latitude = 34.1),
                provider("longitude", longitude = -118.1),
                provider("neither")
            )
        }
        val fixture = fixture(completeProfile(), dataSource)

        runCurrent()

        assertEquals(
            listOf("both", "latitude", "longitude", "neither"),
            fixture.store.state.value.providers.map(Provider::id)
        )
        assertEquals(listOf("both"), fixture.store.state.value.mapProviders.map(Provider::id))
    }

    @Test
    fun `incomplete profile advances generation cancels work and resets transient state`() =
        runTest {
            val dataSource = FakeProviderDiscoveryDataSource().apply {
                enqueueProviders(provider("existing"))
                enqueueProviders(provider("filtered", therapyTypes = listOf(TherapyType.ABA.apiValue)))
                enqueueProviders(provider("located"))
            }
            val fixture = fixture(completeProfile(), dataSource)
            runCurrent()

            fixture.store.applyFilters(
                therapyTypes = setOf(TherapyType.ABA),
                ageGroup = AgeGroup.ADULT,
                diagnosis = "Other",
                insurance = "Private Pay",
                radiusMiles = 50
            )
            runCurrent()
            fixture.store.useDeviceLocation(34.0, -118.0)
            runCurrent()
            val staleResponse = dataSource.enqueuePending(ignoreCancellation = true)
            fixture.store.setQuery("private query")
            advanceTimeBy(300)
            runCurrent()

            val incomplete = UserProfile(zipCode = "90210")
            fixture.profileRepository.replaceProfile(incomplete)
            runCurrent()

            assertEquals(DiscoveryState(profile = incomplete), fixture.store.state.value)
            staleResponse.succeed(listOf(provider("stale")))
            runCurrent()
            assertEquals(DiscoveryState(profile = incomplete), fixture.store.state.value)
        }

    @Test
    fun `new completed profile after reset is clean and rejects old late response`() = runTest {
        val oldProfile = completeProfile("90001", AgeGroup.EARLY_INTERVENTION)
        val dataSource = FakeProviderDiscoveryDataSource().apply {
            enqueueProviders(provider("old-seed"))
        }
        val fixture = fixture(oldProfile, dataSource)
        runCurrent()

        val oldResponse = dataSource.enqueuePending(ignoreCancellation = true)
        fixture.store.refresh()
        runCurrent()
        fixture.profileRepository.clearProfile()
        runCurrent()

        val newResponse = dataSource.enqueuePending(ignoreCancellation = true)
        val newProfile = completeProfile("91101", AgeGroup.ADULT)
        fixture.profileRepository.replaceProfile(newProfile)
        runCurrent()

        newResponse.succeed(listOf(provider("new")))
        runCurrent()
        oldResponse.succeed(listOf(provider("stale-old")))
        runCurrent()

        assertEquals(newProfile, fixture.store.state.value.profile)
        assertEquals(
            DiscoveryCriteria(
                ageGroup = AgeGroup.ADULT,
                origin = DiscoveryOrigin.ProfileZip("91101")
            ),
            fixture.store.state.value.criteria
        )
        assertEquals(listOf("new"), fixture.store.state.value.providers.map(Provider::id))
    }

    @Test
    fun `ensureLoaded dedupes attempted key after failure until explicit retry`() = runTest {
        val dataSource = FakeProviderDiscoveryDataSource()
        val first = dataSource.enqueuePending(ignoreCancellation = true)
        val fixture = fixture(completeProfile(), dataSource)
        runCurrent()

        fixture.store.ensureLoaded()
        fixture.store.ensureLoaded()
        fixture.store.ensureLoaded()
        runCurrent()
        assertEquals(1, dataSource.calls.size)

        first.fail(IOException("failed once"))
        runCurrent()
        fixture.store.ensureLoaded()
        runCurrent()
        assertEquals(1, dataSource.calls.size)
        assertEquals(DiscoveryError.NETWORK, fixture.store.state.value.error)

        dataSource.enqueueProviders(provider("retried"))
        fixture.store.retry()
        runCurrent()
        assertEquals(2, dataSource.calls.size)
        assertEquals(listOf("retried"), fixture.store.state.value.providers.map(Provider::id))
    }

    private fun TestScope.fixture(
        initialProfile: UserProfile,
        dataSource: FakeProviderDiscoveryDataSource = FakeProviderDiscoveryDataSource()
    ): Fixture {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val profileRepository = FakeUserProfileRepository(initialProfile)
        val store = DiscoveryStore(
            dataSource = dataSource,
            profileRepository = profileRepository,
            applicationScope = backgroundScope,
            ioDispatcher = dispatcher,
            planner = DiscoveryRequestPlanner()
        )
        return Fixture(store, dataSource, profileRepository)
    }

    private fun completeProfile(
        zipCode: String = "90001",
        ageGroup: AgeGroup? = AgeGroup.SCHOOL_AGE
    ): UserProfile = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.FAMILY,
        zipCode = zipCode,
        journeyStage = JourneyStage.EXPLORING,
        ageGroup = ageGroup
    )

    private fun provider(
        id: String,
        therapyTypes: List<String>? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Provider = Provider(
        id = id,
        name = "Provider $id",
        therapyTypes = therapyTypes,
        latitude = latitude,
        longitude = longitude
    )

    private data class Fixture(
        val store: DiscoveryStore,
        val dataSource: FakeProviderDiscoveryDataSource,
        val profileRepository: FakeUserProfileRepository
    )
}
