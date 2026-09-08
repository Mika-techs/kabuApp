package org.kabuapp.kabuapp.feature.exam;

import org.kabuapp.kabuapp.core.data.LifetimeController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.feature.exam.ExamResponse;
import org.kabuapp.kabuapp.feature.exam.MemExams;
import org.kabuapp.kabuapp.feature.exam.ExamMapper;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.domain.DbType;
import org.kabuapp.kabuapp.core.net.Callback;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ExamController
{
    /** How many months of exams to fetch, starting with the current one. */
    private static final short MONTHS_AHEAD = 3;

    /** August is summer holidays - the API never returns exams for it, so the month is skipped. */
    private static final int SUMMER_HOLIDAY_MONTH = 8;

    @Getter
    private MemExams exams;
    private ExamMapper examMapper;
    private LifetimeController lifetimeController;
    private ExamApi examApi;
    private ExecutorService executorService;
    private AppDatabase db;

    public void updateExams(Callback ce, Object[] objects, Duration duration, UUID userId)
    {
        executorService.execute(() ->
        {
            if (lifetimeController.isLifetimeExpired(duration, DbType.EXAM))
            {
                fetchExams(userId);
                if (ce != null)
                {
                    ce.callback(objects);
                }
                lifetimeController.updateLifetime(DbType.EXAM);
                lifetimeController.saveLifetimeToDb(userId);
            }
            else
            {
                executorService.execute(() -> db.examDao().deletePerUserBeforeDate(userId, DateTimeUtils.getFirstDayOfMonth()));
            }
        });
    }

    /** A 401 is retried by the HTTP layer, so this runs once. */
    private void fetchExams(UUID userId)
    {
        exams.getExams().clear();
        try
        {
            fetchExams(DateTimeUtils.getFirstDayOfMonth(), userId, MONTHS_AHEAD);
        }
        catch (ApiException e)
        {
            Logger.getLogger("ExamController").log(Level.WARNING, "exam refresh failed: " + e.getKind());
        }
    }

    private void fetchExams(LocalDate date, UUID userId, short months) throws ApiException
    {
        Set<LocalDate> datesToRemove = exams.getExams().keySet().stream()
                .filter(key -> key.isBefore(date.withDayOfMonth(1)))
                .collect(Collectors.toSet());
        datesToRemove.forEach(exams.getExams()::remove);
        for (int i = 0; i < months; i++)
        {
            if (date.plusMonths(i).getMonthValue() != SUMMER_HOLIDAY_MONTH)
            {
                fetchExams(date.plusMonths(i).getMonthValue(), userId);
            }
        }
    }

    public void getDbExams(UUID userId)
    {
        executorService.execute(() -> examMapper.mapDbToExams(db.examDao().get(userId), exams));
    }

    private void fetchExams(int month, UUID userId) throws ApiException
    {
        List<ExamResponse> responses = examApi.getExams(month);
        examMapper.mapApiToExams(responses, exams);
        db.examDao().replaceForUser(userId, examMapper.mapExamsToDb(exams, userId));
    }

    public void resetExams(UUID userId)
    {
        resetState();
        executorService.execute(() -> db.examDao().deletePerUser(userId));
    }

    public void resetState()
    {
        exams.getExams().clear();
    }

}
