package com.chla.kindd.data.api

import com.google.gson.JsonParser
import com.chla.kindd.data.discovery.ComprehensiveProviderRequest
import com.chla.kindd.data.discovery.RegionalCenterProviderRequest
import com.chla.kindd.data.repository.ProviderRepository
import com.chla.kindd.data.repository.RegionalCenterRepository
import com.chla.kindd.data.source.LookupFailure
import com.chla.kindd.data.source.RegionalCenterLookup
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `response report posts the exact fixed JSON to the LLM endpoint`() = runBlocking {
        server.enqueue(jsonResponse("""{"id":17,"status":"received"}"""))

        val response = api.reportAssistantResponse(
            AssistantResponseReportRequest(
                reason = AssistantResponseReportReason.INACCURATE_OR_MISLEADING,
                reportedResponse = "An assistant answer",
                locale = "es-US",
                appVersion = "1.4.1",
                responseFingerprint = "opaque-signed-token"
            )
        )
        val recorded = server.takeRequest()
        val json = JsonParser().parse(recorded.body.readUtf8()).asJsonObject

        assertEquals("POST", recorded.method)
        assertEquals("/api/llm/response-reports/", recorded.path)
        assertEquals(17L, response.id)
        assertEquals("received", response.status)
        assertEquals(
            setOf(
                "reason",
                "reported_response",
                "locale",
                "platform",
                "app_version",
                "response_fingerprint"
            ),
            json.keySet()
        )
        assertEquals("inaccurate_or_misleading", json["reason"].asString)
        assertEquals("An assistant answer", json["reported_response"].asString)
        assertEquals("es-US", json["locale"].asString)
        assertEquals("android", json["platform"].asString)
        assertEquals("1.4.1", json["app_version"].asString)
        assertEquals("opaque-signed-token", json["response_fingerprint"].asString)
    }

    @Test
    fun `ask response decodes additive response fingerprint`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "query": "A question",
                  "answer": "An assistant answer",
                  "providers_referenced": [],
                  "regional_center": null,
                  "response_fingerprint": "opaque-signed-token"
                }
                """.trimIndent()
            )
        )

        val response = api.askLLM(LLMRequest(query = "A question"))

        assertEquals("opaque-signed-token", response.responseFingerprint)
    }

    @Test
    fun `provider text compatibility search uses comprehensive search and caps after decoding`() = runBlocking {
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
        val request = server.takeRequest().requestUrl!!

        assertEquals(listOf("First Provider"), providers.map { it.name })
        assertEquals("/api/providers-v2/comprehensive_search/", request.encodedPath)
        assertEquals("ABA", request.queryParameter("q"))
        assertNull(request.queryParameter("limit"))
    }

    @Test
    fun `provider nearby compatibility search sorts and caps after decoding`() = runBlocking {
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
        val request = server.takeRequest().requestUrl!!

        assertEquals(listOf("Nearest Provider"), providers.map { it.name })
        assertTrue(providers.single().distance != null)
        assertEquals("/api/providers-v2/comprehensive_search/", request.encodedPath)
        assertEquals("34.0522", request.queryParameter("lat"))
        assertEquals("-118.2437", request.queryParameter("lng"))
        assertEquals("25", request.queryParameter("radius"))
        assertNull(request.queryParameter("limit"))
    }

    @Test
    fun `comprehensive search sends every exact query and caps after decoding`() = runBlocking {
        server.enqueue(twoProviderResponse())

        val providers = ProviderRepository(api).searchProviders(
            request = ComprehensiveProviderRequest(
                query = "ABA",
                latitude = 34.0522,
                longitude = -118.2437,
                radiusMiles = 15,
                therapyTypes = listOf("ABA Therapy", "Speech Therapy"),
                ageGroup = "6-12",
                diagnosis = "Autism",
                insurance = "Medi-Cal"
            ),
            limit = 1
        ).getOrThrow()
        val request = server.takeRequest().requestUrl!!

        assertEquals(listOf("First Provider"), providers.map { it.name })
        assertEquals("/api/providers-v2/comprehensive_search/", request.encodedPath)
        assertEquals("ABA", request.queryParameter("q"))
        assertEquals("34.0522", request.queryParameter("lat"))
        assertEquals("-118.2437", request.queryParameter("lng"))
        assertEquals("15", request.queryParameter("radius"))
        assertEquals(
            listOf("ABA Therapy", "Speech Therapy"),
            request.queryParameterValues("therapy")
        )
        assertEquals("6-12", request.queryParameter("age"))
        assertEquals("Autism", request.queryParameter("diagnosis"))
        assertEquals("Medi-Cal", request.queryParameter("insurance"))
        assertNull(request.queryParameter("limit"))
    }

    @Test
    fun `Los Angeles comprehensive search without coordinates omits location queries`() = runBlocking {
        server.enqueue(jsonResponse("[]"))

        ProviderRepository(api).searchProviders(
            request = ComprehensiveProviderRequest(query = "ABA", radiusMiles = 15),
            limit = 50
        ).getOrThrow()
        val request = server.takeRequest().requestUrl!!

        assertEquals("ABA", request.queryParameter("q"))
        assertNull(request.queryParameter("lat"))
        assertNull(request.queryParameter("lng"))
        assertNull(request.queryParameter("radius"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `comprehensive request rejects a half coordinate pair`() {
        ComprehensiveProviderRequest(latitude = 34.0522)
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

        val lookup = RegionalCenterRepository(api).lookupRegionalCenter("90001")
        assertTrue(lookup is RegionalCenterLookup.Matched)
        val center = (lookup as RegionalCenterLookup.Matched).center

        assertEquals("South Central Los Angeles Regional Center", center.name)
        assertEquals(
            "/api/regional-centers/by_zip_code/?zip_code=90001",
            server.takeRequest().path
        )
    }

    @Test
    fun `regional center ZIP lookup maps 404 to unmatched`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        val lookup = RegionalCenterRepository(api).lookupRegionalCenter("90001")

        assertEquals(RegionalCenterLookup.Unmatched, lookup)
    }

    @Test
    fun `regional center ZIP lookup sanitizes server failures`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        val lookup = RegionalCenterRepository(api).lookupRegionalCenter("90001")

        assertEquals(
            RegionalCenterLookup.Unavailable(LookupFailure.SERVER),
            lookup
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
    fun `regional center provider search sends exact ZIP filters and caps after decoding`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "count": 2,
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
                    },
                    {
                      "id": "00000000-0000-0000-0000-000000000004",
                      "name": "Second Provider",
                      "insurance_accepted": "Medi-Cal",
                      "insurance_carriers": []
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val providers = ProviderRepository(api)
            .getProvidersByRegionalCenter(
                request = RegionalCenterProviderRequest(
                    zipCode = "90001",
                    ageGroup = "6-12",
                    diagnosis = "Autism",
                    insurance = "Medi-Cal"
                ),
                limit = 1
            )
            .getOrThrow()
        val request = server.takeRequest().requestUrl!!

        assertEquals(listOf("Test Provider"), providers.map { it.name })
        assertEquals("/api/providers-v2/by_regional_center/", request.encodedPath)
        assertEquals("90001", request.queryParameter("zip_code"))
        assertEquals("6-12", request.queryParameter("age"))
        assertEquals("Autism", request.queryParameter("diagnosis"))
        assertEquals("Medi-Cal", request.queryParameter("insurance"))
        assertNull(request.queryParameter("therapy"))
        assertNull(request.queryParameter("limit"))
    }

    @Test
    fun `regional center provider API exposes no therapy argument`() {
        val method = KINDDApi::class.java.declaredMethods
            .single { it.name == "getProvidersByRegionalCenter" }
        val queryNames = method.parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Query>() }
            .map(Query::value)

        assertEquals(setOf("zip_code", "insurance", "age", "diagnosis"), queryNames.toSet())
        assertFalse("therapy" in queryNames)
        assertFalse("age_group" in queryNames)
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

    private fun twoProviderResponse() = jsonResponse(
        """
        [
          {
            "id": "00000000-0000-0000-0000-000000000031",
            "name": "First Provider"
          },
          {
            "id": "00000000-0000-0000-0000-000000000032",
            "name": "Second Provider"
          }
        ]
        """.trimIndent()
    )
}
