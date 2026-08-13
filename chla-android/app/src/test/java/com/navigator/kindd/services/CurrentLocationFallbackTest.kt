package com.navigator.kindd.services

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentLocationFallbackTest {

    @Test
    fun `cached location is returned without requesting a current fix`() = runTest {
        var requestedCurrent = false

        val result = resolveCachedOrCurrentLocation(
            timeoutMillis = 500,
            cachedLocation = { "cached" },
            currentLocation = {
                requestedCurrent = true
                "current"
            }
        )

        assertEquals("cached", result)
        assertFalse(requestedCurrent)
    }

    @Test
    fun `missing cache requests and returns a current fix`() = runTest {
        val result = resolveCachedOrCurrentLocation(
            timeoutMillis = 500,
            cachedLocation = { null },
            currentLocation = { "current" }
        )

        assertEquals("current", result)
    }

    @Test
    fun `current fix is cancelled and returns null when the bound expires`() = runTest {
        var requestStarted = false
        var requestCancelled = false

        val result = resolveCachedOrCurrentLocation<String>(
            timeoutMillis = 500,
            cachedLocation = { null },
            currentLocation = {
                requestStarted = true
                try {
                    awaitCancellation()
                } finally {
                    requestCancelled = true
                }
            }
        )

        assertNull(result)
        assertTrue(requestStarted)
        assertTrue(requestCancelled)
    }

    @Test
    fun `caller cancellation propagates to the current fix`() = runTest {
        var requestCancelled = false
        val job = launch {
            resolveCachedOrCurrentLocation<String>(
                timeoutMillis = 10_000,
                cachedLocation = { null },
                currentLocation = {
                    try {
                        awaitCancellation()
                    } finally {
                        requestCancelled = true
                    }
                }
            )
        }
        yield()

        job.cancelAndJoin()

        assertTrue(requestCancelled)
    }
}
