package org.kabuapp.kabuapp.feature.schedule;

import io.lilithtechs.metisJson.JsonMapper;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Deserialises a response shaped like the real digikabu.de payload. MetisJson reflects over the
 * DTO field names, so this pins the names the API actually sends - if one is renamed the DTO
 * silently yields nulls rather than failing.
 */
public class LessonResponseJsonTest
{
    private static final String PAYLOAD = """
        [
          {"datum":"07.09.2026","anfStd":1,"endStd":1,"lehrer":"Muster","uFachBez":"Mathematik","raumLongtext":"A101","gruppe":"1/1"},
          {"datum":"07.09.2026","anfStd":2,"endStd":2,"lehrer":"Muster","uFachBez":"Mathematik","raumLongtext":"A101","gruppe":"1/1"},
          {"datum":"07.09.2026","anfStd":5,"endStd":5,"lehrer":"","uFachBez":"Sport","raumLongtext":"Halle","gruppe":"2/3"}
        ]
        """;

    @Test
    public void everyFieldTheApiSendsIsPopulated()
    {
        LessonResponse[] responses = new JsonMapper().fromJson(PAYLOAD, LessonResponse[].class);

        assertEquals(3, responses.length);
        LessonResponse first = responses[0];
        assertEquals("07.09.2026", first.getDatum());
        assertEquals(1, first.getAnfStd());
        assertEquals(1, first.getEndStd());
        assertEquals("Muster", first.getLehrer());
        assertEquals("Mathematik", first.getUFachBez());
        assertEquals("A101", first.getRaumLongtext());
        assertEquals("1/1", first.getGruppe());
    }

    @Test
    public void aDeserialisedPayloadMapsThroughToMergedEntities()
    {
        LessonResponse[] responses = new JsonMapper().fromJson(PAYLOAD, LessonResponse[].class);
        List<Lesson> lessons = new ScheduleMapper().toEntities(Arrays.asList(responses), UUID.randomUUID());

        // The two Maths periods merge into one block; Sport stays separate.
        assertEquals(2, lessons.size());
        Lesson maths = lessons.stream().filter(l -> "Mathematik".equals(l.name())).findFirst().orElseThrow();
        assertEquals(1, maths.begin());
        assertEquals(2, maths.end());
        assertEquals(LocalDate.of(2026, 9, 7), maths.date());

        Lesson sport = lessons.stream().filter(l -> "Sport".equals(l.name())).findFirst().orElseThrow();
        assertEquals(2, sport.group());
        assertEquals(3, sport.maxGroup());
        assertEquals("", sport.teacher());
    }
}
