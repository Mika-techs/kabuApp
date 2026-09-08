package org.kabuapp.kabuapp.core.net;

/**
 * Supplies bearer tokens to the HTTP layer. Implemented by the auth feature so that
 * {@link AuthInterceptor} and {@link TokenAuthenticator} can attach and refresh credentials
 * without any endpoint or repository knowing that authentication exists.
 */
public interface TokenSource
{
    /** Current bearer token, or {@code null} when no user is logged in. */
    String currentToken();

    /**
     * Re-authenticates with the stored credentials of the active user.
     * Called on the OkHttp dispatcher thread, so it must block rather than call back.
     *
     * @return the fresh token, or {@code null} when re-authentication failed
     */
    String reauthenticate();
}
