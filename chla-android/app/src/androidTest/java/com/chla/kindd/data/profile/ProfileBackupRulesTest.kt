package com.chla.kindd.data.profile

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chla.kindd.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class ProfileBackupRulesTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun profileDataStoreUsesTheExcludedRelativeFilePath() {
        val relativePath = context.preferencesDataStoreFile(PROFILE_DATASTORE_NAME)
            .relativeTo(context.filesDir)
            .invariantSeparatorsPath

        assertEquals(PROFILE_DATASTORE_PATH, relativePath)
    }

    @Test
    fun legacyCloudBackupExcludesTheProfileDataStoreFile() {
        assertTrue(
            exclusionsIn(R.xml.backup_rules).contains(
                Exclusion(
                    parent = "full-backup-content",
                    domain = "file",
                    path = PROFILE_DATASTORE_PATH
                )
            )
        )
    }

    @Test
    fun android12CloudBackupAndDeviceTransferExcludeTheProfileDataStoreFile() {
        val exclusions = exclusionsIn(R.xml.data_extraction_rules)

        assertTrue(
            exclusions.contains(
                Exclusion(
                    parent = "cloud-backup",
                    domain = "file",
                    path = PROFILE_DATASTORE_PATH
                )
            )
        )
        assertTrue(
            exclusions.contains(
                Exclusion(
                    parent = "device-transfer",
                    domain = "file",
                    path = PROFILE_DATASTORE_PATH
                )
            )
        )
    }

    private fun exclusionsIn(resourceId: Int): Set<Exclusion> {
        val parser = context.resources.getXml(resourceId)
        val exclusions = mutableSetOf<Exclusion>()
        var currentParent: String? = null
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "full-backup-content", "cloud-backup", "device-transfer" -> {
                        currentParent = parser.name
                    }
                    "exclude" -> exclusions += Exclusion(
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
        return exclusions
    }

    private data class Exclusion(
        val parent: String?,
        val domain: String?,
        val path: String?
    )

    private companion object {
        const val PROFILE_DATASTORE_PATH = "datastore/user_profile.preferences_pb"
    }
}
