package org.kabuapp.kabuapp.core.data;

import android.content.Context;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.kabuapp.kabuapp.core.net.AuthInterceptor;
import org.kabuapp.kabuapp.core.net.TokenAuthenticator;
import org.kabuapp.kabuapp.feature.auth.AuthApi;
import org.kabuapp.kabuapp.feature.auth.AuthController;
import org.kabuapp.kabuapp.feature.auth.AuthStateholder;
import org.kabuapp.kabuapp.feature.auth.SessionController;
import org.kabuapp.kabuapp.feature.exam.ExamApi;
import org.kabuapp.kabuapp.feature.exam.ExamController;
import org.kabuapp.kabuapp.feature.exam.ExamMapper;
import org.kabuapp.kabuapp.feature.exam.MemExams;
import org.kabuapp.kabuapp.feature.schedule.MemSchedule;
import org.kabuapp.kabuapp.feature.schedule.ScheduleApi;
import org.kabuapp.kabuapp.feature.schedule.ScheduleController;
import org.kabuapp.kabuapp.feature.schedule.ScheduleMapper;
import org.kabuapp.kabuapp.feature.settings.SettingsController;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The object graph. Constructed once by the Application and exposed read-only, so nothing can
 * swap a dependency out at runtime.
 *
 * <p>Two executors on purpose. {@code dbExecutor} is single-threaded, which makes the ordering of
 * database writes an invariant instead of a race - the previous shared cached pool let a delete
 * and the insert that followed it run in either order. {@code ioExecutor} is a small bounded pool
 * for network work, where parallelism is useful and ordering is not.
 */
@Getter
public class AppContainer
{
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);
    /** Upper bound for a whole call including redirects and the 401 retry. */
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(45);
    private static final int IO_THREADS = 2;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(IO_THREADS);

    private final AppDatabase db;
    private final MemSchedule schedule;
    private final LifetimeController lifetimeController;
    private final AuthController authController;
    private final ScheduleController scheduleController;
    private final ExamController examController;
    private final SessionController sessionController;
    private final SettingsController settingsController;

    public AppContainer(Context context)
    {
        db = AppDatabase.getDatabase(context.getApplicationContext());
        schedule = new MemSchedule();

        // The authenticate call must not be intercepted, so it runs on a client without the auth
        // stack. That also breaks the cycle: AuthController needs AuthApi, while the interceptor
        // and the authenticator need AuthController.
        OkHttpClient baseClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT)
            .readTimeout(READ_TIMEOUT)
            .callTimeout(CALL_TIMEOUT)
            .build();

        authController = new AuthController(
            new AuthStateholder(), db, new AuthApi(baseClient), dbExecutor, ioExecutor, new CredentialCipher());

        OkHttpClient authedClient = baseClient.newBuilder()
            .addInterceptor(new AuthInterceptor(authController))
            .authenticator(new TokenAuthenticator(authController))
            .build();

        lifetimeController = new LifetimeController(db, dbExecutor);
        scheduleController = new ScheduleController(
            new ScheduleApi(authedClient), new ScheduleMapper(), lifetimeController, schedule, db, dbExecutor, ioExecutor);
        examController = new ExamController(
            new MemExams(), new ExamMapper(), lifetimeController, new ExamApi(authedClient), dbExecutor, ioExecutor, db);
        sessionController = new SessionController(
            db, examController, lifetimeController, authController, scheduleController, dbExecutor);
        settingsController = new SettingsController(dbExecutor, db);
    }
}
