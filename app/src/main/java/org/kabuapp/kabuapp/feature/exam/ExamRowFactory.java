package org.kabuapp.kabuapp.feature.exam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        List<ExamRow.ExamEntryRow> entries = exams.stream()
            .map(ExamRowFactory::toRow)
            .sorted(Comparator.comparing(ExamRow.ExamEntryRow::begin))
            .collect(Collectors.toList());

        List<ExamRow> rows = new ArrayList<>(entries);
        insertTodayDivider(rows, entries, today);
        return rows;
    }

    private static ExamRow.ExamEntryRow toRow(Exam exam)
    {
        return new ExamRow.ExamEntryRow(exam.date(), exam.duration(), exam.info());
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
