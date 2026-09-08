package org.kabuapp.kabuapp.domain;

import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LessonPeriodsTest
{
    @Test
    public void firstPeriodStartsAtHalfPastEight()
    {
        assertEquals(LocalTime.of(8, 30), LessonPeriods.begin((short) 1).orElseThrow());
    }

    @Test
    public void periodEndsWhenTheNextOneStarts()
    {
        assertEquals(LessonPeriods.begin((short) 5).orElseThrow(), LessonPeriods.end((short) 4).orElseThrow());
    }

    @Test
    public void secondPeriodEndsEarlyBecauseOfTheLongBreak()
    {
        assertEquals(LocalTime.of(10, 0), LessonPeriods.end((short) 2).orElseThrow());
        assertEquals(LocalTime.of(10, 15), LessonPeriods.begin((short) 3).orElseThrow());
    }

    @Test
    public void lastPeriodHasNoEndDerivedFromANextPeriod()
    {
        assertTrue(LessonPeriods.begin((short) 14).isPresent());
        assertTrue(LessonPeriods.end((short) 14).isEmpty());
    }

    @Test
    public void periodsOutsideTheTimetableAreEmpty()
    {
        assertTrue(LessonPeriods.begin((short) 0).isEmpty());
        assertTrue(LessonPeriods.begin((short) 15).isEmpty());
        assertTrue(LessonPeriods.begin((short) -1).isEmpty());
    }

    @Test
    public void spansFirstBreakOnlyWhenCrossingIt()
    {
        assertTrue(LessonPeriods.spansFirstBreak((short) 1, (short) 3));
        assertTrue(LessonPeriods.spansFirstBreak((short) 2, (short) 4));
        assertFalse(LessonPeriods.spansFirstBreak((short) 1, (short) 2));
        assertFalse(LessonPeriods.spansFirstBreak((short) 3, (short) 5));
    }

    @Test
    public void isCurrentIncludesBothBoundaries()
    {
        assertTrue(LessonPeriods.isCurrent((short) 1, (short) 1, LocalTime.of(8, 30)));
        assertTrue(LessonPeriods.isCurrent((short) 1, (short) 1, LocalTime.of(9, 15)));
        assertTrue(LessonPeriods.isCurrent((short) 1, (short) 1, LocalTime.of(9, 0)));
        assertFalse(LessonPeriods.isCurrent((short) 1, (short) 1, LocalTime.of(8, 29)));
        assertFalse(LessonPeriods.isCurrent((short) 1, (short) 1, LocalTime.of(9, 16)));
    }

    @Test
    public void isCurrentIsFalseForAnUnknownPeriod()
    {
        assertFalse(LessonPeriods.isCurrent((short) -1, (short) -1, LocalTime.of(9, 0)));
    }

    @Test
    public void labelsAreFormattedWithoutALeadingZero()
    {
        assertEquals("8:30", LessonPeriods.formatBegin((short) 1));
        assertEquals("10:00", LessonPeriods.formatEnd((short) 2));
        assertEquals("", LessonPeriods.formatBegin((short) 99));
    }
}
