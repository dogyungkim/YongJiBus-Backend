package com.yongjibus.place.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.member.domain.Member;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceRequest;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceRequestResult;
import com.yongjibus.place.controller.dto.PlaceDTOs.KakaoSearchItem;
import com.yongjibus.place.domain.PlaceCategory;
import com.yongjibus.place.domain.PlaceStatus;
import com.yongjibus.place.domain.ReviewStatus;
import com.yongjibus.place.service.PlaceImageService;
import com.yongjibus.place.service.PlaceService;
import com.yongjibus.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
class PlaceControllerTest extends ControllerTestSupport {
    @Mock
    private PlaceService placeService;

    @Mock
    private PlaceImageService placeImageService;

    private MockMvc mockMvc;
    private Member member;
    private MemberDetail principal;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new PlaceController(placeService, placeImageService));
        member = Member.builder().id(1L).username("user").build();
        principal = new MemberDetail(member);
        authenticate(auth());
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    @Test
    void multipartRequestDelegatesImagesAfterSavingTheRequest() throws Exception {
        PlaceRequest request = request();
        PlaceRequestResult result = result();
        MockMultipartFile requestPart = requestPart(request);
        MockMultipartFile image = new MockMultipartFile("images", "place.png", "image/png",
                "image".getBytes(StandardCharsets.UTF_8));
        when(placeService.requestPlace(member, request)).thenReturn(result);

        mockMvc.perform(multipart("/places/requests")
                        .file(requestPart)
                        .file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeId").value(42));

        verify(placeService).requestPlace(member, request);
        verify(placeImageService).addForRequest(42L, java.util.List.of(image));
    }

    @Test
    void multipartRequestWithoutImagesSkipsImageService() throws Exception {
        PlaceRequest request = request();
        when(placeService.requestPlace(member, request)).thenReturn(result());

        mockMvc.perform(multipart("/places/requests")
                        .file(requestPart(request)))
                .andExpect(status().isOk());

        verify(placeService).requestPlace(member, request);
        verifyNoInteractions(placeImageService);
    }

    @Test
    void multipartRequestWithoutRequestPartIsInvalidRequest() throws Exception {
        MockMultipartFile image = new MockMultipartFile("images", "place.png", "image/png",
                "image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/places/requests").file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verifyNoInteractions(placeService, placeImageService);
    }

    @Test
    void jsonRequestStillUsesTheOriginalContract() throws Exception {
        PlaceRequest request = request();
        when(placeService.requestPlace(member, request)).thenReturn(result());

        mockMvc.perform(post("/places/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeId").value(42));

        verify(placeService).requestPlace(member, request);
        verifyNoInteractions(placeImageService);
    }

    @Test
    void viewportSearchReturnsExistingKakaoPlaceResponseForAuthenticatedMember() throws Exception {
        KakaoSearchItem item = new KakaoSearchItem("123", "카페", "음식점 > 카페", "도로명", "지번",
                new BigDecimal("37.2242"), new BigDecimal("127.18766"),
                "https://place.map.kakao.com/123", null, null, "signed-proof");
        when(placeService.searchViewport(member, 37.223, 127.186, 37.225, 127.189)).thenReturn(List.of(item));

        mockMvc.perform(get("/places/kakao-viewport")
                        .param("minLatitude", "37.223")
                        .param("minLongitude", "127.186")
                        .param("maxLatitude", "37.225")
                        .param("maxLongitude", "127.189"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].placeId").value("123"))
                .andExpect(jsonPath("$.data[0].selectionProof").value("signed-proof"));

        verify(placeService).searchViewport(member, 37.223, 127.186, 37.225, 127.189);
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private PlaceRequest request() {
        return new PlaceRequest("proof", "장소", PlaceCategory.CAFE, null, 5, "좋아요");
    }

    private PlaceRequestResult result() {
        return new PlaceRequestResult(42L, PlaceStatus.PENDING, ReviewStatus.PENDING);
    }

    private MockMultipartFile requestPart(PlaceRequest request) throws Exception {
        return new MockMultipartFile("request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
    }
}
