package com.yongjibus.timetable.controller.dto;

import java.util.List;

public record TimetablePayloadDTO(
        List<MyongjiWeekdayTime> myongjiWeekday,
        List<MyongjiWeekendTime> myongjiWeekend,
        List<GiheungWeekdayTime> giheungWeekday) {

    public record MyongjiWeekdayTime(Integer id, String type, String startTime, String predTime) {
    }

    public record MyongjiWeekendTime(Integer id, String startTime, String predTime) {
    }

    public record GiheungWeekdayTime(Integer id, String startTime, String predTime, String schoolArrival,
            Integer runCount) {
    }
}
