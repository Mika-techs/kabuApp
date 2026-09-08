package org.kabuapp.kabuapp.feature.schedule;

import org.kabuapp.kabuapp.feature.schedule.LessonResponse;
import org.kabuapp.kabuapp.feature.schedule.MemLesson;
import org.kabuapp.kabuapp.feature.schedule.MemSchedule;
import org.kabuapp.kabuapp.feature.schedule.Lesson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ScheduleMapper
{
    public void mapApiResToSchedule(List<LessonResponse> lessonResponses, MemSchedule schedule)
    {
        if (lessonResponses == null)
        {
            return;
        }

        Map<LocalDate, List<MemLesson>> lessons = schedule.getLessons();

        lessonResponses.forEach(lessonResponse ->
        {
            MemLesson lesson = new MemLesson(
                    (short) lessonResponse.getAnfStd(),
                    (short) lessonResponse.getEndStd(),
                    LocalDate.of(
                            Integer.parseInt(lessonResponse.getDatum().substring(6)),
                            Integer.parseInt(lessonResponse.getDatum().substring(3, 5)),
                            Integer.parseInt(lessonResponse.getDatum().substring(0, 2))),
                    (short) Character.getNumericValue(lessonResponse.getGruppe().charAt(0)),
                    (short) Character.getNumericValue(lessonResponse.getGruppe().charAt(2)),
                    lessonResponse.getUFachBez(),
                    lessonResponse.getLehrer(),
                    lessonResponse.getRaumLongtext(),
                    null);
            if (!lessons.containsKey(lesson.getDate()))
            {
                lessons.put(lesson.getDate(), new ArrayList<>());
            }
            List<MemLesson> dateLessons = lessons.get(lesson.getDate());
            Optional<MemLesson> existingLesson = dateLessons.stream().filter(memLesson -> memLesson.getDate().equals(
                    lesson.getDate()) && memLesson.getBegin() == lesson.getBegin() && lesson.getGroup() == memLesson.getGroup()).findAny();

            existingLesson.ifPresent(memLesson ->
            {
                lesson.setDbId(memLesson.getDbId());
                dateLessons.remove(memLesson);
            });

            dateLessons.stream().filter(lesson::isFollowingLessonTo).findFirst().ifPresentOrElse(dateLesson ->
                dateLesson.setEnd(lesson.getEnd()), () -> dateLessons.add(lesson));
        });
    }

    public List<Lesson> mapScheduleToDb(MemSchedule schedule, UUID userId)
    {
        List<Lesson> dbLessons = new ArrayList<>();
        if (schedule != null && schedule.getLessons() != null)
        {
            schedule.getLessons().values().forEach(lessonList ->
            {
                lessonList.forEach(lesson ->
                {
                    if (lesson.getDbId() == null)
                    {
                        lesson.setDbId(UUID.randomUUID());
                    }
                    Lesson dbLesson = new Lesson(
                            lesson.getDbId(),
                            userId,
                            lesson.getBegin(),
                            lesson.getEnd(),
                            lesson.getDate(),
                            lesson.getGroup(),
                            lesson.getMaxGroup(),
                            lesson.getName(),
                            lesson.getTeacher(),
                            lesson.getRoom()
                    );
                    dbLessons.add(dbLesson);
                });
            });
        }
        return dbLessons;
    }

    public void mapDbLessonToSchedule(List<Lesson> dbLessons, MemSchedule schedule)
    {
        dbLessons.forEach(dbLesson ->
        {
            MemLesson lesson = new MemLesson(
                    dbLesson.getBegin(),
                    dbLesson.getEnd(),
                    dbLesson.getDate(),
                    dbLesson.getGroup(),
                    dbLesson.getMaxGroup(),
                    dbLesson.getName(),
                    dbLesson.getTeacher(),
                    dbLesson.getRoom(),
                    dbLesson.getId());
            if (!schedule.getLessons().containsKey(lesson.getDate()))
            {
                schedule.getLessons().put(lesson.getDate(), new ArrayList<>());
            }
            schedule.getLessons().get(lesson.getDate()).add(lesson);
        });
    }
}
