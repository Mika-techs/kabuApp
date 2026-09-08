package org.kabuapp.kabuapp.feature.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import org.kabuapp.kabuapp.core.data.ActiveUserStore;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;
import org.kabuapp.kabuapp.domain.RefreshState;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Holds the schedule screen's state. The lesson stream re-points itself when the active account
 * changes, so switching account no longer recreates the activity to force a re-read.
 */
public class ScheduleViewModel extends ViewModel
{
    private final ScheduleRepository repository;
    private final ActiveUserStore activeUserStore;

    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<RefreshState> refreshState = new MutableLiveData<>(RefreshState.idle());
    private final LiveData<List<Lesson>> lessons;
    private final MediatorLiveData<List<ScheduleRow>> rows = new MediatorLiveData<>();

    public ScheduleViewModel(ScheduleRepository repository, ActiveUserStore activeUserStore)
    {
        this.repository = repository;
        this.activeUserStore = activeUserStore;
        this.lessons = Transformations.switchMap(activeUserStore.observe(), repository::observe);

        rows.addSource(lessons, value -> rebuildRows());
        rows.addSource(selectedDate, value -> rebuildRows());
        selectedDate.setValue(DateTimeUtils.getLocalDate());
    }

    public LiveData<List<ScheduleRow>> getRows()
    {
        return rows;
    }

    public LiveData<RefreshState> getRefreshState()
    {
        return refreshState;
    }

    public LiveData<LocalDate> getSelectedDate()
    {
        return selectedDate;
    }

    /** Days with at least one lesson, used by the date strip and by swipe navigation. */
    public LiveData<List<LocalDate>> getSchoolDays()
    {
        return Transformations.map(lessons, value ->
            value == null ? List.of() : ScheduleRowFactory.schoolDays(value));
    }

    public List<Lesson> getCurrentLessons()
    {
        return lessons.getValue() == null ? List.of() : lessons.getValue();
    }

    public void select(LocalDate date)
    {
        selectedDate.setValue(date);
    }

    public void refresh(Duration maxAge)
    {
        repository.refresh(activeUserStore.get(), maxAge, refreshState);
    }

    /** Re-renders without refetching, so the "now" divider and highlight follow the clock. */
    public void onClockTick()
    {
        rebuildRows();
    }

    private void rebuildRows()
    {
        LocalDate date = selectedDate.getValue();
        List<Lesson> value = lessons.getValue();
        if (date == null || value == null)
        {
            rows.setValue(List.of());
            return;
        }
        rows.setValue(ScheduleRowFactory.rowsFor(value, date, DateTimeUtils.getLocalDate(), DateTimeUtils.getLocalTime()));
    }

    public UUID getActiveUserId()
    {
        return activeUserStore.get();
    }
}
