package org.kabuapp.kabuapp.feature.auth;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * A stored account. Password and token are held as Keystore-encrypted blobs; see
 * {@link org.kabuapp.kabuapp.core.data.CredentialCipher}.
 */
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
    @ColumnInfo(name = "password", typeAffinity = ColumnInfo.BLOB)
    private byte[] password;
    @ColumnInfo(name = "token", typeAffinity = ColumnInfo.BLOB)
    private byte[] token;
    @ColumnInfo(name = "standard")
    private Boolean standard;
}
