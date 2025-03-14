package com.yongjibus.arrivaltime.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.annotation.CreatedDate;

@AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "arrival_time")
public class ArrivalTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time_id", nullable = false)
    private int timeId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Column(name = "is_holiday", nullable = false)
    private boolean isHoliday;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public ArrivalTime(int timeId, LocalDate date, String dayOfWeek, LocalTime time, boolean isHoliday) {
        this.timeId = timeId;
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.isHoliday = isHoliday;
    }
}

