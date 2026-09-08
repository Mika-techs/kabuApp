package org.kabuapp.kabuapp.feature.settings;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.kabuapp.kabuapp.feature.settings.Settings;

@Dao
public interface SettingsDao
{
    @Query("SELECT * FROM settings")
    Settings get();
    @Insert
    void insert(Settings settings);
    @Update
    void update(Settings settings);
}
