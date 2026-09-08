package org.kabuapp.kabuapp.feature.auth;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@AllArgsConstructor
@Entity(tableName = "users")
public class User
{
    @NotNull
    @PrimaryKey()
    private UUID id;
    @ColumnInfo(name = "username")
    private String username;
    @ColumnInfo(name = "password")
    private String password;
    @ColumnInfo(name = "token")
    private String token;
    @ColumnInfo(name = "standard")
    private Boolean standard;
}
