package com.yongjibus.arrivaltime.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.yongjibus.arrivaltime.controller.dto.GetArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.controller.dto.SaveArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.domain.ArrivalTime;
import com.yongjibus.arrivaltime.repository.ArrivalTimeRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.DateInfoNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@RequiredArgsConstructor
@Service
@Slf4j
public class ArrivalTimeService {
    private final ArrivalTimeRepository arrivalTimeRepository;

    /**
     * 버스 도착 시간 정보를 저장하는 메서드
     * 
     * @param dto 저장할 버스 도착 시간 정보를 담은 DTO
     */
    public void saveArrivalTime(SaveArrivalTimeRequestDTO dto) {
        log.info("saveArrivalTime: {}", dto);
        arrivalTimeRepository.save(ArrivalTime.builder()
            .timeId(dto.busId())
            .date(dto.date())
            .dayOfWeek(dto.date().getDayOfWeek().toString())
            .time(dto.time())
            .isHoliday(dto.isHoliday())
            .build());
    }

    /**
     * 특정 날짜의 특정 버스의 도착 시간 정보를 조회하는 메서드
     * 
     * @param dto 조회할 버스 ID와 날짜 정보를 담은 DTO
     * @return 해당하는 도착 시간 정보 목록
     * @throws DateInfoNotFoundException 해당 버스 ID와 날짜에 대한 도착 시간 정보가 없을 경우
     */
    public List<ArrivalTime> getArrivalTimeByBusIdAndDate(GetArrivalTimeRequestDTO dto) {
        List<ArrivalTime> arrivalTimes = arrivalTimeRepository.findByTimeIdAndDate(dto.busId(), dto.date());
        log.info("arrivalTimes: {}", arrivalTimes);
        if (arrivalTimes.isEmpty()) {
            throw new DateInfoNotFoundException(ErrorCode.DATE_INFO_NOT_FOUND, dto.date().toString());
        }
        return arrivalTimes;
    }

    /**
     * 특정 버스 ID와 날짜에 대한 최근 5개의 도착 시간 정보를 조회하는 메서드
     * 
     * @param dto 조회할 버스 ID와 날짜 정보를 담은 DTO
     * @return 해당하는 최근 5개의 도착 시간 정보 목록
     * @throws DateInfoNotFoundException 해당 버스 ID와 날짜에 대한 도착 시간 정보가 없을 경우
     */
    public List<ArrivalTime> getFiveArrivalTimeByBusIdAndDate(GetArrivalTimeRequestDTO dto) {
        List<ArrivalTime> arrivalTimes = arrivalTimeRepository.findTop5ByTimeIdAndDateOrderByTimeDesc(dto.busId(), dto.date());
        if (arrivalTimes.isEmpty()) {
            throw new DateInfoNotFoundException(ErrorCode.DATE_INFO_NOT_FOUND, dto.date().toString());
        }
        return arrivalTimes.stream().limit(5).toList();
    }

    /**
     * 특정 날짜의 모든 버스 도착 시간 정보를 버스 ID별로 그룹화하여 조회하는 메서드
     * 
     * @param date 조회할 날짜
     * @return 해당 날짜의 모든 도착 시간 정보를 버스 ID별로 그룹화한 맵
     * @throws DateInfoNotFoundException 해당 날짜에 대한 도착 시간 정보가 없을 경우
     */
    public Map<Integer, List<ArrivalTime>> getArrivalTimesGroupedByBusId(LocalDate date) {
        List<ArrivalTime> arrivalTimes = arrivalTimeRepository.findByDate(date);
        if (arrivalTimes.isEmpty()) {
            throw new DateInfoNotFoundException(ErrorCode.DATE_INFO_NOT_FOUND, date.toString());
        }
        return arrivalTimes.stream()
                .collect(Collectors.groupingBy(ArrivalTime::getTimeId));
    }
}
