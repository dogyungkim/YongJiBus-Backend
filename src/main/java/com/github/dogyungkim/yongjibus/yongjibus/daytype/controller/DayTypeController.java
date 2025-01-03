package com.github.dogyungkim.yongjibus.yongjibus.daytype.controller;

import com.github.dogyungkim.yongjibus.yongjibus.daytype.model.GetDayTypeResponseDTO;
import com.github.dogyungkim.yongjibus.yongjibus.daytype.service.DayTypeService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@AllArgsConstructor
@RestController
@RequestMapping("/day")
public class DayTypeController {

    @Autowired
    DayTypeService dayTypeService;

    @GetMapping
    ResponseEntity<GetDayTypeResponseDTO> getDayType(@RequestParam("date") LocalDate date){
        return ResponseEntity.ok(GetDayTypeResponseDTO.fromEntity(dayTypeService.getDayType(date)));
    }
}
