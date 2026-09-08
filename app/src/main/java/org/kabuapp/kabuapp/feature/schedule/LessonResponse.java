package org.kabuapp.kabuapp.feature.schedule;

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
public class LessonResponse
{
    private String datum;
    private int anfStd;
    private int endStd;
    private String lehrer;
    private String uFachBez;
    private String raumLongtext;
    private String gruppe;
}