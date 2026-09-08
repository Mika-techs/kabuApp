package org.kabuapp.kabuapp.feature.auth;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.UUID;

/**
 * A stored account. Password and token are held as Keystore-encrypted blobs; see
 * {@link org.kabuapp.kabuapp.core.data.CredentialCipher}.
 */
@Entity(tableName = "users")
public record User(
    @NonNull @PrimaryKey @ColumnInfo(name = "id") UUID id,
    @ColumnInfo(name = "username") String username,
    @ColumnInfo(name = "password", typeAffinity = ColumnInfo.BLOB) byte[] password,
    @ColumnInfo(name = "token", typeAffinity = ColumnInfo.BLOB) byte[] token)
{
}
