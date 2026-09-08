package org.kabuapp.kabuapp.feature.exam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns stored exams into list rows. Pure functions, so the divider placement is testable.
 */
public final class ExamRowFactory
{
    private ExamRowFactory()
    {
    }

    /**
     * Exams in date order, with a divider marking today when today lies between a finished and an
     * upcoming exam and no exam is currently running.
     */
    public static List<ExamRow> rowsFor(List<Exam> exams, LocalDate today)
    {
        List<ExamRow.ExamEntryRow> entries = new ArrayList<>(exams.stream()
            .map(ExamRowFactory::toRow)
            .sorted(Comparator.comparing(ExamRow.ExamEntryRow::begin))
            .toList());

        List<ExamRow> rows = new ArrayList<>(entries);
        insertTodayDivider(rows, entries, today);
        return rows;
    }

    private static ExamRow.ExamEntryRow toRow(Exam exam)
    {
        return new ExamRow.ExamEntryRow(
            exam.getDate(), exam.getDuration() == null ? 1 : exam.getDuration(), exam.getInfo());
    }

    private static void insertTodayDivider(List<ExamRow> rows, List<ExamRow.ExamEntryRow> entries, LocalDate today)
    {
        boolean hasPast = entries.stream().anyMatch(row -> row.begin().isBefore(today));
        boolean hasUpcoming = entries.stream().anyMatch(row -> row.begin().isAfter(today));
        boolean running = entries.stream().anyMatch(row -> row.isCurrent(today));
        if (!hasPast || !hasUpcoming || running)
        {
            return;
        }
        int position = 0;
        while (position < entries.size() && entries.get(position).begin().isBefore(today))
        {
            position++;
        }
        rows.add(position, new ExamRow.TodayDividerRow());
    }
}
