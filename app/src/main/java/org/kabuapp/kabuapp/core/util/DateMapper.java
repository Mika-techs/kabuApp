package org.kabuapp.kabuapp.core.util;

import java.time.LocalDate;

public class DateMapper
{
    private static final int DAY_START = 3;
    private static final int DAY_END = 5;
    private static final int MONTH_END = 2;
    private static final int YEAR_START = 6;

    private DateMapper()
    {
    }

    /** The API sends MM/dd/yyyy */
    public static LocalDate parseDate(String value)
    {
        return LocalDate.of(
            Integer.parseInt(value.substring(YEAR_START)),
            Integer.parseInt(value.substring(0, MONTH_END)),
            Integer.parseInt(value.substring(DAY_START, DAY_END)));
    }
}
