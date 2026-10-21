package com.chla.kindd.data.survey

import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.api.SurveyResponseRequest
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiSurveyRepositoryTest {
    private val server = MockWebServer()
    private val repository = ApiSurveyRepository(
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KINDDApi::class.java),
        ioDispatcher = Dispatchers.Unconfined
    )

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `created posts only answered contract keys to the survey endpoint`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":42,"status":"received"}""")
        )

        val result = repository.submit(
            SurveyAnswers(
                texts = mapOf(SurveyTextQuestion.DIAGNOSIS to "ADHD"),
                ratings = mapOf(SurveyRatingQuestion.HELPFULNESS to 4)
            ).toRequest(locale = "en", appVersion = "1.4.3")
        )
        val recorded = server.takeRequest()
        val json = JsonParser().parse(recorded.body.readUtf8()).asJsonObject

        assertEquals(SurveySubmitResult.SUCCESS, result)
        assertEquals("POST", recorded.method)
        assertEquals("/api/surveys/responses/", recorded.path)
        assertEquals(
            setOf("diagnosis", "helpfulness", "locale", "platform", "app_version"),
            json.keySet()
        )
        assertEquals("ADHD", json["diagnosis"].asString)
        assertEquals(4, json["helpfulness"].asInt)
        assertEquals("en", json["locale"].asString)
        assertEquals("android", json["platform"].asString)
        assertEquals("1.4.3", json["app_version"].asString)
    }

    @Test
    fun `too many requests maps to rate limited`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"detail":"Throttled"}"""))

        assertEquals(SurveySubmitResult.RATE_LIMITED, repository.submit(request()))
    }

    @Test
    fun `validation and server errors map to failed`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"non_field_errors":["x"]}"""))
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(SurveySubmitResult.FAILED, repository.submit(request()))
        assertEquals(SurveySubmitResult.FAILED, repository.submit(request()))
    }

    @Test
    fun `dropped connection maps to failed`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertEquals(SurveySubmitResult.FAILED, repository.submit(request()))
    }

    private fun request() = SurveyResponseRequest(
        improvements = "More weekend hours",
        locale = "es",
        appVersion = "1.4.3"
    )
}
