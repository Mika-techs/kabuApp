package org.kabuapp.kabuapp.feature.schedule;

import okhttp3.OkHttpClient;
import org.kabuapp.kabuapp.core.net.ApiClient;
import org.kabuapp.kabuapp.core.net.ApiException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The stundenplan endpoint. */
public class ScheduleApi extends ApiClient
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ScheduleApi(OkHttpClient httpClient)
    {
        super(httpClient);
    }

    public List<LessonResponse> getSchedule(LocalDate date, int days) throws ApiException
    {
        LessonResponse[] responses = get(
            "stundenplan",
            Map.of("datum", date.format(DATE_FORMAT) + "T00:00:01.123Z", "anzahl", days),
            LessonResponse[].class);
        return responses == null ? List.of() : Arrays.asList(responses);
    }
}
