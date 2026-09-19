package com.yongjibus.timetable.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.TimetableException;
import com.yongjibus.timetable.controller.dto.TimetablePayloadDTO;
import com.yongjibus.timetable.controller.dto.TimetableReleaseResponseDTO;
import com.yongjibus.timetable.domain.TimetableRelease;
import com.yongjibus.timetable.repository.TimetableReleaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private final TimetableReleaseRepository timetableReleaseRepository;
    private final ObjectMapper objectMapper;

    public TimetableReleaseResponseDTO findCurrent() {
        TimetableRelease release = timetableReleaseRepository.findTopByOrderByVersionDesc()
                .orElseThrow(() -> new TimetableException(ErrorCode.TIMETABLE_UNAVAILABLE));

        try {
            if (release.getVersion() == null || release.getVersion() < 1) {
                throw new IllegalArgumentException("timetable version is invalid");
            }
            TimetablePayloadDTO timetable = objectMapper.readValue(release.getPayload(), TimetablePayloadDTO.class);
            validate(timetable);
            return new TimetableReleaseResponseDTO(release.getVersion(), timetable);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("시간표 릴리스를 역직렬화하거나 검증하지 못했습니다. version={}", release.getVersion(), e);
            throw new TimetableException(ErrorCode.INVALID_TIMETABLE_RELEASE);
        }
    }

    private void validate(TimetablePayloadDTO timetable) {
        if (timetable == null) {
            throw new IllegalArgumentException("timetable is null");
        }
        validateMyongjiWeekday(timetable.myongjiWeekday());
        validateMyongjiWeekend(timetable.myongjiWeekend());
        validateGiheungWeekday(timetable.giheungWeekday());
    }

    private void validateMyongjiWeekday(List<TimetablePayloadDTO.MyongjiWeekdayTime> rows) {
        Set<Integer> ids = ids(rows, TimetablePayloadDTO.MyongjiWeekdayTime::id);
        for (TimetablePayloadDTO.MyongjiWeekdayTime row : rows) {
            requireId(row.id(), ids);
            requireText(row.type());
            requireTime(row.startTime());
            requireTime(row.predTime());
        }
    }

    private void validateMyongjiWeekend(List<TimetablePayloadDTO.MyongjiWeekendTime> rows) {
        Set<Integer> ids = ids(rows, TimetablePayloadDTO.MyongjiWeekendTime::id);
        for (TimetablePayloadDTO.MyongjiWeekendTime row : rows) {
            requireId(row.id(), ids);
            requireTime(row.startTime());
            requireTime(row.predTime());
        }
    }

    private void validateGiheungWeekday(List<TimetablePayloadDTO.GiheungWeekdayTime> rows) {
        Set<Integer> ids = ids(rows, TimetablePayloadDTO.GiheungWeekdayTime::id);
        for (TimetablePayloadDTO.GiheungWeekdayTime row : rows) {
            requireId(row.id(), ids);
            requireTime(row.startTime());
            requireTime(row.predTime());
            requireTime(row.schoolArrival());
            if (row.runCount() == null || row.runCount() < 1) {
                throw new IllegalArgumentException("runCount must be positive");
            }
        }
    }

    private <T> Set<Integer> ids(List<T> rows, Function<T, Integer> idGetter) {
        if (rows == null) {
            throw new IllegalArgumentException("timetable rows are missing");
        }
        Set<Integer> ids = new HashSet<>();
        for (T row : rows) {
            if (row == null) {
                throw new IllegalArgumentException("timetable row is null");
            }
            Integer id = idGetter.apply(row);
            if (id == null || id < 0 || !ids.add(id)) {
                throw new IllegalArgumentException("timetable row id is invalid");
            }
        }
        return ids;
    }

    private void requireId(Integer id, Set<Integer> ids) {
        if (id == null || id < 0 || !ids.contains(id)) {
            throw new IllegalArgumentException("timetable row id is invalid");
        }
    }

    private void requireText(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("timetable text is invalid");
        }
    }

    private void requireTime(String value) {
        requireText(value);
        try {
            LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("timetable time is invalid");
        }
    }
}
