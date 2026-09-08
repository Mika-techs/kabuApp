package org.kabuapp.kabuapp.feature.auth;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.databinding.ActivityLoginBinding;
import org.kabuapp.kabuapp.core.net.Callback;
import org.kabuapp.kabuapp.core.ui.Activity;
import org.kabuapp.kabuapp.feature.schedule.ScheduleActivity;

import static org.kabuapp.kabuapp.core.ui.NoticeGenerator.setNotice;

public class LoginActivity extends Activity implements Callback
{
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getAuthController().isInitialized())
        {
            var i = new Intent(this, ScheduleActivity.class);
            startActivity(i);
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        authHandler();
        setNotice(this, findViewById(R.id.notice_code_login));

        boolean isAddNewAccountFlow = getIntent().getBooleanExtra("ADD_NEW_ACCOUNT", false);
        if (isAddNewAccountFlow)
        {
            binding.loginButtonBack.setVisibility(View.VISIBLE);
            binding.loginButtonBack.setOnClickListener((v) ->
            {
                getSessionController().loadSession(this, null, null);
            });
        }
    }

    private void authHandler()
    {
        final EditText username = binding.username;
        final EditText password = binding.password;

        binding.login.setOnClickListener(v ->
        {
            Object[] args = { null };
            if (!getAuthController().setCredentials(username.getText().toString(), password.getText().toString(), this, args))
            {
                if (username.isFocused())
                {
                    username.setError(getString(R.string.login_wrong));
                }
                else
                {
                    password.setError(getString(R.string.login_wrong));
                }
            }
        });
    }

    public void callback(Object[] args)
    {
        if (args == null || args[0] == null)
        {
            Intent i = new Intent(this, ScheduleActivity.class);
            startActivity(i);
        }
        else
        {
            if (binding.username.isFocused())
            {
                binding.username.setError(getString(R.string.login_wrong));
            }
            else
            {
                binding.password.setError(getString(R.string.login_wrong));
            }
        }
    }
}