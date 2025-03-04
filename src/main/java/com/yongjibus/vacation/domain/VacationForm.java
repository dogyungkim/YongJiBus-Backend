package com.yongjibus.vacation.domain;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank; 
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VacationForm {
    
    @NotNull(message = "시작일을 선택해주세요")
    private LocalDate startDate;
    
    @NotNull(message = "종료일을 선택해주세요")
    private LocalDate endDate;
    
    @NotBlank(message = "방학 설명을 입력해주세요")
    private String vacationDescription;
} 