package com.yongjibus.arrivaltime.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yongjibus.arrivaltime.domain.ArrivalTime;
import com.yongjibus.arrivaltime.domain.SaveArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.domain.GetArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.repository.ArrivalTimeRepository;
import com.yongjibus.exception.NotFoundException;

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

    public List<ArrivalTime> getAllArrivalTIme() {
        return arrivalTimeRepository.findAll();
    }

    /**
     * 특정 버스의 특정 날짜 도착 시간 정보를 조회하는 메서드
     * 
     * @param dto 조회할 버스 ID와 날짜 정보를 담은 DTO
     * @return 해당하는 도착 시간 정보 목록
     * @throws NotFoundException 해당 버스 ID와 날짜에 대한 도착 시간 정보가 없을 경우
     */
    public List<ArrivalTime> getArrivalTime(GetArrivalTimeRequestDTO dto) {
        return arrivalTimeRepository.findByTimeIdAndDate(dto.busId(), dto.date())
                .orElseThrow(() -> new NotFoundException("실제 버스 도착 시간 정보가 없습니다."));
    }
}
