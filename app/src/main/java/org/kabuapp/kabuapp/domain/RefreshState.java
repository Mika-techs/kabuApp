package org.kabuapp.kabuapp.domain;

import org.kabuapp.kabuapp.core.net.ApiException;

/**
 * Progress of a network refresh, kept separate from the cached data it refreshes. Because Room is
 * the source of truth the UI always has rows to draw, so a failure is "these lessons may be
 * stale" rather than "show nothing".
 *
 * @param status what the refresh is doing
 * @param errorKind why it failed, only set when {@code status} is {@code ERROR}
 */
public record RefreshState(Status status, ApiException.Kind errorKind)
{
    public enum Status
    {
        IDLE,
        LOADING,
        ERROR
    }

    public static RefreshState idle()
    {
        return new RefreshState(Status.IDLE, null);
    }

    public static RefreshState loading()
    {
        return new RefreshState(Status.LOADING, null);
    }

    public static RefreshState error(ApiException.Kind kind)
    {
        return new RefreshState(Status.ERROR, kind);
    }
}
