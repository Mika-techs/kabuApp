package org.kabuapp.kabuapp.core.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import org.kabuapp.kabuapp.feature.exam.ExamDao;
import org.kabuapp.kabuapp.feature.schedule.LessonDao;
import org.kabuapp.kabuapp.feature.auth.UserDao;
import org.kabuapp.kabuapp.feature.exam.Exam;
import org.kabuapp.kabuapp.feature.schedule.Lesson;
import org.kabuapp.kabuapp.feature.auth.User;

@Database(entities = { User.class, Lesson.class, Lifetime.class, Exam.class }, version = 5)
@TypeConverters({ LocalDateConverter.class, LocalDateTimeConverter.class, DbTypeConverter.class })
public abstract class AppDatabase extends RoomDatabase
{
    private static final String DB_NAME = "kabuApp-db";

    private static volatile AppDatabase instance;
    public abstract UserDao userDao();
    public abstract ExamDao examDao();
    public abstract LessonDao lessonDao();
    public abstract LifetimeDao lifetimeDao();

    public static AppDatabase getDatabase(final Context context)
    {
        if (instance == null)
        {
            synchronized (AppDatabase.class)
            {
                if (instance == null)
                {
                    // Everything except users is a cache that re-fetches from digikabu.de, so a
                    // schema change costs a re-login rather than real data. No migration chain.
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                        .fallbackToDestructiveMigration(true)
                        .build();
                }
            }
        }
        return instance;
    }
}