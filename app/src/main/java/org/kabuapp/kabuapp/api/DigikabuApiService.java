package org.kabuapp.kabuapp.api;

import org.kabuapp.kabuapp.api.exceptions.BadRequestException;
import org.kabuapp.kabuapp.api.exceptions.UnauthorisedException;
import org.kabuapp.kabuapp.api.models.AuthRequest;
import org.kabuapp.kabuapp.api.models.ExamResponse;
import org.kabuapp.kabuapp.api.models.LessonResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DigikabuApiService extends ApiService
{
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public DigikabuApiService()
    {
        super();
        baseUrl = "https://digikabu.de/api/";
    }

    public String auth(String username, String password) throws BadRequestException
    {
        if (username == null || password == null)
        {
            Logger.getLogger("API").log(Level.WARNING, "username or password is null");
            throw new BadRequestException();
        }
        try
        {
            String response = executeRequest(
                    "authenticate",
                    "POST",
                    String.class,
                    null,
                    null,
                    new AuthRequest(username, password));
            return response.substring(1, response.length() - 1);
        }
        catch (Exception e)
        {
            if (e instanceof BadRequestException)
            {
                throw (BadRequestException) e;
            }
            Logger.getLogger("API").log(Level.WARNING, e.toString());
            return null;
        }
    }

    public List<LessonResponse> getSchedule(String token, LocalDate date, int days) throws UnauthorisedException
    {
        if (token == null)
        {
            Logger.getLogger("API").log(Level.WARNING, "token is null");
            throw new UnauthorisedException();
        }
        try
        {
            return Arrays.asList(executeRequest(
                    "stundenplan",
                    "GET",
                    LessonResponse[].class,
                    Map.of("datum",
                            date.format(formatter) + "T00:00:01.123Z",
                            "anzahl", days),
                    Map.of("Authorization", "Bearer " + token),
                    null
            ));
        }
        catch (Exception e)
        {
            if (e instanceof UnauthorisedException)
            {
                throw new UnauthorisedException();
            }
            Logger.getLogger("API").log(Level.WARNING, e.toString());
            return null;
        }
    }

    public List<ExamResponse> getExams(String token, int month) throws UnauthorisedException
    {
        if (token == null)
        {
            Logger.getLogger("API").log(Level.WARNING, "token is null");
            throw new UnauthorisedException();
        }
        try
        {
            return Arrays.asList(executeRequest(
                    "termine/schulaufgaben",
                    "GET",
                    ExamResponse[].class,
                    Map.of("monat", month),
                    Map.of("Authorization", "Bearer " + token),
                    null
            ));
        }
        catch (Exception e)
        {
            if (e instanceof UnauthorisedException)
            {
                throw new UnauthorisedException();
            }
            Logger.getLogger("API").log(Level.WARNING, e.toString());
            return null;
        }
    }
}
