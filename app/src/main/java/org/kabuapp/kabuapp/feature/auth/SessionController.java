package org.kabuapp.kabuapp.feature.auth;

import org.kabuapp.kabuapp.core.data.LifetimeController;
import org.kabuapp.kabuapp.feature.exam.ExamController;
import org.kabuapp.kabuapp.feature.schedule.ScheduleController;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.core.net.Callback;
import org.kabuapp.kabuapp.feature.schedule.ScheduleUpdateTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SessionController
{
    private AppDatabase db;
    private ExamController examController;
    private LifetimeController lifetimeController;
    private AuthController authController;
    private ScheduleController scheduleController;
    private ExecutorService executorService;

    public void loadSession(Callback callback, Object[] objects, ScheduleUpdateTask runnable)
    {
        executorService.execute(() ->
        {
            loadSyncSession(runnable);
            callback.callback(objects);
        });
    }

    public void loadSession(ScheduleUpdateTask runnable)
    {
        executorService.execute(() -> loadSyncSession(runnable));
    }

    private void loadSyncSession(ScheduleUpdateTask runnable)
    {
        UUID userId = authController.getDbUser();
        authController.getDbUsers();
        examController.getDbExams(userId);
        lifetimeController.getDbLifetime(userId);
        scheduleController.getDbSchedule(userId);
        if (runnable != null)
        {
            runnable.run();
        }
    }

    public void removeUser(UUID userId, Callback callback)
    {
        removeUser(userId);
        callback.callback(null);
    }

    public void removeUser(UUID userId)
    {
        db.userDao().delete(userId);
        authController.removeUser(userId);
        scheduleController.resetSchedule(userId);
        examController.resetExams(userId);
        lifetimeController.resetLifetimes(userId);
    }

    public void resetSate()
    {
        authController.getId();
        authController.resetState();
        scheduleController.resetState();
        examController.resetState();
        lifetimeController.resetState();
    }

    public List<Map<UUID, String>> getUsers()
    {
        return db.userDao().getAll().stream().map(user -> Map.of(user.getId(), user.getUsername())).collect(Collectors.toList());
    }

    public void switchAccount(String selectedUsername, Callback callback)
    {
        resetSate();
        UUID userId = authController.getDbUserByNameAndLoad(selectedUsername);
        examController.getDbExams(userId);
        lifetimeController.getDbLifetime(userId);
        scheduleController.getDbSchedule(userId);
        if (callback != null)
        {
            callback.callback(new Object[] { });
        }
    }
}
