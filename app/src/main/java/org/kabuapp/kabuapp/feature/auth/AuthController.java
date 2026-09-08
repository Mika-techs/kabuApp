package org.kabuapp.kabuapp.feature.auth;

import lombok.AllArgsConstructor;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.core.data.ActiveUserStore;
import org.kabuapp.kabuapp.core.data.CredentialCipher;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.core.net.TokenSource;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

@AllArgsConstructor
public class AuthController implements TokenSource
{
    private static final Logger LOG = Logger.getLogger("AuthController");

    private AuthStateholder stateholder;
    private AppDatabase db;
    private AuthApi authApi;
    private ActiveUserStore activeUserStore;
    private ExecutorService dbExecutor;
    private ExecutorService ioExecutor;
    private CredentialCipher cipher;

    @Override
    public String currentToken()
    {
        return stateholder.getToken();
    }

    /**
     * Called from the HTTP layer on a 401. Blocks on purpose: OkHttp expects the fresh
     * credentials to be returned, not delivered through a callback.
     */
    @Override
    public synchronized String reauthenticate()
    {
        try
        {
            String token = authApi.auth(stateholder.getUsername(), stateholder.getPassword());
            stateholder.setToken(token);
            dbExecutor.execute(this::save);
            return token;
        }
        catch (ApiException e)
        {
            LOG.log(Level.WARNING, "re-authentication failed: " + e.getKind());
            return null;
        }
    }

    public UUID getId()
    {
        return activeUserStore.get();
    }

    public void removeUser(UUID id)
    {
        if (stateholder.getUsers().entrySet().stream().filter(entry ->
                entry.getValue().equals(id)).findAny().isEmpty())
        {
            return;
        }
        stateholder.getUsers().remove(stateholder.getUsers().entrySet().stream().filter(entry -> entry.getValue().equals(id)).findAny().get().getKey());
        stateholder.setUsername(null);
        stateholder.setPassword(null);
        stateholder.setToken(null);
    }

    public void resetState()
    {
        stateholder.setUsername(null);
        stateholder.setPassword(null);
        stateholder.setToken(null);
        activeUserStore.set(null);
    }

    /**
     * Authenticates and stores the account. Blocks, so it must be called off the main thread.
     *
     * @return {@code null} on success, otherwise why it failed
     */
    public ApiException.Kind login(String username, String password)
    {
        stateholder.setUsername(username);
        stateholder.setPassword(password);
        stateholder.getUsers().put(username, activeUserStore.get());
        try
        {
            stateholder.setToken(authApi.auth(username, password));
            save();
            return null;
        }
        catch (ApiException e)
        {
            LOG.log(Level.WARNING, "authentication failed: " + e.getKind());
            return e.getKind();
        }
    }

    private void save()
    {
        UUID activeId = activeUserStore.get();
        User existingUser = activeId == null ? null : db.userDao().get(activeId);
        if (existingUser == null)
        {
            User user = new User(UUID.randomUUID(), stateholder.getUsername(),
                encrypt(stateholder.getPassword()), encrypt(stateholder.getToken()));
            db.userDao().insert(user);
            activeUserStore.set(user.getId());
        }
        else
        {
            existingUser.setUsername(stateholder.getUsername());
            existingUser.setPassword(encrypt(stateholder.getPassword()));
            existingUser.setToken(encrypt(stateholder.getToken()));
            db.userDao().update(existingUser);
        }
    }

    /** Loads the account named by the active-user preference, or any stored account. */
    public UUID getDbUser()
    {
        List<User> users = db.userDao().getAll();
        UUID activeId = activeUserStore.get();
        users.stream()
            .filter(user -> user.getId().equals(activeId) && !user.getUsername().isEmpty())
            .findAny()
            .ifPresentOrElse(this::load,
                () -> users.stream().filter(user -> !user.getUsername().isEmpty()).findFirst().ifPresent(this::load));
        return activeUserStore.get();
    }

    public void getDbUsers()
    {
        dbExecutor.execute(() ->
        {
            List<User> users = db.userDao().getAll();
            Map<String, UUID> userMap = new LinkedHashMap<>();
            users.forEach(user -> userMap.put(user.getUsername(), user.getId()));
            stateholder.setUsers(userMap);
        });
    }

    /**
     * Switches to a stored account. Writing the preference re-points every observed query, so no
     * screen has to recreate itself to pick up the change.
     */
    public UUID getDbUserByNameAndLoad(String name)
    {
        UUID id = stateholder.getUsers().get(name);
        db.userDao().getAll().stream().filter(user -> user.getId().equals(id)).findAny().ifPresent(this::load);
        return activeUserStore.get();
    }

    private void load(User user)
    {
        stateholder.setUsername(user.getUsername());
        stateholder.setPassword(decrypt(user.getPassword()));
        stateholder.setToken(decrypt(user.getToken()));
        activeUserStore.set(user.getId());
    }

    private byte[] encrypt(String value)
    {
        try
        {
            return cipher.encrypt(value);
        }
        catch (GeneralSecurityException e)
        {
            LOG.log(Level.SEVERE, "could not encrypt credentials", e);
            return null;
        }
    }

    private String decrypt(byte[] value)
    {
        try
        {
            return cipher.decrypt(value);
        }
        catch (GeneralSecurityException e)
        {
            LOG.log(Level.SEVERE, "could not decrypt credentials, account needs a new login", e);
            return null;
        }
    }

    public boolean isInitialized()
    {
        return  stateholder.getUsername() != null && !stateholder.getUsername().isEmpty()
                && stateholder.getPassword() != null && !stateholder.getPassword().isEmpty()
                && stateholder.getToken() != null && !stateholder.getToken().isEmpty();
    }

    public List<String> getUsers()
    {
        return new ArrayList<>(stateholder.getUsers().keySet());
    }

    public String getUser()
    {
        return stateholder.getUsername();
    }
}
