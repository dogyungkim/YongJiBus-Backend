package com.yongjibus.place.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.global.common.response.SliceResponse;
import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.member.domain.Member;
import com.yongjibus.place.controller.dto.PlaceDTOs.KakaoSearchItem;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceDetail;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceRequest;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceRequestResult;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceReviewItem;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceSummary;
import com.yongjibus.place.controller.dto.PlaceDTOs.ReviewRequest;
import com.yongjibus.place.service.PlaceImageService;
import com.yongjibus.place.service.PlaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceService placeService;
    private final PlaceImageService placeImageService;

    @GetMapping
    public ResponseEntity<YongJiResponse<List<PlaceSummary>>> getPlaces() {
        return YongJiResponse.success(placeService.getApprovedPlaces());
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<YongJiResponse<PlaceDetail>> getPlace(
            @PathVariable Long placeId, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.getPlace(placeId, member(principal)));
    }

    @GetMapping("/{placeId}/reviews")
    public ResponseEntity<YongJiResponse<SliceResponse<PlaceReviewItem>>> getReviews(
            @PathVariable Long placeId, Pageable pageable, @AuthenticationPrincipal MemberDetail principal) {
        Slice<PlaceReviewItem> reviews = placeService.getReviews(placeId, member(principal), pageable);
        return YongJiResponse.success(SliceResponse.from(reviews));
    }

    @GetMapping("/kakao-search")
    public ResponseEntity<YongJiResponse<List<KakaoSearchItem>>> search(
            @RequestParam String query, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.searchKakao(principal.getMember(), query));
    }

    @GetMapping("/kakao-viewport")
    public ResponseEntity<YongJiResponse<List<KakaoSearchItem>>> searchViewport(
            @RequestParam double minLatitude, @RequestParam double minLongitude,
            @RequestParam double maxLatitude, @RequestParam double maxLongitude,
            @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.searchViewport(principal.getMember(),
                minLatitude, minLongitude, maxLatitude, maxLongitude));
    }

    @PostMapping(value = "/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<YongJiResponse<PlaceRequestResult>> requestPlace(
            @Valid @RequestBody PlaceRequest request, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.requestPlace(principal.getMember(), request));
    }

    @PostMapping(value = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<YongJiResponse<PlaceRequestResult>> requestPlaceWithImages(
            @Valid @RequestPart("request") PlaceRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal MemberDetail principal) {
        PlaceRequestResult result = placeService.requestPlace(principal.getMember(), request);
        if (images != null && !images.isEmpty()) {
            placeImageService.addForRequest(result.placeId(), images);
        }
        return YongJiResponse.success(result);
    }

    @PutMapping("/{placeId}/reviews/me")
    public ResponseEntity<YongJiResponse<PlaceRequestResult>> saveMyReview(
            @PathVariable Long placeId, @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.saveMyReview(placeId, principal.getMember(), request));
    }

    @DeleteMapping("/{placeId}/reviews/me")
    public ResponseEntity<YongJiResponse<String>> deleteMyReview(
            @PathVariable Long placeId, @AuthenticationPrincipal MemberDetail principal) {
        placeService.deleteMyReview(placeId, principal.getMember());
        return YongJiResponse.success("평가가 삭제되었습니다.");
    }

    private static Member member(MemberDetail principal) {
        return principal == null ? null : principal.getMember();
    }
}
