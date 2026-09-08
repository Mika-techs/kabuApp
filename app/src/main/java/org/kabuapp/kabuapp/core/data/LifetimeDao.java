package org.kabuapp.kabuapp.core.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
import java.util.UUID;

@Dao
public interface LifetimeDao
{
    @Query("SELECT * FROM lifetimes WHERE userId = :userId")
    List<Lifetime> get(UUID userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Lifetime lifetime);
}
