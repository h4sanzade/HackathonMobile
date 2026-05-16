package com.hasanzade.hackathonmobile.domain.repository

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.domain.model.AiAnalysisModel

interface AiRepository {
    suspend fun getAiAnalysis(): NetworkResult<AiAnalysisModel>
}