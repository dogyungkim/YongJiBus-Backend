package com.yongjibus.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.TimetableException;
import com.yongjibus.timetable.controller.dto.TimetablePayloadDTO;
import com.yongjibus.timetable.controller.dto.TimetableReleaseResponseDTO;
import com.yongjibus.timetable.domain.TimetableRelease;
import com.yongjibus.timetable.repository.TimetableReleaseRepository;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TimetableReleaseRepository timetableReleaseRepository;

    private ObjectMapper objectMapper;
    private TimetableService timetableService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        timetableService = new TimetableService(timetableReleaseRepository, objectMapper);
    }

    @Test
    void returnsTheLatestReleaseAfterValidatingItsPayload() throws Exception {
        TimetablePayloadDTO expected = validPayload();
        TimetableRelease release = release(8L, objectMapper.writeValueAsString(expected));
        when(timetableReleaseRepository.findTopByOrderByVersionDesc())
                .thenReturn(Optional.of(release));

        TimetableReleaseResponseDTO actual = timetableService.findCurrent();

        assertThat(actual).isEqualTo(new TimetableReleaseResponseDTO(8L, expected));
    }

    @Test
    void validatesTheEntireInitialReleasePayload() throws Exception {
        givenRelease(1L, initialPayload());

        TimetableReleaseResponseDTO actual = timetableService.findCurrent();

        assertThat(actual.timetable().myongjiWeekday()).hasSize(64);
        assertThat(actual.timetable().myongjiWeekend()).hasSize(10);
        assertThat(actual.timetable().giheungWeekday()).hasSize(14);
    }

    @Test
    void rejectsAReleaseWithDuplicateIds() throws Exception {
        TimetablePayloadDTO invalid = new TimetablePayloadDTO(
                List.of(
                        new TimetablePayloadDTO.MyongjiWeekdayTime(0, "명지대역", "8:00", "8:15"),
                        new TimetablePayloadDTO.MyongjiWeekdayTime(0, "시내", "8:05", "8:20")),
                validPayload().myongjiWeekend(),
                validPayload().giheungWeekday());
        givenRelease(invalid);

        assertInvalidRelease();
    }

    @Test
    void rejectsAnInvalidTime() throws Exception {
        TimetablePayloadDTO invalid = new TimetablePayloadDTO(
                List.of(new TimetablePayloadDTO.MyongjiWeekdayTime(0, "명지대역", "24:00", "8:15")),
                validPayload().myongjiWeekend(),
                validPayload().giheungWeekday());
        givenRelease(invalid);

        assertInvalidRelease();
    }

    @Test
    void rejectsANonPositiveRunCount() throws Exception {
        TimetablePayloadDTO invalid = new TimetablePayloadDTO(
                validPayload().myongjiWeekday(),
                validPayload().myongjiWeekend(),
                List.of(new TimetablePayloadDTO.GiheungWeekdayTime(0, "8:00", "8:15", "8:30", 0)));
        givenRelease(invalid);

        assertInvalidRelease();
    }

    @Test
    void returnsServiceUnavailableWhenNoReleaseExists() {
        when(timetableReleaseRepository.findTopByOrderByVersionDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(timetableService::findCurrent)
                .isInstanceOf(TimetableException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TIMETABLE_UNAVAILABLE);
    }

    @Test
    void rejectsMalformedJsonAsAnInvalidRelease() {
        TimetableRelease release = release(8L, "{\"myongjiWeekday\":");
        when(timetableReleaseRepository.findTopByOrderByVersionDesc())
                .thenReturn(Optional.of(release));

        assertInvalidRelease();
    }

    private void givenRelease(TimetablePayloadDTO payload) throws Exception {
        givenRelease(8L, objectMapper.writeValueAsString(payload));
    }

    private void givenRelease(long version, String payload) {
        TimetableRelease release = release(version, payload);
        when(timetableReleaseRepository.findTopByOrderByVersionDesc())
                .thenReturn(Optional.of(release));
    }

    private String initialPayload() throws Exception {
        String resource = "/db/migration/V6__add_timetable_release.sql";
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(resource))) {
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String prefix = "INSERT INTO timetable_release (payload) VALUES ('";
            int payloadStart = sql.indexOf(prefix);
            int payloadEnd = sql.lastIndexOf("');");
            assertThat(payloadStart).isGreaterThanOrEqualTo(0);
            assertThat(payloadEnd).isGreaterThan(payloadStart);
            return sql.substring(payloadStart + prefix.length(), payloadEnd);
        }
    }

    private void assertInvalidRelease() {
        assertThatThrownBy(timetableService::findCurrent)
                .isInstanceOf(TimetableException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TIMETABLE_RELEASE);
    }

    private TimetableRelease release(long version, String payload) {
        TimetableRelease release = mock(TimetableRelease.class);
        when(release.getVersion()).thenReturn(version);
        when(release.getPayload()).thenReturn(payload);
        return release;
    }

    private TimetablePayloadDTO validPayload() {
        return new TimetablePayloadDTO(
                List.of(new TimetablePayloadDTO.MyongjiWeekdayTime(0, "명지대역", "8:00", "8:15")),
                List.of(new TimetablePayloadDTO.MyongjiWeekendTime(0, "8:20", "8:45")),
                List.of(new TimetablePayloadDTO.GiheungWeekdayTime(0, "-", "8:15", "8:30", 1)));
    }
}
