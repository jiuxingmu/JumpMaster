package com.jumpmaster.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumpmaster.app.data.local.db.JumpRecord
import com.jumpmaster.app.data.local.db.JumpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DailySummary(
    val date: LocalDate,
    val sessionCount: Int,
    val totalCount: Int,
    val totalCalories: Int,
)

fun List<JumpRecord>.toDailySummaries(): List<DailySummary> =
    groupBy { it.createdAt.toLocalDate() }
        .map { (date, sessions) ->
            DailySummary(
                date = date,
                sessionCount = sessions.size,
                totalCount = sessions.sumOf { it.count },
                totalCalories = sessions.sumOf { it.calories },
            )
        }
        .sortedByDescending { it.date }

private fun Map<String, List<JumpRecord>>.toDailySummariesByMonth(): Map<String, List<DailySummary>> =
    mapValues { (_, monthRecords) -> monthRecords.toDailySummaries() }

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val jumpRepository: JumpRepository,
) : ViewModel() {

    private val _recordsByMonth = MutableStateFlow<Map<String, List<JumpRecord>>>(emptyMap())
    val recordsByMonth: StateFlow<Map<String, List<JumpRecord>>> = _recordsByMonth.asStateFlow()

    private val _dailySummariesByMonth = MutableStateFlow<Map<String, List<DailySummary>>>(emptyMap())
    val dailySummariesByMonth: StateFlow<Map<String, List<DailySummary>>> =
        _dailySummariesByMonth.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val records = jumpRepository.getRecordsGroupedByMonth()
                applyRecords(records)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun seedDemoRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                jumpRepository.seedDemoRecords()
                val records = jumpRepository.getRecordsGroupedByMonth()
                applyRecords(records)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun formatDate(record: JumpRecord): String = formatDate(record.createdAt.toLocalDate())

    fun formatDate(date: LocalDate): String =
        "${date.monthValue}月${date.dayOfMonth}日"

    fun formatWeekday(record: JumpRecord): String = formatWeekday(record.createdAt.toLocalDate())

    fun formatWeekday(date: LocalDate): String {
        val weekdays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
        return weekdays[date.dayOfWeek.value % 7]
    }

    private fun applyRecords(records: Map<String, List<JumpRecord>>) {
        _recordsByMonth.value = records
        _dailySummariesByMonth.value = records.toDailySummariesByMonth()
    }
}
