package com.navigator.kindd.di

import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModuleTest {

    @Test
    fun okHttpClient_hasNoHttpLoggingInterceptor() {
        val client = NetworkModule.provideOkHttpClient()

        assertTrue(client.interceptors.none {
            it.javaClass.name == "okhttp3.logging.HttpLoggingInterceptor"
        })
        assertTrue(client.networkInterceptors.none {
            it.javaClass.name == "okhttp3.logging.HttpLoggingInterceptor"
        })
    }
}
