package com.jumpmaster.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [JumpRecord::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class JumpDatabase : RoomDatabase() {

    abstract fun jumpRecordDao(): JumpRecordDao

    companion object {
        @Volatile
        private var INSTANCE: JumpDatabase? = null

        fun getInstance(context: Context): JumpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.inMemoryDatabaseBuilder(
                    context.applicationContext,
                    JumpDatabase::class.java,
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}