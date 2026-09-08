package org.kabuapp.kabuapp.feature.settings;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.domain.DbType;
import org.kabuapp.kabuapp.feature.exam.ExamActivity;
import org.kabuapp.kabuapp.core.ui.Activity;
import org.kabuapp.kabuapp.feature.auth.LoginActivity;
import org.kabuapp.kabuapp.feature.schedule.ScheduleActivity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import static org.kabuapp.kabuapp.databinding.ActivitySettingsBinding.inflate;
import static org.kabuapp.kabuapp.core.ui.NoticeGenerator.setNotice;

public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener
{
    private org.kabuapp.kabuapp.databinding.ActivitySettingsBinding binding;
    private android.widget.Spinner accountSpinner;
    private ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->
        {
            if (isGranted)
            {
                getSettingsController().setNotificationNextDayExam(true);
            }
            else
            {
                binding.settingNotNextDayExam.setChecked(false);
                getSettingsController().setNotificationNextDayExam(false);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        accountSpinner = findViewById(R.id.account_spinner);
        accountSpinner.setOnItemSelectedListener(this);
        refreshAccountsSpinner();

        findViewById(R.id.add_account_button).setOnClickListener(v -> onAddAccountClicked());
        findViewById(R.id.delete_account_button).setOnClickListener(v -> onDeleteAccountClicked());

        barButtonRefListener(binding.barExam, ExamActivity.class);
        barButtonRefListener(binding.barSchedule, ScheduleActivity.class);

        debugSwitchListener();
        isoSwitchSetup();
        setNotice(this, findViewById(R.id.notice_code_settings));
        notificationNextDayExamSetup();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (!getAuthController().isInitialized())
        {
            var i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finish();
        }
        getDelegate().onStart();
    }

    private void refreshAccountsSpinner()
    {
        List<String> usernames = getAuthController().getUsers();
        String currentUsername = getAuthController().getUser();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            usernames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        accountSpinner.setAdapter(adapter);

        if (currentUsername != null && !usernames.isEmpty())
        {
            int selectionPosition = adapter.getPosition(currentUsername);
            if (selectionPosition >= 0)
            {
                accountSpinner.setSelection(selectionPosition);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        String selectedUsername = (String) parent.getItemAtPosition(position);
        String currentActiveUsername = getAuthController().getUser();

        if (!selectedUsername.equals(currentActiveUsername))
        {
            getExecutorService().execute(() ->
            {
                getSessionController().switchAccount(selectedUsername, objects -> runOnUiThread(() ->
                {
                    Intent intent = new Intent(SettingsActivity.this, SettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }));
            });
        }
    }


    private void onDeleteAccountClicked()
    {
        getExecutorService().execute(() ->
        {
            getSessionController().removeUser(getAuthController().getId(), objects ->
            {
                runOnUiThread(this::refreshAccountsSpinner);
                if (getAuthController().getUser() == null && !getAuthController().getUsers().isEmpty())
                {
                    getSessionController().switchAccount(getAuthController().getUsers().get(0), null);
                }
                else
                {
                    runOnUiThread(() ->
                    {
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                }
            });
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {
    }

    private void onAddAccountClicked()
    {
        getSessionController().resetSate();
        startActivity(new Intent(this, LoginActivity.class).putExtra("ADD_NEW_ACCOUNT", true));
    }

    private void isoSwitchSetup()
    {
        binding.settingIsoDate.setChecked(getSettingsController().isIsoDate());
        binding.settingIsoDate.setOnCheckedChangeListener((c, ac) ->
        {
            getSettingsController().setIsoDate(ac);
        });
    }

    private void notificationNextDayExamSetup()
    {
        binding.settingNotNextDayExam.setChecked(getSettingsController().isNotificationNextDayExam());
        binding.settingNotNextDayExam.setOnCheckedChangeListener((c, ac) ->
        {
            if (ac)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED)
                    {
                        getSettingsController().setNotificationNextDayExam(true);
                    }
                    else
                    {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                }
                else
                {
                    getSettingsController().setNotificationNextDayExam(true);
                }
            }
            else
            {
                getSettingsController().setNotificationNextDayExam(false);
            }
        });
    }

    private void debugSwitchListener()
    {
        binding.debugSwitch.setOnCheckedChangeListener((c, ac) ->
        {
            LocalDateTime scheduleUpdate = getLifetimeController().getLastUpdate(DbType.SCHEDULE);
            LocalDateTime examUpdate = getLifetimeController().getLastUpdate(DbType.EXAM);
            if (ac && scheduleUpdate != null && examUpdate != null)
            {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
                binding.debugScheduleLifetime.setText(HtmlCompat.fromHtml(getApplicationContext().getString(R.string.schedule_title) + "<br/>" +
                    formatter.format(scheduleUpdate), HtmlCompat.FROM_HTML_MODE_LEGACY));
                binding.debugExamLifetime.setText(HtmlCompat.fromHtml(getApplicationContext().getString(R.string.exam_title) + "<br/>" +
                    formatter.format(examUpdate), HtmlCompat.FROM_HTML_MODE_LEGACY));
            }
            else
            {
                binding.debugScheduleLifetime.setText("");
                binding.debugExamLifetime.setText("");
            }
        });
    }
}