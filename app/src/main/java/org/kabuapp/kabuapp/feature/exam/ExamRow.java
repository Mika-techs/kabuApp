package org.kabuapp.kabuapp.feature.exam;

import java.time.LocalDate;

/**
 * What the exam list draws. The "today" marker used to be smuggled in as an exam with a duration
 * of -1; here it is its own kind of row.
 */
public sealed interface ExamRow
{
    /**
     * One exam.
     *
     * @param duration number of consecutive days it spans
     */
    record ExamEntryRow(LocalDate begin, short duration, String info) implements ExamRow
    {
        public LocalDate end()
        {
            return begin.plusDays(duration - 1L);
        }

        public boolean singleDay()
        {
            return duration == 1;
        }

        /** True while today falls inside the exam's span. */
        public boolean isCurrent(LocalDate today)
        {
            return !today.isBefore(begin) && !today.isAfter(end());
        }
    }

    /** Marks today's position between a past and an upcoming exam. */
    record TodayDividerRow() implements ExamRow
    {
    }
}
