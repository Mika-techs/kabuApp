package org.kabuapp.kabuapp.feature.auth;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import java.util.UUID;


@Dao
public interface UserDao
{
    @Query("SELECT * FROM users")
    List<User> getAll();
    @Query("SELECT * FROM users WHERE id = :userId")
    User get(UUID userId);
    @Insert
    void insert(User user);
    @Update
    void update(User user);
    @Query("DELETE FROM users WHERE id = :userId")
    void delete(UUID userId);
}
