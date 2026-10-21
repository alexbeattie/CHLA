package com.chla.kindd.data.survey

import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.api.SurveyResponseRequest
import com.chla.kindd.di.IoDispatcher
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface SurveyRepository {
    suspend fun submit(request: SurveyResponseRequest): SurveySubmitResult
}

class ApiSurveyRepository(
    private val api: KINDDApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SurveyRepository {
    override suspend fun submit(request: SurveyResponseRequest): SurveySubmitResult =
        withContext(ioDispatcher) {
            try {
                api.submitSurveyResponse(request)
                SurveySubmitResult.SUCCESS
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                if (exception is HttpException && exception.code() == 429) {
                    SurveySubmitResult.RATE_LIMITED
                } else {
                    SurveySubmitResult.FAILED
                }
            }
        }
}
