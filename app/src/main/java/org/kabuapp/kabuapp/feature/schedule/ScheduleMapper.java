package org.kabuapp.kabuapp.feature.schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts API responses into stored lessons. The API returns one entry per single period, so
 * consecutive identical periods are merged into one block here.
 */
public class ScheduleMapper
{
    private static final int DAY_END = 2;
    private static final int MONTH_START = 3;
    private static final int MONTH_END = 5;
    private static final int YEAR_START = 6;
    private static final int GROUP_INDEX = 0;
    private static final int MAX_GROUP_INDEX = 2;

    /** Parsed but not yet merged; keyed so that merging only compares periods of the same block. */
    private record Key(LocalDate date, short group, String name, String teacher, String room, short maxGroup)
    {
    }

    public List<Lesson> toEntities(List<LessonResponse> responses, UUID userId)
    {
        if (responses == null)
        {
            return List.of();
        }
        Map<Key, List<Lesson>> byBlock = new LinkedHashMap<>();
        for (LessonResponse response : responses)
        {
            Lesson lesson = toLesson(response, userId);
            byBlock.computeIfAbsent(keyOf(lesson), key -> new ArrayList<>()).add(lesson);
        }
        List<Lesson> merged = new ArrayList<>();
        byBlock.values().forEach(block -> merged.addAll(mergeConsecutive(block)));
        return merged;
    }

    /**
     * Collapses periods that follow each other without a gap into a single row, so a
     * double lesson is one card rather than two.
     */
    private static List<Lesson> mergeConsecutive(List<Lesson> block)
    {
        block.sort(Comparator.comparing(Lesson::getBegin));
        List<Lesson> merged = new ArrayList<>();
        for (Lesson lesson : block)
        {
            Lesson previous = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (previous != null && previous.getEnd() != null && lesson.getBegin() == previous.getEnd() + 1)
            {
                previous.setEnd(lesson.getEnd());
            }
            else
            {
                merged.add(lesson);
            }
        }
        return merged;
    }

    private static Key keyOf(Lesson lesson)
    {
        return new Key(lesson.getDate(), lesson.getGroup(), lesson.getName(),
            lesson.getTeacher(), lesson.getRoom(), lesson.getMaxGroup());
    }

    private static Lesson toLesson(LessonResponse response, UUID userId)
    {
        return new Lesson(
            userId,
            parseDate(response.getDatum()),
            (short) response.getAnfStd(),
            (short) Character.getNumericValue(response.getGruppe().charAt(GROUP_INDEX)),
            (short) response.getEndStd(),
            (short) Character.getNumericValue(response.getGruppe().charAt(MAX_GROUP_INDEX)),
            response.getUFachBez(),
            response.getLehrer(),
            response.getRaumLongtext());
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
