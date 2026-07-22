package com.navigator.kindd

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.w3c.dom.Document

internal const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

internal fun releaseManifest(): Document {
    val releaseManifest = File(
        "build/intermediates/merged_manifests/release/" +
            "processReleaseManifest/AndroidManifest.xml"
    )
    assertTrue("Merged release manifest must exist", releaseManifest.isFile)
    return DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(releaseManifest)
}
