package org.kabuapp.kabuapp.core.net;

import lombok.Getter;

/**
 * Every failure the API layer can produce. Endpoint methods throw these instead of returning
 * {@code null}, so callers can tell "offline" apart from "nothing scheduled".
 */
@Getter
public class ApiException extends Exception
{
    /** Which kind of failure occurred, so the UI can pick a message without instanceof chains. */
    public enum Kind
    {
        /** No usable connection, DNS failure, timeout. */
        NETWORK,
        /** Credentials rejected (HTTP 400 on authenticate). */
        BAD_CREDENTIALS,
        /** Token missing or rejected and re-authentication did not help (HTTP 401). */
        UNAUTHORISED,
        /** Anything else the server returned. */
        SERVER
    }

    private final Kind kind;

    public ApiException(Kind kind, String message)
    {
        super(message);
        this.kind = kind;
    }

    public ApiException(Kind kind, String message, Throwable cause)
    {
        super(message, cause);
        this.kind = kind;
    }
}
