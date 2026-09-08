package org.kabuapp.kabuapp.core.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import lombok.AllArgsConstructor;
import org.kabuapp.kabuapp.core.data.AppContainer;
import org.kabuapp.kabuapp.feature.auth.AuthViewModel;
import org.kabuapp.kabuapp.feature.exam.ExamViewModel;
import org.kabuapp.kabuapp.feature.schedule.ScheduleViewModel;

/**
 * Builds view models from the {@link AppContainer}, since a ViewModel cannot be constructed by
 * the Application the way the old controllers were.
 */
@AllArgsConstructor
public class ViewModelFactory implements ViewModelProvider.Factory
{
    private final AppContainer container;

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass)
    {
        if (modelClass.isAssignableFrom(ScheduleViewModel.class))
        {
            return (T) new ScheduleViewModel(container.getScheduleRepository(), container.getActiveUserStore());
        }
        if (modelClass.isAssignableFrom(ExamViewModel.class))
        {
            return (T) new ExamViewModel(container.getExamRepository(), container.getActiveUserStore());
        }
        if (modelClass.isAssignableFrom(AuthViewModel.class))
        {
            return (T) new AuthViewModel(
                container.getAuthController(), container.getSessionController(), container.getIoExecutor());
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
