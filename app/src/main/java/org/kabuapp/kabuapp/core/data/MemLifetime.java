package org.kabuapp.kabuapp.core.data;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class MemLifetime
{
    private UUID dbId;
    private LocalDateTime scheduleLastUpdate;
    private LocalDateTime examLastUpdate;
}
