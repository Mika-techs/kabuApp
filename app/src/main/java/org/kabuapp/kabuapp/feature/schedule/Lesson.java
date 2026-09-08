package org.kabuapp.kabuapp.feature.schedule;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.kabuapp.kabuapp.core.data.LocalDateConverter;

@Getter
@Setter
@AllArgsConstructor
@Entity(tableName = "schedule")
@TypeConverters({LocalDateConverter.class})
public class Lesson
{
    @PrimaryKey()
    @NotNull
    private UUID id;
    @ColumnInfo(name = "userId")
    private UUID userId;
    @ColumnInfo(name = "begin")
    private Short begin;
    @ColumnInfo(name = "end")
    private Short end;
    @ColumnInfo(name = "date")
    private LocalDate date;
    @ColumnInfo(name = "group")
    private Short group;
    @ColumnInfo(name = "maxGroup")
    private Short maxGroup;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "teacher")
    private String teacher;
    @ColumnInfo(name = "room")
    private String room;
}
