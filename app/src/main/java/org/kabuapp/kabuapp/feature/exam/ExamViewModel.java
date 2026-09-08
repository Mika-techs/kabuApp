package org.kabuapp.kabuapp.feature.exam;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import org.kabuapp.kabuapp.core.data.ActiveUserStore;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;
import org.kabuapp.kabuapp.domain.RefreshState;

import java.time.Duration;
import java.util.List;

/** Holds the exam screen's state; rows re-point themselves when the active account changes. */
public class ExamViewModel extends ViewModel
{
    private final ExamRepository repository;
    private final ActiveUserStore activeUserStore;

    private final MutableLiveData<RefreshState> refreshState = new MutableLiveData<>(RefreshState.idle());
    private final LiveData<List<ExamRow>> rows;

    public ExamViewModel(ExamRepository repository, ActiveUserStore activeUserStore)
    {
        this.repository = repository;
        this.activeUserStore = activeUserStore;
        this.rows = Transformations.map(
            Transformations.switchMap(activeUserStore.observe(), repository::observe),
            exams -> exams == null ? List.of() : ExamRowFactory.rowsFor(exams, DateTimeUtils.getLocalDate()));
    }

    public LiveData<List<ExamRow>> getRows()
    {
        return rows;
    }

    public LiveData<RefreshState> getRefreshState()
    {
        return refreshState;
    }

    public void refresh(Duration maxAge)
    {
        repository.refresh(activeUserStore.get(), maxAge, refreshState);
    }
}
