package org.kabuapp.kabuapp.feature.exam;

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
 * One exam. Keyed by user, start date and description so that two exams on the same day are
 * distinct rows - the previous single-exam-per-day map silently dropped the second one.
 *
 * @param duration number of consecutive days the exam spans
 */
@Entity(
    tableName = "exams",
    primaryKeys = { "userId", "date", "info" },
    indices = @Index(value = { "userId", "date" }),
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE))
@TypeConverters({LocalDateConverter.class})
public record Exam(
    @NonNull @ColumnInfo(name = "userId") UUID userId,
    @NonNull @ColumnInfo(name = "date") LocalDate date,
    @NonNull @ColumnInfo(name = "info") String info,
    @ColumnInfo(name = "duration") short duration)
{
    /** Same exam, spanning one more day. */
    public Exam extendedByADay()
    {
        return new Exam(userId, date, info, (short) (duration + 1));
    }
}
