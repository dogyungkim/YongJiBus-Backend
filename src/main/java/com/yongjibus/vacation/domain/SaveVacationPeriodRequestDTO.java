package com.yongjibus.vacation.domain;

import java.time.LocalDate;

public record SaveVacationPeriodRequestDTO(LocalDate startDate, LocalDate endDate, String vacationDescription) {
} 