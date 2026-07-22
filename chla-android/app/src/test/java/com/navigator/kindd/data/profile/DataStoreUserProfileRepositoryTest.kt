package com.navigator.kindd.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.core.CorruptionException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserProfileRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val storeScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        storeScopes.forEach(CoroutineScope::cancel)
    }

    @Test
    fun `empty storage emits the default incomplete profile`() = runTest {
        val repository = DataStoreUserProfileRepository(createStore(profileFile()))

        val profile = repository.profile.first()

        assertEquals(UserProfile(), profile)
        assertFalse(profile.isComplete)
    }

    @Test
    fun `IOException from DataStore data emits the default profile`() = runTest {
        val repository = DataStoreUserProfileRepository(
            failingStore(IOException("read failed"))
        )

        assertEquals(UserProfile(), repository.profile.first())
    }

    @Test
    fun `non-IOException from DataStore data propagates unchanged`() = runTest {
        val failure = IllegalStateException("read failed")
        val repository = DataStoreUserProfileRepository(failingStore(failure))
        var propagated: Throwable? = null

        try {
            repository.profile.first()
        } catch (caught: Throwable) {
            propagated = caught
        }

        assertSame(failure, propagated)
    }

    @Test
    fun `replaceProfile writes every field and a new repository restores it`() = runTest {
        val file = profileFile()
        val firstScope = createStoreScope()
        val firstRepository = DataStoreUserProfileRepository(createStore(file, firstScope))
        val expected = UserProfile(
            onboardingCompleted = true,
            audienceType = AudienceType.CLINICIAN,
            zipCode = "90001",
            regionalCenter = RegionalCenterIdentity(
                id = 42,
                name = "North Los Angeles County Regional Center",
                shortName = "NLACRC"
            ),
            journeyStage = JourneyStage.RECEIVING_SERVICES,
            ageGroup = AgeGroup.ADOLESCENT
        )

        firstRepository.replaceProfile(expected)
        firstScope.coroutineContext[Job]!!.cancelAndJoin()

        val restoredRepository = DataStoreUserProfileRepository(createStore(file))

        assertEquals(expected, restoredRepository.profile.first())
    }

    @Test
    fun `replacement with no regional center removes the complete old center tuple`() = runTest {
        val store = createStore(profileFile())
        val repository = DataStoreUserProfileRepository(store)
        val matched = UserProfile(
            onboardingCompleted = true,
            audienceType = AudienceType.FAMILY,
            zipCode = "90001",
            regionalCenter = RegionalCenterIdentity(
                id = 42,
                name = "North Los Angeles County Regional Center",
                shortName = "NLACRC"
            ),
            journeyStage = JourneyStage.JUST_DIAGNOSED,
            ageGroup = AgeGroup.EARLY_INTERVENTION
        )

        repository.replaceProfile(matched)
        repository.replaceProfile(matched.copy(zipCode = "90002", regionalCenter = null))

        val preferences = store.data.first()
        assertNull(preferences[intPreferencesKey("regional_center_id")])
        assertNull(preferences[stringPreferencesKey("regional_center_name")])
        assertNull(preferences[stringPreferencesKey("regional_center_short_name")])
        assertNull(repository.profile.first().regionalCenter)
    }

    @Test
    fun `compare and replace is atomic against the current stored profile`() = runTest {
        val repository = DataStoreUserProfileRepository(createStore(profileFile()))
        val original = completeProfile(zipCode = "90001")
        val externallyEdited = completeProfile(zipCode = "91311")
        val lateLookupReplacement = completeProfile(zipCode = "90210")
        repository.replaceProfile(original)
        repository.replaceProfile(externallyEdited)

        val staleReplaceApplied = repository.replaceProfileIfCurrent(
            expected = original,
            replacement = lateLookupReplacement
        )

        assertFalse(staleReplaceApplied)
        assertEquals(externallyEdited, repository.profile.first())

        val currentReplaceApplied = repository.replaceProfileIfCurrent(
            expected = externallyEdited,
            replacement = lateLookupReplacement
        )

        assertTrue(currentReplaceApplied)
        assertEquals(lateLookupReplacement, repository.profile.first())
    }

    @Test
    fun `production factory recovers malformed bytes while no-handler control fails`() = runTest {
        val malformedBytes = byteArrayOf(0x0A, 0x05, 0x01)
        val controlFile = temporaryFolder.newFile("control.preferences_pb")
            .apply { writeBytes(malformedBytes) }
        val controlStore = createStore(controlFile)
        var controlFailure: Throwable? = null
        try {
            controlStore.data.first()
        } catch (caught: Throwable) {
            controlFailure = caught
        }
        assertTrue(controlFailure is CorruptionException)

        val productionFile = temporaryFolder.newFile("production.preferences_pb")
            .apply { writeBytes(malformedBytes) }
        val productionStore = createUserProfileDataStore(
            produceFile = { productionFile },
            scope = createStoreScope()
        )
        val recovered = DataStoreUserProfileRepository(productionStore).profile.first()

        assertEquals(UserProfile(), recovered)
        assertFalse(productionFile.readBytes().contentEquals(malformedBytes))
    }

    @Test
    fun `unknown enums and an incomplete regional center tuple decode to null`() = runTest {
        val store = createStore(profileFile())
        store.edit { preferences ->
            preferences[stringPreferencesKey("audience_type")] = "unknown-audience"
            preferences[stringPreferencesKey("journey_stage")] = "unknown-journey"
            preferences[stringPreferencesKey("age_group")] = "unknown-age"
            preferences[intPreferencesKey("regional_center_id")] = 42
            preferences[stringPreferencesKey("regional_center_name")] =
                "North Los Angeles County Regional Center"
        }

        val profile = DataStoreUserProfileRepository(store).profile.first()

        assertNull(profile.audienceType)
        assertNull(profile.journeyStage)
        assertNull(profile.ageGroup)
        assertNull(profile.regionalCenter)
    }

    @Test
    fun `malformed ZIP remains incomplete when stored completion flag is true`() = runTest {
        val store = createStore(profileFile())
        store.edit { preferences ->
            preferences[booleanPreferencesKey("onboarding_completed")] = true
            preferences[stringPreferencesKey("audience_type")] = "family"
            preferences[stringPreferencesKey("zip_code")] = "9000A"
            preferences[stringPreferencesKey("journey_stage")] = "exploring"
        }

        val profile = DataStoreUserProfileRepository(store).profile.first()

        assertTrue(profile.onboardingCompleted)
        assertFalse(profile.isComplete)
    }

    @Test
    fun `clearProfile removes every key and emits the default profile`() = runTest {
        val store = createStore(profileFile())
        val repository = DataStoreUserProfileRepository(store)
        repository.replaceProfile(
            UserProfile(
                onboardingCompleted = true,
                audienceType = AudienceType.FAMILY,
                zipCode = "90001",
                regionalCenter = RegionalCenterIdentity(
                    id = 42,
                    name = "North Los Angeles County Regional Center",
                    shortName = "NLACRC"
                ),
                journeyStage = JourneyStage.WAITING_FOR_INTAKE,
                ageGroup = AgeGroup.ALL_AGES
            )
        )

        repository.clearProfile()

        assertTrue(store.data.first().asMap().isEmpty())
        assertEquals(UserProfile(), repository.profile.first())
    }

    private fun profileFile(): File = temporaryFolder.newFile("user_profile.preferences_pb")

    private fun completeProfile(zipCode: String) = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.FAMILY,
        zipCode = zipCode,
        regionalCenter = null,
        journeyStage = JourneyStage.EXPLORING,
        ageGroup = AgeGroup.ALL_AGES
    )

    private fun TestScope.createStoreScope(): CoroutineScope =
        CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
            .also(storeScopes::add)

    private fun TestScope.createStore(
        file: File,
        scope: CoroutineScope = createStoreScope()
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { file }
    )

    private fun failingStore(failure: Throwable): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw failure }

            override suspend fun updateData(
                transform: suspend (Preferences) -> Preferences
            ): Preferences = error("updateData should not be called")
        }
}
