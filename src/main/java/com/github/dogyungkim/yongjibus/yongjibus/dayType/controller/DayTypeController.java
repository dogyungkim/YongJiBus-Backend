package com.github.dogyungkim.yongjibus.yongjibus.dayType.controller;

import com.github.dogyungkim.yongjibus.yongjibus.dayType.service.DayTypeService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;

@AllArgsConstructor
@RestController
@RequestMapping("/day")
public class DayTypeController {

    @Autowired
    DayTypeService dayTypeService;

    @GetMapping
    ResponseEntity<Boolean> getDayType(@RequestParam("date") LocalDate date){
        System.out.println("date = " + date);
        return ResponseEntity.ok(dayTypeService.getDayType(date));
    }
}
