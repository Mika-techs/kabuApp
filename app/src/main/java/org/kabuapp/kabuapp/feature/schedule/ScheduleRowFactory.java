package org.kabuapp.kabuapp.feature.schedule;

import org.kabuapp.kabuapp.domain.LessonPeriods;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns stored lessons into the rows the list draws. Pure functions over immutable input, so the
 * awkward parts - splitting a block at the long break, placing the "now" divider - are testable
 * without an Activity.
 */
public final class ScheduleRowFactory
{
    private ScheduleRowFactory()
    {
    }

    /**
     * Rows for one day: lessons in period order, blocks that cross the long morning break split
     * in two, and a divider at the current time when that day is today.
     */
    public static List<ScheduleRow> rowsFor(List<Lesson> lessons, LocalDate date, LocalDate today, LocalTime now)
    {
        // Sorting has to happen after the break split, not before: a block split in two must take
        // its place in the day by its own start period, not follow the half it came from.
        List<ScheduleRow.LessonRow> dayRows = lessons.stream()
            .filter(lesson -> date.equals(lesson.date()))
            .flatMap(lesson -> splitAtFirstBreak(toRow(lesson)).stream())
            .sorted(Comparator.comparing(ScheduleRow.LessonRow::begin)
                .thenComparing(ScheduleRow.LessonRow::group))
            .collect(Collectors.toList());

        List<ScheduleRow> rows = new ArrayList<>(dayRows);
        if (date.equals(today))
        {
            insertNowDivider(rows, dayRows, now);
        }
        return rows;
    }

    /** The days that have at least one lesson, in order - the date strip's contents. */
    public static List<LocalDate> schoolDays(List<Lesson> lessons)
    {
        return lessons.stream()
            .map(Lesson::date)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    private static ScheduleRow.LessonRow toRow(Lesson lesson)
    {
        return new ScheduleRow.LessonRow(
            lesson.date(),
            lesson.begin(),
            lesson.end(),
            lesson.group(),
            lesson.maxGroup(),
            lesson.name(),
            lesson.teacher(),
            lesson.room());
    }

    /**
     * A block booked across the long morning break is drawn as two cards, because sitting through
     * the break is not part of the lesson.
     */
    static List<ScheduleRow.LessonRow> splitAtFirstBreak(ScheduleRow.LessonRow row)
    {
        if (!LessonPeriods.spansFirstBreak(row.begin(), row.end()))
        {
            return List.of(row);
        }
        return List.of(
            withPeriods(row, row.begin(), LessonPeriods.FIRST_BREAK_LAST_PERIOD),
            withPeriods(row, (short) (LessonPeriods.FIRST_BREAK_LAST_PERIOD + 1), row.end()));
    }

    private static ScheduleRow.LessonRow withPeriods(ScheduleRow.LessonRow row, short begin, short end)
    {
        return new ScheduleRow.LessonRow(
            row.date(), begin, end, row.group(), row.maxGroup(), row.name(), row.teacher(), row.room());
    }

    /**
     * Places the divider after the last lesson that has already ended, but only while no lesson is
     * running and only between lessons - never before the first or after the last.
     */
    private static void insertNowDivider(List<ScheduleRow> rows, List<ScheduleRow.LessonRow> dayRows, LocalTime now)
    {
        boolean inLesson = dayRows.stream()
            .anyMatch(row -> LessonPeriods.isCurrent(row.begin(), row.end(), now));
        if (inLesson || dayRows.isEmpty())
        {
            return;
        }
        int lastFinished = -1;
        for (int i = 0; i < dayRows.size(); i++)
        {
            if (LessonPeriods.end(dayRows.get(i).end()).filter(end -> end.isBefore(now)).isPresent())
            {
                lastFinished = i;
            }
            else
            {
                break;
            }
        }
        if (lastFinished != -1 && lastFinished != dayRows.size() - 1)
        {
            rows.add(lastFinished + 1, new ScheduleRow.NowDividerRow());
        }
    }
}
