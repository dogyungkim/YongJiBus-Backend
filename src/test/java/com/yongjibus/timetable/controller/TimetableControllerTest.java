package com.yongjibus.timetable.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.yongjibus.timetable.controller.dto.TimetablePayloadDTO;
import com.yongjibus.timetable.controller.dto.TimetableReleaseResponseDTO;
import com.yongjibus.timetable.service.TimetableService;
import com.yongjibus.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
class TimetableControllerTest extends ControllerTestSupport {

    @Mock
    private TimetableService timetableService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new TimetableController(timetableService));
        when(timetableService.findCurrent()).thenReturn(new TimetableReleaseResponseDTO(
                8L,
                new TimetablePayloadDTO(
                        List.of(new TimetablePayloadDTO.MyongjiWeekdayTime(0, "명지대역", "8:00", "8:15")),
                        List.of(new TimetablePayloadDTO.MyongjiWeekendTime(0, "8:20", "8:45")),
                        List.of(new TimetablePayloadDTO.GiheungWeekdayTime(0, "8:00", "8:15", "8:30", 1)))));
    }

    @Test
    void returnsTheCurrentReleaseWithAStrongEtag() throws Exception {
        mockMvc.perform(get("/timetables/current"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"8\""))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.version").value(8))
                .andExpect(jsonPath("$.data.timetable.myongjiWeekday[0].predTime").value("8:15"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"W/\"8\"", "\"7\", \"8\"", "*"})
    void returnsAnEmpty304ForAStandardMatchingEtag(String ifNoneMatch) throws Exception {
        mockMvc.perform(get("/timetables/current").header("If-None-Match", ifNoneMatch))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"8\""))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(content().string(""));
    }

    @Test
    void returnsTheBodyForAnOldEtag() throws Exception {
        mockMvc.perform(get("/timetables/current").header("If-None-Match", "\"7\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(8));
    }
}
