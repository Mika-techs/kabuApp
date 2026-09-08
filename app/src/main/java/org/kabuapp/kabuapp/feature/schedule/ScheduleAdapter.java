package org.kabuapp.kabuapp.feature.schedule;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;
import org.kabuapp.kabuapp.domain.LessonPeriods;

import static org.kabuapp.kabuapp.core.ui.ThemeColorResolver.resolveColorAttribute;

/**
 * Draws the schedule. Rows are immutable records, so DiffUtil can compare them by value and only
 * the cards that actually changed are rebound - the previous generator removed and re-inflated
 * every view on each update.
 */
public class ScheduleAdapter extends ListAdapter<ScheduleRow, RecyclerView.ViewHolder>
{
    private static final int TYPE_LESSON = 0;
    private static final int TYPE_NOW_DIVIDER = 1;

    private static final DiffUtil.ItemCallback<ScheduleRow> DIFF = new DiffUtil.ItemCallback<>()
    {
        @Override
        public boolean areItemsTheSame(@NonNull ScheduleRow oldItem, @NonNull ScheduleRow newItem)
        {
            if (oldItem instanceof ScheduleRow.LessonRow a && newItem instanceof ScheduleRow.LessonRow b)
            {
                return a.date().equals(b.date()) && a.begin() == b.begin() && a.group() == b.group();
            }
            return oldItem instanceof ScheduleRow.NowDividerRow && newItem instanceof ScheduleRow.NowDividerRow;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ScheduleRow oldItem, @NonNull ScheduleRow newItem)
        {
            return oldItem.equals(newItem);
        }
    };

    public ScheduleAdapter()
    {
        super(DIFF);
    }

    @Override
    public int getItemViewType(int position)
    {
        return getItem(position) instanceof ScheduleRow.NowDividerRow ? TYPE_NOW_DIVIDER : TYPE_LESSON;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_NOW_DIVIDER)
        {
            return new DividerViewHolder(inflater.inflate(R.layout.layout_current_item, parent, false));
        }
        return new LessonViewHolder(inflater.inflate(R.layout.layout_lesson_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if (holder instanceof LessonViewHolder lessonHolder)
        {
            lessonHolder.bind((ScheduleRow.LessonRow) getItem(position));
        }
    }

    private static class DividerViewHolder extends RecyclerView.ViewHolder
    {
        DividerViewHolder(@NonNull View itemView)
        {
            super(itemView);
        }
    }

    private static class LessonViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView time;
        private final TextView name;
        private final TextView group;
        private final TextView room;
        private final TextView teacher;

        LessonViewHolder(@NonNull View itemView)
        {
            super(itemView);
            time = itemView.findViewById(R.id.text_view_lesson_time);
            name = itemView.findViewById(R.id.text_view_lesson_name);
            group = itemView.findViewById(R.id.text_view_lesson_group);
            room = itemView.findViewById(R.id.text_view_lesson_room);
            teacher = itemView.findViewById(R.id.text_view_lesson_teacher);
        }

        @SuppressLint("SetTextI18n")
        void bind(ScheduleRow.LessonRow row)
        {
            String label = LessonPeriods.formatBegin(row.begin()) + " - " + LessonPeriods.formatEnd(row.end());
            if (row.cancelled())
            {
                name.setText(HtmlCompat.fromHtml("<s>" + row.name() + "</s>", HtmlCompat.FROM_HTML_MODE_LEGACY));
                time.setText(HtmlCompat.fromHtml("<s>" + label + "</s>", HtmlCompat.FROM_HTML_MODE_LEGACY));
                room.setVisibility(View.GONE);
                teacher.setVisibility(View.GONE);
            }
            else
            {
                name.setText(row.name());
                time.setText(label);
                room.setVisibility(View.VISIBLE);
                teacher.setVisibility(View.VISIBLE);
                teacher.setText(itemView.getContext().getString(R.string.lesson_teacher_prefix) + ": " + row.teacher());
                room.setText(itemView.getContext().getString(R.string.lesson_room_prefix) + ": " + row.room());
            }

            group.setText(row.singleGroup() ? "" : row.group() + "/" + row.maxGroup());

            boolean current = row.date().isEqual(DateTimeUtils.getLocalDate())
                && LessonPeriods.isCurrent(row.begin(), row.end(), DateTimeUtils.getLocalTime());
            ((CardView) itemView).setCardBackgroundColor(current
                ? resolveColorAttribute(itemView.getContext(), android.R.attr.textColorHighlight)
                : resolveColorAttribute(itemView.getContext(), android.R.attr.colorBackground));
        }
    }
}
