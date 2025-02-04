package com.yongjibus.arrivaltime.domain;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record ArrivalTimeListResponseDTO(
    int busId,
    List<String> arrivalTimes
) {
    public static ArrivalTimeListResponseDTO from(int busId, List<ArrivalTime> arrivalTimes) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return new ArrivalTimeListResponseDTO(
            busId,
            arrivalTimes.stream()
            .map(arrivalTime -> arrivalTime.getTime().format(formatter))
            .toList()
        );  
    }
}
