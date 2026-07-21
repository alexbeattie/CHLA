package com.chla.kindd.data.discovery

import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.source.ProviderDiscoveryDataSource
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

sealed interface ProviderDiscoveryCall {
    val limit: Int

    data class Catalog(
        override val limit: Int,
        val page: Int
    ) : ProviderDiscoveryCall

    data class Comprehensive(
        val request: ComprehensiveProviderRequest,
        override val limit: Int
    ) : ProviderDiscoveryCall

    data class ProfileZip(
        val request: RegionalCenterProviderRequest,
        override val limit: Int
    ) : ProviderDiscoveryCall
}

class PendingProviderResponse internal constructor(
    internal val result: CompletableDeferred<Result<List<Provider>>>,
    internal val ignoreCancellation: Boolean
) {
    fun succeed(providers: List<Provider>) {
        result.complete(Result.success(providers))
    }

    fun fail(exception: Throwable) {
        result.complete(Result.failure(exception))
    }
}

class FakeProviderDiscoveryDataSource : ProviderDiscoveryDataSource {
    private sealed interface Response {
        data class Immediate(val result: Result<List<Provider>>) : Response
        data class Pending(val response: PendingProviderResponse) : Response
    }

    val calls = mutableListOf<ProviderDiscoveryCall>()
    private val responses = ArrayDeque<Response>()

    fun enqueueProviders(vararg providers: Provider) {
        responses.addLast(Response.Immediate(Result.success(providers.toList())))
    }

    fun enqueueFailure(exception: Throwable) {
        responses.addLast(Response.Immediate(Result.failure(exception)))
    }

    fun enqueuePending(ignoreCancellation: Boolean = true): PendingProviderResponse {
        val response = PendingProviderResponse(
            result = CompletableDeferred(),
            ignoreCancellation = ignoreCancellation
        )
        responses.addLast(Response.Pending(response))
        return response
    }

    override suspend fun getProviderCatalog(
        limit: Int,
        page: Int
    ): Result<List<Provider>> = respond(ProviderDiscoveryCall.Catalog(limit, page))

    override suspend fun searchProviders(
        request: ComprehensiveProviderRequest,
        limit: Int
    ): Result<List<Provider>> = respond(
        ProviderDiscoveryCall.Comprehensive(request, limit)
    )

    override suspend fun getProvidersByRegionalCenter(
        request: RegionalCenterProviderRequest,
        limit: Int
    ): Result<List<Provider>> = respond(ProviderDiscoveryCall.ProfileZip(request, limit))

    private suspend fun respond(call: ProviderDiscoveryCall): Result<List<Provider>> {
        calls += call
        return when (val response = responses.pollFirst()) {
            is Response.Immediate -> response.result
            is Response.Pending -> if (response.response.ignoreCancellation) {
                withContext(NonCancellable) { response.response.result.await() }
            } else {
                response.response.result.await()
            }
            null -> error("No fake provider response queued")
        }
    }
}
