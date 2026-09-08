package org.kabuapp.kabuapp.feature.exam;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.core.ui.Activity;
import org.kabuapp.kabuapp.core.ui.ViewModelFactory;
import org.kabuapp.kabuapp.databinding.ActivityExamBinding;
import org.kabuapp.kabuapp.domain.RefreshState;
import org.kabuapp.kabuapp.feature.schedule.ScheduleActivity;
import org.kabuapp.kabuapp.feature.settings.SettingsActivity;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** The exam screen. Rows come from {@link ExamViewModel} through LiveData. */
public class ExamActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener
{
    private static final Duration EXAM_MAX_AGE = Duration.ofMinutes(5);

    private ActivityExamBinding binding;
    private ExamViewModel viewModel;
    private ExamAdapter examAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = ActivityExamBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new ViewModelFactory(getContainer())).get(ExamViewModel.class);

        // ISO dates are deliberately limited to this screen; they do not fit the schedule's date strip.
        examAdapter = new ExamAdapter(getSettingsController().isIsoDate()
            ? DateTimeFormatter.ISO_LOCAL_DATE
            : DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));

        binding.recyclerViewExams.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewExams.setAdapter(examAdapter);

        barButtonRefListener(binding.barSettings, SettingsActivity.class);
        barButtonRefListener(binding.barSchedule, ScheduleActivity.class);

        binding.swipeRefreshExam.setOnRefreshListener(this);

        viewModel.getRows().observe(this, examAdapter::submitList);
        viewModel.getRefreshState().observe(this, this::onRefreshState);
        viewModel.refresh(EXAM_MAX_AGE);
    }

    private void onRefreshState(RefreshState state)
    {
        binding.swipeRefreshExam.setRefreshing(state.status() == RefreshState.Status.LOADING);
        if (state.status() != RefreshState.Status.ERROR)
        {
            return;
        }
        int message = state.errorKind() == ApiException.Kind.NETWORK ? R.string.error_network : R.string.error_server;
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onRefresh()
    {
        viewModel.refresh(Duration.ofSeconds(1));
    }
}
