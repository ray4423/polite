package me.camsteffen.polite.db

import androidx.room.TypeConverter
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime

object Converters {

    @TypeConverter @JvmStatic
    fun instantToLong(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter @JvmStatic
    fun longToInstant(long: Long): Instant = Instant.ofEpochMilli(long)

    @TypeConverter @JvmStatic
    fun localTimeFromInt(minutes: Int): LocalTime = LocalTime.ofSecondOfDay(minutes.toLong())

    @TypeConverter @JvmStatic
    fun localTimeToInt(localTime: LocalTime): Int = localTime.toSecondOfDay()
}
