package org.kabuapp.kabuapp.feature.exam;

import okhttp3.OkHttpClient;
import org.kabuapp.kabuapp.core.net.ApiClient;
import org.kabuapp.kabuapp.core.net.ApiException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The termine/schulaufgaben endpoint. */
public class ExamApi extends ApiClient
{
    public ExamApi(OkHttpClient httpClient)
    {
        super(httpClient);
    }

    public List<ExamResponse> getExams(int month) throws ApiException
    {
        ExamResponse[] responses = get("termine/schulaufgaben", Map.of("monat", month), ExamResponse[].class);
        return responses == null ? List.of() : Arrays.asList(responses);
    }
}
