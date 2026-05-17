package com.hasanzade.hackathonmobile.data.repository

import com.hasanzade.hackathonmobile.ui.SavedProductEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchRepository @Inject constructor() {

    private val _savedBatches = MutableStateFlow<List<SavedProductEntry>>(emptyList())
    val savedBatches: StateFlow<List<SavedProductEntry>> = _savedBatches

    fun addBatch(entry: SavedProductEntry) {
        _savedBatches.value = _savedBatches.value + entry
    }

    fun clear() {
        _savedBatches.value = emptyList()
    }
}