package org.kabuapp.kabuapp.data.memory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MemExam
{
    @Setter
    private UUID dbId;
    private LocalDate beginn;
    private short duration;
    private String info;

    public void addDuration()
    {
        this.duration++;
    }
}
