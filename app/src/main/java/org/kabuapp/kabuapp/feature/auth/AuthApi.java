package org.kabuapp.kabuapp.feature.auth;

import okhttp3.OkHttpClient;
import org.kabuapp.kabuapp.core.net.ApiClient;
import org.kabuapp.kabuapp.core.net.ApiException;

/** The authenticate endpoint. */
public class AuthApi extends ApiClient
{
    public AuthApi(OkHttpClient httpClient)
    {
        super(httpClient);
    }

    /**
     * Exchanges credentials for a bearer token.
     *
     * @throws ApiException {@code BAD_CREDENTIALS} when the server rejects username or password
     */
    public String auth(String username, String password) throws ApiException
    {
        if (username == null || password == null)
        {
            throw new ApiException(ApiException.Kind.BAD_CREDENTIALS, "username or password is null");
        }
        String response;
        try
        {
            response = post(PATH_AUTHENTICATE, new AuthRequest(username, password), String.class);
        }
        catch (ApiException e)
        {
            // On this endpoint alone, a rejected request means rejected credentials.
            if (e.getKind() == ApiException.Kind.BAD_REQUEST)
            {
                throw new ApiException(ApiException.Kind.BAD_CREDENTIALS, "credentials rejected", e);
            }
            throw e;
        }
        if (response == null)
        {
            throw new ApiException(ApiException.Kind.SERVER, "authenticate returned an empty body");
        }
        return unquote(response);
    }

    /** The token comes back as a bare quoted JSON string. */
    private static String unquote(String value)
    {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
        {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
