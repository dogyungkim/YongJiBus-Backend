package com.yongjibus.place.client;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;

@Component
public class KakaoLocalClient {
    private final RestClient restClient;
    private final BigDecimal centerLatitude;
    private final BigDecimal centerLongitude;
    private final int radiusMeters;

    public KakaoLocalClient(RestClient.Builder builder,
            @Value("${kakao.rest-api-key}") String apiKey,
            @Value("${nearby.search-center-latitude}") BigDecimal centerLatitude,
            @Value("${nearby.search-center-longitude}") BigDecimal centerLongitude,
            @Value("${nearby.search-radius-meters}") int radiusMeters) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder.baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .requestFactory(requestFactory)
                .build();
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.radiusMeters = radiusMeters;
    }

    public List<KakaoPlace> search(String query) {
        try {
            KakaoResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("x", centerLongitude)
                            .queryParam("y", centerLatitude)
                            .queryParam("radius", radiusMeters)
                            .queryParam("size", 15)
                            .queryParam("sort", "accuracy")
                            .build())
                    .retrieve()
                    .body(KakaoResponse.class);
            return response == null || response.documents() == null
                    ? List.of()
                    : response.documents().stream().map(KakaoPlace::validated).toList();
        } catch (PlaceException e) {
            throw e;
        } catch (RestClientException | IllegalArgumentException e) {
            throw new PlaceException(ErrorCode.PLACE_SEARCH_UNAVAILABLE);
        }
    }

    public static String normalizePlaceUrl(String placeId, String value) {
        if (placeId == null || !placeId.matches("\\d{1,32}") || value == null) {
            throw new PlaceException(ErrorCode.INVALID_KAKAO_PLACE);
        }
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || !"place.map.kakao.com".equalsIgnoreCase(uri.getHost())
                    || !('/' + placeId).equals(uri.getPath())
                    || uri.getUserInfo() != null || uri.getPort() != -1
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new PlaceException(ErrorCode.INVALID_KAKAO_PLACE);
            }
            return "https://place.map.kakao.com/" + placeId;
        } catch (IllegalArgumentException e) {
            throw new PlaceException(ErrorCode.INVALID_KAKAO_PLACE);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoResponse(List<KakaoPlace> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoPlace(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("road_address_name") String roadAddressName,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("y") String latitude,
            @JsonProperty("x") String longitude,
            @JsonProperty("place_url") String placeUrl) {

        private KakaoPlace validated() {
            String normalizedUrl = normalizePlaceUrl(id, placeUrl);
            if (isBlank(placeName) || tooLong(roadAddressName) || tooLong(addressName)
                    || (isBlank(roadAddressName) && isBlank(addressName))) {
                throw new PlaceException(ErrorCode.INVALID_KAKAO_PLACE);
            }
            try {
                BigDecimal lat = new BigDecimal(latitude);
                BigDecimal lon = new BigDecimal(longitude);
                if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0
                        || lon.compareTo(BigDecimal.valueOf(-180)) < 0 || lon.compareTo(BigDecimal.valueOf(180)) > 0) {
                    throw new PlaceException(ErrorCode.INVALID_KAKAO_PLACE);
                }
            } catch (NumberFormatException e) {
                throw new PlaceException(ErrorCode.INVALID_KAKAO_PLACE);
            }
            return new KakaoPlace(id, placeName.trim(), categoryName, blankToEmpty(roadAddressName),
                    blankToEmpty(addressName), latitude, longitude, normalizedUrl);
        }

        public BigDecimal latitudeValue() {
            return new BigDecimal(latitude);
        }

        public BigDecimal longitudeValue() {
            return new BigDecimal(longitude);
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }

        private static boolean tooLong(String value) {
            return value != null && value.length() > 255;
        }

        private static String blankToEmpty(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
