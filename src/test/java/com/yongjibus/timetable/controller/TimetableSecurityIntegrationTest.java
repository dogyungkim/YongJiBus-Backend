package com.yongjibus.timetable.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TimetableSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM timetable_release");
        jdbcTemplate.update("""
                INSERT INTO timetable_release (payload, published_at) VALUES (?, CURRENT_TIMESTAMP)
                """, """
                {"myongjiWeekday":[{"id":0,"type":"명지대역","startTime":"8:00","predTime":"8:15"}],
                 "myongjiWeekend":[{"id":0,"startTime":"8:20","predTime":"8:45"}],
                 "giheungWeekday":[{"id":0,"startTime":"8:00","predTime":"8:15","schoolArrival":"8:30","runCount":1}]}
                """.replaceAll("\\s+", ""));
    }

    @Test
    void currentTimetableIsPubliclyReadable() throws Exception {
        mockMvc.perform(get("/timetables/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timetable.giheungWeekday[0].runCount").value(1));
    }
}
