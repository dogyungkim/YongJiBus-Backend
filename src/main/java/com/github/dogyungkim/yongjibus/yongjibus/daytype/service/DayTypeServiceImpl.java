package com.github.dogyungkim.yongjibus.yongjibus.daytype.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.dogyungkim.yongjibus.yongjibus.daytype.model.DateInfo;
import com.github.dogyungkim.yongjibus.yongjibus.daytype.model.HolidayInfoExteranlResponseDTO;
import com.github.dogyungkim.yongjibus.yongjibus.daytype.repository.DayTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Service
@Slf4j
public class DayTypeServiceImpl implements DayTypeService {

    private final RestClient restClient;
    private final DayTypeRepository dayTypeRepository;

    public DayTypeServiceImpl(RestClient restClient, DayTypeRepository dayTypeRepository) {
        this.restClient = restClient;
        this.dayTypeRepository = dayTypeRepository;
        setHolidayInfo();
    }

    /**
     * 해당 날짜가 공휴일인지 확인하는 메서드
     * @param date 확인할 날짜
     * @return boolean 공휴일 여부
     */
    @Override
    public DateInfo getDayType(LocalDate date) {
        return dayTypeRepository.findByDate(date);
    }

    /**
     * 메모리에 공휴일 정보 저장 하는 메서드
     */
    @Override
    public void setHolidayInfo(){
        dayTypeRepository.setDateData();

        String response = fetchHolidayInfoFromAPI();
        System.out.println("response = " + response);
        XmlMapper xmlMapper = new XmlMapper();

        try{
            HolidayInfoExteranlResponseDTO dto = xmlMapper.readValue(response, HolidayInfoExteranlResponseDTO.class);
            dayTypeRepository.setHolidayData(dto.toEntity());

        } catch (Exception e) {
            log.error("Failed to get holiday data: {}", e.getMessage(), e);
        }
    }

    /**
     * 공공데이터 API를 통해 공휴일 정보를 가져오는 메서드
     * @return List<HolidayInfo> 공휴일 데이터
     */
    private String fetchHolidayInfoFromAPI() {
        LocalDate date = LocalDate.now();
        try {
            byte[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/getRestDeInfo")
                            .queryParam("solYear", Integer.toString(date.getYear()))
                            .queryParam("solMonth", String.format("%02d", 2))
                            .queryParam("serviceKey", "{ServiceKey}")
                            .build())
                    .retrieve()
                    .toEntity(byte[].class)
                    .getBody();

            // UTF-8로 명시적 디코딩
            return new String(response, "UTF-8");

        } catch (Exception e){
            log.error("Failed to fetch holiday data: {}", e.getMessage(), e);
            return null;
        }
    }
}