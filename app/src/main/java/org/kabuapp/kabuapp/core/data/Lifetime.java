package org.kabuapp.kabuapp.core.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.TypeConverters;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.kabuapp.kabuapp.domain.DbType;
import org.kabuapp.kabuapp.feature.auth.User;

/**
 * When a given kind of cached data was last refreshed, one row per user and DbType. Keeping the
 * kind in a column rather than a column per kind means a new DbType needs no schema change.
 */
@Getter
@Setter
@AllArgsConstructor
@Entity(
    tableName = "lifetimes",
    primaryKeys = { "userId", "dbType" },
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE))
@TypeConverters({LocalDateTimeConverter.class})
public class Lifetime
{
    @NonNull
    @ColumnInfo(name = "userId")
    private UUID userId;
    @NonNull
    @ColumnInfo(name = "dbType")
    private DbType dbType;
    @ColumnInfo(name = "lastUpdate")
    private LocalDateTime lastUpdate;
}
