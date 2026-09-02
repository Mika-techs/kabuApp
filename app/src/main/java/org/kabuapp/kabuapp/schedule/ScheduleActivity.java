package org.kabuapp.kabuapp.schedule;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.kabuapp.kabuapp.KabuApp;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.data.memory.MemLesson;
import org.kabuapp.kabuapp.databinding.ActivityScheduleBinding;
import org.kabuapp.kabuapp.exam.ExamActivity;
import org.kabuapp.kabuapp.integration.Activity;
import org.kabuapp.kabuapp.interfaces.Callback;
import org.kabuapp.kabuapp.login.LoginActivity;
import org.kabuapp.kabuapp.settings.SettingsActivity;
import org.kabuapp.kabuapp.utils.DateTimeUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


public class ScheduleActivity extends Activity implements Callback, DateAdapter.OnDateSelectedListener, SwipeRefreshLayout.OnRefreshListener
{
    private static final int SWIPE_VELOCITY_THRESHOLD_DP = 69;
    private static final int SWIPE_THRESHOLD_DP = 69;
    private final DateTimeFormatter weekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault());
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault());
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");
    private final Handler timerHandler = new Handler();
    private ScheduleUiGenerator scheduleUiGenerator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayoutManager layoutManager;
    private GestureDetector gestureDetector;
    private TextView durationTextView;
    private List<DateItem> dateItems;
    private DateAdapter dateAdapter;
    private Runnable timerRunnable;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ((KabuApp) getApplication()).getScheduleUpdateTask().setRef(this);

        getScheduleController().updateSchedule(
            getAuthController().getToken(), getAuthController(), this, new Object[1], Duration.ofHours(2),
            getAuthController().getId(), !getScheduleController().getSchedule().getLessons().isEmpty(), s -> getAuthController().setToken(s));

        scheduleUiGenerator = new ScheduleUiGenerator();

        ActivityScheduleBinding binding = ActivityScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_schedule);
        ScrollView scheduleScrollView = findViewById(R.id.schedule_scroll_view);

        gestureDetector = new GestureDetector(this, new ScheduleGestureListener());

        if (scheduleScrollView != null)
        {
            scheduleScrollView.setOnTouchListener((v, event) ->
            {
                if (gestureDetector.onTouchEvent(event))
                {
                    return true;
                }
                return v.onTouchEvent(event);
            });
        }

        barButtonRefListener(binding.barSettings, SettingsActivity.class);
        barButtonRefListener(binding.barExam, ExamActivity.class);

        getExamController().updateExams(getAuthController().getToken(), getAuthController(), null, null, Duration.ofHours(1), getAuthController().getId());

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_schedule);
        swipeRefreshLayout.setOnRefreshListener(this);

        RecyclerView dateRecyclerView = findViewById(R.id.recycler_view_date_selector);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        dateRecyclerView.setLayoutManager(layoutManager);

        dateItems = generateDateItems(
            DateTimeUtils.getLocalDate().minusDays(DateTimeUtils.getLocalDate().getDayOfWeek().getValue() - 1), 14);

        dateAdapter = new DateAdapter(this, dateItems, this);
        dateRecyclerView.setAdapter(dateAdapter);
        if (getScheduleController().getSchedule().getSelectedDate().isEqual(DateTimeUtils.getLocalDate()))
        {
            getScheduleController().getSchedule().setSelectedDate(getDate(dateItems));
        }

        dateAdapter.setSelectedDate(getScheduleController().getSchedule().getSelectedDate());

        dateRecyclerView.post(() ->
        {
            int selectedPosition = dateAdapter.getSelectedItemPosition();
            if (selectedPosition != RecyclerView.NO_POSITION)
            {
                layoutManager.scrollToPositionWithOffset(selectedPosition, 0);
            }
        });
        durationTextView = findViewById(R.id.text_view_next_lesson_timer);

        checkIfNoSchool();
        updateScheduleLoop();
        setupDurationNextLesson();
    }

    @Override
    public void onRefresh()
    {
        getScheduleController().updateSchedule(getAuthController().getToken(), getAuthController(), this,
            new Object[1], Duration.ofSeconds(1), getAuthController().getId(), true, s -> getAuthController().setToken(s));
        swipeRefreshLayout.setRefreshing(false);
    }

    public void callback(Object[] objects)
    {
        updateSchedule();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if (!getAuthController().isInitialized())
        {
            var i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }

        getDelegate().onStart();
    }

    private void updateSchedule()
    {
        ViewGroup linearSchedule = findViewById(R.id.linear_schedule);
        LocalDate selectedDate = getScheduleController().getSchedule().getSelectedDate();
        Map<LocalDate, List<MemLesson>> lessonRef = getScheduleController().getSchedule().getLessons();
        if (lessonRef == null || linearSchedule == null || selectedDate == null)
        {
            return;
        }
        Map<LocalDate, List<MemLesson>> lessons = lessonRef.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .sorted(Comparator.comparing(MemLesson::getBegin)
                        .thenComparing(MemLesson::getGroup))
                    .collect(Collectors.toList()),
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        List<DateItem> dateItems = new ArrayList<>();
        lessons.keySet().forEach(date ->
        {
            if (dateAdapter.getDateList().stream().noneMatch(dateItem -> dateItem.getDate().isEqual(date)))
            {
                dateItems.add(generateDateItems(date, 1).get(0));
            }
        });
        if (lessons.containsKey(selectedDate))
        {
            splitFirstBreakLessons(lessons.get(selectedDate));
            if (DateTimeUtils.getLocalDate().equals(selectedDate))
            {
                List<MemLesson> currentLessons = lessons.get(DateTimeUtils.getLocalDate());
                if (currentLessons != null && currentLessons.stream().noneMatch(this::isInLesson))
                {
                    addNullLessonAtCurrentTime(currentLessons);
                }
            }
        }
        runOnUiThread(() ->
        {
            linearSchedule.removeAllViews();
            if (lessons.containsKey(selectedDate))
            {
                lessons.get(selectedDate).forEach(lesson ->
                    scheduleUiGenerator.addLessonElement(this, linearSchedule, lesson));
            }
            dateItems.forEach(dateAdapter::addDate);
        });
    }

    private void addNullLessonAtCurrentTime(List<MemLesson> lessons)
    {
        int i = -1;
        for (int k = 0; k < lessons.size(); k++)
        {
            if (scheduleUiGenerator.endToLocaleTime(lessons.get(k).getEnd()).isBefore(DateTimeUtils.getLocalTime()))
            {
                i = k;
            }
            else
            {
                break;
            }
        }
        if (i != -1 && i != lessons.size() - 1)
        {
            lessons.add(i + 1, new MemLesson((short) -1, (short) -1, null, (short) -1, (short) -1, null, null, null, null));
        }
    }

    private void splitFirstBreakLessons(List<MemLesson> lessons)
    {
        Function<MemLesson, Boolean> isOverBreak = l -> l.getBegin() <= 2 && l.getEnd() >= 3;
        List<MemLesson> overBreakLessons = lessons.stream().filter(isOverBreak::apply).collect(Collectors.toList());
        if (overBreakLessons.isEmpty())
        {
            return;
        }
        int startIndex = lessons.indexOf(overBreakLessons.get(0));
        int size = overBreakLessons.size();
        for (int i = 0; i < size; i++)
        {
            MemLesson l = overBreakLessons.get(i);
            MemLesson fl = new MemLesson(
                l.getBegin(), (short) 2, l.getDate(), l.getGroup(), l.getMaxGroup(), l.getName(), l.getTeacher(), l.getRoom(), UUID.randomUUID());
            MemLesson sl = new MemLesson(
                (short) 3, l.getEnd(), l.getDate(), l.getGroup(), l.getMaxGroup(), l.getName(), l.getTeacher(), l.getRoom(), UUID.randomUUID());
            lessons.add(startIndex + size + i - 1 + l.getGroup(), sl);
            lessons.add(startIndex + size + i - 1 + l.getGroup(), fl);
            lessons.remove(l);
        }

    }

    private boolean isInLesson(MemLesson lesson)
    {
        return scheduleUiGenerator.beginToLocaleTime(lesson.getBegin()) == null
            || (!DateTimeUtils.getLocalTime().isBefore(scheduleUiGenerator.beginToLocaleTime(lesson.getBegin()))
            && !DateTimeUtils.getLocalTime().isAfter(scheduleUiGenerator.endToLocaleTime(lesson.getEnd())));
    }

    private void updateScheduleLoop()
    {
        getExecutorService().execute(() ->
        {
            while (!this.isDestroyed())
            {
                updateSchedule();
                try
                {
                    Thread.sleep(8000);
                }
                catch (InterruptedException ignored)
                {
                    return;
                }
            }
        });
    }

    private List<DateItem> generateDateItems(LocalDate startDate, int numberOfDays)
    {
        List<DateItem> items = new ArrayList<>();

        for (int i = 0; i < numberOfDays; i++)
        {
            LocalDate currentDate = startDate.plusDays(i);
            if (!getScheduleController().isSchool(currentDate))
            {
                continue;
            }
            items.add(new DateItem(
                currentDate, currentDate.format(monthFormatter), currentDate.format(dayFormatter), currentDate.format(weekdayFormatter), false));
        }
        if (items.isEmpty())
        {
            LocalDate currentDate = DateTimeUtils.getLocalDate();
            items.add(new DateItem(
                currentDate, currentDate.format(monthFormatter), currentDate.format(dayFormatter), currentDate.format(weekdayFormatter), false));
        }
        return items;
    }

    @Override
    public void onDateSelected(LocalDate date)
    {
        getScheduleController().getSchedule().setSelectedDate(date);
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 0);
        updateSchedule();
    }

    private void changeSelectedDate(LocalDate newDate)
    {
        int newPosition = IntStream.range(0, dateItems.size()).filter(i ->
            dateItems.get(i).getDate().equals(newDate)).findFirst().orElse(-1);

        if (newPosition != -1)
        {
            getScheduleController().getSchedule().setSelectedDate(newDate);
            dateAdapter.setSelectedDate(newDate);
            updateSchedule();

        }
    }

    private LocalDate getDate(List<DateItem> dateItems)
    {
        LocalDate date = DateTimeUtils.getLocalDate();
        if (date.getDayOfWeek().equals(DayOfWeek.SUNDAY) && !getScheduleController().isSchool(date))
        {
            date = date.plusDays(1);
        }
        LocalDate finalDate = date;
        if (dateItems.stream().noneMatch(item -> item.getDate().equals(finalDate)) && !dateItems.isEmpty())
        {
            for (LocalDate dateItem : dateItems.stream().map(DateItem::getDate).collect(Collectors.toList()))
            {
                if (dateItem.isAfter(date))
                {
                    return dateItem;
                }
            }
            return dateItems.get(dateItems.size() - 1).getDate();
        }
        return date;
    }

    private class ScheduleGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY)
        {
            boolean result = false;
            try
            {
                if (e1 == null)
                {
                    return false;
                }

                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                float density = getResources().getDisplayMetrics().density;
                int swipeThresholdPx = (int) (SWIPE_THRESHOLD_DP * density);
                int swipeVelocityThresholdPx = (int) (SWIPE_VELOCITY_THRESHOLD_DP * density);

                if (Math.abs(diffX) > Math.abs(diffY)
                    && Math.abs(diffX) > swipeThresholdPx
                    && Math.abs(velocityX) > swipeVelocityThresholdPx)
                {
                    if (diffX > 0)
                    {
                        onSwipeRight();
                    }
                    else
                    {
                        onSwipeLeft();
                    }
                    result = true;
                }
            }
            catch (Exception exception)
            {
                Logger.getLogger("GestureListener").log(Level.SEVERE, "Error in onFling", exception);
            }
            return result;
        }
    }

    private void onSwipeRight()
    {
        int days = 1;
        for (; days < 15; days++)
        {
            if (getScheduleController().isSchool(getScheduleController().getSchedule().getSelectedDate().minusDays(days)))
            {
                break;
            }
        }
        changeSelectedDate(getScheduleController().getSchedule().getSelectedDate().minusDays(days));
    }

    private void onSwipeLeft()
    {
        int days = 1;
        for (; days < 15; days++)
        {
            if (getScheduleController().isSchool(getScheduleController().getSchedule().getSelectedDate().plusDays(days)))
            {
                break;
            }
        }
        changeSelectedDate(getScheduleController().getSchedule().getSelectedDate().plusDays(days));
    }

    private void checkIfNoSchool()
    {
        boolean isSchool = getScheduleController().getSchedule().getLessons().values().stream().mapToLong(Collection::size).sum() > 0;
        if (!isSchool)
        {
            findViewById(R.id.noSchoolHint).setVisibility(VISIBLE);
        }
        else
        {
            findViewById(R.id.noSchoolHint).setVisibility(GONE);
        }
    }

    private void setupDurationNextLesson()
    {
        boolean isSchool = getScheduleController().getSchedule().getLessons().values().stream().mapToLong(Collection::size).sum() > 0;

        if (!isSchool)
        {
            durationTextView.setVisibility(GONE);
            return;
        }
        Supplier<Boolean> isCurrentDaySelected = () -> dateItems.stream().anyMatch(di -> di.isSelected() && di.getDate().equals(DateTimeUtils.getLocalDate()));

        AtomicReference<LocalTime> localTime = new AtomicReference<>();
        Supplier<Stream<LocalTime>> getLessons = () -> getScheduleController().getSchedule().getLessons()
            .getOrDefault(DateTimeUtils.getLocalDate(), List.of())
            .stream()
            .flatMap(l -> Stream.of(scheduleUiGenerator.beginToLocaleTime(l.getBegin()), scheduleUiGenerator.endToLocaleTime(l.getEnd()),
                scheduleUiGenerator.beginToLocaleTime((short) 2), scheduleUiGenerator.endToLocaleTime((short) 2)))
            .distinct()
            .sorted();
        Function<Stream<LocalTime>, Optional<LocalTime>> getNextLesson = lessons -> lessons
            .filter(event -> event.isAfter(localTime.get()))
            .findFirst();
        Function<Optional<LocalTime>, Optional<String>> formatTime = ot -> ot.map(targetTime ->
        {
            long seconds = ChronoUnit.SECONDS.between(localTime.get(), targetTime);
            if (seconds <= 0)
            {
                return "00:00:00";
            }
            return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        });
        Supplier<Optional<String>> getTime = () -> formatTime.apply(getNextLesson.apply(getLessons.get()));

        timerRunnable = () ->
        {
            if (isCurrentDaySelected.get())
            {
                localTime.set(DateTimeUtils.getLocalTime());
                getTime.get().ifPresentOrElse(s ->
                {
                    durationTextView.setText(s);
                    durationTextView.setVisibility(VISIBLE);
                }, () -> durationTextView.setVisibility(GONE));
            }
            else
            {
                durationTextView.setVisibility(GONE);
            }
            timerHandler.postDelayed(timerRunnable, 999);
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (timerHandler != null)
        {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}