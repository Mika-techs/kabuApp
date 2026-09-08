package org.kabuapp.kabuapp.feature.exam;

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
 * One exam. Keyed by user, start date and description so that two exams on the same day are
 * distinct rows - the previous single-exam-per-day map silently dropped the second one.
 */
@Getter
@Setter
@AllArgsConstructor
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
public class Exam
{
    @NonNull
    @ColumnInfo(name = "userId")
    private UUID userId;
    @NonNull
    @ColumnInfo(name = "date")
    private LocalDate date;
    @NonNull
    @ColumnInfo(name = "info")
    private String info;
    /** Number of consecutive days the exam spans. */
    @ColumnInfo(name = "duration")
    private Short duration;
}
