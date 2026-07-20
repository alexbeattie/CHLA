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
    fun legacyCloudBackupHasTheExactRuleInventory() {
        val rules = rulesIn(R.xml.backup_rules)

        assertEquals(2, rules.size)
        assertEquals(
            listOf(
                Rule(
                    type = "include",
                    parent = "full-backup-content",
                    domain = "sharedpref",
                    path = "."
                ),
                Rule(
                    type = "exclude",
                    parent = "full-backup-content",
                    domain = "sharedpref",
                    path = "device.xml"
                )
            ),
            rules
        )
    }

    @Test
    fun android12BackupHasTheExactCloudAndDeviceTransferRuleInventory() {
        val rules = rulesIn(R.xml.data_extraction_rules)

        assertEquals(3, rules.size)
        assertEquals(
            listOf(
                Rule(
                    type = "include",
                    parent = "cloud-backup",
                    domain = "sharedpref",
                    path = "."
                ),
                Rule(
                    type = "exclude",
                    parent = "cloud-backup",
                    domain = "sharedpref",
                    path = "device.xml"
                ),
                Rule(
                    type = "exclude",
                    parent = "device-transfer",
                    domain = "file",
                    path = PROFILE_DATASTORE_PATH
                )
            ),
            rules
        )
    }

    private fun rulesIn(resourceId: Int): List<Rule> {
        val parser = context.resources.getXml(resourceId)
        val rules = mutableListOf<Rule>()
        var currentParent: String? = null
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "full-backup-content", "cloud-backup", "device-transfer" -> {
                        currentParent = parser.name
                    }
                    "include", "exclude" -> rules += Rule(
                        type = parser.name,
                        parent = currentParent,
                        domain = parser.getAttributeValue(null, "domain"),
                        path = parser.getAttributeValue(null, "path")
                    )
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
        val type: String,
        val parent: String?,
        val domain: String?,
        val path: String?
    )

    private companion object {
        const val PROFILE_DATASTORE_PATH = "datastore/user_profile.preferences_pb"
    }
}
