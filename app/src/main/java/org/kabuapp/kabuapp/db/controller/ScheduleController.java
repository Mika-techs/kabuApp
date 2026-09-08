package org.kabuapp.kabuapp.db.controller;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kabuapp.kabuapp.api.DigikabuApiService;
import org.kabuapp.kabuapp.api.exceptions.UnauthorisedException;
import org.kabuapp.kabuapp.api.models.LessonResponse;
import org.kabuapp.kabuapp.data.memory.MemSchedule;
import org.kabuapp.kabuapp.db.ScheduleMapper;
import org.kabuapp.kabuapp.db.model.AppDatabase;
import org.kabuapp.kabuapp.db.model.DbType;
import org.kabuapp.kabuapp.interfaces.AuthCallback;
import org.kabuapp.kabuapp.interfaces.Callback;
import org.kabuapp.kabuapp.utils.DateTimeUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@AllArgsConstructor
public class ScheduleController
{
    private DigikabuApiService apiService;
    private ScheduleMapper scheduleMapper;
    private LifetimeController lifetimeController;
    @Getter
    private MemSchedule schedule;
    private AppDatabase db;
    private ExecutorService executorService;

    public void updateSchedule(String token, AuthCallback re, Callback ce, Object[] objects, Duration duration, UUID userId, boolean async,
                               Consumer<String> setToken)
    {
        Future<?> future = executorService.submit(() ->
        {
            if (lifetimeController.isLifetimeExpired(duration, DbType.SCHEDULE))
            {
                String newToken = updateSchedule(token, re, userId);
                if (!Objects.equals(token, newToken))
                {
                    setToken.accept(newToken);
                }
                lifetimeController.updateLifetime(DbType.SCHEDULE);
                lifetimeController.saveLifetimeToDb(userId);
                if (ce != null)
                {
                    ce.callback(objects);
                }
            }
            else
            {
                executorService.execute(() -> db.lessonDao().deletePerUserBeforeDate(userId, DateTimeUtils.getFirstDayOfWeek()));
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

    private String updateSchedule(String tokenIn, AuthCallback re, UUID userId)
    {
        executorService.execute(() -> db.lessonDao().deletePerUser(userId));
        String token = tokenIn;
        LocalDate begin = DateTimeUtils.getFirstDayOfWeek();
        try
        {
            updateSchedule(begin, 14, token, userId);
            return token;
        }
        catch (UnauthorisedException ignored)
        {
            token = re.renewToken();
        }
        try
        {
            updateSchedule(begin, 14, token, userId);
            return token;
        }
        catch (UnauthorisedException ignored)
        {
        }
        return token;
    }

    private void updateSchedule(LocalDate date, int days, String token, UUID userId) throws UnauthorisedException
    {
        List<LessonResponse> responses = apiService.getSchedule(token, date, days);
        if (responses == null)
        {
            return;
        }
        schedule.getLessons().clear();
        scheduleMapper.mapApiResToSchedule(responses, schedule);
        executorService.execute(() -> db.lessonDao().insertAll(scheduleMapper.mapScheduleToDb(schedule, userId)));
    }

    public void getDbSchedule(UUID userId)
    {
        schedule.getLessons().clear();
        executorService.execute(() -> scheduleMapper.mapDbLessonToSchedule(db.lessonDao().get(userId), schedule));
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
        executorService.execute(() -> db.lessonDao().deletePerUser(userId));
    }

    public void resetState()
    {
        schedule.getLessons().clear();
    }
}
