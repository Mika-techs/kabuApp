package org.kabuapp.kabuapp.feature.schedule;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScheduleRowFactoryTest
{
    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);
    private static final LocalTime MORNING = LocalTime.of(8, 45);

    private static Lesson lesson(short begin, short end, short group, short maxGroup, String name)
    {
        return new Lesson(USER, MONDAY, begin, group, end, maxGroup, name, "Muster", "A1");
    }

    @Test
    public void lessonsAreOrderedByPeriodThenGroup()
    {
        List<ScheduleRow> rows = ScheduleRowFactory.rowsFor(
            List.of(lesson((short) 5, (short) 5, (short) 2, (short) 2, "Late"),
                    lesson((short) 5, (short) 5, (short) 1, (short) 2, "LateA"),
                    lesson((short) 4, (short) 4, (short) 1, (short) 1, "Early")),
            MONDAY, MONDAY.minusDays(1), MORNING);

        assertEquals(List.of("Early", "LateA", "Late"), rows.stream()
            .map(row -> ((ScheduleRow.LessonRow) row).name())
            .toList());
    }

    @Test
    public void onlyTheSelectedDayIsIncluded()
    {
        Lesson otherDay = new Lesson(USER, MONDAY.plusDays(1), (short) 1, (short) 1, (short) 1, (short) 1, "Tue", "M", "A1");
        List<ScheduleRow> rows = ScheduleRowFactory.rowsFor(
            List.of(lesson((short) 1, (short) 1, (short) 1, (short) 1, "Mon"), otherDay),
            MONDAY, MONDAY.minusDays(1), MORNING);

        assertEquals(1, rows.size());
        assertEquals("Mon", ((ScheduleRow.LessonRow) rows.get(0)).name());
    }

    @Test
    public void blockAcrossTheLongBreakIsSplitInTwo()
    {
        List<ScheduleRow.LessonRow> split = ScheduleRowFactory.splitAtFirstBreak(
            new ScheduleRow.LessonRow(MONDAY, (short) 1, (short) 4, (short) 1, (short) 1, "Maths", "M", "A1"));

        assertEquals(2, split.size());
        assertEquals(1, split.get(0).begin());
        assertEquals(2, split.get(0).end());
        assertEquals(3, split.get(1).begin());
        assertEquals(4, split.get(1).end());
        assertEquals("Maths", split.get(1).name());
    }

    @Test
    public void blockNotCrossingTheBreakIsLeftAlone()
    {
        ScheduleRow.LessonRow row =
            new ScheduleRow.LessonRow(MONDAY, (short) 3, (short) 6, (short) 1, (short) 1, "Maths", "M", "A1");
        assertEquals(List.of(row), ScheduleRowFactory.splitAtFirstBreak(row));
    }

    @Test
    public void splitKeepsTheGroupOfAMultiGroupBlock()
    {
        List<ScheduleRow.LessonRow> split = ScheduleRowFactory.splitAtFirstBreak(
            new ScheduleRow.LessonRow(MONDAY, (short) 2, (short) 3, (short) 2, (short) 3, "Sport", "M", "Hall"));

        assertEquals(2, split.size());
        assertTrue(split.stream().allMatch(row -> row.group() == 2 && row.maxGroup() == 3));
    }

    @Test
    public void nowDividerSitsBetweenAFinishedAndAComingLesson()
    {
        List<ScheduleRow> rows = ScheduleRowFactory.rowsFor(
            List.of(lesson((short) 1, (short) 1, (short) 1, (short) 1, "Done"),
                    lesson((short) 6, (short) 6, (short) 1, (short) 1, "Later")),
            MONDAY, MONDAY, LocalTime.of(11, 0));

        assertEquals(3, rows.size());
        assertTrue(rows.get(1) instanceof ScheduleRow.NowDividerRow);
    }

    @Test
    public void noDividerWhileALessonIsRunning()
    {
        List<ScheduleRow> rows = ScheduleRowFactory.rowsFor(
            List.of(lesson((short) 1, (short) 1, (short) 1, (short) 1, "Now"),
                    lesson((short) 6, (short) 6, (short) 1, (short) 1, "Later")),
            MONDAY, MONDAY, LocalTime.of(8, 45));

        assertTrue(rows.stream().noneMatch(row -> row instanceof ScheduleRow.NowDividerRow));
    }

    @Test
    public void noDividerAfterTheLastLessonOfTheDay()
    {
        List<ScheduleRow> rows = ScheduleRowFactory.rowsFor(
            List.of(lesson((short) 1, (short) 1, (short) 1, (short) 1, "Done")),
            MONDAY, MONDAY, LocalTime.of(20, 0));

        assertEquals(1, rows.size());
    }

    @Test
    public void noDividerOnADayThatIsNotToday()
    {
        List<ScheduleRow> rows = ScheduleRowFactory.rowsFor(
            List.of(lesson((short) 1, (short) 1, (short) 1, (short) 1, "Done"),
                    lesson((short) 6, (short) 6, (short) 1, (short) 1, "Later")),
            MONDAY, MONDAY.plusDays(1), LocalTime.of(11, 0));

        assertTrue(rows.stream().noneMatch(row -> row instanceof ScheduleRow.NowDividerRow));
    }

    @Test
    public void schoolDaysAreDistinctAndSorted()
    {
        Lesson later = new Lesson(USER, MONDAY.plusDays(2), (short) 1, (short) 1, (short) 1, (short) 1, "W", "M", "A1");
        List<LocalDate> days = ScheduleRowFactory.schoolDays(List.of(
            lesson((short) 2, (short) 2, (short) 1, (short) 1, "B"),
            later,
            lesson((short) 1, (short) 1, (short) 1, (short) 1, "A")));

        assertEquals(List.of(MONDAY, MONDAY.plusDays(2)), days);
    }

    @Test
    public void splitHalvesTakeTheirOwnPlaceInTheDay()
    {
        // Two groups both booked across the long break: the day must read in time order,
        // not group-by-group with each group's second half tucked behind its first.
        List<ScheduleRow> rows = ScheduleRowFactory.rowsFor(
            List.of(lesson((short) 2, (short) 3, (short) 1, (short) 2, "Sport A"),
                    lesson((short) 2, (short) 3, (short) 2, (short) 2, "Sport B")),
            MONDAY, MONDAY.minusDays(1), MORNING);

        assertEquals(List.of("Sport A", "Sport B", "Sport A", "Sport B"), rows.stream()
            .map(row -> ((ScheduleRow.LessonRow) row).name())
            .toList());
        assertEquals(List.of(2, 2, 3, 3), rows.stream()
            .map(row -> (int) ((ScheduleRow.LessonRow) row).begin())
            .toList());
    }

    @Test
    public void aSplitBlockSortsAheadOfALaterLesson()
    {
        List<ScheduleRow> rows = ScheduleRowFactory.rowsFor(
            List.of(lesson((short) 1, (short) 4, (short) 1, (short) 1, "Long"),
                    lesson((short) 3, (short) 3, (short) 2, (short) 2, "Other")),
            MONDAY, MONDAY.minusDays(1), MORNING);

        assertEquals(List.of("Long", "Long", "Other"), rows.stream()
            .map(row -> ((ScheduleRow.LessonRow) row).name())
            .toList());
    }
}
