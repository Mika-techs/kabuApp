package org.kabuapp.kabuapp.feature.exam;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ExamMapperTest
{
    private static final UUID USER = UUID.randomUUID();

    private ExamMapper mapper;

    @Before
    public void setUp()
    {
        mapper = new ExamMapper();
    }

    @Test
    public void twoExamsOnTheSameDayAreBothKept()
    {
        List<Exam> exams = mapper.toEntities(
            List.of(new ExamResponse("07.09.2026", "Maths"), new ExamResponse("07.09.2026", "German")), USER);

        assertEquals(2, exams.size());
    }

    @Test
    public void consecutiveDaysWithTheSameDescriptionBecomeOneExam()
    {
        List<Exam> exams = mapper.toEntities(
            List.of(new ExamResponse("07.09.2026", "Project"),
                    new ExamResponse("08.09.2026", "Project"),
                    new ExamResponse("09.09.2026", "Project")), USER);

        assertEquals(1, exams.size());
        assertEquals(Short.valueOf((short) 3), exams.get(0).getDuration());
        assertEquals(LocalDate.of(2026, 9, 7), exams.get(0).getDate());
    }

    @Test
    public void anExamSpanningAMonthBoundaryStaysOneExam()
    {
        List<Exam> exams = mapper.toEntities(
            List.of(new ExamResponse("30.09.2026", "Project"), new ExamResponse("01.10.2026", "Project")), USER);

        assertEquals(1, exams.size());
        assertEquals(Short.valueOf((short) 2), exams.get(0).getDuration());
    }

    @Test
    public void aGapBetweenDaysKeepsThemSeparate()
    {
        List<Exam> exams = mapper.toEntities(
            List.of(new ExamResponse("07.09.2026", "Project"), new ExamResponse("09.09.2026", "Project")), USER);

        assertEquals(2, exams.size());
    }

    @Test
    public void entriesWithoutADescriptionAreSkipped()
    {
        List<Exam> exams = mapper.toEntities(
            List.of(new ExamResponse("07.09.2026", ""), new ExamResponse("08.09.2026", "Maths")), USER);

        assertEquals(1, exams.size());
        assertEquals("Maths", exams.get(0).getInfo());
    }

    @Test
    public void nullResponsesYieldNoExams()
    {
        assertEquals(List.of(), mapper.toEntities(null, USER));
    }
}
