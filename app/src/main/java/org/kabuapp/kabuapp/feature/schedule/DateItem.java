package org.kabuapp.kabuapp.feature.schedule;

import java.time.LocalDate;

/** One entry of the horizontal date strip. */
public record DateItem(LocalDate date, String month, String day, String weekday)
{
}
