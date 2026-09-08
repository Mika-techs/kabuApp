package org.kabuapp.kabuapp.feature.exam;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.TypeConverters;
import org.kabuapp.kabuapp.core.data.LocalDateConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Dao
public interface ExamDao
{
    @Query("SELECT * FROM exams WHERE userId = :userId")
    List<Exam> get(UUID userId);

    @Query("SELECT * FROM exams")
    List<Exam> getAll();

    @TypeConverters({LocalDateConverter.class})
    @Query("SELECT * FROM exams WHERE date = :date")
    List<Exam> getByDate(LocalDate date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Exam> exams);

    @Query("DELETE FROM exams WHERE userId = :userId")
    void deletePerUser(UUID userId);

    @TypeConverters({LocalDateConverter.class})
    @Query("DELETE FROM exams WHERE userId = :userId AND date < :date")
    void deletePerUserBeforeDate(UUID userId, LocalDate date);

    /** Swaps a user's whole exam list atomically; see {@link LessonDao#replaceForUser}. */
    @Transaction
    default void replaceForUser(UUID userId, List<Exam> exams)
    {
        deletePerUser(userId);
        insertAll(exams);
    }
}
