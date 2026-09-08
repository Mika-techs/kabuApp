package org.kabuapp.kabuapp.feature.schedule;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DateItem
{
    private LocalDate date;
    private String month;
    private String day;
    private String weekday;
    private boolean isSelected;
}