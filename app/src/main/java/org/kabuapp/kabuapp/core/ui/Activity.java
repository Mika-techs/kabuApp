package org.kabuapp.kabuapp.core.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.kabuapp.kabuapp.KabuApp;
import org.kabuapp.kabuapp.feature.auth.AuthController;
import org.kabuapp.kabuapp.feature.exam.ExamController;
import org.kabuapp.kabuapp.core.data.LifetimeController;
import org.kabuapp.kabuapp.feature.schedule.ScheduleController;
import org.kabuapp.kabuapp.feature.auth.SessionController;
import org.kabuapp.kabuapp.feature.settings.SettingsController;

import java.util.concurrent.ExecutorService;

public abstract class Activity extends AppCompatActivity
{
    protected ScheduleController getScheduleController()
    {
        return getKabuApplication().getScheduleController();
    }

    protected ExamController getExamController()
    {
        return getKabuApplication().getExamController();
    }

    protected LifetimeController getLifetimeController()
    {
        return getKabuApplication().getLifetimeController();
    }

    protected SettingsController getSettingsController()
    {
        return getKabuApplication().getSettingsController();
    }

    protected SessionController getSessionController()
    {
        return getKabuApplication().getSessionController();
    }

    protected ExecutorService getExecutorService()
    {
        return getKabuApplication().getExecutorService();
    }

    protected AuthController getAuthController()
    {
        return getKabuApplication().getAuthController();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    protected void barButtonRefListener(ImageButton settingsButton, Class<?> activity)
    {
        settingsButton.setOnClickListener(v ->
        {
            var i = new Intent(this, activity);
            startActivity(i);
        });
    }

    private KabuApp getKabuApplication()
    {
        return ((KabuApp) getApplication());
    }

}
