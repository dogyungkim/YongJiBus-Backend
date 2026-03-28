package com.yongjibus.arrivaltime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.arrivaltime.service.ArrivalTimeService;
import com.yongjibus.global.common.response.YongJiResponse;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import com.yongjibus.arrivaltime.controller.dto.ArrivalTimeListResponseDTO;
import com.yongjibus.arrivaltime.controller.dto.ArrivalTimeResponseDTO;
import com.yongjibus.arrivaltime.controller.dto.GetArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.controller.dto.SaveArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.domain.ArrivalTime;

import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/arrivaltime")
public class ArrivalTimeController {
    private final ArrivalTimeService arrivalTimeService;

    @PostMapping("/save")
    public ResponseEntity<YongJiResponse<String>> saveArrivalTime(@Valid @RequestBody SaveArrivalTimeRequestDTO request) {
        arrivalTimeService.saveArrivalTime(request);
        return YongJiResponse.success("Success");
    }

    @GetMapping("/{date}")
    public ResponseEntity<YongJiResponse<List<ArrivalTimeListResponseDTO>>> getAllArrivalTime(
            @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date){
                
        Map<Integer, List<ArrivalTime>> arrivalTimesGroupedByBusId = arrivalTimeService.getArrivalTimesGroupedByBusId(date);
        
        List<ArrivalTimeListResponseDTO> response = arrivalTimesGroupedByBusId.entrySet().stream()
                .map(entry -> ArrivalTimeListResponseDTO.from(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
        
        return YongJiResponse.success(response);
    }

    @GetMapping("/{date}/{busId}")
    public ResponseEntity<YongJiResponse<List<ArrivalTimeResponseDTO>>> getArrivalTime(
        @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @PathVariable("busId") int busId) {
        GetArrivalTimeRequestDTO request = new GetArrivalTimeRequestDTO(busId, date);
        List<ArrivalTimeResponseDTO> response = arrivalTimeService.getFiveArrivalTimeByBusIdAndDate(request)
            .stream()
            .map(ArrivalTimeResponseDTO::from)
            .toList();
        return YongJiResponse.success(response);
    }
}
