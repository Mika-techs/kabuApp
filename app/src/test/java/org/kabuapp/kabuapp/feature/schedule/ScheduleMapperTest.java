package org.kabuapp.kabuapp.feature.schedule;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ScheduleMapperTest
{
    private static final UUID USER = UUID.randomUUID();

    private ScheduleMapper mapper;

    @Before
    public void setUp()
    {
        mapper = new ScheduleMapper();
    }

    private static LessonResponse response(int begin, int end, String name, String group)
    {
        return new LessonResponse("07.09.2026", begin, end, "Muster", name, "A1", group);
    }

    @Test
    public void consecutiveIdenticalPeriodsBecomeOneBlock()
    {
        List<Lesson> lessons = mapper.toEntities(
            List.of(response(1, 1, "Maths", "1/1"), response(2, 2, "Maths", "1/1")), USER);

        assertEquals(1, lessons.size());
        assertEquals(1, lessons.get(0).begin());
        assertEquals((short) 2, lessons.get(0).end());
    }

    @Test
    public void aGapBetweenPeriodsKeepsThemSeparate()
    {
        List<Lesson> lessons = mapper.toEntities(
            List.of(response(1, 1, "Maths", "1/1"), response(3, 3, "Maths", "1/1")), USER);

        assertEquals(2, lessons.size());
    }

    @Test
    public void differentGroupsAreNotMerged()
    {
        List<Lesson> lessons = mapper.toEntities(
            List.of(response(1, 1, "Sport", "1/2"), response(2, 2, "Sport", "2/2")), USER);

        assertEquals(2, lessons.size());
    }

    @Test
    public void differentSubjectsAreNotMerged()
    {
        List<Lesson> lessons = mapper.toEntities(
            List.of(response(1, 1, "Maths", "1/1"), response(2, 2, "German", "1/1")), USER);

        assertEquals(2, lessons.size());
    }

    @Test
    public void theApiDateFormatIsParsed()
    {
        List<Lesson> lessons = mapper.toEntities(List.of(response(1, 1, "Maths", "1/1")), USER);

        assertEquals(LocalDate.of(2026, 9, 7), lessons.get(0).date());
    }

    @Test
    public void groupAndMaxGroupComeFromTheGroupString()
    {
        List<Lesson> lessons = mapper.toEntities(List.of(response(4, 4, "Sport", "2/3")), USER);

        assertEquals(2, lessons.get(0).group());
        assertEquals((short) 3, lessons.get(0).maxGroup());
    }

    @Test
    public void nullResponsesYieldNoLessons()
    {
        assertEquals(List.of(), mapper.toEntities(null, USER));
    }
}
