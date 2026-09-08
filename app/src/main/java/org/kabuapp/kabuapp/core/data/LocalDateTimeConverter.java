package org.kabuapp.kabuapp.core.data;

import androidx.room.TypeConverter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LocalDateTimeConverter
{
    private LocalDateTimeConverter()
    {

    }
    @TypeConverter
    public static LocalDateTime fromTimestamp(Long value)
    {
        return value == null ? null : LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.MIN);
    }

    @TypeConverter
    public static Long dateToTimestamp(LocalDateTime date)
    {
        return date == null ? null : date.toEpochSecond(ZoneOffset.MIN);
    }
}