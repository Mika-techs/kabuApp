package org.kabuapp.kabuapp.data.memory;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class AuthStateholder implements Serializable
{
    private String username;
    private String password;
    private String token;
    private Map<String, UUID> users;
    private UUID dbId;
}
