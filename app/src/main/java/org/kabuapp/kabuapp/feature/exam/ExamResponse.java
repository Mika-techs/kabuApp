package org.kabuapp.kabuapp.feature.exam;

import androidx.annotation.Keep;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
/** Deserialised by MetisJson reflectively, so the field names must survive R8. */
@Keep
public class ExamResponse
{
    private String date;
    private String info;
}
