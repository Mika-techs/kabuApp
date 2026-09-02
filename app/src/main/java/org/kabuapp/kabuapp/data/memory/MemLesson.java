package org.kabuapp.kabuapp.data.memory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MemLesson
{
    private short begin;
    @Setter
    private short end;
    private LocalDate date;
    private short group;
    private short maxGroup;
    private String name;
    private String teacher;
    private String room;
    @Setter
    private UUID dbId;

    public boolean isFollowingLessonTo(MemLesson lesson)
    {
        return this.getName().equals(lesson.getName())
                && this.getRoom().equals(lesson.getRoom())
                && this.getTeacher().equals(lesson.getTeacher())
                && this.getMaxGroup() == lesson.getMaxGroup()
                && this.getGroup() == lesson.getGroup()
                && this.getBegin() == (lesson.getEnd() + 1);
    }
}
