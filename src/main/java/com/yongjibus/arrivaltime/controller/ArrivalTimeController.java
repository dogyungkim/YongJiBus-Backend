package com.yongjibus.arrivaltime.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.arrivaltime.domain.SaveArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.service.ArrivalTimeService;
import com.yongjibus.arrivaltime.domain.GetArrivalTimeRequestDTO;
import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.yongjibus.arrivaltime.domain.ArrivalTimeResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/arrivaltime")
public class ArrivalTimeController {
    private final ArrivalTimeService arrivalTimeService;

    @PostMapping("/save")
    public ResponseEntity<String> saveArrivalTime(@RequestBody SaveArrivalTimeRequestDTO request) {
        arrivalTimeService.saveArrivalTime(request);
        //return ResponseEntity.ok("Success");
        return ResponseEntity.status(HttpStatusCode.valueOf(200)).body("Success");
    }

    @GetMapping("/{date}")
    public ResponseEntity<List<ArrivalTimeResponseDTO>> getAllArrivalTime(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date){
        List<ArrivalTimeResponseDTO> response = arrivalTimeService.getAllArrivalTIme()
                .stream()
                .map(ArrivalTimeResponseDTO::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{date}/{busId}")
    public ResponseEntity<List<ArrivalTimeResponseDTO>> getArrivalTime(
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @PathVariable int busId) {
            
        GetArrivalTimeRequestDTO request = new GetArrivalTimeRequestDTO(busId, date);
        List<ArrivalTimeResponseDTO> response = arrivalTimeService.getArrivalTime(request)
            .stream()
            .map(ArrivalTimeResponseDTO::from)
            .toList();
        return ResponseEntity.ok(response);
    }
}