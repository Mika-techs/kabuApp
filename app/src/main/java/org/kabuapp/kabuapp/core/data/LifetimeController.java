package org.kabuapp.kabuapp.core.data;

import lombok.AllArgsConstructor;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;
import org.kabuapp.kabuapp.domain.DbType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Tracks when each kind of cached data was last refreshed, so callers can express staleness as a
 * Duration instead of invalidating caches by hand.
 */
@AllArgsConstructor
public class LifetimeController
{
    private final Map<DbType, LocalDateTime> lastUpdates = new EnumMap<>(DbType.class);

    private AppDatabase db;
    private ExecutorService dbExecutor;

    public void updateLifetime(DbType type)
    {
        lastUpdates.put(type, DateTimeUtils.getLocalDateTime());
    }

    public LocalDateTime getLastUpdate(DbType type)
    {
        return lastUpdates.get(type);
    }

    public boolean isLifetimeExpired(Duration duration, DbType type)
    {
        LocalDateTime lastUpdate = lastUpdates.get(type);
        return lastUpdate == null || lastUpdate.isBefore(DateTimeUtils.getLocalDateTime().minus(duration));
    }

    public void resetLifetimes(UUID userId)
    {
        resetState();
        saveLifetimeToDb(userId);
    }

    public void resetState()
    {
        lastUpdates.clear();
    }

    public void saveLifetimeToDb(UUID userId)
    {
        dbExecutor.execute(() ->
        {
            for (DbType type : DbType.values())
            {
                db.lifetimeDao().upsert(new Lifetime(userId, type, lastUpdates.get(type)));
            }
        });
    }

    public void getDbLifetime(UUID userId)
    {
        dbExecutor.execute(() ->
        {
            List<Lifetime> lifetimes = db.lifetimeDao().get(userId);
            lifetimes.forEach(lifetime -> lastUpdates.put(lifetime.dbType(), lifetime.lastUpdate()));
        });
    }
}
