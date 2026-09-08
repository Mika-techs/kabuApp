package org.kabuapp.kabuapp.feature.exam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Converts API responses into stored exams. The API returns one entry per day, so an exam
 * spanning several days arrives as repeated entries with the same description and is collapsed
 * into a single row with a duration.
 */
public class ExamMapper
{
    private static final int DAY_END = 2;
    private static final int MONTH_START = 3;
    private static final int MONTH_END = 5;
    private static final int YEAR_START = 6;

    /**
     * Merges the responses of every requested month at once. Merging per month, as the previous
     * version did, recorded an exam spanning a month boundary as two separate exams.
     */
    public List<Exam> toEntities(List<ExamResponse> responses, UUID userId)
    {
        if (responses == null)
        {
            return List.of();
        }
        List<Exam> parsed = new ArrayList<>();
        responses.stream()
            .filter(response -> response.getInfo() != null && !response.getInfo().isEmpty())
            .map(response -> new Exam(userId, parseDate(response.getDate()), response.getInfo(), (short) 1))
            .sorted(Comparator.comparing(Exam::getDate))
            .forEach(exam -> merge(parsed, exam));
        return parsed;
    }

    /** Extends the previous exam when this one continues it on the next day. */
    private static void merge(List<Exam> merged, Exam exam)
    {
        for (Exam existing : merged)
        {
            if (existing.getInfo().equals(exam.getInfo())
                && existing.getDate().plusDays(existing.getDuration()).equals(exam.getDate()))
            {
                existing.setDuration((short) (existing.getDuration() + 1));
                return;
            }
        }
        merged.add(exam);
    }

    /** The API sends dd.MM.yyyy. */
    private static LocalDate parseDate(String value)
    {
        return LocalDate.of(
            Integer.parseInt(value.substring(YEAR_START)),
            Integer.parseInt(value.substring(MONTH_START, MONTH_END)),
            Integer.parseInt(value.substring(0, DAY_END)));
    }
}
