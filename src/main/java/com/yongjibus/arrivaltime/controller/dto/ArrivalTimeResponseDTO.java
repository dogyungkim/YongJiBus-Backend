package com.yongjibus.arrivaltime.controller.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yongjibus.arrivaltime.domain.ArrivalTime;

public record ArrivalTimeResponseDTO(
    int busId,
    @JsonFormat(pattern = "HH:mm")
    LocalTime time
) {
    public static ArrivalTimeResponseDTO from(ArrivalTime arrivalTime) {
        return new ArrivalTimeResponseDTO(
            arrivalTime.getTimeId(),
            arrivalTime.getTime()
        );
    }
}