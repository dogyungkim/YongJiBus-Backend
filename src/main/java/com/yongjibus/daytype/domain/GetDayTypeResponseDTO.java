package com.yongjibus.daytype.domain;

import java.time.LocalDate;

public record GetDayTypeResponseDTO(
    LocalDate date,
    boolean isHoliday,
    String dateKind
) {
    public static GetDayTypeResponseDTO fromEntity(DateInfo dateInfo) {
        return new GetDayTypeResponseDTO(
            dateInfo.getDate(),
            dateInfo.isHoliday(),
            dateInfo.getDateKind());
    }
}
