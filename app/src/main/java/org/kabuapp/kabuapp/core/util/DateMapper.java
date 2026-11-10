package org.kabuapp.kabuapp.core.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Parses the dates digikabu.de sends. The API has switched between {@code dd.MM.yyyy} and
 * {@code MM/dd/yyyy} without notice and back again, so the separator decides the format
 * rather than a hardcoded field order.
 */
public final class DateMapper
{
    private static final DateTimeFormatter GERMAN = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter US = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private DateMapper()
    {
    }

    /** Accepts {@code dd.MM.yyyy} and {@code MM/dd/yyyy}. */
    public static LocalDate parseDate(String value)
    {
        return LocalDate.parse(value, value.indexOf('/') >= 0 ? US : GERMAN);
    }
}
