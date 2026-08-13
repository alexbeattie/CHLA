package com.navigator.kindd.data.repository

import com.navigator.kindd.data.api.KINDDApi
import java.lang.reflect.Proxy
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test

class RepositoryCancellationTest {
    private val api = Proxy.newProxyInstance(
        KINDDApi::class.java.classLoader,
        arrayOf(KINDDApi::class.java)
    ) { _, _, _ ->
        throw CancellationException("private cancellation detail")
    } as KINDDApi

    @Test
    fun `provider repository rethrows cancellation`() = runBlocking {
        assertCancellation {
            ProviderRepository(api).getProviders()
        }
    }

    @Test
    fun `regional center lookup rethrows cancellation`() = runBlocking {
        assertCancellation {
            RegionalCenterRepository(api).lookupRegionalCenter("90001")
        }
    }

    private suspend fun assertCancellation(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected: structured concurrency cancellation must escape repositories.
        }
    }
}
