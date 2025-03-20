package com.yongjibus.arrivaltime.controller.dto;

import java.time.LocalDate;

public record GetArrivalTimeRequestDTO(
    int busId,
    LocalDate date
) {
}