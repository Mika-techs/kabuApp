package org.kabuapp.kabuapp.core.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import org.kabuapp.kabuapp.feature.exam.ExamDao;
import org.kabuapp.kabuapp.feature.schedule.LessonDao;
import org.kabuapp.kabuapp.core.data.LifetimeDao;
import org.kabuapp.kabuapp.feature.settings.SettingsDao;
import org.kabuapp.kabuapp.feature.auth.UserDao;
import org.kabuapp.kabuapp.feature.exam.Exam;
import org.kabuapp.kabuapp.feature.schedule.Lesson;
import org.kabuapp.kabuapp.core.data.Lifetime;
import org.kabuapp.kabuapp.feature.settings.Settings;
import org.kabuapp.kabuapp.feature.auth.User;

@Database(entities = { User.class, Lesson.class, Lifetime.class, Exam.class, Settings.class }, version = 2)
public abstract class AppDatabase extends RoomDatabase
{
    private static final Migration MIGRATION_1_2 = new Migration(1, 2)
    {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database)
        {
            database.execSQL("ALTER TABLE settings ADD COLUMN notificationNextDayExam INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static volatile AppDatabase instance;
    public abstract UserDao userDao();
    public abstract ExamDao examDao();
    public abstract LessonDao lessonDao();
    public abstract LifetimeDao lifetimeDao();
    public abstract SettingsDao settingsDao();

    public static AppDatabase getDatabase(final Context context)
    {
        if (instance == null)
        {
            synchronized (AppDatabase.class)
            {
                if (instance == null)
                {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "kabuApp-db")
                        .addMigrations(MIGRATION_1_2)
                        .build();
                }
            }
        }
        return instance;
    }
}