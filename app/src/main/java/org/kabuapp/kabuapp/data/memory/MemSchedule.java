package org.kabuapp.kabuapp.data.memory;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class MemSchedule
{
    private final Map<LocalDate, List<MemLesson>> lessons = new HashMap<>();
    @Setter
    private LocalDate selectedDate;
}
