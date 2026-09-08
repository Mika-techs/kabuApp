package org.kabuapp.kabuapp.feature.exam;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import org.jetbrains.annotations.NotNull;
import org.kabuapp.kabuapp.core.data.LocalDateConverter;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Entity(tableName = "exams")
@TypeConverters({LocalDateConverter.class})
public class Exam
{
    @NotNull
    @PrimaryKey()
    private UUID id;
    @ColumnInfo(name = "userId")
    private UUID userID;
    @ColumnInfo(name = "date")
    private LocalDate date;
    @ColumnInfo(name = "duration")
    private Short duration;
    @ColumnInfo(name = "info")
    private String info;
}
