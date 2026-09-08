package org.kabuapp.kabuapp;

import android.app.Application;
import android.os.StrictMode;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.color.DynamicColors;
import lombok.Getter;
import org.kabuapp.kabuapp.core.data.AppContainer;
import org.kabuapp.kabuapp.feature.notification.ExamNotificationWorker;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Owns the object graph and the one-off startup work. Dependencies live in {@link AppContainer}
 * rather than as fields here, so nothing can replace them at runtime.
 */
@Getter
public class KabuApp extends Application
{
    private static final int NOTIFICATION_HOUR = 9;
    private static final String DAILY_WORK_NAME = "DailyNotify";

    private AppContainer container;

    @Override
    public void onCreate()
    {
        super.onCreate();

        enableStrictModeInDebug();
        DynamicColors.applyToActivitiesIfAvailable(this);

        container = new AppContainer(this);
        container.getSessionController().loadSession();

        startNotificationWorker();
    }

    /**
     * The permissive policy this replaces was hiding real violations: authentication used to run
     * its HTTP call on the main thread, and two account operations touched the database there.
     */
    private void enableStrictModeInDebug()
    {
        if (!BuildConfig.DEBUG)
        {
            return;
        }
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectNetwork()
            .detectDiskReads()
            .detectDiskWrites()
            .penaltyLog()
            .build());
    }

    private void startNotificationWorker()
    {
        Constraints constraints = new Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build();

        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, NOTIFICATION_HOUR);
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
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest);
    }
}
