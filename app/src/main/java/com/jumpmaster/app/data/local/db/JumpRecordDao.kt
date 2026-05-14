package com.jumpmaster.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface JumpRecordDao {

    @Insert
    suspend fun insertRecord(record: JumpRecord)

    @Insert
    suspend fun insertRecords(records: List<JumpRecord>)

    @Query("SELECT * FROM jump_records ORDER BY createdAt DESC")
    fun getAllRecordsFlow(): Flow<List<JumpRecord>>

    @Query("SELECT * FROM jump_records ORDER BY createdAt DESC")
    suspend fun getAllRecords(): List<JumpRecord>

    @Query("SELECT * FROM jump_records WHERE strftime('%Y-%m', createdAt) = :month ORDER BY createdAt DESC")
    suspend fun getRecordsByMonth(month: String): List<JumpRecord>

    @Query("SELECT strftime('%Y-%m', createdAt) as month, COUNT(*) as count FROM jump_records GROUP BY month ORDER BY month DESC")
    suspend fun getMonthStatistics(): List<MonthStat>

    @Query("SELECT SUM(count) FROM jump_records")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(DISTINCT strftime('%Y-%m-%d', createdAt)) FROM jump_records")
    suspend fun getTotalDays(): Int

    @Query("DELETE FROM jump_records")
    suspend fun deleteAllRecords()

    @Transaction
    @Query("SELECT * FROM jump_records ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestRecord(): JumpRecord?
}

data class MonthStat(
    val month: String,
    val count: Int,
)