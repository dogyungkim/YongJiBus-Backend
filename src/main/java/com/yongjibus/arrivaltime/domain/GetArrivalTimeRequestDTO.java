package com.yongjibus.arrivaltime.domain;

import java.time.LocalDate;

public record GetArrivalTimeRequestDTO(
    int busId,
    LocalDate date
) {
}