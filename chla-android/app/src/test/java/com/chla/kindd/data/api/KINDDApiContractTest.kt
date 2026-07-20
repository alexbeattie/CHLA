package com.chla.kindd.data.api

import com.chla.kindd.data.repository.ProviderRepository
import com.chla.kindd.data.repository.RegionalCenterRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Query

class KINDDApiContractTest {
    private val server = MockWebServer()
    private val api = Retrofit.Builder()
        .baseUrl(server.url("/api/"))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(KINDDApi::class.java)

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `provider text search uses comprehensive search and enforces its limit`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                [
                  {
                    "id": "00000000-0000-0000-0000-000000000011",
                    "name": "First Provider"
                  },
                  {
                    "id": "00000000-0000-0000-0000-000000000012",
                    "name": "Second Provider"
                  }
                ]
                """.trimIndent()
            )
        )

        val providers = ProviderRepository(api).searchProviders("ABA", limit = 1).getOrThrow()

        assertEquals(listOf("First Provider"), providers.map { it.name })
        assertEquals(
            "/api/providers-v2/comprehensive_search/?q=ABA&limit=1",
            server.takeRequest().path
        )
    }

    @Test
    fun `provider nearby search uses working comprehensive endpoint and enforces its limit`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                [
                  {
                    "id": "00000000-0000-0000-0000-000000000021",
                    "name": "Farther Provider",
                    "latitude": 34.5000,
                    "longitude": -118.5000
                  },
                  {
                    "id": "00000000-0000-0000-0000-000000000022",
                    "name": "Nearest Provider",
                    "latitude": 34.0523,
                    "longitude": -118.2437
                  }
                ]
                """.trimIndent()
            )
        )

        val providers = ProviderRepository(api)
            .getProvidersNearby(34.0522, -118.2437, radiusMiles = 25, limit = 1)
            .getOrThrow()

        assertEquals(listOf("Nearest Provider"), providers.map { it.name })
        assertTrue(providers.single().distance != null)
        assertEquals(
            "/api/providers-v2/comprehensive_search/?lat=34.0522&lng=-118.2437&radius=25&limit=1",
            server.takeRequest().path
        )
    }

    @Test
    fun `regional center ZIP lookup uses deployed endpoint`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "id": 64,
                  "regional_center": "South Central Los Angeles Regional Center"
                }
                """.trimIndent()
            )
        )

        val center = RegionalCenterRepository(api).getRegionalCenterByZip("90001").getOrThrow()

        assertEquals("South Central Los Angeles Regional Center", center.name)
        assertEquals(
            "/api/regional-centers/by_zip_code/?zip_code=90001",
            server.takeRequest().path
        )
    }

    @Test
    fun `regional center list keeps only Los Angeles County centers`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "count": 2,
                  "next": null,
                  "previous": null,
                  "results": [
                    {
                      "id": 64,
                      "regional_center": "South Central Los Angeles Regional Center",
                      "county_served": "Los Angeles"
                    },
                    {
                      "id": 42,
                      "regional_center": "Golden Gate Regional Center",
                      "county_served": "Marin"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val centers = RegionalCenterRepository(api).getRegionalCenters().getOrThrow()

        assertEquals(
            listOf("South Central Los Angeles Regional Center"),
            centers.map { it.name }
        )
    }

    @Test
    fun `regional center provider search unwraps results envelope`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "count": 1,
                  "regional_center": {
                    "id": 64,
                    "name": "South Central Los Angeles Regional Center",
                    "zip_codes": ["90001"]
                  },
                  "results": [
                    {
                      "id": "00000000-0000-0000-0000-000000000003",
                      "name": "Test Provider",
                      "insurance_accepted": "Medi-Cal",
                      "insurance_carriers": []
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val providers = ProviderRepository(api)
            .getProvidersByRegionalCenter("90001")
            .getOrThrow()

        assertEquals(listOf("Test Provider"), providers.map { it.name })
        assertEquals(
            "/api/providers-v2/by_regional_center/?zip_code=90001",
            server.takeRequest().path
        )
    }

    @Test
    fun `regional center provider age filter uses deployed query name`() {
        val method = KINDDApi::class.java.declaredMethods
            .single { it.name == "getProvidersByRegionalCenter" }
        val queryNames = method.parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Query>() }
            .map(Query::value)

        assertTrue("age" in queryNames)
        assertTrue("age_group" !in queryNames)
    }

    @Test
    fun `regional center nearby search sends lng`() = runBlocking {
        server.enqueue(jsonResponse("[]"))

        val centers = RegionalCenterRepository(api)
            .getRegionalCentersNearby(34.0522, -118.2437)
            .getOrThrow()

        assertTrue(centers.isEmpty())
        assertEquals(
            "/api/regional-centers/nearby/?lat=34.0522&lng=-118.2437",
            server.takeRequest().path
        )
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
