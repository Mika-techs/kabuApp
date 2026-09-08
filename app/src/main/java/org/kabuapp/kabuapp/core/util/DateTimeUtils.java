package org.kabuapp.kabuapp.core.util;

import java.time.LocalDate;
import java.time.LocalTime;

public class DateTimeUtils
{
    private DateTimeUtils()
    {
    }

    public static LocalTime getLocalTime()
    {
        return LocalTime.now();
    }

    public static LocalDate getLocalDate()
    {
        return LocalDate.now();
    }

    public static LocalDate getFirstDayOfWeek()
    {
        return DateTimeUtils.getLocalDate().minusDays(DateTimeUtils.getLocalDate().getDayOfWeek().getValue() - 1);
    }

    public static LocalDate getFirstDayOfMonth()
    {
        return DateTimeUtils.getLocalDate().withDayOfMonth(1);
    }
}
