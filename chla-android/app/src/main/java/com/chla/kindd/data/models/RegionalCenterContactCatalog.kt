package com.chla.kindd.data.models

/** Stable LA County contact fallback matching the iPhone's bundled center catalog. */
internal object RegionalCenterContactCatalog {
    private val phoneByAcronym = mapOf(
        "NLACRC" to "(818) 778-1900",
        "WRC" to "(310) 258-4000",
        "SCLARC" to "(213) 744-7000",
        "ELARC" to "(626) 299-4700",
        "HRC" to "(310) 540-1711",
        "FDLRC" to "(213) 383-1300",
        "SGPRC" to "(909) 620-7722"
    )

    fun phoneFor(shortName: String?): String? = shortName
        ?.trim()
        ?.uppercase()
        ?.replace("/", "")
        ?.let(phoneByAcronym::get)
}
