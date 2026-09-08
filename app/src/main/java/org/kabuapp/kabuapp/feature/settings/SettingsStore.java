package org.kabuapp.kabuapp.feature.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The app's two preferences. Both are device settings rather than account data, so they live in
 * SharedPreferences instead of a one-row database table.
 *
 * <p>Reads are synchronous and in-memory after the first access, which removes two crash paths
 * the table version had: the settings screen reading a value before the async load finished, and
 * the notification worker dereferencing a missing row.
 */
public class SettingsStore
{
    private static final String PREFS = "kabuapp-settings";
    private static final String KEY_ISO_DATE = "isoDate";
    private static final String KEY_NOTIFY_NEXT_DAY_EXAM = "notificationNextDayExam";

    private final SharedPreferences prefs;

    public SettingsStore(Context context)
    {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** ISO date format on the exam screen; deliberately not applied to the schedule date strip. */
    public boolean isIsoDate()
    {
        return prefs.getBoolean(KEY_ISO_DATE, false);
    }

    public void setIsoDate(boolean isoDate)
    {
        prefs.edit().putBoolean(KEY_ISO_DATE, isoDate).apply();
    }

    public boolean isNotificationNextDayExam()
    {
        return prefs.getBoolean(KEY_NOTIFY_NEXT_DAY_EXAM, false);
    }

    public void setNotificationNextDayExam(boolean enabled)
    {
        prefs.edit().putBoolean(KEY_NOTIFY_NEXT_DAY_EXAM, enabled).apply();
    }
}
