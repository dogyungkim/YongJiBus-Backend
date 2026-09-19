package com.yongjibus.timetable.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.timetable.controller.dto.TimetableReleaseResponseDTO;
import com.yongjibus.timetable.service.TimetableService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping("/current")
    public ResponseEntity<YongJiResponse<TimetableReleaseResponseDTO>> getCurrent(
            WebRequest request) {
        TimetableReleaseResponseDTO current = timetableService.findCurrent();
        String etag = "\"" + current.version() + "\"";
        HttpHeaders headers = new HttpHeaders();
        headers.setETag(etag);
        headers.setCacheControl(CacheControl.noCache());

        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        // Spring handles weak/list validators; wildcard is explicit because the current
        // WebRequest implementation does not treat "*" as a GET match.
        if ("*".equals(ifNoneMatch == null ? null : ifNoneMatch.trim()) || request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).headers(headers).build();
        }
        return ResponseEntity.ok()
                .headers(headers)
                .body(new YongJiResponse<>(HttpStatus.OK.value(), current));
    }
}
