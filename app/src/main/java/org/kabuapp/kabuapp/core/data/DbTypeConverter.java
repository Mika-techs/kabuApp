package org.kabuapp.kabuapp.core.data;

import androidx.room.TypeConverter;
import org.kabuapp.kabuapp.domain.DbType;

/** Stores {@link DbType} by name so that reordering the enum cannot corrupt existing rows. */
public class DbTypeConverter
{
    @TypeConverter
    public String fromDbType(DbType dbType)
    {
        return dbType == null ? null : dbType.name();
    }

    @TypeConverter
    public DbType toDbType(String name)
    {
        return name == null ? null : DbType.valueOf(name);
    }
}
