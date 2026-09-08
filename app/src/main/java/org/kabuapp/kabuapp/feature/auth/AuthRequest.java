package org.kabuapp.kabuapp.feature.auth;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AuthRequest implements Serializable
{
    private String userName;
    private String password;
}
