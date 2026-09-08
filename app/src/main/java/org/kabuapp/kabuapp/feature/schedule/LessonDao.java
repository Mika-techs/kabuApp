package org.kabuapp.kabuapp.feature.schedule;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;
import org.kabuapp.kabuapp.core.data.LocalDateConverter;
import org.kabuapp.kabuapp.feature.schedule.Lesson;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Dao
public interface LessonDao
{
    @Query("SELECT * FROM schedule WHERE userId = :userId")
    List<Lesson> get(UUID userId);
    @Query("SELECT * FROM schedule")
    List<Lesson> getAll();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Lesson> lessons);
    @Update
    void update(Lesson lesson);
    @Query("DELETE FROM schedule")
    void deleteAll();
    @Query("DELETE FROM schedule WHERE userId = :userId")
    void deletePerUser(UUID userId);
    @TypeConverters({LocalDateConverter.class})
    @Query("DELETE FROM schedule WHERE userId = :userId AND date < :date")
    void deletePerUserBeforeDate(UUID userId, LocalDate date);
}
