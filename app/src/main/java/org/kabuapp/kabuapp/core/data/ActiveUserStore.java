package org.kabuapp.kabuapp.core.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.UUID;

/**
 * Which stored account is active. This is a device preference rather than per-account data, so it
 * lives in SharedPreferences and not in the users table - that also removes the old split between
 * a {@code standard} column and an in-memory id that could disagree.
 *
 * <p>Exposed as LiveData so every query re-points itself when the account changes, instead of the
 * settings screen recreating itself to force a re-read.
 */
public class ActiveUserStore
{
    private static final String PREFS = "kabuapp-session";
    private static final String KEY_ACTIVE_USER = "activeUserId";

    private final SharedPreferences prefs;
    private final MutableLiveData<UUID> activeUserId = new MutableLiveData<>();

    public ActiveUserStore(Context context)
    {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        activeUserId.setValue(read());
    }

    public LiveData<UUID> observe()
    {
        return activeUserId;
    }

    public UUID get()
    {
        return activeUserId.getValue();
    }

    public void set(UUID userId)
    {
        if (userId == null)
        {
            prefs.edit().remove(KEY_ACTIVE_USER).apply();
        }
        else
        {
            prefs.edit().putString(KEY_ACTIVE_USER, userId.toString()).apply();
        }
        activeUserId.postValue(userId);
    }

    private UUID read()
    {
        String stored = prefs.getString(KEY_ACTIVE_USER, null);
        if (stored == null)
        {
            return null;
        }
        try
        {
            return UUID.fromString(stored);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}
