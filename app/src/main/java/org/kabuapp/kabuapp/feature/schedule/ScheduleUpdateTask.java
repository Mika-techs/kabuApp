package org.kabuapp.kabuapp.feature.schedule;

import android.os.Handler;

import org.kabuapp.kabuapp.KabuApp;

import java.lang.ref.WeakReference;

public class ScheduleUpdateTask implements Runnable
{

    private final Handler uiHandler = new KabuApp.GlobalTaskManager().getMainHandler();

    private WeakReference<ScheduleActivity> activityRef;

    public ScheduleUpdateTask(ScheduleActivity activity)
    {
        this.activityRef = new WeakReference<>(activity);
    }

    public void setRef(ScheduleActivity activity)
    {
        activityRef = new WeakReference<>(activity);
    }

    @Override
    public void run()
    {
        final ScheduleActivity activity = activityRef.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed())
        {
            Runnable uiUpdateAction = () -> activity.callback(null);
            uiHandler.post(uiUpdateAction);
        }
    }
}