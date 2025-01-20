package com.yongjibus.daytype.model;

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
    private String dateKind;
}
