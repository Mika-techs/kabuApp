package org.kabuapp.kabuapp.domain;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * The school's period timetable: which wall-clock times a numbered period covers.
 *
 * <p>This used to live in the schedule's view generator, which meant the activity reached into a
 * view class to do time arithmetic for its countdown.
 */
public final class LessonPeriods
{
    /** Periods 1 and 2 sit before the long morning break; period 2 ends early because of it. */
    public static final short FIRST_BREAK_LAST_PERIOD = 2;

    private static final short FIRST_PERIOD = 1;
    private static final short LAST_PERIOD = 14;
    private static final LocalTime PERIOD_TWO_END = LocalTime.of(10, 0);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("H:mm");

    /** Start time of period n at index n - 1. */
    private static final LocalTime[] STARTS =
    {
        LocalTime.of(8, 30),
        LocalTime.of(9, 15),
        LocalTime.of(10, 15),
        LocalTime.of(11, 0),
        LocalTime.of(11, 45),
        LocalTime.of(12, 30),
        LocalTime.of(13, 15),
        LocalTime.of(14, 0),
        LocalTime.of(14, 45),
        LocalTime.of(15, 30),
        LocalTime.of(16, 15),
        LocalTime.of(17, 0),
        LocalTime.of(17, 45),
        LocalTime.of(18, 30),
    };

    private LessonPeriods()
    {
    }

    /** Start of the given period, empty when the period number is outside the timetable. */
    public static Optional<LocalTime> begin(short period)
    {
        if (period < FIRST_PERIOD || period > LAST_PERIOD)
        {
            return Optional.empty();
        }
        return Optional.of(STARTS[period - 1]);
    }

    /**
     * End of the given period. A period normally ends when the next one starts; period 2 ends at
     * 10:00 because the long break follows it.
     */
    public static Optional<LocalTime> end(short period)
    {
        if (period == FIRST_BREAK_LAST_PERIOD)
        {
            return Optional.of(PERIOD_TWO_END);
        }
        return begin((short) (period + 1));
    }

    /** True when a block from {@code begin} to {@code end} runs across the long morning break. */
    public static boolean spansFirstBreak(short begin, short end)
    {
        return begin <= FIRST_BREAK_LAST_PERIOD && end > FIRST_BREAK_LAST_PERIOD;
    }

    /** True when {@code now} falls inside the block, boundaries included. */
    public static boolean isCurrent(short beginPeriod, short endPeriod, LocalTime now)
    {
        Optional<LocalTime> begin = begin(beginPeriod);
        Optional<LocalTime> end = end(endPeriod);
        if (begin.isEmpty() || end.isEmpty())
        {
            return false;
        }
        return !now.isBefore(begin.get()) && !now.isAfter(end.get());
    }

    /** "8:30" style label for a period boundary, or an empty string for an unknown period. */
    public static String formatBegin(short period)
    {
        return begin(period).map(DISPLAY::format).orElse("");
    }

    public static String formatEnd(short period)
    {
        return end(period).map(DISPLAY::format).orElse("");
    }
}
