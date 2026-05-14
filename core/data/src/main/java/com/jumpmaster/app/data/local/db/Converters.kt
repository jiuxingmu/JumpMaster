package com.jumpmaster.app.data.local.db

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Converters {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromDateTime(dateTime: LocalDateTime): String {
        return dateTime.format(formatter)
    }

    @TypeConverter
    fun toDateTime(dateString: String): LocalDateTime {
        return LocalDateTime.parse(dateString, formatter)
    }
}