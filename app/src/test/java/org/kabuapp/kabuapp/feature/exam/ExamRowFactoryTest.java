package org.kabuapp.kabuapp.feature.exam;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExamRowFactoryTest
{
    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 8);

    private static Exam exam(LocalDate date, int duration, String info)
    {
        return new Exam(USER, date, info, (short) duration);
    }

    @Test
    public void examsAreSortedByDate()
    {
        List<ExamRow> rows = ExamRowFactory.rowsFor(
            List.of(exam(TODAY.plusDays(5), 1, "Later"), exam(TODAY.plusDays(1), 1, "Sooner")), TODAY);

        assertEquals("Sooner", ((ExamRow.ExamEntryRow) rows.get(0)).info());
    }

    @Test
    public void dividerSitsBetweenPastAndUpcomingExams()
    {
        List<ExamRow> rows = ExamRowFactory.rowsFor(
            List.of(exam(TODAY.minusDays(3), 1, "Past"), exam(TODAY.plusDays(3), 1, "Future")), TODAY);

        assertEquals(3, rows.size());
        assertTrue(rows.get(1) instanceof ExamRow.TodayDividerRow);
    }

    @Test
    public void noDividerWhileAnExamIsRunning()
    {
        List<ExamRow> rows = ExamRowFactory.rowsFor(
            List.of(exam(TODAY.minusDays(1), 3, "Running"), exam(TODAY.plusDays(5), 1, "Future")), TODAY);

        assertTrue(rows.stream().noneMatch(row -> row instanceof ExamRow.TodayDividerRow));
    }

    @Test
    public void noDividerWithoutAPastExam()
    {
        List<ExamRow> rows = ExamRowFactory.rowsFor(List.of(exam(TODAY.plusDays(3), 1, "Future")), TODAY);

        assertEquals(1, rows.size());
    }

    @Test
    public void noDividerWithoutAnUpcomingExam()
    {
        List<ExamRow> rows = ExamRowFactory.rowsFor(List.of(exam(TODAY.minusDays(3), 1, "Past")), TODAY);

        assertEquals(1, rows.size());
    }

    @Test
    public void multiDayExamEndIsInclusive()
    {
        ExamRow.ExamEntryRow row = new ExamRow.ExamEntryRow(TODAY, (short) 3, "Project");

        assertEquals(TODAY.plusDays(2), row.end());
        assertTrue(row.isCurrent(TODAY.plusDays(2)));
        assertTrue(!row.isCurrent(TODAY.plusDays(3)));
    }

    @Test
    public void singleDayExamStartsAndEndsOnTheSameDay()
    {
        ExamRow.ExamEntryRow row = new ExamRow.ExamEntryRow(TODAY, (short) 1, "Maths");

        assertEquals(TODAY, row.end());
        assertTrue(row.singleDay());
    }
}
