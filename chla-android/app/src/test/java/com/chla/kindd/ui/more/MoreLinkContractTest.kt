package com.chla.kindd.ui.more

import com.chla.kindd.ui.screens.KINDD_PRIVACY_URL
import com.chla.kindd.ui.screens.KINDD_TERMS_URL
import com.chla.kindd.ui.screens.KINDD_WEBSITE_URL
import org.junit.Assert.assertEquals
import org.junit.Test

class MoreLinkContractTest {

    @Test
    fun legalAndWebsiteLinks_useTheCurrentKinddOrgSourceOfTruth() {
        assertEquals("https://kinddhelp.org", KINDD_WEBSITE_URL)
        assertEquals("https://kinddhelp.org/privacy", KINDD_PRIVACY_URL)
        assertEquals("https://kinddhelp.org/terms", KINDD_TERMS_URL)
    }
}
