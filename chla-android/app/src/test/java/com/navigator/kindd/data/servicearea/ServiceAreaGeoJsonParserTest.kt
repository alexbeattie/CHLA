package com.navigator.kindd.data.servicearea

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ServiceAreaGeoJsonParserTest {

    @Test
    fun `parser converts GeoJSON longitude latitude pairs to latitude longitude coordinates`() {
        val result = parser().parse(
            """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","properties":{"OBJECTID":1,"REGIONALCENTER":"Example","ACRONYM":"EX","CATCHMENT_AREA_DESC":"Example area"},"geometry":{"type":"Polygon","coordinates":[[[-118.25,34.05],[-118.20,34.05],[-118.20,34.10],[-118.25,34.05]]]}}
            ]}
            """.trimIndent()
        )

        assertTrue(result.isSuccess)
        val feature = result.getOrThrow().single()
        assertEquals(ServiceAreaCoordinate(latitude = 34.05, longitude = -118.25), feature.polygons.single().first())
    }

    @Test
    fun `parser keeps MultiPolygon outer rings and excludes interior rings`() {
        val result = parser().parse(
            """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","properties":{"OBJECTID":2,"REGIONALCENTER":"Example","ACRONYM":"EX","CATCHMENT_AREA_DESC":"Example area"},"geometry":{"type":"MultiPolygon","coordinates":[
                [[[-118.30,34.00],[-118.20,34.00],[-118.20,34.10],[-118.30,34.00]],[[-118.28,34.02],[-118.25,34.02],[-118.25,34.05],[-118.28,34.02]]],
                [[[-118.10,34.00],[-118.00,34.00],[-118.00,34.10],[-118.10,34.00]]]
              ]}}
            ]}
            """.trimIndent()
        )

        assertTrue(result.isSuccess)
        val polygons = result.getOrThrow().single().polygons
        assertEquals(2, polygons.size)
        assertEquals(4, polygons.first().size)
        assertEquals(ServiceAreaCoordinate(latitude = 34.0, longitude = -118.30), polygons.first().first())
    }

    @Test
    fun `parser skips unsupported and malformed geometries without emitting corrupt coordinates`() {
        val result = parser().parse(
            """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","properties":{"OBJECTID":3,"REGIONALCENTER":"Unsupported","ACRONYM":"UN","CATCHMENT_AREA_DESC":"Example"},"geometry":{"type":"LineString","coordinates":[[-118.0,34.0],[-117.0,35.0]]}},
              {"type":"Feature","properties":{"OBJECTID":4,"REGIONALCENTER":"Malformed","ACRONYM":"MA","CATCHMENT_AREA_DESC":"Example"},"geometry":{"type":"Polygon","coordinates":[[[-118.0],["bad",34.0]]]}},
              {"type":"Feature","properties":{"OBJECTID":5,"REGIONALCENTER":"Valid","ACRONYM":"VA","CATCHMENT_AREA_DESC":"Example"},"geometry":{"type":"Polygon","coordinates":[[[-118.0,34.0],[-117.0,34.0],[-117.0,35.0],[-118.0,34.0]]]}}
            ]}
            """.trimIndent()
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("VA"), result.getOrThrow().map { it.acronym })
    }

    @Test
    fun `bundled asset has the iPhone checksum and seven expected service areas`() {
        val asset = File("src/main/res/raw/la_regional_centers.geojson")
        assertTrue("Bundled GeoJSON asset is missing", asset.isFile)
        assertEquals(
            "27bcaa63cb143e55abe9cdfccbf52b86f02522f28da3f280a2d8a001bd28070b",
            asset.inputStream().use { stream ->
                MessageDigest.getInstance("SHA-256").digest(stream.readBytes()).joinToString("") { "%02x".format(it) }
            }
        )

        val result = parser().parse(asset.readText())

        assertTrue(result.isSuccess)
        val areas = result.getOrThrow()
        assertEquals(7, areas.size)
        assertTrue(areas.all { it.polygons.isNotEmpty() && it.polygons.all(List<ServiceAreaCoordinate>::isNotEmpty) })
        assertEquals(
            setOf("NLACRC", "FDLRC", "HRC", "SCLARC", "ELARC", "WRC", "SG/PRC"),
            areas.map { it.acronym }.toSet()
        )
    }

    @Test
    fun `bundled data source rethrows cancellation and caches only successful loads`() = runBlocking {
        val cancelledSource = BundledServiceAreaDataSource(
            resourceReader = { throw CancellationException("cancel test") }
        )
        try {
            cancelledSource.getServiceAreas()
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Cancellation must escape the data source to preserve structured concurrency.
        }

        var reads = 0
        val cachingSource = BundledServiceAreaDataSource(
            resourceReader = {
                reads += 1
                """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"OBJECTID":6,"REGIONALCENTER":"Cached","ACRONYM":"CA","CATCHMENT_AREA_DESC":"Example"},"geometry":{"type":"Polygon","coordinates":[[[-118.0,34.0],[-117.0,34.0],[-117.0,35.0],[-118.0,34.0]]]}}]}""".reader()
            }
        )

        val first = cachingSource.getServiceAreas()
        val second = cachingSource.getServiceAreas()
        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(1, reads)
        @Suppress("UNCHECKED_CAST")
        val mutableView = first.getOrThrow() as MutableList<ServiceAreaFeature>
        try {
            mutableView.clear()
            fail("Expected cached service areas to be immutable")
        } catch (_: UnsupportedOperationException) {
            // Cached results must not be mutable by callers.
        }
    }

    private fun parser(): ServiceAreaGeoJsonParser = ServiceAreaGeoJsonParser()
}
