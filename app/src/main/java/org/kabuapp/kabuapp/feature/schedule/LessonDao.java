package org.kabuapp.kabuapp.feature.schedule;

import androidx.lifecycle.LiveData;
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
public interface LessonDao
{
    @Query("SELECT * FROM schedule WHERE userId = :userId")
    List<Lesson> get(UUID userId);

    /** Room re-emits on every write, which is what removes the need for manual UI notification. */
    @Query("SELECT * FROM schedule WHERE userId = :userId")
    LiveData<List<Lesson>> observe(UUID userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Lesson> lessons);

    @Query("DELETE FROM schedule WHERE userId = :userId")
    void deletePerUser(UUID userId);

    @TypeConverters({LocalDateConverter.class})
    @Query("DELETE FROM schedule WHERE userId = :userId AND date < :date")
    void deletePerUserBeforeDate(UUID userId, LocalDate date);

    /**
     * Swaps a user's whole schedule atomically. Previously the delete and the insert were two
     * unordered tasks on a cached thread pool, so the delete could land after the insert and
     * wipe the rows that had just been fetched.
     */
    @Transaction
    default void replaceForUser(UUID userId, List<Lesson> lessons)
    {
        deletePerUser(userId);
        insertAll(lessons);
    }
}
