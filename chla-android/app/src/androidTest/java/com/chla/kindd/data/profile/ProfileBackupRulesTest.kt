package com.chla.kindd.data.profile

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chla.kindd.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class ProfileBackupRulesTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun profileDataStoreUsesTheExpectedRelativeFilePath() {
        val relativePath = context.preferencesDataStoreFile(PROFILE_DATASTORE_NAME)
            .relativeTo(context.filesDir)
            .invariantSeparatorsPath

        assertEquals(PROFILE_DATASTORE_PATH, relativePath)
    }

    @Test
    fun cloudBackupsAllowOnlySharedPreferencesAndExcludeDevicePreferences() {
        assertEquals(
            setOf(
                Rule(
                    parent = "full-backup-content",
                    domain = "sharedpref",
                    path = "."
                )
            ),
            includesIn(R.xml.backup_rules)
        )
        assertEquals(
            setOf(
                Rule(
                    parent = "full-backup-content",
                    domain = "sharedpref",
                    path = "device.xml"
                )
            ),
            exclusionsIn(R.xml.backup_rules)
        )

        assertEquals(
            setOf(
                Rule(
                    parent = "cloud-backup",
                    domain = "sharedpref",
                    path = "."
                )
            ),
            includesIn(R.xml.data_extraction_rules)
        )
        assertEquals(
            setOf(
                Rule(
                    parent = "cloud-backup",
                    domain = "sharedpref",
                    path = "device.xml"
                )
            ),
            exclusionsIn(R.xml.data_extraction_rules, parent = "cloud-backup")
        )
    }

    @Test
    fun deviceTransferExplicitlyExcludesOnlyTheProfileDataStoreFile() {
        assertEquals(
            emptySet<Rule>(),
            includesIn(R.xml.data_extraction_rules, parent = "device-transfer")
        )
        assertEquals(
            setOf(
                Rule(
                    parent = "device-transfer",
                    domain = "file",
                    path = PROFILE_DATASTORE_PATH
                )
            ),
            exclusionsIn(R.xml.data_extraction_rules, parent = "device-transfer")
        )
    }

    private fun includesIn(resourceId: Int, parent: String? = null): Set<Rule> =
        rulesIn(resourceId, tag = "include", parent = parent)

    private fun exclusionsIn(resourceId: Int, parent: String? = null): Set<Rule> =
        rulesIn(resourceId, tag = "exclude", parent = parent)

    private fun rulesIn(resourceId: Int, tag: String, parent: String?): Set<Rule> {
        val parser = context.resources.getXml(resourceId)
        val rules = mutableSetOf<Rule>()
        var currentParent: String? = null
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "full-backup-content", "cloud-backup", "device-transfer" -> {
                        currentParent = parser.name
                    }
                    tag -> if (parent == null || currentParent == parent) {
                        rules += Rule(
                            parent = currentParent,
                            domain = parser.getAttributeValue(null, "domain"),
                            path = parser.getAttributeValue(null, "path")
                        )
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == currentParent) {
                    currentParent = null
                }
            }
            event = parser.next()
        }

        parser.close()
        return rules
    }

    private data class Rule(
        val parent: String?,
        val domain: String?,
        val path: String?
    )

    private companion object {
        const val PROFILE_DATASTORE_PATH = "datastore/user_profile.preferences_pb"
    }
}
