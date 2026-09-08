package org.kabuapp.kabuapp.feature.notification;

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
import org.kabuapp.kabuapp.feature.exam.Exam;
import org.kabuapp.kabuapp.feature.settings.SettingsStore;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.feature.exam.ExamActivity;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;

import static androidx.core.content.ContextCompat.getSystemService;

public class ExamNotificationWorker extends Worker
{
    private static final short SINGLE_DAY = 1;

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
        Exam exam = getExamNextDay(db);
        if (exam != null)
        {
            showNotification(exam);
        }
        return Result.success();
    }

    private void showNotification(Exam exam)
    {
        Intent intent = new Intent(getApplicationContext(), ExamActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
            PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "KabuAppExamNextDay")
            .setSmallIcon(R.drawable.kabu_app_mc)
            .setContentTitle(getApplicationContext().getString(R.string.nofification_exam_next_day_title))
            .setContentText(exam.getInfo())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        NotificationManagerCompat.from(getApplicationContext()).notify(1, builder.build());
    }

    /** A single-day exam starting tomorrow, for any stored account, or null. */
    private Exam getExamNextDay(AppDatabase db)
    {
        if (!new SettingsStore(getApplicationContext()).isNotificationNextDayExam())
        {
            return null;
        }
        return db.examDao().getByDate(DateTimeUtils.getLocalDate().plusDays(1)).stream()
            .filter(exam -> exam.getDuration() != null && exam.getDuration() == SINGLE_DAY)
            .findFirst()
            .orElse(null);
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


}
