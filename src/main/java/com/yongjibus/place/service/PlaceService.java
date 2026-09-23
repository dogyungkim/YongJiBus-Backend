package com.yongjibus.place.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.place.client.JusoAddressClient;
import com.yongjibus.place.client.JusoAddressClient.ResolvedAddress;
import com.yongjibus.place.client.KakaoLocalClient;
import com.yongjibus.place.client.KakaoLocalClient.KakaoPlace;
import com.yongjibus.place.controller.dto.PlaceDTOs.AdminPlace;
import com.yongjibus.place.controller.dto.PlaceDTOs.AdminPlaceRequest;
import com.yongjibus.place.controller.dto.PlaceDTOs.AdminReview;
import com.yongjibus.place.controller.dto.PlaceDTOs.ApprovePlaceRequest;
import com.yongjibus.place.controller.dto.PlaceDTOs.KakaoSearchItem;
import com.yongjibus.place.controller.dto.PlaceDTOs.MyReview;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceDetail;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceImageItem;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceRequest;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceRequestResult;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceReviewItem;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceSummary;
import com.yongjibus.place.controller.dto.PlaceDTOs.ReviewRequest;
import com.yongjibus.place.domain.Place;
import com.yongjibus.place.domain.PlaceReview;
import com.yongjibus.place.domain.PlaceStatus;
import com.yongjibus.place.domain.ReviewStatus;
import com.yongjibus.place.repository.PlaceRepository;
import com.yongjibus.place.repository.PlaceRepository.PlaceSummaryProjection;
import com.yongjibus.place.repository.PlaceReviewRepository;
import com.yongjibus.place.service.PlaceSelectionProofService.SelectedPlace;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository reviewRepository;
    private final KakaoLocalClient kakaoLocalClient;
    private final JusoAddressClient jusoAddressClient;
    private final PlaceSelectionProofService proofService;
    private final PlaceImageService imageService;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;
    private final Cache<Long, AtomicInteger> searchCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    @Transactional(readOnly = true)
    public List<PlaceSummary> getApprovedPlaces() {
        try {
            return approvedSummaries();
        } catch (RuntimeException e) {
            count("nearby.place.public.failure");
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PlaceDetail getPlace(Long placeId, Member member) {
        PlaceSummaryProjection projection = placeRepository.findApprovedSummaries().stream()
                .filter(item -> item.getId().equals(placeId))
                .findFirst()
                .orElseThrow(() -> new PlaceException(ErrorCode.PLACE_NOT_FOUND));
        List<PlaceImageItem> images = imageService.findByPlaceId(placeId);
        PlaceSummary summary = PlaceSummary.from(projection,
                images.isEmpty() ? null : images.get(0).thumbnailUrl());
        MyReview myReview = member == null ? null : reviewRepository.findByPlaceIdAndMemberId(placeId, member.getId())
                .map(MyReview::from)
                .orElse(null);
        List<String> imageUrls = images.stream()
                .map(PlaceImageItem::imageUrl)
                .toList();
        return PlaceDetail.of(summary, imageUrls, myReview);
    }

    @Transactional(readOnly = true)
    public Slice<PlaceReviewItem> getReviews(Long placeId, Member member, Pageable pageable) {
        approvedPlace(placeId);
        Long memberId = member == null ? null : member.getId();
        return reviewRepository.findByPlaceIdAndStatusOrderByCreatedAtDescIdDesc(
                placeId, ReviewStatus.APPROVED, limited(pageable)).map(review -> PlaceReviewItem.from(review, memberId));
    }

    public List<KakaoSearchItem> searchKakao(Member member, String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 2 || query.length() > 50) {
            throw new PlaceException(ErrorCode.INVALID_REQUEST);
        }
        if (searchCounts.asMap().computeIfAbsent(member.getId(), ignored -> new AtomicInteger()).incrementAndGet() > 10) {
            count("nearby.kakao.search.rate_limited");
            throw new PlaceException(ErrorCode.PLACE_SEARCH_RATE_LIMITED);
        }
        try {
            List<KakaoPlace> results = kakaoLocalClient.search(query);
            Map<String, Place> registered = results.isEmpty() ? Map.of() : placeRepository.findByKakaoPlaceIdIn(
                    results.stream().map(KakaoPlace::id).toList()).stream()
                    .collect(Collectors.toMap(Place::getKakaoPlaceId, place -> place));
            List<KakaoSearchItem> response = results.stream().map(result -> {
                Place place = registered.get(result.id());
                return new KakaoSearchItem(result.id(), result.placeName(), result.categoryName(),
                        result.roadAddressName(), result.addressName(), result.latitudeValue(), result.longitudeValue(),
                        result.placeUrl(), place == null ? null : place.getId(), publicRegistrationStatus(place),
                        proofService.issue(member.getId(), result));
            }).toList();
            count("nearby.kakao.search.success");
            return response;
        } catch (RuntimeException e) {
            count("nearby.kakao.search.failure");
            throw e;
        }
    }

    public PlaceRequestResult requestPlace(Member member, PlaceRequest request) {
        SelectedPlace selected = proofService.verify(request.selectionProof(), member.getId());
        Place existing = placeRepository.findByKakaoPlaceId(selected.placeId()).orElse(null);
        ensureRequestable(existing);
        ResolvedAddress resolved = existing == null ? resolve(selected) : null;

        try {
            PlaceRequestResult result = inTransaction(() -> persistRequest(member, request, selected, resolved));
            count("nearby.place.request.success");
            return result;
        } catch (DataIntegrityViolationException first) {
            if (!knownRequestCollision(first)) {
                throw first;
            }
            try {
                return inTransaction(() -> persistRequest(member, request, selected, resolved));
            } catch (DataIntegrityViolationException second) {
                if (knownRequestCollision(second)) {
                    throw new PlaceException(ErrorCode.PLACE_REQUEST_CONFLICT);
                }
                throw second;
            }
        }
    }

    @Transactional
    public PlaceRequestResult saveMyReview(Long placeId, Member member, ReviewRequest request) {
        Place place = approvedPlace(placeId);
        PlaceReview review = reviewRepository.findByPlaceIdAndMemberId(placeId, member.getId())
                .map(existing -> {
                    existing.updateByMember(request.rating(), request.comment());
                    return existing;
                })
                .orElseGet(() -> PlaceReview.pending(place, member, request.rating(), request.comment()));
        reviewRepository.save(review);
        count("nearby.review.request.success");
        return new PlaceRequestResult(place.getId(), place.getStatus(), review.getStatus());
    }

    @Transactional
    public void deleteMyReview(Long placeId, Member member) {
        PlaceReview review = reviewRepository.findByPlaceIdAndMemberId(placeId, member.getId())
                .orElseThrow(() -> new PlaceException(ErrorCode.REVIEW_NOT_FOUND));
        reviewRepository.delete(review);
    }

    public AdminPlace createApprovedPlace(Member operator, AdminPlaceRequest request) {
        SelectedPlace selected = proofService.verify(request.selectionProof(), operator.getId());
        if (placeRepository.findByKakaoPlaceId(selected.placeId()).isPresent()) {
            throw new PlaceException(ErrorCode.PLACE_ALREADY_EXISTS);
        }
        ResolvedAddress resolved = resolve(selected);
        try {
            return inTransaction(() -> {
                if (placeRepository.findByKakaoPlaceId(selected.placeId()).isPresent()) {
                    throw new PlaceException(ErrorCode.PLACE_ALREADY_EXISTS);
                }
                Place place = Place.approved(request.displayName(), resolved.addressText(), resolved.latitude(),
                        resolved.longitude(), resolved.buildingManagementNumber(), request.category(),
                        request.subcategory(), selected.placeId(), selected.placeUrl(), operator);
                return AdminPlace.from(placeRepository.saveAndFlush(place), List.of());
            });
        } catch (DataIntegrityViolationException e) {
            if (messageChain(e).contains("uk_place_kakao_place_id")) {
                throw new PlaceException(ErrorCode.PLACE_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Slice<AdminPlace> getAdminPlaces(PlaceStatus status, Pageable pageable) {
        Slice<Place> places = placeRepository.findByStatusOrderByCreatedAtDescIdDesc(status, limited(pageable));
        Map<Long, List<PlaceImageItem>> images = imageService.findByPlaceIds(
                places.getContent().stream().map(Place::getId).toList());
        return places.map(place -> AdminPlace.from(place, images.getOrDefault(place.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public Slice<AdminReview> getAdminReviews(ReviewStatus status, Pageable pageable) {
        return reviewRepository.findByStatusOrderByCreatedAtDescIdDesc(status, limited(pageable)).map(AdminReview::from);
    }

    @Transactional
    public AdminPlace approvePlace(Long placeId, Member operator, ApprovePlaceRequest request) {
        Place place = place(placeId);
        PlaceStatus previous = place.getStatus();
        place.approve(operator, request.displayName(), request.category(), request.subcategory());
        logTransition("place", placeId, previous, place.getStatus(), operator.getId());
        count("nearby.place.approve");
        return adminPlace(place);
    }

    @Transactional
    public AdminPlace rejectPlace(Long placeId, Member operator) {
        Place place = place(placeId);
        PlaceStatus previous = place.getStatus();
        place.reject();
        reviewRepository.rejectPendingByPlaceId(placeId);
        logTransition("place", placeId, previous, place.getStatus(), operator.getId());
        count("nearby.place.reject");
        return adminPlace(place);
    }

    @Transactional
    public AdminPlace hidePlace(Long placeId, Member operator) {
        Place place = place(placeId);
        PlaceStatus previous = place.getStatus();
        place.hide();
        logTransition("place", placeId, previous, place.getStatus(), operator.getId());
        return adminPlace(place);
    }

    @Transactional
    public AdminPlace restorePlace(Long placeId, Member operator) {
        Place place = place(placeId);
        PlaceStatus previous = place.getStatus();
        place.restore(operator);
        logTransition("place", placeId, previous, place.getStatus(), operator.getId());
        return adminPlace(place);
    }

    @Transactional
    public AdminReview approveReview(Long reviewId, Member operator) {
        PlaceReview review = review(reviewId);
        ReviewStatus previous = review.getStatus();
        review.approve(operator);
        logTransition("review", reviewId, previous, review.getStatus(), operator.getId());
        count("nearby.review.approve");
        return AdminReview.from(review);
    }

    @Transactional
    public AdminReview rejectReview(Long reviewId, Member operator) {
        PlaceReview review = review(reviewId);
        ReviewStatus previous = review.getStatus();
        review.reject();
        logTransition("review", reviewId, previous, review.getStatus(), operator.getId());
        count("nearby.review.reject");
        return AdminReview.from(review);
    }

    @Transactional
    public AdminReview hideReview(Long reviewId, Member operator) {
        PlaceReview review = review(reviewId);
        ReviewStatus previous = review.getStatus();
        review.hide();
        logTransition("review", reviewId, previous, review.getStatus(), operator.getId());
        return AdminReview.from(review);
    }

    @Transactional
    public AdminReview restoreReview(Long reviewId, Member operator) {
        PlaceReview review = review(reviewId);
        ReviewStatus previous = review.getStatus();
        review.restore(operator);
        logTransition("review", reviewId, previous, review.getStatus(), operator.getId());
        return AdminReview.from(review);
    }

    private PlaceRequestResult persistRequest(Member member, PlaceRequest request, SelectedPlace selected,
            ResolvedAddress resolved) {
        Place place = placeRepository.findByKakaoPlaceId(selected.placeId()).orElse(null);
        ensureRequestable(place);
        if (place == null) {
            if (resolved == null) {
                throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
            }
            place = Place.pending(request.displayName(), resolved.addressText(), resolved.latitude(),
                    resolved.longitude(), resolved.buildingManagementNumber(), request.category(), request.subcategory(),
                    selected.placeId(), selected.placeUrl(), member);
            placeRepository.saveAndFlush(place);
        }
        Place persistedPlace = place;
        PlaceReview review = reviewRepository.findByPlaceIdAndMemberId(place.getId(), member.getId())
                .map(existing -> {
                    existing.updateByMember(request.rating(), request.comment());
                    return existing;
                })
                .orElseGet(() -> PlaceReview.pending(persistedPlace, member, request.rating(), request.comment()));
        reviewRepository.saveAndFlush(review);
        return new PlaceRequestResult(place.getId(), place.getStatus(), review.getStatus());
    }

    private ResolvedAddress resolve(SelectedPlace selected) {
        try {
            ResolvedAddress resolved = jusoAddressClient.resolve(selected.roadAddress(), selected.jibunAddress());
            count("nearby.juso.resolve.success");
            return resolved;
        } catch (RuntimeException e) {
            count("nearby.juso.resolve.failure");
            throw e;
        }
    }

    private static void ensureRequestable(Place place) {
        if (place != null && (place.getStatus() == PlaceStatus.REJECTED || place.getStatus() == PlaceStatus.HIDDEN)) {
            throw new PlaceException(ErrorCode.PLACE_REQUEST_NOT_ALLOWED);
        }
    }

    private Place approvedPlace(Long id) {
        Place place = place(id);
        place.requireApproved();
        return place;
    }

    private Place place(Long id) {
        return placeRepository.findById(id).orElseThrow(() -> new PlaceException(ErrorCode.PLACE_NOT_FOUND));
    }

    private PlaceReview review(Long id) {
        return reviewRepository.findById(id).orElseThrow(() -> new PlaceException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private List<PlaceSummary> approvedSummaries() {
        List<PlaceSummaryProjection> summaries = placeRepository.findApprovedSummaries();
        Map<Long, List<PlaceImageItem>> images = imageService.findByPlaceIds(
                summaries.stream().map(PlaceSummaryProjection::getId).toList());
        return summaries.stream().map(summary -> {
            List<PlaceImageItem> placeImages = images.getOrDefault(summary.getId(), List.of());
            String thumbnailUrl = placeImages.isEmpty() ? null : placeImages.get(0).thumbnailUrl();
            return PlaceSummary.from(summary, thumbnailUrl);
        }).toList();
    }

    private AdminPlace adminPlace(Place place) {
        return AdminPlace.from(place, imageService.findByPlaceId(place.getId()));
    }

    private <T> T inTransaction(Supplier<T> supplier) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> supplier.get()));
    }

    private static Pageable limited(Pageable pageable) {
        return PageRequest.of(Math.max(0, pageable.getPageNumber()), Math.min(50, pageable.getPageSize()));
    }

    private static String publicRegistrationStatus(Place place) {
        if (place == null) {
            return null;
        }
        return switch (place.getStatus()) {
            case PENDING -> "PENDING";
            case APPROVED -> "APPROVED";
            case REJECTED, HIDDEN -> "UNAVAILABLE";
        };
    }

    private static boolean knownRequestCollision(Throwable error) {
        String messages = messageChain(error);
        return messages.contains("uk_place_kakao_place_id") || messages.contains("uk_place_review_member");
    }

    private static String messageChain(Throwable error) {
        StringBuilder value = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            value.append(' ').append(current.getMessage());
        }
        return value.toString().toLowerCase();
    }

    private void count(String name) {
        meterRegistry.counter(name).increment();
    }

    private static void logTransition(String target, Long id, Object previous, Object next, Long operatorId) {
        log.info("Nearby {} status changed: targetId={}, previous={}, next={}, operatorId={}",
                target, id, previous, next, operatorId);
    }
}
