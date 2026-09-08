package org.kabuapp.kabuapp.feature.auth;

import org.kabuapp.kabuapp.core.data.LifetimeController;
import org.kabuapp.kabuapp.feature.exam.ExamController;
import org.kabuapp.kabuapp.feature.schedule.ScheduleRepository;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.core.net.Callback;

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
    private ScheduleRepository scheduleRepository;
    private ExecutorService dbExecutor;

    public void loadSession(Callback callback, Object[] objects)
    {
        dbExecutor.execute(() ->
        {
            loadSyncSession();
            callback.callback(objects);
        });
    }

    public void loadSession()
    {
        dbExecutor.execute(this::loadSyncSession);
    }

    /**
     * Restores the active account. The schedule is not loaded here any more - it is observed
     * straight out of Room by the schedule view model.
     */
    private void loadSyncSession()
    {
        UUID userId = authController.getDbUser();
        authController.getDbUsers();
        examController.getDbExams(userId);
        lifetimeController.getDbLifetime(userId);
    }

    public void removeUser(UUID userId, Callback callback)
    {
        removeUser(userId);
        callback.callback(null);
    }

    /**
     * Deleting the user row cascades to schedule, exams and lifetimes, so only the in-memory
     * state still needs clearing.
     */
    public void removeUser(UUID userId)
    {
        db.userDao().delete(userId);
        authController.removeUser(userId);
        scheduleRepository.deleteFor(userId);
        examController.resetState();
        lifetimeController.resetState();
    }

    public void resetSate()
    {
        authController.resetState();
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
        if (callback != null)
        {
            callback.callback(new Object[] { });
        }
    }
}
