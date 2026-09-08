package org.kabuapp.kabuapp.feature.schedule;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.TypeConverters;
import java.time.LocalDate;
import java.util.UUID;
import org.kabuapp.kabuapp.core.data.LocalDateConverter;
import org.kabuapp.kabuapp.feature.auth.User;

/**
 * One lesson block. The primary key is natural - a user cannot have two different lessons in the
 * same period of the same group on the same day - so a refresh upserts instead of needing the
 * user's rows deleted first.
 */
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
public record Lesson(
    @NonNull @ColumnInfo(name = "userId") UUID userId,
    @NonNull @ColumnInfo(name = "date") LocalDate date,
    @ColumnInfo(name = "begin") short begin,
    @ColumnInfo(name = "group") short group,
    @ColumnInfo(name = "end") short end,
    @ColumnInfo(name = "maxGroup") short maxGroup,
    @ColumnInfo(name = "name") String name,
    @ColumnInfo(name = "teacher") String teacher,
    @ColumnInfo(name = "room") String room)
{
    /** Same block, extended to a later final period. */
    public Lesson withEnd(short newEnd)
    {
        return new Lesson(userId, date, begin, group, newEnd, maxGroup, name, teacher, room);
    }
}
