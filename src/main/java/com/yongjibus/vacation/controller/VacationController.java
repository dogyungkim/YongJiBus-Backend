package com.yongjibus.vacation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.vacation.domain.SaveVacationPeriodRequestDTO;
import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.service.VacationService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/vacation")
public class VacationController {

    private final VacationService vacationService;
    
    @PostMapping("/vacation-period")
    public ResponseEntity<String> addVacationPeriod(@RequestBody SaveVacationPeriodRequestDTO vacationPeriodDTO) {
        VacationPeriod vacationPeriod = VacationPeriod.builder()
            .startDate(vacationPeriodDTO.startDate())
            .endDate(vacationPeriodDTO.endDate())
            .vacationDescription(vacationPeriodDTO.vacationDescription())
            .build();
        vacationService.saveVacationPeriod(vacationPeriod);
        return ResponseEntity.ok("success");
    }
}
