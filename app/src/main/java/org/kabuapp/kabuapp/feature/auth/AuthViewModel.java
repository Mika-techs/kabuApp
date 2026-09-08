package org.kabuapp.kabuapp.feature.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.kabuapp.kabuapp.core.net.ApiException;

import java.util.concurrent.ExecutorService;

/**
 * Drives the login screen. Replaces the untyped {@code Callback(Object[])} hand-off, where
 * {@code args[0] = false} meant "authentication failed".
 */
public class AuthViewModel extends ViewModel
{
    private final AuthController authController;
    private final SessionController sessionController;
    private final ExecutorService ioExecutor;

    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    public AuthViewModel(AuthController authController, SessionController sessionController, ExecutorService ioExecutor)
    {
        this.authController = authController;
        this.sessionController = sessionController;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<LoginResult> getLoginResult()
    {
        return loginResult;
    }

    /** Authenticates off the main thread and reports the outcome through {@link #getLoginResult()}. */
    public void login(String username, String password)
    {
        if (username == null || username.isEmpty() || password == null || password.isEmpty())
        {
            loginResult.setValue(LoginResult.failure(ApiException.Kind.BAD_CREDENTIALS));
            return;
        }
        ioExecutor.execute(() ->
        {
            ApiException.Kind failure = authController.login(username, password);
            loginResult.postValue(failure == null ? LoginResult.ok() : LoginResult.failure(failure));
        });
    }

    /** Abandons an add-account attempt and restores the previously active session. */
    public void cancelAddAccount()
    {
        sessionController.loadSession(() -> loginResult.postValue(LoginResult.ok()));
    }
}
