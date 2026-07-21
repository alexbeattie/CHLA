package com.chla.kindd.data.repository

import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.source.LookupFailure
import com.chla.kindd.data.source.RegionalCenterDataSource
import com.chla.kindd.data.source.RegionalCenterLookup
import com.chla.kindd.di.IoDispatcher
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RegionalCenterRepository(
    private val api: KINDDApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RegionalCenterDataSource {
    override suspend fun getRegionalCenters(): Result<List<RegionalCenter>> =
        regionalCenterResult {
            api.getRegionalCenters().results.filter { center ->
                center.countyServed.equals("Los Angeles", ignoreCase = true)
            }
        }

    override suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup =
        withContext(ioDispatcher) {
            try {
                RegionalCenterLookup.Matched(api.getRegionalCenterByZip(zipCode))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                when {
                    exception is HttpException && exception.code() == 404 ->
                        RegionalCenterLookup.Unmatched
                    exception is SocketTimeoutException ->
                        RegionalCenterLookup.Unavailable(LookupFailure.TIMEOUT)
                    exception is IOException ->
                        RegionalCenterLookup.Unavailable(LookupFailure.NETWORK)
                    exception is HttpException ->
                        RegionalCenterLookup.Unavailable(LookupFailure.SERVER)
                    else -> RegionalCenterLookup.Unavailable(LookupFailure.UNKNOWN)
                }
            }
        }

    override suspend fun getRegionalCentersNearby(
        latitude: Double,
        longitude: Double
    ): Result<List<RegionalCenter>> = regionalCenterResult {
        api.getRegionalCentersNearby(latitude, longitude)
    }

    private suspend fun <T> regionalCenterResult(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            try {
                Result.success(block())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
}
