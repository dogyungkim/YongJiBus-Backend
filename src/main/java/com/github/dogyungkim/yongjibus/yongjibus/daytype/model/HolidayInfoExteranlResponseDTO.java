package com.github.dogyungkim.yongjibus.yongjibus.daytype.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@JacksonXmlRootElement(localName = "response")
public class HolidayInfoExteranlResponseDTO {
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
