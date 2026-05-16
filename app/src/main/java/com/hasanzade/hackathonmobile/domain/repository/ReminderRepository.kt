package com.hasanzade.hackathonmobile.domain.repository

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.domain.model.ReminderModel

interface ReminderRepository {
    suspend fun getActiveReminders(store: String, department: String?): NetworkResult<List<ReminderModel>>
    suspend fun resolveReminder(batchId: Long): NetworkResult<String>
}