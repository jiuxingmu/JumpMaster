package com.jumpmaster.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumpmaster.app.data.local.db.JumpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CounterHomeState(
    val dailyTotals: Map<LocalDate, Int>,
    val totalJumps: Int,
    val sessionCount: Int,
) {
    companion object {
        val Empty = CounterHomeState(emptyMap(), 0, 0)
    }
}

@HiltViewModel
class CounterHomeViewModel @Inject constructor(
    private val jumpRepository: JumpRepository,
) : ViewModel() {

    val homeState: StateFlow<CounterHomeState> =
        jumpRepository
            .getAllRecordsFlow()
            .map { records ->
                val daily =
                    records.groupBy { r -> r.createdAt.toLocalDate() }
                        .mapValues { e -> e.value.sumOf { it.count } }
                CounterHomeState(
                    dailyTotals = daily,
                    totalJumps = records.sumOf { it.count },
                    sessionCount = records.size,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CounterHomeState.Empty,
            )
}
