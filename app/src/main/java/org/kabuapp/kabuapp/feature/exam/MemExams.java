package org.kabuapp.kabuapp.feature.exam;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemExams
{
    private final Map<LocalDate, MemExam> exams = new HashMap<>();
}
