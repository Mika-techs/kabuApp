package org.kabuapp.kabuapp.feature.auth;

import java.io.Serializable;
import androidx.annotation.Keep;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
/** Deserialised by MetisJson reflectively, so the field names must survive R8. */
@Keep
public class AuthRequest implements Serializable
{
    private String userName;
    private String password;
}
