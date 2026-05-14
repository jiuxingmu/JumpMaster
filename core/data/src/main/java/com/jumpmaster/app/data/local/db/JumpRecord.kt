package com.jumpmaster.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "jump_records")
data class JumpRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val count: Int,
    val calories: Int,
    val durationMs: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)