package org.kabuapp.kabuapp.feature.exam;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;
import org.kabuapp.kabuapp.core.data.LocalDateConverter;
import org.kabuapp.kabuapp.feature.exam.Exam;

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Exam> exams);

    @Update
    void update(Exam exam);

    @Query("DELETE FROM exams WHERE userId = :userId")
    void deletePerUser(UUID userId);

    @TypeConverters({LocalDateConverter.class})
    @Query("DELETE FROM exams WHERE userId = :userId AND date < :date")
    void deletePerUserBeforeDate(UUID userId, LocalDate date);

    @Query("DELETE FROM exams")
    void deleteAll();
}