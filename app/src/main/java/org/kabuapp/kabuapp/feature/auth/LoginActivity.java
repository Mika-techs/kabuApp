package org.kabuapp.kabuapp.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.core.ui.Activity;
import org.kabuapp.kabuapp.core.ui.ViewModelFactory;
import org.kabuapp.kabuapp.databinding.ActivityLoginBinding;
import org.kabuapp.kabuapp.feature.schedule.ScheduleActivity;

import static org.kabuapp.kabuapp.core.ui.NoticeGenerator.setNotice;

/** The login screen, and the entry point for adding a further account. */
public class LoginActivity extends Activity
{
    private static final String EXTRA_ADD_NEW_ACCOUNT = "ADD_NEW_ACCOUNT";

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getAuthController().isInitialized())
        {
            goToSchedule();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new ViewModelFactory(getContainer())).get(AuthViewModel.class);
        viewModel.getLoginResult().observe(this, this::onLoginResult);

        binding.login.setOnClickListener(v -> viewModel.login(
            binding.username.getText().toString(), binding.password.getText().toString()));

        setNotice(this, findViewById(R.id.notice_code_login));

        if (getIntent().getBooleanExtra(EXTRA_ADD_NEW_ACCOUNT, false))
        {
            binding.loginButtonBack.setVisibility(View.VISIBLE);
            binding.loginButtonBack.setOnClickListener(v -> viewModel.cancelAddAccount());
        }
    }

    private void onLoginResult(LoginResult result)
    {
        if (result.success())
        {
            goToSchedule();
            return;
        }
        showError(result.errorKind());
    }

    private void goToSchedule()
    {
        startActivity(new Intent(this, ScheduleActivity.class));
        finish();
    }

    /** Wrong credentials and an unreachable server used to look identical to the user. */
    private void showError(ApiException.Kind kind)
    {
        int message;
        switch (kind)
        {
            case NETWORK:
                message = R.string.error_network;
                break;
            case SERVER:
                message = R.string.error_server;
                break;
            case UNAUTHORISED:
                message = R.string.error_session_expired;
                break;
            default:
                message = R.string.login_wrong;
                break;
        }
        EditText target = binding.username.isFocused() ? binding.username : binding.password;
        target.setError(getString(message));
    }
}
