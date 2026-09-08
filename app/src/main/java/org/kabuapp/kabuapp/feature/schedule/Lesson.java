package org.kabuapp.kabuapp.feature.schedule;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.TypeConverters;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.kabuapp.kabuapp.core.data.LocalDateConverter;
import org.kabuapp.kabuapp.feature.auth.User;

/**
 * One lesson slot. The primary key is natural - a user cannot have two different lessons in the
 * same period of the same group on the same day - so a refresh upserts instead of needing the
 * user's rows deleted first.
 */
@Getter
@Setter
@AllArgsConstructor
@Entity(
    tableName = "schedule",
    primaryKeys = { "userId", "date", "begin", "group" },
    indices = @Index(value = { "userId", "date" }),
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE))
@TypeConverters({LocalDateConverter.class})
public class Lesson
{
    @NonNull
    @ColumnInfo(name = "userId")
    private UUID userId;
    @NonNull
    @ColumnInfo(name = "date")
    private LocalDate date;
    @ColumnInfo(name = "begin")
    private short begin;
    @ColumnInfo(name = "group")
    private short group;
    @ColumnInfo(name = "end")
    private Short end;
    @ColumnInfo(name = "maxGroup")
    private Short maxGroup;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "teacher")
    private String teacher;
    @ColumnInfo(name = "room")
    private String room;
}
