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
        /** Credentials rejected: HTTP 400 from the authenticate endpoint only. */
        BAD_CREDENTIALS,
        /**
         * The server rejected the request itself (HTTP 400 or 404 on a data endpoint). Seen in
         * normal operation between school years, when digikabu.de answers
         * "Zeitbereich ausserhalb des Schuljahres" for every date.
         */
        BAD_REQUEST,
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
