package org.kabuapp.kabuapp.core.data;

import org.kabuapp.kabuapp.core.data.MemLifetime;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.domain.DbType;
import org.kabuapp.kabuapp.core.data.Lifetime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class LifetimeController
{
    private AppDatabase db;
    private ExecutorService executorService;
    @Getter
    private MemLifetime memLifetime;

    public void updateLifetime(DbType type)
    {
        switch (type)
        {
            case EXAM -> memLifetime.setExamLastUpdate(LocalDateTime.now());
            case SCHEDULE -> memLifetime.setScheduleLastUpdate(LocalDateTime.now());
        }
    }

    public boolean isLifetimeExpired(Duration duration, DbType type)
    {
        return switch (type)
        {
            case EXAM -> memLifetime.getExamLastUpdate() == null  || memLifetime.getExamLastUpdate().isBefore(LocalDateTime.now().minus(duration));
            case SCHEDULE -> memLifetime.getScheduleLastUpdate() == null  || memLifetime.getScheduleLastUpdate().isBefore(LocalDateTime.now().minus(duration));
        };
    }

    public void resetLifetimes(UUID userId)
    {
        resetState();
        saveLifetimeToDb(userId);
    }

    public void resetState()
    {
        memLifetime.setScheduleLastUpdate(null);
        memLifetime.setExamLastUpdate(null);
    }

    public void saveLifetimeToDb(UUID userId)
    {
        executorService.execute(() ->
        {
            Lifetime lifetime = db.lifetimeDao().get(userId);
            if (lifetime != null)
            {
                lifetime.setExamLastUpdate(memLifetime.getExamLastUpdate());
                lifetime.setScheduleLastUpdate(memLifetime.getScheduleLastUpdate());
                db.lifetimeDao().update(lifetime);
            }
            else
            {
                lifetime = new Lifetime(UUID.randomUUID(), userId, memLifetime.getScheduleLastUpdate(), memLifetime.getExamLastUpdate());
                db.lifetimeDao().insert(lifetime);
            }
        });
    }

    public void getDbLifetime(UUID userId)
    {
        executorService.execute(() ->
        {
            Lifetime lifetime = db.lifetimeDao().get(userId);
            if (lifetime != null)
            {
                memLifetime.setDbId(lifetime.getId());
                memLifetime.setScheduleLastUpdate(lifetime.getScheduleLastUpdate());
                memLifetime.setExamLastUpdate(lifetime.getExamLastUpdate());
            }
        });
    }
}
