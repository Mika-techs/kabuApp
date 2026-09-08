package org.kabuapp.kabuapp.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.data.memory.MemExam;
import org.kabuapp.kabuapp.data.memory.MemExams;
import org.kabuapp.kabuapp.data.memory.MemSettings;
import org.kabuapp.kabuapp.db.ExamMapper;
import org.kabuapp.kabuapp.db.controller.ExamController;
import org.kabuapp.kabuapp.db.controller.LifetimeController;
import org.kabuapp.kabuapp.db.controller.SettingsController;
import org.kabuapp.kabuapp.db.model.AppDatabase;
import org.kabuapp.kabuapp.exam.ExamActivity;
import org.kabuapp.kabuapp.utils.DateTimeUtils;

import static androidx.core.content.ContextCompat.getSystemService;

public class ExamNotificationWorker extends Worker
{
    public ExamNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params)
    {
        super(context, params);
        createChannel();
    }

    @NonNull
    @Override
    public Result doWork()
    {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        MemExam memExam = getExamNextDay(db);
        if (memExam != null)
        {
            showNotification(memExam);
        }
        return Result.success();
    }

    private void showNotification(MemExam memExam)
    {
        Intent intent = new Intent(getApplicationContext(), ExamActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
            PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "KabuAppExamNextDay")
            .setSmallIcon(R.drawable.kabu_app_mc)
            .setContentTitle(getApplicationContext().getString(R.string.nofification_exam_next_day_title))
            .setContentText(memExam.getInfo())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        NotificationManagerCompat.from(getApplicationContext()).notify(1, builder.build());
    }

    private MemExam getExamNextDay(AppDatabase db)
    {
        if (isNotificationExamNextDay(db))
        {
            MemExam memExam = getMemExams(db).getExams().get(DateTimeUtils.getLocalDate().plusDays(1));
            if (memExam != null && memExam.getDuration() == 1)
            {
                return memExam;
            }
        }
        return null;
    }

    private void createChannel()
    {
        NotificationChannel channel = new NotificationChannel(
            "KabuAppExamNextDay",
            "Exam Alert",
            NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(getApplicationContext(), NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    /* Custom DB access */

    private boolean isNotificationExamNextDay(AppDatabase db)
    {
        SettingsController settingsController = new SettingsController(null, db);
        MemSettings memSettings = settingsController.getMemSettingsFromDb();
        return memSettings != null && memSettings.isNotificationExamNextDay();
    }

    private MemExams getMemExams(AppDatabase db)
    {
        ExamController examController =
            new ExamController(new MemExams(), new ExamMapper(),
                new LifetimeController(null, null, null), null, null, db);
        return examController.getAllMemExamsFromDb();
    }
}
