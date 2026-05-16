package com.hasanzade.hackathonmobile.domain.usecase

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.domain.model.DashboardModel
import com.hasanzade.hackathonmobile.domain.repository.DashboardRepository
import javax.inject.Inject

class GetDashboardUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    suspend operator fun invoke(): NetworkResult<DashboardModel> =
        repository.getDashboard()
}

class GetProductByBarcodeUseCase @Inject constructor(
    private val repository: com.hasanzade.hackathonmobile.domain.repository.ProductRepository
) {
    suspend operator fun invoke(barcode: String) =
        repository.getProductByBarcode(barcode)
}

class GetRemindersUseCase @Inject constructor(
    private val repository: com.hasanzade.hackathonmobile.domain.repository.ReminderRepository
) {
    suspend operator fun invoke(store: String, department: String?) =
        repository.getActiveReminders(store, department)
}

class ResolveReminderUseCase @Inject constructor(
    private val repository: com.hasanzade.hackathonmobile.domain.repository.ReminderRepository
) {
    suspend operator fun invoke(batchId: Long) =
        repository.resolveReminder(batchId)
}

class GetAiAnalysisUseCase @Inject constructor(
    private val repository: com.hasanzade.hackathonmobile.domain.repository.AiRepository
) {
    suspend operator fun invoke() =
        repository.getAiAnalysis()
}