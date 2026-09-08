package org.kabuapp.kabuapp.feature.exam;

import io.lilithtechs.metisJson.JsonMapper;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/** Deserialises a response shaped like the real digikabu.de exam payload. */
public class ExamResponseJsonTest
{
    private static final String PAYLOAD = """
        [
          {"date":"30.09.2026","info":"Projektarbeit"},
          {"date":"01.10.2026","info":"Projektarbeit"},
          {"date":"05.10.2026","info":"Deutsch"},
          {"date":"05.10.2026","info":""}
        ]
        """;

    @Test
    public void bothFieldsTheApiSendsArePopulated()
    {
        ExamResponse[] responses = new JsonMapper().fromJson(PAYLOAD, ExamResponse[].class);

        assertEquals(4, responses.length);
        assertEquals("30.09.2026", responses[0].getDate());
        assertEquals("Projektarbeit", responses[0].getInfo());
    }

    @Test
    public void aDeserialisedPayloadMapsThroughToMergedEntities()
    {
        ExamResponse[] responses = new JsonMapper().fromJson(PAYLOAD, ExamResponse[].class);
        List<Exam> exams = new ExamMapper().toEntities(Arrays.asList(responses), UUID.randomUUID());

        // The cross-month Projektarbeit is one two-day exam; the blank entry is dropped.
        assertEquals(2, exams.size());
        Exam project = exams.stream().filter(e -> "Projektarbeit".equals(e.info())).findFirst().orElseThrow();
        assertEquals(2, project.duration());
    }
}
