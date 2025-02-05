package com.yongjibus.daytype.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.yongjibus.daytype.domain.GetDayTypeResponseDTO;
import com.yongjibus.daytype.service.DayTypeService;

import java.time.LocalDate;

@AllArgsConstructor
@RestController
@RequestMapping("/day")
public class DayTypeController {

    private final DayTypeService dayTypeService;

    @GetMapping
    ResponseEntity<GetDayTypeResponseDTO> getDayType(@RequestParam("date") LocalDate date){
        return ResponseEntity.ok(GetDayTypeResponseDTO.fromEntity(dayTypeService.findDayInfo(date)));
    }
}
