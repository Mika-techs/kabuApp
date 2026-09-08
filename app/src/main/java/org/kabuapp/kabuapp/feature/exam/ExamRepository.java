package org.kabuapp.kabuapp.feature.exam;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import lombok.AllArgsConstructor;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.core.data.LifetimeController;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;
import org.kabuapp.kabuapp.domain.DbType;
import org.kabuapp.kabuapp.domain.RefreshState;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Reads exams out of Room and refreshes them from the API. */
@AllArgsConstructor
public class ExamRepository
{
    private static final Logger LOG = Logger.getLogger("ExamRepository");

    /** How many months of exams to fetch, starting with the current one. */
    private static final int MONTHS_AHEAD = 3;
    /** August is summer holidays - the API never returns exams for it, so the month is skipped. */
    private static final int SUMMER_HOLIDAY_MONTH = 8;

    private final ExamApi examApi;
    private final ExamMapper examMapper;
    private final LifetimeController lifetimeController;
    private final AppDatabase db;
    private final ExecutorService dbExecutor;
    private final ExecutorService ioExecutor;

    public LiveData<List<Exam>> observe(UUID userId)
    {
        return db.examDao().observe(userId);
    }

    public void refresh(UUID userId, Duration maxAge, MutableLiveData<RefreshState> state)
    {
        if (userId == null)
        {
            return;
        }
        if (!lifetimeController.isLifetimeExpired(maxAge, DbType.EXAM))
        {
            dbExecutor.execute(() -> db.examDao().deletePerUserBeforeDate(userId, DateTimeUtils.getFirstDayOfMonth()));
            state.postValue(RefreshState.idle());
            return;
        }
        state.postValue(RefreshState.loading());
        ioExecutor.execute(() ->
        {
            try
            {
                db.examDao().replaceForUser(userId, examMapper.toEntities(fetchMonths(), userId));
                lifetimeController.updateLifetime(DbType.EXAM);
                lifetimeController.saveLifetimeToDb(userId);
                state.postValue(RefreshState.idle());
            }
            catch (ApiException e)
            {
                LOG.log(Level.WARNING, "exam refresh failed: " + e.getKind());
                state.postValue(RefreshState.error(e.getKind()));
            }
        });
    }

    /** All requested months collected before mapping, so a cross-month exam stays one exam. */
    private List<ExamResponse> fetchMonths() throws ApiException
    {
        LocalDate start = DateTimeUtils.getFirstDayOfMonth();
        List<ExamResponse> responses = new ArrayList<>();
        for (int offset = 0; offset < MONTHS_AHEAD; offset++)
        {
            int month = start.plusMonths(offset).getMonthValue();
            if (month != SUMMER_HOLIDAY_MONTH)
            {
                responses.addAll(examApi.getExams(month));
            }
        }
        return responses;
    }

    public void deleteFor(UUID userId)
    {
        dbExecutor.execute(() -> db.examDao().deletePerUser(userId));
    }
}
