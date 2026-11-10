package org.kabuapp.kabuapp.core.util;

import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.assertEquals;

public class DateMapperTest
{
    private static final LocalDate NOV_13 = LocalDate.of(2026, 11, 13);

    @Test
    public void germanFormatIsParsed()
    {
        assertEquals(NOV_13, DateMapper.parseDate("13.11.2026"));
    }

    @Test
    public void usFormatIsParsed()
    {
        assertEquals(NOV_13, DateMapper.parseDate("11/13/2026"));
    }

    @Test
    public void ambiguousDayAndMonthFollowTheSeparator()
    {
        assertEquals(LocalDate.of(2026, 9, 7), DateMapper.parseDate("07.09.2026"));
        assertEquals(LocalDate.of(2026, 7, 9), DateMapper.parseDate("07/09/2026"));
    }

    @Test(expected = DateTimeParseException.class)
    public void anImpossibleDateIsRejected()
    {
        DateMapper.parseDate("13/11/2026");
    }
}
