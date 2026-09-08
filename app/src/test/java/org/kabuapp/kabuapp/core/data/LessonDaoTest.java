package org.kabuapp.kabuapp.core.data;

import android.app.Application;
import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kabuapp.kabuapp.feature.auth.User;
import org.kabuapp.kabuapp.feature.schedule.Lesson;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Covers the invariant that replaced the delete-then-insert refresh: the natural primary key
 * makes an insert an upsert, and the foreign key cascades a deleted account's rows away.
 */
@RunWith(RobolectricTestRunner.class)
// A plain Application: this exercises the DAO, not the app's startup graph.
@Config(application = Application.class)
public class LessonDaoTest
{
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);

    private AppDatabase db;
    private UUID userId;

    @Before
    public void setUp()
    {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
            .allowMainThreadQueries()
            .build();
        userId = UUID.randomUUID();
        db.userDao().insert(new User(userId, "student", new byte[] { 1 }, new byte[] { 2 }));
    }

    @After
    public void tearDown()
    {
        db.close();
    }

    private Lesson lesson(short begin, short group, String name)
    {
        return new Lesson(userId, MONDAY, begin, group, begin, (short) 1, name, "Muster", "A1");
    }

    @Test
    public void insertingTheSameKeyTwiceReplacesRatherThanDuplicates()
    {
        db.lessonDao().insertAll(List.of(lesson((short) 1, (short) 1, "Maths")));
        db.lessonDao().insertAll(List.of(lesson((short) 1, (short) 1, "German")));

        List<Lesson> stored = db.lessonDao().getAllForTest(userId);
        assertEquals(1, stored.size());
        assertEquals("German", stored.get(0).name());
    }

    @Test
    public void lessonsDifferingOnlyByGroupAreSeparateRows()
    {
        db.lessonDao().insertAll(List.of(
            lesson((short) 1, (short) 1, "Sport A"),
            lesson((short) 1, (short) 2, "Sport B")));

        assertEquals(2, db.lessonDao().getAllForTest(userId).size());
    }

    @Test
    public void replaceForUserSwapsTheWholeScheduleAtomically()
    {
        db.lessonDao().insertAll(List.of(lesson((short) 1, (short) 1, "Old"), lesson((short) 2, (short) 1, "Also old")));
        db.lessonDao().replaceForUser(userId, List.of(lesson((short) 5, (short) 1, "New")));

        List<Lesson> stored = db.lessonDao().getAllForTest(userId);
        assertEquals(1, stored.size());
        assertEquals("New", stored.get(0).name());
    }

    @Test
    public void replaceForUserLeavesOtherAccountsAlone()
    {
        UUID other = UUID.randomUUID();
        db.userDao().insert(new User(other, "second", new byte[] { 1 }, new byte[] { 2 }));
        db.lessonDao().insertAll(List.of(new Lesson(other, MONDAY, (short) 1, (short) 1, (short) 1, (short) 1, "Theirs", "M", "B2")));

        db.lessonDao().replaceForUser(userId, List.of(lesson((short) 3, (short) 1, "Mine")));

        assertEquals(1, db.lessonDao().getAllForTest(other).size());
        assertEquals(1, db.lessonDao().getAllForTest(userId).size());
    }

    @Test
    public void deletingAnAccountCascadesItsLessonsAway()
    {
        db.lessonDao().insertAll(List.of(lesson((short) 1, (short) 1, "Maths")));

        db.userDao().delete(userId);

        assertTrue(db.lessonDao().getAllForTest(userId).isEmpty());
    }

    @Test
    public void deletePerUserBeforeDateKeepsLaterLessons()
    {
        db.lessonDao().insertAll(List.of(
            new Lesson(userId, MONDAY.minusDays(7), (short) 1, (short) 1, (short) 1, (short) 1, "Last week", "M", "A1"),
            lesson((short) 1, (short) 1, "This week")));

        db.lessonDao().deletePerUserBeforeDate(userId, MONDAY);

        List<Lesson> stored = db.lessonDao().getAllForTest(userId);
        assertEquals(1, stored.size());
        assertEquals("This week", stored.get(0).name());
    }
}
