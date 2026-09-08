package org.kabuapp.kabuapp.feature.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import lombok.AllArgsConstructor;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.core.data.LifetimeController;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;
import org.kabuapp.kabuapp.domain.DbType;
import org.kabuapp.kabuapp.domain.RefreshState;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads lessons out of Room and refreshes them from the API. Room notifies its observers on
 * write, so this never has to tell the UI that something changed.
 */
@AllArgsConstructor
public class ScheduleRepository
{
    private static final Logger LOG = Logger.getLogger("ScheduleRepository");

    /** The API is always asked for two weeks starting at the first day of the current week. */
    private static final int SCHEDULE_DAYS = 14;

    private final ScheduleApi scheduleApi;
    private final ScheduleMapper scheduleMapper;
    private final LifetimeController lifetimeController;
    private final AppDatabase db;
    private final ExecutorService dbExecutor;
    private final ExecutorService ioExecutor;

    public LiveData<List<Lesson>> observe(UUID userId)
    {
        return db.lessonDao().observe(userId);
    }

    /**
     * Fetches a fresh schedule unless the cached one is younger than {@code maxAge}.
     * Progress and failures are reported through {@code state}; the cached rows stay visible
     * either way.
     */
    public void refresh(UUID userId, Duration maxAge, MutableLiveData<RefreshState> state)
    {
        if (userId == null)
        {
            return;
        }
        if (!lifetimeController.isLifetimeExpired(maxAge, DbType.SCHEDULE))
        {
            dbExecutor.execute(() -> db.lessonDao().deletePerUserBeforeDate(userId, DateTimeUtils.getFirstDayOfWeek()));
            state.postValue(RefreshState.idle());
            return;
        }
        state.postValue(RefreshState.loading());
        ioExecutor.execute(() ->
        {
            LocalDate begin = DateTimeUtils.getFirstDayOfWeek();
            try
            {
                List<Lesson> lessons = scheduleMapper.toEntities(scheduleApi.getSchedule(begin, SCHEDULE_DAYS), userId);
                db.lessonDao().replaceForUser(userId, lessons);
                lifetimeController.updateLifetime(DbType.SCHEDULE);
                lifetimeController.saveLifetimeToDb(userId);
                state.postValue(RefreshState.idle());
            }
            catch (ApiException e)
            {
                LOG.log(Level.WARNING, "schedule refresh failed: " + e.getKind());
                state.postValue(RefreshState.error(e.getKind()));
            }
        });
    }

    public void deleteFor(UUID userId)
    {
        dbExecutor.execute(() -> db.lessonDao().deletePerUser(userId));
    }
}
