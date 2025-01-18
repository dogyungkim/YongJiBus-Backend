package com.github.dogyungkim.yongjibus.yongjibus.daytype.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
public class GetDayTypeResponseDTO {
    private LocalDate date;
    private boolean isHoliday;
    private String dateKind;

    public static GetDayTypeResponseDTO fromEntity(DateInfo dateInfo){
        return new GetDayTypeResponseDTO(
                dateInfo.getDate(),
                dateInfo.isHoliday(),
                dateInfo.getDateKind());
    }
}
