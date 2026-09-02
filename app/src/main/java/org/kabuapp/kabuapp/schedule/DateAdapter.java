package org.kabuapp.kabuapp.schedule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import lombok.Getter;
import lombok.Setter;
import org.kabuapp.kabuapp.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.kabuapp.kabuapp.ui.ThemeColorResolver.resolveColorAttribute;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder>
{
    @Getter
    private int selectedItemPosition = RecyclerView.NO_POSITION;
    private final OnDateSelectedListener onDateSelectedListener;
    private final DateTimeFormatter weekdayFormatter;
    private final DateTimeFormatter monthFormatter;
    @Getter
    private List<DateItem> dateList;
    private final Context context;

    public interface OnDateSelectedListener
    {
        void onDateSelected(LocalDate date);
    }

    public DateAdapter(Context context, List<DateItem> dateList, OnDateSelectedListener listener)
    {
        this.context = context;
        this.dateList = dateList;
        this.onDateSelectedListener = listener;
        this.monthFormatter = DateTimeFormatter.ofPattern("MMM", context.getResources().getConfiguration().getLocales().get(0));
        this.weekdayFormatter = DateTimeFormatter.ofPattern("EE", context.getResources().getConfiguration().getLocales().get(0));
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_date_item, parent, false);
        return new DateViewHolder(view);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position)
    {
        DateItem dateItem = dateList.get(position);

        holder.monthTextView.setText(dateItem.getDate().format(monthFormatter));
        holder.dayTextView.setText(dateItem.getDay());
        holder.weekdayTextView.setText(dateItem.getDate().format(weekdayFormatter));

        if (dateItem.isSelected())
        {
            int selectedBackgroundColor = android.R.attr.colorPrimary;
            try
            {
                android.util.TypedValue typedValue = new android.util.TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.colorBackgroundFloating, typedValue, true);
                selectedBackgroundColor = typedValue.data;
            }
            catch (Exception ignored)
            {
            }

            holder.itemView.setBackgroundColor(selectedBackgroundColor);
            holder.itemView.setElevation(10f);
            holder.dayTextView.setTextColor(resolveColorAttribute(context, android.R.attr.textColorPrimary));
            holder.monthTextView.setTextColor(resolveColorAttribute(context, android.R.attr.textColorPrimary));
            holder.weekdayTextView.setTextColor(resolveColorAttribute(context, android.R.attr.textColorPrimary));
        }
        else
        {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
            holder.dayTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            holder.monthTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            holder.weekdayTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        }

        holder.itemView.setOnClickListener(v ->
        {
            int clickedPosition = holder.getAdapterPosition();
            if (clickedPosition != RecyclerView.NO_POSITION)
            {
                if (selectedItemPosition != RecyclerView.NO_POSITION && selectedItemPosition < dateList.size())
                {
                    dateList.get(selectedItemPosition).setSelected(false);
                    notifyItemChanged(selectedItemPosition);
                }

                selectedItemPosition = clickedPosition;
                dateList.get(selectedItemPosition).setSelected(true);
                notifyItemChanged(selectedItemPosition);

                if (onDateSelectedListener != null)
                {
                    onDateSelectedListener.onDateSelected(dateItem.getDate());
                }
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return dateList.size();
    }

    public void setSelectedDate(LocalDate date)
    {
        int old = selectedItemPosition;
        int positionToSelect = -1;
        for (int i = 0; i < dateList.size(); i++)
        {
            if (dateList.get(i).getDate().equals(date))
            {
                positionToSelect = i;
                break;
            }
        }
        if (positionToSelect != -1)
        {
            if (selectedItemPosition != RecyclerView.NO_POSITION && selectedItemPosition < dateList.size())
            {
                dateList.get(selectedItemPosition).setSelected(false);
            }
            selectedItemPosition = positionToSelect;
            dateList.get(selectedItemPosition).setSelected(true);
            notifyItemChanged(positionToSelect);
            notifyItemChanged(old);
        }
    }

    public void addDate(DateItem dateItem)
    {
        if (dateList.stream().noneMatch(date -> date.getDate().equals(dateItem.getDate())))
        {
            int insertionPosition = 0;
            for (int i = 0; i < dateList.size(); i++)
            {
                if (dateItem.getDate().isBefore(dateList.get(i).getDate()))
                {
                    insertionPosition = i;
                    break;
                }
                insertionPosition = i + 1;
            }
            LocalDate selected = null;
            if (dateList.size() >= selectedItemPosition)
            {
                selected = dateList.get(selectedItemPosition).getDate();
            }
            dateList.add(insertionPosition, dateItem);
            notifyItemInserted(insertionPosition);
            if (selected != null)
            {
                setSelectedDate(selected);
            }
        }
    }

    @Getter
    @Setter
    public static class DateViewHolder extends RecyclerView.ViewHolder
    {
        private TextView monthTextView;
        private TextView dayTextView;
        private TextView weekdayTextView;
        private LinearLayout dateItemLayout;

        public DateViewHolder(@NonNull View itemView)
        {
            super(itemView);
            monthTextView = itemView.findViewById(R.id.text_view_month);
            dayTextView = itemView.findViewById(R.id.text_view_day);
            weekdayTextView = itemView.findViewById(R.id.text_view_weekday);
            dateItemLayout = itemView.findViewById(R.id.date_item_layout);
        }
    }
}