package org.kabuapp.kabuapp.feature.settings;

import org.kabuapp.kabuapp.feature.settings.MemSettings;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.feature.settings.Settings;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class SettingsController
{
    private MemSettings settings;
    private ExecutorService executorService;
    private AppDatabase db;

    public SettingsController(ExecutorService executorService, AppDatabase db)
    {
        this.executorService = executorService;
        this.db = db;
    }

    public boolean isIsoDate()
    {
        return settings.isIsoDate();
    }

    public void setIsoDate(boolean isoDate)
    {
        if (!Objects.equals(isoDate, settings.isIsoDate()))
        {
            settings.setIsoDate(isoDate);
            updateSettings();
        }
    }

    public boolean isNotificationNextDayExam()
    {
        return settings.isNotificationExamNextDay();
    }

    public void setNotificationNextDayExam(boolean notificationNextDayExam)
    {
        if (!Objects.equals(notificationNextDayExam, settings.isNotificationExamNextDay()))
        {
            settings.setNotificationExamNextDay(notificationNextDayExam);
            updateSettings();
        }
    }

    private void updateSettings()
    {
        executorService.execute(() -> db.settingsDao().update(toSettings()));
    }

    public void loadSettings()
    {
        executorService.execute(() ->
        {
            Settings dbSettings = db.settingsDao().get();
            if (dbSettings == null)
            {
                dbSettings = new Settings(0, false, false);
                db.settingsDao().insert(dbSettings);
            }
            toMemSettings(dbSettings);
        });
    }

    private Settings toSettings()
    {
        return new Settings(0, settings.isIsoDate(), settings.isNotificationExamNextDay());
    }

    private void toMemSettings(Settings dbSettings)
    {
        settings = new MemSettings(dbSettings.isIsoDate(), dbSettings.isNotificationNextDayExam());
    }

    public MemSettings getMemSettingsFromDb()
    {
        Settings dbSettings = db.settingsDao().get();
        if (dbSettings == null)
        {
            return null;
        }
        return new MemSettings(dbSettings.isIsoDate(), dbSettings.isNotificationNextDayExam());
    }
}
