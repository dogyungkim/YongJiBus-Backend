package com.yongjibus.place.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.global.common.response.SliceResponse;
import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.member.domain.Member;
import com.yongjibus.place.controller.dto.PlaceDTOs.AdminPlace;
import com.yongjibus.place.controller.dto.PlaceDTOs.AdminPlaceRequest;
import com.yongjibus.place.controller.dto.PlaceDTOs.AdminReview;
import com.yongjibus.place.controller.dto.PlaceDTOs.ApprovePlaceRequest;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceImageItem;
import com.yongjibus.place.domain.PlaceStatus;
import com.yongjibus.place.domain.ReviewStatus;
import com.yongjibus.place.service.PlaceService;
import com.yongjibus.place.service.PlaceImageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OPERATOR')")
public class AdminPlaceController {
    private final PlaceService placeService;
    private final PlaceImageService placeImageService;

    @PostMapping("/places")
    public ResponseEntity<YongJiResponse<AdminPlace>> createPlace(
            @Valid @RequestBody AdminPlaceRequest request, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.createApprovedPlace(principal.getMember(), request));
    }

    @GetMapping("/places")
    public ResponseEntity<YongJiResponse<SliceResponse<AdminPlace>>> getPlaces(
            @RequestParam(defaultValue = "PENDING") PlaceStatus status, Pageable pageable) {
        return YongJiResponse.success(SliceResponse.from(placeService.getAdminPlaces(status, pageable)));
    }

    @GetMapping("/reviews")
    public ResponseEntity<YongJiResponse<SliceResponse<AdminReview>>> getReviews(
            @RequestParam(defaultValue = "PENDING") ReviewStatus status, Pageable pageable) {
        return YongJiResponse.success(SliceResponse.from(placeService.getAdminReviews(status, pageable)));
    }

    @PostMapping("/places/{placeId}/approve")
    public ResponseEntity<YongJiResponse<AdminPlace>> approvePlace(
            @PathVariable Long placeId, @Valid @RequestBody ApprovePlaceRequest request,
            @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.approvePlace(placeId, principal.getMember(), request));
    }

    @PostMapping("/places/{placeId}/reject")
    public ResponseEntity<YongJiResponse<AdminPlace>> rejectPlace(
            @PathVariable Long placeId, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.rejectPlace(placeId, principal.getMember()));
    }

    @PostMapping("/places/{placeId}/hide")
    public ResponseEntity<YongJiResponse<AdminPlace>> hidePlace(
            @PathVariable Long placeId, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.hidePlace(placeId, principal.getMember()));
    }

    @PostMapping("/places/{placeId}/restore")
    public ResponseEntity<YongJiResponse<AdminPlace>> restorePlace(
            @PathVariable Long placeId, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.restorePlace(placeId, principal.getMember()));
    }

    @PostMapping(value = "/places/{placeId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<YongJiResponse<List<PlaceImageItem>>> addImages(
            @PathVariable Long placeId, @RequestPart("images") List<MultipartFile> images) {
        return YongJiResponse.success(placeImageService.add(placeId, images));
    }

    @DeleteMapping("/places/{placeId}/images/{imageId}")
    public ResponseEntity<YongJiResponse<String>> deleteImage(
            @PathVariable Long placeId, @PathVariable Long imageId) {
        placeImageService.delete(placeId, imageId);
        return YongJiResponse.success("장소 이미지가 삭제되었습니다.");
    }

    @PostMapping("/reviews/{reviewId}/approve")
    public ResponseEntity<YongJiResponse<AdminReview>> approveReview(
            @PathVariable Long reviewId, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.approveReview(reviewId, principal.getMember()));
    }

    @PostMapping("/reviews/{reviewId}/reject")
    public ResponseEntity<YongJiResponse<AdminReview>> rejectReview(
            @PathVariable Long reviewId, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.rejectReview(reviewId, principal.getMember()));
    }

    @PostMapping("/reviews/{reviewId}/hide")
    public ResponseEntity<YongJiResponse<AdminReview>> hideReview(
            @PathVariable Long reviewId, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.hideReview(reviewId, principal.getMember()));
    }

    @PostMapping("/reviews/{reviewId}/restore")
    public ResponseEntity<YongJiResponse<AdminReview>> restoreReview(
            @PathVariable Long reviewId, @AuthenticationPrincipal MemberDetail principal) {
        return YongJiResponse.success(placeService.restoreReview(reviewId, principal.getMember()));
    }
}
