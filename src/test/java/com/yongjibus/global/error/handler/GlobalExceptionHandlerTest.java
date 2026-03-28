package com.yongjibus.global.error.handler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yongjibus.arrivaltime.controller.ArrivalTimeController;
import com.yongjibus.arrivaltime.service.ArrivalTimeService;
import com.yongjibus.chat.controller.ChatController;
import com.yongjibus.chat.service.ChatService;
import com.yongjibus.chat.service.FCMTokenService;
import com.yongjibus.daytype.service.DayTypeService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private ArrivalTimeService arrivalTimeService;

    @Mock
    private ChatService chatService;

    @Mock
    private FCMTokenService fcmTokenService;

    @Mock
    private DayTypeService dayTypeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ArrivalTimeController(arrivalTimeService),
                        new ChatController(chatService, fcmTokenService),
                        new com.yongjibus.daytype.controller.DayTypeController(dayTypeService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("본문 검증 실패는 INVALID_REQUEST 응답으로 감싼다")
    void saveArrivalTime_WhenRequestBodyValidationFails_ShouldReturnYongJiResponse() throws Exception {
        mockMvc.perform(post("/arrivaltime/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "busId": 0,
                                  "date": "2026-03-28",
                                  "time": "09:00:00",
                                  "isHoliday": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").value("버스 ID는 1 이상이어야 합니다."));

        verify(arrivalTimeService, never()).saveArrivalTime(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("JSON 본문의 날짜 형식이 잘못되면 INVALID_DATE_FORMAT 응답을 반환한다")
    void saveArrivalTime_WhenBodyDateFormatIsInvalid_ShouldReturnDateFormatError() throws Exception {
        mockMvc.perform(post("/arrivaltime/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "busId": 1,
                                  "date": "2026/03/28",
                                  "time": "09:00:00",
                                  "isHoliday": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").value("날짜 형식이 올바르지 않습니다."));

        verify(arrivalTimeService, never()).saveArrivalTime(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("쿼리 파라미터 날짜 형식 오류도 YongJiResponse로 감싼다")
    void getDayType_WhenQueryDateIsInvalid_ShouldReturnYongJiResponse() throws Exception {
        mockMvc.perform(get("/day").param("date", "2026/03/28"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").value("날짜 형식이 올바르지 않습니다."));

        verify(dayTypeService, never()).findDayInfo(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("필수 쿼리 파라미터 누락도 YongJiResponse로 감싼다")
    void getDayType_WhenDateParameterIsMissing_ShouldReturnYongJiResponse() throws Exception {
        mockMvc.perform(get("/day"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").value("date 파라미터는 필수입니다."));

        verify(dayTypeService, never()).findDayInfo(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("경로 변수 날짜 형식 오류도 YongJiResponse로 감싼다")
    void getArrivalTime_WhenPathDateIsInvalid_ShouldReturnYongJiResponse() throws Exception {
        mockMvc.perform(get("/arrivaltime/not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").value("날짜 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("채팅방 생성 요청 검증 실패는 필드 메시지를 반환한다")
    void createChatRoom_WhenRequestBodyValidationFails_ShouldReturnValidationMessage() throws Exception {
        mockMvc.perform(post("/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "departureTime": "08:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").value("채팅방 이름을 입력해주세요."));

        verify(chatService, never()).createChatRoom(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("FCM 토큰 등록 요청 검증 실패는 필드 메시지를 반환한다")
    void registerFcmToken_WhenRequestBodyValidationFails_ShouldReturnValidationMessage() throws Exception {
        mockMvc.perform(post("/chat/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").value("FCM 토큰을 입력해주세요."));

        verify(fcmTokenService, never()).saveToken(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
