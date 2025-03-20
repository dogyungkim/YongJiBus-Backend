package com.yongjibus.daytype.controller.dto;

import java.time.LocalDate;

import com.yongjibus.daytype.domain.DateInfo;

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
