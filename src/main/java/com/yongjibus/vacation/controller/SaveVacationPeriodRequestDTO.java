package com.yongjibus.vacation.controller;

import java.time.LocalDate;

public record SaveVacationPeriodRequestDTO(LocalDate startDate, LocalDate endDate, String vacationDescription) {
} 