package org.kabuapp.kabuapp.core.net;

import androidx.annotation.Nullable;
import lombok.AllArgsConstructor;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Re-authenticates once on HTTP 401 and replays the request with the fresh token. This replaces
 * the renew-and-retry loop that every controller used to implement by hand.
 */
@AllArgsConstructor
public class TokenAuthenticator implements Authenticator
{
    private final TokenSource tokenSource;

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, Response response)
    {
        if (ApiClient.isAuthRequest(response.request()) || response.priorResponse() != null)
        {
            return null;
        }
        String token = tokenSource.reauthenticate();
        if (token == null || token.isEmpty())
        {
            return null;
        }
        return response.request().newBuilder().header("Authorization", "Bearer " + token).build();
    }
}
