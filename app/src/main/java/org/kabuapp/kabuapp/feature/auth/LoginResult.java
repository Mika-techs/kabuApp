package org.kabuapp.kabuapp.feature.auth;

import org.kabuapp.kabuapp.core.net.ApiException;

/**
 * Outcome of a login attempt.
 *
 * @param errorKind why it failed, or {@code null} on success
 */
public record LoginResult(boolean success, ApiException.Kind errorKind)
{
    public static LoginResult ok()
    {
        return new LoginResult(true, null);
    }

    public static LoginResult failure(ApiException.Kind kind)
    {
        return new LoginResult(false, kind);
    }
}
