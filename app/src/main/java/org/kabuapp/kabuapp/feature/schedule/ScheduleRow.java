package org.kabuapp.kabuapp.feature.schedule;

import java.time.LocalDate;

/**
 * What the schedule list draws. The "now" divider used to be smuggled through the data model as a
 * lesson with a begin period of -1; here it is simply another kind of row.
 */
public sealed interface ScheduleRow
{
    /**
     * One lesson block, already merged across consecutive periods and split at the long break.
     *
     * @param teacher an empty teacher means the lesson is cancelled
     */
    record LessonRow(
        LocalDate date,
        short begin,
        short end,
        short group,
        short maxGroup,
        String name,
        String teacher,
        String room) implements ScheduleRow
    {
        public boolean cancelled()
        {
            return teacher != null && teacher.isEmpty();
        }

        public boolean singleGroup()
        {
            return group == 1 && maxGroup == 1;
        }
    }

    /** Marks the current time between two lessons. */
    record NowDividerRow() implements ScheduleRow
    {
    }
}
