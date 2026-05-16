package com.hasanzade.hackathonmobile.data.repository

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.safeApiCall
import com.hasanzade.hackathonmobile.domain.model.AiAnalysisModel
import com.hasanzade.hackathonmobile.domain.repository.AiRepository
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val api: ApiService
) : AiRepository {

    override suspend fun getAiAnalysis(): NetworkResult<AiAnalysisModel> {
        return when (val result = safeApiCall { api.getAiAnalysis() }) {
            is NetworkResult.Success -> {
                val dto = result.data
                NetworkResult.Success(
                    AiAnalysisModel(
                        riskScore       = dto.riskScore ?: 0,
                        riskLevel       = dto.riskLevel ?: "--",
                        recommendations = dto.recommendations ?: emptyList(),
                        summary         = dto.summary ?: "--"
                    )
                )
            }
            is NetworkResult.Error   -> result
            is NetworkResult.Loading -> result
        }
    }
}