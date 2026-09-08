package org.kabuapp.kabuapp.core.net;

import io.lilithtechs.metisJson.JsonMapper;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Map;

/**
 * HTTP core: builds requests against a base URL, maps status codes onto {@link ApiException}
 * and deserialises bodies with MetisJson. Timeouts live here, which is why callers no longer
 * need to wrap requests in a blocking {@code Future.get(timeout)}.
 */
public abstract class ApiClient
{
    /** Path of the token endpoint; it must neither carry nor refresh a bearer token. */
    public static final String PATH_AUTHENTICATE = "authenticate";

    private static final String BASE_URL = "https://digikabu.de/api/";
    private static final MediaType JSON = MediaType.get("application/json");

    private final JsonMapper jsonMapper = new JsonMapper();
    private final OkHttpClient httpClient;
    private final HttpUrl baseUrl;

    protected ApiClient(OkHttpClient httpClient)
    {
        this.baseUrl = HttpUrl.get(BASE_URL);
        this.httpClient = httpClient;
    }

    /** True for the authenticate call. */
    public static boolean isAuthRequest(Request request)
    {
        return request.url().pathSegments().contains(PATH_AUTHENTICATE);
    }

    protected <T> T get(String path, Map<String, Object> params, Class<T> clazz) throws ApiException
    {
        return execute(new Request.Builder().url(url(path, params)).get().build(), clazz);
    }

    protected <T> T post(String path, Object body, Class<T> clazz) throws ApiException
    {
        RequestBody requestBody = RequestBody.create(jsonMapper.toJson(body), JSON);
        return execute(new Request.Builder().url(url(path, null)).post(requestBody).build(), clazz);
    }

    private HttpUrl url(String path, Map<String, Object> params)
    {
        HttpUrl.Builder builder = baseUrl.newBuilder().addPathSegments(path);
        if (params != null)
        {
            params.forEach((key, value) -> builder.addQueryParameter(key, String.valueOf(value)));
        }
        return builder.build();
    }

    private <T> T execute(Request request, Class<T> clazz) throws ApiException
    {
        try (Response response = httpClient.newCall(request).execute())
        {
            ResponseBody body = response.body();
            String payload = body != null ? body.string() : null;

            if (!response.isSuccessful())
            {
                throw new ApiException(kindOf(response.code()),
                    "API-Error: " + response.code() + " - " + response.message() + "\nResponse Body: " + payload);
            }
            if (payload == null || payload.isEmpty())
            {
                return null;
            }
            if (clazz.equals(String.class))
            {
                return clazz.cast(payload);
            }
            return jsonMapper.fromJson(payload, clazz);
        }
        catch (IOException e)
        {
            throw new ApiException(ApiException.Kind.NETWORK, "Request to " + request.url() + " failed", e);
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ApiException(ApiException.Kind.SERVER, "Could not parse response from " + request.url(), e);
        }
    }

    private static ApiException.Kind kindOf(int statusCode)
    {
        if (statusCode == 400)
        {
            return ApiException.Kind.BAD_CREDENTIALS;
        }
        if (statusCode == 401)
        {
            return ApiException.Kind.UNAUTHORISED;
        }
        return ApiException.Kind.SERVER;
    }
}
