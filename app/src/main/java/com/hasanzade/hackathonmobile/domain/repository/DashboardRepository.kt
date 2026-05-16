package com.hasanzade.hackathonmobile.domain.repository

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.domain.model.DashboardModel

interface DashboardRepository {
    suspend fun getDashboard(): NetworkResult<DashboardModel>
}