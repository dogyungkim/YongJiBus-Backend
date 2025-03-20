package com.yongjibus.daytype.controller.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.yongjibus.daytype.domain.DateInfo;

import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


/**
 * 공공데이터 포털의 휴일 정보 API 응답을 매핑하기 위한 DTO 클래스들
 * XML 형식의 응답을 Java 객체로 변환하여 처리
 */

@Data
@JacksonXmlRootElement(localName = "response")
public class HolidayInfoExternalResponseDTO {
    private HolidayInfoResponseHeader header;
    private HolidayInfoResponseBody body;

    public List<DateInfo> toEntity(){
        return body.toEntity();
    }
}

@Data
@JacksonXmlRootElement(localName = "header")
class  HolidayInfoResponseHeader {
    private String resultCode;
    private String resultMsg;
}

@Data
@JacksonXmlRootElement(localName = "body")
class HolidayInfoResponseBody {
    @JacksonXmlElementWrapper(localName = "items")
    @JacksonXmlProperty(localName = "item")
    private List<HolidayInfoResponseItem> items;

    private int numOfRows;
    private int pageNo;
    private int totalCount;

    public List<DateInfo> toEntity(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return items.stream().map( e ->
          new DateInfo(
                  LocalDate.parse(e.getLocalDate(), formatter),
                  true,
                  e.getDateName())
        ).toList();
    }
}

@Data
@JacksonXmlRootElement(localName = "Item")
class HolidayInfoResponseItem {
    private String dateKind;
    private String dateName;
    private String isHoliday;
    @JacksonXmlProperty(localName = "locdate")
    private String localDate;
    private int seq;
}
