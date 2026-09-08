package org.kabuapp.kabuapp.core.net;

import androidx.annotation.NonNull;
import lombok.AllArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Attaches the active user's bearer token to every request except the authenticate call itself.
 */
@AllArgsConstructor
public class AuthInterceptor implements Interceptor
{
    private final TokenSource tokenSource;

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException
    {
        Request request = chain.request();
        String token = tokenSource.currentToken();
        if (token == null || token.isEmpty() || ApiClient.isAuthRequest(request))
        {
            return chain.proceed(request);
        }
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer " + token).build());
    }
}
