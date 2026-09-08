package org.kabuapp.kabuapp.core.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.kabuapp.kabuapp.core.data.Lifetime;

import java.util.List;
import java.util.UUID;

@Dao
public interface LifetimeDao
{
    @Query("SELECT * FROM lifetimes WHERE userId = :userId")
    Lifetime get(UUID userId);
    @Query("SELECT * FROM lifetimes")
    List<Lifetime> getAll();
    @Insert
    void insert(Lifetime lifetime);
    @Update
    void update(Lifetime lifetime);
    @Query("DELETE FROM lifetimes WHERE userId = :userId")
    void delete(UUID userId);
}
