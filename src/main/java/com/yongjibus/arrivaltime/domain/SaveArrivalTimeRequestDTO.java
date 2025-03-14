package com.yongjibus.arrivaltime.domain;

import java.time.LocalDate;
import java.time.LocalTime;

public record SaveArrivalTimeRequestDTO(
    int busId,
    LocalDate date,
    LocalTime time,
    boolean isHoliday
) {
}
