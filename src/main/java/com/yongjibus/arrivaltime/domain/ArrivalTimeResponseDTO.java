package com.yongjibus.arrivaltime.domain;

import java.time.LocalTime;

public record ArrivalTimeResponseDTO(
    int busId,
    LocalTime time
) {
    public static ArrivalTimeResponseDTO from(ArrivalTime arrivalTime) {
        return new ArrivalTimeResponseDTO(
            arrivalTime.getTimeId(),
            arrivalTime.getTime()
        );
    }
}