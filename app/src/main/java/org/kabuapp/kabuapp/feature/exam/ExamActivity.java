package org.kabuapp.kabuapp.feature.exam;

import android.os.Bundle;
import android.view.ViewGroup;
import androidx.activity.EdgeToEdge;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.feature.exam.MemExam;
import org.kabuapp.kabuapp.databinding.ActivityExamBinding;
import org.kabuapp.kabuapp.core.net.Callback;
import org.kabuapp.kabuapp.core.ui.Activity;
import org.kabuapp.kabuapp.feature.schedule.ScheduleActivity;
import org.kabuapp.kabuapp.feature.settings.SettingsActivity;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ExamActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener, Callback
{
    private static final Duration EXAM_MAX_AGE = Duration.ofMinutes(5);

    private SwipeRefreshLayout swipeRefreshLayout;
    private ExamUiGenerator uiGenerator;
    private ActivityExamBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        uiGenerator = new ExamUiGenerator(getSettingsController().isIsoDate()
            ? DateTimeFormatter.ISO_LOCAL_DATE : DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));

        binding = ActivityExamBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_exam);
        swipeRefreshLayout.setOnRefreshListener(this);

        barButtonRefListener(binding.barSettings, SettingsActivity.class);
        barButtonRefListener(binding.barSchedule, ScheduleActivity.class);
        updateExams();
    }

    private void updateExams()
    {
        ViewGroup linearExams = findViewById(R.id.linear_exams);
        Map<LocalDate, MemExam> examsRef = getExamController().getExams().getExams();
        if (examsRef != null && !examsRef.isEmpty())
        {
            Map<LocalDate, MemExam> exams = new HashMap<>(examsRef);
            if (exams.keySet().stream().anyMatch(date -> date.isBefore(DateTimeUtils.getLocalDate()))
                && exams.keySet().stream().anyMatch(date -> date.isAfter(DateTimeUtils.getLocalDate()))
                && exams.entrySet().stream().noneMatch(entry -> uiGenerator.isCurrent(entry.getKey(), entry.getValue().getDuration())))
            {
                exams.computeIfAbsent(DateTimeUtils.getLocalDate(), k -> new MemExam(null, DateTimeUtils.getLocalDate(), (short) -1, null));
            }
            runOnUiThread(() ->
            {
                linearExams.removeAllViews();
                exams.values().stream().sorted(Comparator.comparing(MemExam::getBeginn))
                    .forEach(exam ->
                        uiGenerator.addExamElement(
                            this,
                            linearExams,
                            exam));
            });
        }
        else
        {
            runOnUiThread(linearExams::removeAllViews);
        }
    }

    @Override
    public void onRefresh()
    {
        swipeRefreshLayout.setRefreshing(false);
        getExamController().updateExams(this, new Object[1], EXAM_MAX_AGE, getAuthController().getId());
    }

    public void callback(Object[] objects)
    {
        updateExams();
    }
}