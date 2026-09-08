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
import org.kabuapp.kabuapp.core.data.AppContainer;
import org.kabuapp.kabuapp.core.data.LifetimeController;
import org.kabuapp.kabuapp.feature.auth.AuthController;
import org.kabuapp.kabuapp.feature.auth.SessionController;
import org.kabuapp.kabuapp.feature.settings.SettingsController;

import java.util.concurrent.ExecutorService;

/**
 * Applies edge-to-edge insets and provides the nav-bar button wiring. Dependencies come from
 * {@link AppContainer}; this class is not a dependency conduit of its own.
 */
public abstract class Activity extends AppCompatActivity
{
    protected AppContainer getContainer()
    {
        return ((KabuApp) getApplication()).getContainer();
    }

    protected LifetimeController getLifetimeController()
    {
        return getContainer().getLifetimeController();
    }

    protected SettingsController getSettingsController()
    {
        return getContainer().getSettingsController();
    }

    protected SessionController getSessionController()
    {
        return getContainer().getSessionController();
    }

    protected AuthController getAuthController()
    {
        return getContainer().getAuthController();
    }

    /** Bounded pool for work that must leave the main thread but is not a database write. */
    protected ExecutorService getIoExecutor()
    {
        return getContainer().getIoExecutor();
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
}
