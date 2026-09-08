package org.kabuapp.kabuapp.feature.auth;

import lombok.AllArgsConstructor;
import org.kabuapp.kabuapp.core.data.AppDatabase;
import org.kabuapp.kabuapp.core.data.CredentialCipher;
import org.kabuapp.kabuapp.core.net.ApiException;
import org.kabuapp.kabuapp.core.net.Callback;
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
    private ExecutorService executorService;
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
            executorService.execute(this::save);
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
        return stateholder.getDbId();
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
        stateholder.setDbId(UUID.randomUUID());
    }

    public boolean setCredentials(String username, String password, Callback callback, Object[] args)
    {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty())
        {
            stateholder.setUsername(username);
            stateholder.setPassword(password);
            auth(callback, args);
            stateholder.getUsers().put(username, stateholder.getDbId());
            return true;
        }
        return false;
    }

    /**
     * Authenticates with the credentials currently held in the stateholder.
     * On failure {@code args[0]} carries the {@link ApiException.Kind} so the caller can tell
     * bad credentials apart from an unreachable server.
     */
    public void auth(Callback callback, Object[] args)
    {
        try
        {
            stateholder.setToken(authApi.auth(stateholder.getUsername(), stateholder.getPassword()));
            executorService.execute(this::save);
            if (callback != null)
            {
                callback.callback(args);
            }
        }
        catch (ApiException e)
        {
            LOG.log(Level.WARNING, "authentication failed: " + e.getKind());
            if (callback != null && args != null && args.length > 0)
            {
                args[0] = e.getKind();
                callback.callback(args);
            }
        }
    }

    private void save()
    {
        User existingUser = db.userDao().get(stateholder.getDbId());
        if (existingUser == null)
        {
            User user = new User(UUID.randomUUID(), stateholder.getUsername(),
                encrypt(stateholder.getPassword()), encrypt(stateholder.getToken()), true);
            stateholder.setDbId(user.getId());
            db.userDao().insert(user);
        }
        else
        {
            existingUser.setUsername(stateholder.getUsername());
            existingUser.setPassword(encrypt(stateholder.getPassword()));
            existingUser.setToken(encrypt(stateholder.getToken()));
            db.userDao().update(existingUser);
        }
    }

    public UUID getDbUser()
    {
        List<User> users = db.userDao().getAll();
        users.stream().filter(user -> Boolean.TRUE.equals(user.getStandard()) && !user.getUsername().isEmpty()).findAny().ifPresentOrElse(user ->
        {
            load(user);
        }, () -> users.stream().filter(user -> !user.getUsername().isEmpty()).findFirst().ifPresent(this::load));
        return stateholder.getDbId();
    }

    public void getDbUsers()
    {
        executorService.execute(() ->
        {
            List<User> users = db.userDao().getAll();
            Map<String, UUID> userMap = new LinkedHashMap<>();
            users.forEach(user -> userMap.put(user.getUsername(), user.getId()));
            stateholder.setUsers(userMap);
        });
    }

    public UUID getDbUserByNameAndLoad(String name)
    {
        UUID id = stateholder.getUsers().get(name);
        List<User> users = db.userDao().getAll();

        users.stream().filter(user -> Boolean.TRUE.equals(user.getStandard())).findAny().ifPresent(user ->
        {
            user.setStandard(false);
            db.userDao().update(user);
        });
        users.stream().filter(user -> user.getId().equals(id)).findAny().ifPresent(user ->
        {
            user.setStandard(true);
            db.userDao().update(user);
            load(user);
        });
        return stateholder.getDbId();
    }

    private void load(User user)
    {
        stateholder.setUsername(user.getUsername());
        stateholder.setPassword(decrypt(user.getPassword()));
        stateholder.setToken(decrypt(user.getToken()));
        stateholder.setDbId(user.getId());
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
