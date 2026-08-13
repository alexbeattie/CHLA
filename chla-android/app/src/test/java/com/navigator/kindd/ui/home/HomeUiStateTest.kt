package com.navigator.kindd.ui.home

import com.navigator.kindd.data.models.RegionalCenter
import com.navigator.kindd.data.models.RegionalCenterContactCatalog
import com.navigator.kindd.data.profile.RegionalCenterIdentity
import com.navigator.kindd.data.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun canonicalCenterPhoneIsAvailableBeforeNetworkHydration() {
        val profile = matchedProfile()

        assertEquals("2137447000", HomeUiState().dialDigitsFor(profile))
    }

    @Test
    fun hydratedApiPhoneRemainsAuthoritative() {
        val profile = matchedProfile()
        val apiCenter = RegionalCenter(
            id = 64,
            name = "South Central Los Angeles Regional Center",
            telephone = "(213) 555-0100"
        )
        val state = HomeUiState(
            hydratedIdentity = profile.regionalCenter,
            hydratedCenter = apiCenter
        )

        assertEquals("2135550100", state.dialDigitsFor(profile))
        assertEquals("(213) 555-0100", state.formattedPhoneFor(profile))
    }

    @Test
    fun bundledCatalogCoversEveryCanonicalLaCountyCenter() {
        val expected = mapOf(
            "NLACRC" to "(818) 778-1900",
            "WRC" to "(310) 258-4000",
            "SCLARC" to "(213) 744-7000",
            "ELARC" to "(626) 299-4700",
            "HRC" to "(310) 540-1711",
            "FDLRC" to "(213) 383-1300",
            "SG/PRC" to "(909) 620-7722"
        )

        expected.forEach { (shortName, phone) ->
            assertEquals(
                phone,
                RegionalCenterContactCatalog.phoneFor(shortName)
            )
        }
    }

    private fun matchedProfile() = UserProfile(
        regionalCenter = RegionalCenterIdentity(
            id = 64,
            name = "South Central Los Angeles Regional Center",
            shortName = "SCLARC"
        )
    )
}
