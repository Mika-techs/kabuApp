package org.kabuapp.kabuapp.feature.exam;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import org.kabuapp.kabuapp.R;
import org.kabuapp.kabuapp.core.util.DateTimeUtils;

import java.time.format.DateTimeFormatter;

import static org.kabuapp.kabuapp.core.ui.ThemeColorResolver.resolveColorAttribute;

/** Draws the exam list. */
public class ExamAdapter extends ListAdapter<ExamRow, RecyclerView.ViewHolder>
{
    private static final int TYPE_EXAM = 0;
    private static final int TYPE_TODAY_DIVIDER = 1;

    private static final DiffUtil.ItemCallback<ExamRow> DIFF = new DiffUtil.ItemCallback<>()
    {
        @Override
        public boolean areItemsTheSame(@NonNull ExamRow oldItem, @NonNull ExamRow newItem)
        {
            if (oldItem instanceof ExamRow.ExamEntryRow a && newItem instanceof ExamRow.ExamEntryRow b)
            {
                return a.begin().equals(b.begin()) && a.info().equals(b.info());
            }
            return oldItem instanceof ExamRow.TodayDividerRow && newItem instanceof ExamRow.TodayDividerRow;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ExamRow oldItem, @NonNull ExamRow newItem)
        {
            return oldItem.equals(newItem);
        }
    };

    private final DateTimeFormatter dateFormatter;

    public ExamAdapter(DateTimeFormatter dateFormatter)
    {
        super(DIFF);
        this.dateFormatter = dateFormatter;
    }

    @Override
    public int getItemViewType(int position)
    {
        return getItem(position) instanceof ExamRow.TodayDividerRow ? TYPE_TODAY_DIVIDER : TYPE_EXAM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_TODAY_DIVIDER)
        {
            return new DividerViewHolder(inflater.inflate(R.layout.layout_current_item, parent, false));
        }
        return new ExamViewHolder(inflater.inflate(R.layout.layout_exam_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if (holder instanceof ExamViewHolder examHolder)
        {
            examHolder.bind((ExamRow.ExamEntryRow) getItem(position), dateFormatter);
        }
    }

    private static class DividerViewHolder extends RecyclerView.ViewHolder
    {
        DividerViewHolder(@NonNull View itemView)
        {
            super(itemView);
        }
    }

    private static class ExamViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView begin;
        private final TextView end;
        private final TextView info;

        ExamViewHolder(@NonNull View itemView)
        {
            super(itemView);
            begin = itemView.findViewById(R.id.text_view_exam_begin);
            end = itemView.findViewById(R.id.text_view_exam_end);
            info = itemView.findViewById(R.id.text_view_exam_info);
        }

        void bind(ExamRow.ExamEntryRow row, DateTimeFormatter formatter)
        {
            begin.setText(formatter.format(row.begin()));
            info.setText(row.info());
            if (row.singleDay())
            {
                end.setVisibility(View.GONE);
            }
            else
            {
                end.setVisibility(View.VISIBLE);
                end.setText(formatter.format(row.end()));
            }

            ((CardView) itemView).setCardBackgroundColor(row.isCurrent(DateTimeUtils.getLocalDate())
                ? resolveColorAttribute(itemView.getContext(), android.R.attr.textColorHighlight)
                : resolveColorAttribute(itemView.getContext(), android.R.attr.colorBackground));
        }
    }
}
