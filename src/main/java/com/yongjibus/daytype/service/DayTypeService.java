package com.yongjibus.daytype.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.yongjibus.daytype.model.DateInfo;
import com.yongjibus.daytype.model.HolidayInfoExternalResponseDTO;
import com.yongjibus.daytype.repository.DayTypeRepository;
import com.yongjibus.daytype.client.HolidayApiClient;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@AllArgsConstructor
@Service
@Slf4j
public class DayTypeService {

    private final HolidayApiClient holidayApiClient;
    private final DayTypeRepository dayTypeRepository;

    @PostConstruct
    private void init(){
        loadHolidayInfo();
    }

    /**
     * 해당 날짜가 공휴일인지 확인하는 메서드
     * @param date 확인할 날짜
     * @return boolean 공휴일 여부
     */
    public DateInfo findDayInfo(LocalDate date) {
        return dayTypeRepository.findByDate(date);
    }

    /**
     * 메모리에 공휴일 정보 저장 하는 메서드
     */
    public void loadHolidayInfo(){
        
        String response = fetchHolidayInfoFromAPI();
        
        try {
            HolidayInfoExternalResponseDTO dto = parseHolidayXmlResponse(response);
            dayTypeRepository.setHolidayData(dto.toEntity());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get holiday data" + e.getMessage());
        }
    }

    /**
     * 공공데이터 API를 통해 공휴일 정보를 가져오는 메서드
     * @return String 공휴일 데이터
     */
    private String fetchHolidayInfoFromAPI() {
        LocalDate currentDate = LocalDate.now();
        return holidayApiClient.fetchHolidayInfo(currentDate);
    }

    /**
     * XML 형식의 공휴일 데이터를 파싱하는 메서드
     * @param response API로부터 받은 XML 형식의 응답 데이터
     * @return HolidayInfoExternalResponseDTO 파싱된 공휴일 정보 DTO
     * @throws JsonMappingException XML 매핑 실패시 발생
     * @throws JsonProcessingException JSON 처리 실패시 발생
     */
    private HolidayInfoExternalResponseDTO parseHolidayXmlResponse(String response) throws JsonMappingException, JsonProcessingException{
        XmlMapper xmlMapper = new XmlMapper();
        return xmlMapper.readValue(response, HolidayInfoExternalResponseDTO.class);
    }
}