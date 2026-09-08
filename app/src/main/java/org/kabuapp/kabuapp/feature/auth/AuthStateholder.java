package org.kabuapp.kabuapp.feature.auth;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class AuthStateholder implements Serializable
{
    private String username;
    private String password;
    private String token;
    /** Username to stored id, for the account switcher. Never null, so callers need no guard. */
    private Map<String, UUID> users = new LinkedHashMap<>();
}
