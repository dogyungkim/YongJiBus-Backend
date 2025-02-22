package com.yongjibus.daytype.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.yongjibus.daytype.domain.GetDayTypeResponseDTO;
import com.yongjibus.daytype.service.DayTypeService;
import com.yongjibus.global.ApiResponse;

import java.time.LocalDate;

@AllArgsConstructor
@RestController
@RequestMapping("/day")
public class DayTypeController {

    private final DayTypeService dayTypeService;

    @GetMapping
    ResponseEntity<ApiResponse<GetDayTypeResponseDTO>> getDayType(@RequestParam("date") LocalDate date){
        return ApiResponse.success(GetDayTypeResponseDTO.fromEntity(dayTypeService.findDayInfo(date)));
    }
}
