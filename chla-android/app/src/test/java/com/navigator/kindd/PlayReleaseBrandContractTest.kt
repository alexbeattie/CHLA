package com.navigator.kindd

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayReleaseBrandContractTest {

    @Test
    fun userFacingBrandCopyUsesKiNDDCapitalization() {
        listOf(
            File("src/main/res/values/strings.xml"),
            File("src/main/res/values-es/strings.xml"),
            File("src/main/java/com/navigator/kindd/ui/screens/AboutScreen.kt"),
            File("src/main/java/com/navigator/kindd/ui/screens/FAQScreen.kt")
        ).forEach { file ->
            assertFalse(
                "Legacy user-facing KINDD copy remains in ${file.path}",
                file.readText().contains("KINDD")
            )
        }
    }
}
