package com.jumpmaster.app.data.local.db

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.jumpmaster.app.domain.session.JumpSessionCalories
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class JumpRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val db = JumpDatabase.getInstance(context)
    private val dao = db.jumpRecordDao()

    suspend fun saveRecord(count: Int, calories: Int, durationMs: Long) {
        val record = JumpRecord(
            count = count,
            calories = calories,
            durationMs = durationMs,
        )
        dao.insertRecord(record)
    }

    /** Persists a completed training session (C-02); IO on [Dispatchers.IO]. */
    suspend fun insertCompletedTrainingSession(jumpCount: Int, durationMs: Long) {
        withContext(Dispatchers.IO) {
            val calories = JumpSessionCalories.fromJumpCount(jumpCount)
            dao.insertRecord(
                JumpRecord(
                    count = jumpCount,
                    calories = calories,
                    durationMs = durationMs,
                ),
            )
            Log.d(TAG, "inserted training session jumpCount=$jumpCount durationMs=$durationMs")
        }
    }

    fun getAllRecordsFlow(): Flow<List<JumpRecord>> {
        return dao.getAllRecordsFlow()
    }

    suspend fun getAllRecords(): List<JumpRecord> {
        return dao.getAllRecords()
    }

    suspend fun getRecordsGroupedByMonth(): Map<String, List<JumpRecord>> {
        val records = dao.getAllRecords()
        return records.groupBy { record ->
            val dateTime = record.createdAt
            "${dateTime.year}/${String.format("%02d", dateTime.monthValue)}"
        }
    }

    suspend fun getTotalCount(): Int {
        return dao.getTotalCount()
    }

    suspend fun getTotalDays(): Int {
        return dao.getTotalDays()
    }

    suspend fun getLatestRecord(): JumpRecord? {
        return dao.getLatestRecord()
    }

    @WorkerThread
    suspend fun seedDemoRecords() {
        val now = LocalDateTime.now()
        val demoRecords =
            listOf(
                JumpRecord(count = 420, calories = 75, durationMs = 7 * 60_000L, createdAt = now.minusDays(1)),
                JumpRecord(count = 1420, calories = 175, durationMs = 7 * 60_000L, createdAt = now.minusDays(1)),
                JumpRecord(count = 560, calories = 100, durationMs = 9 * 60_000L, createdAt = now.minusDays(3)),
                JumpRecord(count = 680, calories = 122, durationMs = 11 * 60_000L, createdAt = now.minusDays(8)),
                JumpRecord(count = 760, calories = 136, durationMs = 12 * 60_000L, createdAt = now.minusDays(17)),
                JumpRecord(count = 510, calories = 91, durationMs = 8 * 60_000L, createdAt = now.minusMonths(1).minusDays(2)),
                JumpRecord(count = 635, calories = 114, durationMs = 10 * 60_000L, createdAt = now.minusMonths(1).minusDays(9)),
                JumpRecord(count = 790, calories = 142, durationMs = 13 * 60_000L, createdAt = now.minusMonths(2).minusDays(4)),
                JumpRecord(count = 860, calories = 154, durationMs = 14 * 60_000L, createdAt = now.minusMonths(2).minusDays(12)),
            )
        dao.deleteAllRecords()
        dao.insertRecords(demoRecords)
    }

    private companion object {
        private const val TAG = "JumpRepository"
    }
}