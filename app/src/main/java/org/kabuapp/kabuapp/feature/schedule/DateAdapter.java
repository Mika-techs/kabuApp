package org.kabuapp.kabuapp.feature.schedule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import lombok.Getter;
import org.kabuapp.kabuapp.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.kabuapp.kabuapp.core.ui.ThemeColorResolver.resolveColorAttribute;

/**
 * The horizontal date strip. Selection is held only as a position - the item records used to
 * carry an {@code isSelected} flag as well, which could disagree with it.
 */
public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder>
{
    private static final float SELECTED_ELEVATION = 10f;

    private final OnDateSelectedListener onDateSelectedListener;
    private final DateTimeFormatter weekdayFormatter;
    private final DateTimeFormatter monthFormatter;
    private final Context context;

    @Getter
    private int selectedItemPosition = RecyclerView.NO_POSITION;
    private List<DateItem> dateList;

    public interface OnDateSelectedListener
    {
        void onDateSelected(LocalDate date);
    }

    public DateAdapter(Context context, List<DateItem> dateList, OnDateSelectedListener listener)
    {
        this.context = context;
        this.dateList = new ArrayList<>(dateList);
        this.onDateSelectedListener = listener;
        this.monthFormatter = DateTimeFormatter.ofPattern("MMM", context.getResources().getConfiguration().getLocales().get(0));
        this.weekdayFormatter = DateTimeFormatter.ofPattern("EE", context.getResources().getConfiguration().getLocales().get(0));
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new DateViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_date_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position)
    {
        DateItem dateItem = dateList.get(position);

        holder.monthTextView.setText(dateItem.date().format(monthFormatter));
        holder.dayTextView.setText(dateItem.day());
        holder.weekdayTextView.setText(dateItem.date().format(weekdayFormatter));

        if (position == selectedItemPosition)
        {
            holder.itemView.setBackgroundColor(floatingBackgroundColor());
            holder.itemView.setElevation(SELECTED_ELEVATION);
            holder.setTextColor(resolveColorAttribute(context, android.R.attr.textColorPrimary));
        }
        else
        {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
            holder.itemView.setElevation(0f);
            holder.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        }

        holder.itemView.setOnClickListener(v -> select(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount()
    {
        return dateList.size();
    }

    /** Replaces the strip's contents; the dates come from the observed lesson rows. */
    @SuppressLint("NotifyDataSetChanged")
    public void setDates(List<DateItem> dates)
    {
        this.dateList = new ArrayList<>(dates);
        this.selectedItemPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    public void setSelectedDate(LocalDate date)
    {
        for (int i = 0; i < dateList.size(); i++)
        {
            if (dateList.get(i).date().equals(date))
            {
                moveSelectionTo(i);
                return;
            }
        }
    }

    private void select(int position)
    {
        if (position == RecyclerView.NO_POSITION)
        {
            return;
        }
        LocalDate date = dateList.get(position).date();
        moveSelectionTo(position);
        if (onDateSelectedListener != null)
        {
            onDateSelectedListener.onDateSelected(date);
        }
    }

    private void moveSelectionTo(int position)
    {
        int previous = selectedItemPosition;
        selectedItemPosition = position;
        if (previous != RecyclerView.NO_POSITION)
        {
            notifyItemChanged(previous);
        }
        notifyItemChanged(position);
    }

    @SuppressLint("ResourceType")
    private int floatingBackgroundColor()
    {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.colorBackgroundFloating, typedValue, true))
        {
            return typedValue.data;
        }
        return resolveColorAttribute(context, android.R.attr.colorBackground);
    }

    static class DateViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView monthTextView;
        private final TextView dayTextView;
        private final TextView weekdayTextView;

        DateViewHolder(@NonNull View itemView)
        {
            super(itemView);
            monthTextView = itemView.findViewById(R.id.text_view_month);
            dayTextView = itemView.findViewById(R.id.text_view_day);
            weekdayTextView = itemView.findViewById(R.id.text_view_weekday);
        }

        void setTextColor(int color)
        {
            monthTextView.setTextColor(color);
            dayTextView.setTextColor(color);
            weekdayTextView.setTextColor(color);
        }
    }
}
