package org.kabuapp.kabuapp.feature.schedule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.core.text.HtmlCompat;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.feature.schedule.MemLesson;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;

import java.time.LocalTime;
import java.util.Optional;

import static org.kabuapp.kabuapp.core.ui.ThemeColorResolver.resolveColorAttribute;

public class ScheduleUiGenerator
{
    @SuppressLint("SetTextI18n")
    public void addLessonElement(
        Context context,
        ViewGroup parentLayout,
        MemLesson lesson)
    {

        LayoutInflater inflater = LayoutInflater.from(context);
        if (lesson.getBegin() == -1)
        {
            View dividerView = inflater.inflate(R.layout.layout_current_item, parentLayout, false);
            parentLayout.addView(dividerView);
        }
        else
        {
            View lessonView = inflater.inflate(R.layout.layout_lesson_item, parentLayout, false);

            TextView timeTextView = lessonView.findViewById(R.id.text_view_lesson_time);
            TextView nameTextView = lessonView.findViewById(R.id.text_view_lesson_name);
            TextView groupTextView = lessonView.findViewById(R.id.text_view_lesson_group);
            TextView roomTextView = lessonView.findViewById(R.id.text_view_lesson_room);
            TextView teacherTextView = lessonView.findViewById(R.id.text_view_lesson_teacher);

            if (lesson.getTeacher() != null && lesson.getTeacher().isEmpty())
            {
                nameTextView.setText(HtmlCompat.fromHtml("<s>" + lesson.getName() + "</s>", HtmlCompat.FROM_HTML_MODE_LEGACY));
                timeTextView.setText(HtmlCompat.fromHtml("<s>" + mapBeginnToString(lesson.getBegin()) + " - "
                    + mapEndToString(lesson.getEnd()) + "</s>", HtmlCompat.FROM_HTML_MODE_LEGACY));
                teacherTextView.getParent().clearChildFocus(teacherTextView);
                roomTextView.setVisibility(View.GONE);
                teacherTextView.setVisibility(View.GONE);
            }
            else
            {
                nameTextView.setText(lesson.getName());
                timeTextView.setText(mapBeginnToString(lesson.getBegin()) + " - " + mapEndToString(lesson.getEnd()));
                teacherTextView.setText(context.getString(R.string.lesson_teacher_prefix) + ": " + lesson.getTeacher());
                roomTextView.setText(context.getString(R.string.lesson_room_prefix) + ": " + lesson.getRoom());
            }
            if (lesson.getGroup() == 1 && lesson.getMaxGroup() == 1)
            {
                groupTextView.setText("");
            }
            else
            {
                groupTextView.setText(lesson.getGroup() + "/" + lesson.getMaxGroup());
            }

            if (lesson.getDate().isEqual(DateTimeUtils.getLocalDate()) && isCurrent(beginToLocaleTime(lesson.getBegin()), endToLocaleTime(lesson.getEnd())))
            {
                ((CardView) lessonView).setCardBackgroundColor(resolveColorAttribute(context, android.R.attr.textColorHighlight));
            }
            parentLayout.addView(lessonView);
        }
    }

    private boolean isCurrent(LocalTime begin, LocalTime end)
    {
        return !end.isBefore(DateTimeUtils.getLocalTime()) && !begin.isAfter(DateTimeUtils.getLocalTime());
    }

    public Optional<LocalTime> beginToLocaleTime(Optional<Short> time)
    {
        return time.map(this::beginToLocaleTime);
    }

    public Optional<LocalTime> endToLocaleTime(Optional<Short> time)
    {
        return time.map(this::endToLocaleTime);
    }

    public LocalTime beginToLocaleTime(short time)
    {
        return switch (time)
        {
            case 1 -> LocalTime.of(8, 30);
            case 2 -> LocalTime.of(9, 15);
            case 3 -> LocalTime.of(10, 15);
            case 4 -> LocalTime.of(11, 0);
            case 5 -> LocalTime.of(11, 45);
            case 6 -> LocalTime.of(12, 30);
            case 7 -> LocalTime.of(13, 15);
            case 8 -> LocalTime.of(14, 0);
            case 9 -> LocalTime.of(14, 45);
            case 10 -> LocalTime.of(15, 30);
            case 11 -> LocalTime.of(16, 15);
            case 12 -> LocalTime.of(17, 0);
            case 13 -> LocalTime.of(17, 45);
            case 14 -> LocalTime.of(18, 30);
            default -> null;
        };
    }

    public LocalTime endToLocaleTime(short time)
    {
        return time == 2 ? LocalTime.of(10, 0) : beginToLocaleTime((short) (time + 1));
    }

    private String mapBeginnToString(short lesson)
    {
        return switch (lesson)
        {
            case 1 -> "8:30";
            case 2 -> "9:15";
            case 3 -> "10:15";
            case 4 -> "11:00";
            case 5 -> "11:45";
            case 6 -> "12:30";
            case 7 -> "13:15";
            case 8 -> "14:00";
            case 9 -> "14:45";
            case 10 -> "15:30";
            case 11 -> "16:15";
            case 12 -> "17:00";
            case 13 -> "17:45";
            case 14 -> "18:30";
            default -> "ERROR";
        };
    }

    private String mapEndToString(short lesson)
    {
        return lesson == 2 ? "10:00" : mapBeginnToString((short) (lesson + 1));
    }
}
