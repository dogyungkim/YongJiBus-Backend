package com.yongjibus.arrivaltime.controller.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaveArrivalTimeRequestDTO(
    @Positive(message = "버스 ID는 1 이상이어야 합니다.")
    int busId,

    @NotNull(message = "날짜는 필수입니다.")
    LocalDate date,

    @NotNull(message = "도착 시간은 필수입니다.")
    LocalTime time,

    boolean isHoliday
) {
}
