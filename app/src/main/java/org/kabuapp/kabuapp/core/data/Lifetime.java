package org.kabuapp.kabuapp.core.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.TypeConverters;
import java.time.LocalDateTime;
import java.util.UUID;
import org.kabuapp.kabuapp.domain.DbType;
import org.kabuapp.kabuapp.feature.auth.User;

@Entity(
    tableName = "lifetimes",
    primaryKeys = { "userId", "dbType" },
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE))
@TypeConverters({LocalDateTimeConverter.class})
public record Lifetime(
    @NonNull @ColumnInfo(name = "userId") UUID userId,
    @NonNull @ColumnInfo(name = "dbType") DbType dbType,
    @ColumnInfo(name = "lastUpdate") LocalDateTime lastUpdate)
{
}
