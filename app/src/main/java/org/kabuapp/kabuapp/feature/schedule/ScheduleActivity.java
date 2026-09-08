package org.kabuapp.kabuapp.feature.schedule;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.core.ui.Activity;
import org.kabuapp.kabuapp.core.ui.ViewModelFactory;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;
import org.kabuapp.kabuapp.databinding.ActivityScheduleBinding;
import org.kabuapp.kabuapp.domain.LessonPeriods;
import org.kabuapp.kabuapp.domain.RefreshState;
import org.kabuapp.kabuapp.feature.auth.LoginActivity;
import org.kabuapp.kabuapp.feature.exam.ExamActivity;
import org.kabuapp.kabuapp.feature.settings.SettingsActivity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * The schedule screen. Rows come from {@link ScheduleViewModel} through LiveData, so the previous
 * eight-second polling loop that rebuilt the whole day is gone; the only timer left is the
 * one-second countdown to the next lesson.
 */
public class ScheduleActivity extends Activity implements DateAdapter.OnDateSelectedListener, SwipeRefreshLayout.OnRefreshListener
{
    private static final Duration SCHEDULE_MAX_AGE = Duration.ofHours(2);
    /** Pull-to-refresh: short enough that the TTL never blocks the request. */
    private static final Duration FORCE_REFRESH = Duration.ofSeconds(1);
    private static final long COUNTDOWN_INTERVAL_MS = 999;
    private static final int SEARCH_DAYS = 15;
    private static final int SWIPE_VELOCITY_THRESHOLD_DP = 69;
    private static final int SWIPE_THRESHOLD_DP = 69;

    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault());
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");
    private final DateTimeFormatter weekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault());
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private ActivityScheduleBinding binding;
    private ScheduleViewModel viewModel;
    private ScheduleAdapter scheduleAdapter;
    private DateAdapter dateAdapter;
    private GestureDetector gestureDetector;
    private Runnable countdownRunnable;
    private List<LocalDate> schoolDays = List.of();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = ActivityScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new ViewModelFactory(getContainer())).get(ScheduleViewModel.class);

        barButtonRefListener(binding.barSettings, SettingsActivity.class);
        barButtonRefListener(binding.barExam, ExamActivity.class);

        setUpScheduleList();
        setUpDateStrip();
        observeViewModel();

        binding.swipeRefreshLayoutSchedule.setOnRefreshListener(this);
        viewModel.refresh(SCHEDULE_MAX_AGE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpScheduleList()
    {
        scheduleAdapter = new ScheduleAdapter();
        RecyclerView list = binding.recyclerViewSchedule;
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(scheduleAdapter);

        gestureDetector = new GestureDetector(this, new ScheduleGestureListener());
        list.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener()
        {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView view, @NonNull MotionEvent event)
            {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void setUpDateStrip()
    {
        dateAdapter = new DateAdapter(this, new ArrayList<>(), this);
        binding.recyclerViewDateSelector.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewDateSelector.setAdapter(dateAdapter);
    }

    private void observeViewModel()
    {
        viewModel.getRows().observe(this, rows ->
        {
            scheduleAdapter.submitList(rows);
            binding.noSchoolHint.setVisibility(schoolDays.isEmpty() ? VISIBLE : GONE);
        });

        viewModel.getSchoolDays().observe(this, days ->
        {
            schoolDays = days;
            dateAdapter.setDates(toDateItems(days));
            LocalDate selected = viewModel.getSelectedDate().getValue();
            if (selected != null && !days.contains(selected) && !days.isEmpty())
            {
                viewModel.select(nextSchoolDayFrom(selected));
            }
            else if (selected != null)
            {
                dateAdapter.setSelectedDate(selected);
            }
            binding.noSchoolHint.setVisibility(days.isEmpty() ? VISIBLE : GONE);
        });

        viewModel.getSelectedDate().observe(this, date ->
        {
            dateAdapter.setSelectedDate(date);
            scrollDateStripToSelection();
        });

        viewModel.getRefreshState().observe(this, this::onRefreshState);
    }

    /**
     * The spinner is cleared from the refresh state rather than immediately after firing the
     * request, so it now reflects the request actually finishing - or failing.
     */
    private void onRefreshState(RefreshState state)
    {
        binding.swipeRefreshLayoutSchedule.setRefreshing(state.status() == RefreshState.Status.LOADING);
        if (state.status() != RefreshState.Status.ERROR)
        {
            return;
        }
        if (state.errorKind() == ApiException.Kind.UNAUTHORISED)
        {
            goToLogin();
            return;
        }
        int message = state.errorKind() == ApiException.Kind.NETWORK
            ? R.string.error_network
            : R.string.error_server;
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onRefresh()
    {
        viewModel.refresh(FORCE_REFRESH);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (!getAuthController().isInitialized())
        {
            goToLogin();
            return;
        }
        startCountdown();
        getDelegate().onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        stopCountdown();
    }

    private void goToLogin()
    {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onDateSelected(LocalDate date)
    {
        viewModel.select(date);
    }

    private List<DateItem> toDateItems(List<LocalDate> days)
    {
        List<LocalDate> dates = days.isEmpty() ? List.of(DateTimeUtils.getLocalDate()) : days;
        List<DateItem> items = new ArrayList<>();
        dates.forEach(date -> items.add(new DateItem(
            date, date.format(monthFormatter), date.format(dayFormatter), date.format(weekdayFormatter))));
        return items;
    }

    private void scrollDateStripToSelection()
    {
        binding.recyclerViewDateSelector.post(() ->
        {
            int position = dateAdapter.getSelectedItemPosition();
            if (position != RecyclerView.NO_POSITION)
            {
                ((LinearLayoutManager) binding.recyclerViewDateSelector.getLayoutManager())
                    .scrollToPositionWithOffset(position, 0);
            }
        });
    }

    /** Next day that has lessons, falling back to the last known school day. */
    private LocalDate nextSchoolDayFrom(LocalDate from)
    {
        return schoolDays.stream()
            .filter(day -> !day.isBefore(from))
            .findFirst()
            .orElse(schoolDays.get(schoolDays.size() - 1));
    }

    private void startCountdown()
    {
        stopCountdown();
        countdownRunnable = () ->
        {
            updateCountdown();
            viewModel.onClockTick();
            timerHandler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL_MS);
        };
        timerHandler.post(countdownRunnable);
    }

    private void stopCountdown()
    {
        if (countdownRunnable != null)
        {
            timerHandler.removeCallbacks(countdownRunnable);
        }
    }

    /** Time until the next period boundary today, hidden unless today is the selected day. */
    private void updateCountdown()
    {
        TextView countdown = binding.textViewNextLessonTimer;
        LocalDate today = DateTimeUtils.getLocalDate();
        if (!today.equals(viewModel.getSelectedDate().getValue()))
        {
            countdown.setVisibility(GONE);
            return;
        }
        LocalTime now = DateTimeUtils.getLocalTime();
        Optional<LocalTime> next = viewModel.getCurrentLessons().stream()
            .filter(lesson -> today.equals(lesson.date()))
            .flatMap(lesson -> Stream.of(
                LessonPeriods.begin(lesson.begin()),
                LessonPeriods.end(lesson.end()),
                LessonPeriods.begin(LessonPeriods.FIRST_BREAK_LAST_PERIOD),
                LessonPeriods.end(LessonPeriods.FIRST_BREAK_LAST_PERIOD)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .sorted()
            .filter(time -> time.isAfter(now))
            .findFirst();

        if (next.isEmpty())
        {
            countdown.setVisibility(GONE);
            return;
        }
        long seconds = Math.max(0, ChronoUnit.SECONDS.between(now, next.get()));
        countdown.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60));
        countdown.setVisibility(VISIBLE);
    }

    private void changeSelectedDateBy(int direction)
    {
        LocalDate current = viewModel.getSelectedDate().getValue();
        if (current == null)
        {
            return;
        }
        for (int days = 1; days < SEARCH_DAYS; days++)
        {
            LocalDate candidate = current.plusDays((long) direction * days);
            if (schoolDays.contains(candidate))
            {
                viewModel.select(candidate);
                return;
            }
        }
    }

    private class ScheduleGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY)
        {
            if (e1 == null)
            {
                return false;
            }
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            float density = getResources().getDisplayMetrics().density;

            if (Math.abs(diffX) <= Math.abs(diffY)
                || Math.abs(diffX) <= SWIPE_THRESHOLD_DP * density
                || Math.abs(velocityX) <= SWIPE_VELOCITY_THRESHOLD_DP * density)
            {
                return false;
            }
            changeSelectedDateBy(diffX > 0 ? -1 : 1);
            return true;
        }
    }
}
