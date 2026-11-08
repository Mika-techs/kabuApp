package org.kabuapp.kabuapp.feature.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import org.kabuapp.kabuapp.core.data.LifetimeController;
import org.kabuapp.kabuapp.feature.exam.ExamRepository;
import org.kabuapp.kabuapp.feature.schedule.ScheduleRepository;
import org.kabuapp.kabuapp.core.data.AppDatabase;

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
    private ExamRepository examRepository;
    private LifetimeController lifetimeController;
    private AuthController authController;
    private ScheduleRepository scheduleRepository;
    private ExecutorService dbExecutor;
    /**
     * Whether the stored account has been restored into memory. Activities must not decide
     * "logged in or not" before this is true: after the process was killed, the in-memory
     * credentials are empty until {@link #loadSession()} has run on the db executor.
     */
    private final MutableLiveData<Boolean> loaded = new MutableLiveData<>(false);

    public LiveData<Boolean> isLoaded()
    {
        return loaded;
    }

    public void loadSession(Runnable onLoaded)
    {
        dbExecutor.execute(() ->
        {
            loadSyncSession();
            loaded.postValue(true);
            onLoaded.run();
        });
    }

    public void loadSession()
    {
        dbExecutor.execute(() ->
        {
            loadSyncSession();
            loaded.postValue(true);
        });
    }

    /**
     * Restores the active account. The schedule is not loaded here any more - it is observed
     * straight out of Room by the schedule view model.
     */
    private void loadSyncSession()
    {
        UUID userId = authController.getDbUser();
        authController.loadDbUsers();
        lifetimeController.getDbLifetime(userId);
    }

    public void removeUser(UUID userId, Runnable onRemoved)
    {
        removeUser(userId);
        onRemoved.run();
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
        examRepository.deleteFor(userId);
        lifetimeController.resetState();
    }

    public void resetSate()
    {
        authController.resetState();
        lifetimeController.resetState();
    }

    public List<Map<UUID, String>> getUsers()
    {
        return db.userDao().getAll().stream().map(user -> Map.of(user.id(), user.username())).collect(Collectors.toList());
    }

    public void switchAccount(String selectedUsername, Runnable onSwitched)
    {
        resetSate();
        UUID userId = authController.getDbUserByNameAndLoad(selectedUsername);
        lifetimeController.getDbLifetime(userId);
        if (onSwitched != null)
        {
            onSwitched.run();
        }
    }
}
