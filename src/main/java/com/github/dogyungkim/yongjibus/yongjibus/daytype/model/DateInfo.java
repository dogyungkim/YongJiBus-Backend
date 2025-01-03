package com.github.dogyungkim.yongjibus.yongjibus.daytype.model;

import jakarta.annotation.Nullable;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DateInfo {
    private LocalDate date;
    private boolean isHoliday;
    @Nullable
    private String dateKind;
}
