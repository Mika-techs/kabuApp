package org.kabuapp.kabuapp.feature.schedule;

import org.kabuapp.kabuapp.core.data.LifetimeController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.feature.schedule.LessonResponse;
import org.kabuapp.kabuapp.feature.schedule.MemSchedule;
import org.kabuapp.kabuapp.feature.schedule.ScheduleMapper;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.domain.DbType;
import org.kabuapp.kabuapp.core.net.Callback;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@AllArgsConstructor
public class ScheduleController
{
    /** The API is always asked for two weeks starting at the first day of the current week. */
    private static final int SCHEDULE_DAYS = 14;

    private ScheduleApi scheduleApi;
    private ScheduleMapper scheduleMapper;
    private LifetimeController lifetimeController;
    @Getter
    private MemSchedule schedule;
    private AppDatabase db;
    private ExecutorService dbExecutor;
    private ExecutorService ioExecutor;

    public void updateSchedule(Callback ce, Object[] objects, Duration duration, UUID userId, boolean async)
    {
        Future<?> future = ioExecutor.submit(() ->
        {
            if (lifetimeController.isLifetimeExpired(duration, DbType.SCHEDULE))
            {
                fetchSchedule(userId);
                lifetimeController.updateLifetime(DbType.SCHEDULE);
                lifetimeController.saveLifetimeToDb(userId);
                if (ce != null)
                {
                    ce.callback(objects);
                }
            }
            else
            {
                dbExecutor.execute(() -> db.lessonDao().deletePerUserBeforeDate(userId, DateTimeUtils.getFirstDayOfWeek()));
            }
        });
        if (!async)
        {
            try
            {
                future.get(60, TimeUnit.SECONDS);
            }
            catch (ExecutionException | InterruptedException | TimeoutException e)
            {
                Logger.getLogger("updateSchedule").log(Level.WARNING, e.toString());
            }
        }
    }

    /** Fetches two weeks from the start of the current week. A 401 is retried by the HTTP layer. */
    private void fetchSchedule(UUID userId)
    {
        LocalDate begin = DateTimeUtils.getFirstDayOfWeek();
        try
        {
            List<LessonResponse> responses = scheduleApi.getSchedule(begin, SCHEDULE_DAYS);
            schedule.getLessons().clear();
            scheduleMapper.mapApiResToSchedule(responses, schedule);
            db.lessonDao().replaceForUser(userId, scheduleMapper.mapScheduleToDb(schedule, userId));
        }
        catch (ApiException e)
        {
            Logger.getLogger("ScheduleController").log(Level.WARNING, "schedule refresh failed: " + e.getKind());
        }
    }

    public void getDbSchedule(UUID userId)
    {
        schedule.getLessons().clear();
        dbExecutor.execute(() -> scheduleMapper.mapDbLessonToSchedule(db.lessonDao().get(userId), schedule));
    }

    public boolean isSchool(LocalDate date)
    {
        return schedule.getLessons() != null
            && schedule.getLessons().containsKey(date)
            && !schedule.getLessons().get(date).isEmpty();
    }

    public void resetSchedule(UUID userId)
    {
        resetState();
        dbExecutor.execute(() -> db.lessonDao().deletePerUser(userId));
    }

    public void resetState()
    {
        schedule.getLessons().clear();
    }
}
