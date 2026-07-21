package com.chla.kindd.data.servicearea

import com.chla.kindd.di.IoDispatcher
import java.io.Reader
import java.util.Collections
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads and caches the bundled GeoJSON resource on the injected IO dispatcher. */
class BundledServiceAreaDataSource(
    private val resourceReader: () -> Reader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val parser: ServiceAreaGeoJsonParser = ServiceAreaGeoJsonParser()
) : ServiceAreaDataSource {
    private val cacheLock = Mutex()

    @Volatile
    private var cachedServiceAreas: List<ServiceAreaFeature>? = null

    override suspend fun getServiceAreas(): Result<List<ServiceAreaFeature>> = withContext(ioDispatcher) {
        cachedServiceAreas?.let { Result.success(it) } ?: cacheLock.withLock {
            cachedServiceAreas?.let { Result.success(it) } ?: loadServiceAreas()
        }
    }

    private fun loadServiceAreas(): Result<List<ServiceAreaFeature>> = try {
        resourceReader().use { reader ->
            parser.parse(reader).map { features ->
                Collections.unmodifiableList(features.toList()).also { cachedServiceAreas = it }
            }
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
