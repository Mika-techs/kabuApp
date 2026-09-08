package org.kabuapp.kabuapp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.color.DynamicColors;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.kabuapp.kabuapp.core.net.AuthInterceptor;
import org.kabuapp.kabuapp.core.net.TokenAuthenticator;
import org.kabuapp.kabuapp.feature.auth.AuthApi;
import org.kabuapp.kabuapp.feature.exam.ExamApi;
import org.kabuapp.kabuapp.feature.schedule.ScheduleApi;
import org.kabuapp.kabuapp.feature.auth.AuthStateholder;
import org.kabuapp.kabuapp.feature.exam.MemExams;
import org.kabuapp.kabuapp.core.data.CredentialCipher;
import org.kabuapp.kabuapp.feature.schedule.MemSchedule;
import org.kabuapp.kabuapp.feature.exam.ExamMapper;
import org.kabuapp.kabuapp.feature.schedule.ScheduleMapper;
import org.kabuapp.kabuapp.feature.auth.AuthController;
import org.kabuapp.kabuapp.feature.exam.ExamController;
import org.kabuapp.kabuapp.core.data.LifetimeController;
import org.kabuapp.kabuapp.feature.schedule.ScheduleController;
import org.kabuapp.kabuapp.feature.auth.SessionController;
import org.kabuapp.kabuapp.feature.settings.SettingsController;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.feature.notification.ExamNotificationWorker;
import org.kabuapp.kabuapp.feature.schedule.ScheduleUpdateTask;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;

import java.time.Duration;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class KabuApp extends Application
{
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);
    /** Upper bound for a whole call including redirects and the 401 retry. */
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(45);

    @Getter
    public static class GlobalTaskManager
    {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ScheduleController scheduleController;
    private LifetimeController lifetimeController;
    private ScheduleUpdateTask scheduleUpdateTask;
    private SettingsController settingsController;
    private SessionController sessionController;
    private ExecutorService executorService;
    private ScheduleMapper scheduleMapper;
    private AuthController authController;
    private ExamController examController;
    private ExamMapper examMapper;
    private MemSchedule schedule;
    private AppDatabase db;

    @Override
    public void onCreate()
    {
        super.onCreate();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        DynamicColors.applyToActivitiesIfAvailable(this);

        executorService = Executors.newCachedThreadPool();

        schedule = new MemSchedule();
        schedule.setSelectedDate(DateTimeUtils.getLocalDate());

        db = AppDatabase.getDatabase(getApplicationContext());
        scheduleMapper = new ScheduleMapper();
        examMapper = new ExamMapper();

        // The authenticate call must not be intercepted, so it runs on a client without the
        // auth stack. That also breaks the cycle: AuthController needs AuthApi, and the
        // interceptor and authenticator need AuthController.
        OkHttpClient baseClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT)
            .readTimeout(READ_TIMEOUT)
            .callTimeout(CALL_TIMEOUT)
            .build();
        AuthApi authApi = new AuthApi(baseClient);
        authController = new AuthController(new AuthStateholder(), db, authApi, executorService, new CredentialCipher());

        OkHttpClient authedClient = baseClient.newBuilder()
            .addInterceptor(new AuthInterceptor(authController))
            .authenticator(new TokenAuthenticator(authController))
            .build();

        lifetimeController = new LifetimeController(db, executorService);
        scheduleController = new ScheduleController(
            new ScheduleApi(authedClient), scheduleMapper, lifetimeController, schedule, db, executorService);
        examController = new ExamController(
            new MemExams(), examMapper, lifetimeController, new ExamApi(authedClient), executorService, db);
        sessionController = new SessionController(db, examController, lifetimeController, authController, scheduleController, executorService);
        scheduleUpdateTask = new ScheduleUpdateTask(null);
        settingsController = new SettingsController(executorService, db);

        sessionController.loadSession(scheduleUpdateTask);

        settingsController.loadSettings();
        startNotificationWorker();
    }

    private void startNotificationWorker()
    {
        Constraints constraints = new Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build();

        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= now)
        {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = calendar.getTimeInMillis() - now;
        PeriodicWorkRequest dailyWorkRequest =
            new PeriodicWorkRequest.Builder(ExamNotificationWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("daily_notification_check")
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyNotify",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        );
    }
}
