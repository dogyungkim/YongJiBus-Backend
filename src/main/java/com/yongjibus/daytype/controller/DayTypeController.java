package com.yongjibus.daytype.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.yongjibus.daytype.controller.dto.GetDayTypeResponseDTO;
import com.yongjibus.daytype.service.DayTypeService;
import com.yongjibus.global.common.response.YongJiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@AllArgsConstructor
@RestController
@RequestMapping("/day")
@Tag(name = "날짜 정보 API", description = "특정 날짜의 유형 정보를 조회하는 API")
public class DayTypeController {

    private final DayTypeService dayTypeService;

    @Operation(
        summary = "날짜 유형 조회", 
        description = "지정된 날짜의 유형 정보(공휴일, 평일 등)를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "날짜 정보 조회 성공",
            content = @Content(schema = @Schema(implementation = GetDayTypeResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 날짜 형식", 
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "날짜 정보를 찾을 수 없음", 
            content = @Content
        )
    })
    @GetMapping
    ResponseEntity<YongJiResponse<GetDayTypeResponseDTO>> getDayType(
        @Parameter(
            description = "조회할 날짜 (YYYY-MM-DD 형식)", 
            required = true, 
            example = "2023-12-25"
        )
        @RequestParam("date") LocalDate date
    ){
        return YongJiResponse.success(GetDayTypeResponseDTO.fromEntity(dayTypeService.findDayInfo(date)));
    }
}
