package org.kabuapp.kabuapp.core.data;

import androidx.room.TypeConverter;
import java.time.LocalDate;

public class LocalDateConverter
{
    private LocalDateConverter()
    {

    }
    @TypeConverter
    public static LocalDate fromTimestamp(Long value)
    {
        return value == null ? null : LocalDate.ofEpochDay(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(LocalDate date)
    {
        return date == null ? null : date.toEpochDay();
    }
}