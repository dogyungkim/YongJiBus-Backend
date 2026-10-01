package com.yongjibus.place.client;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
    private static final int CATEGORY_PAGE_SIZE = 15;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final RestClient restClient;
    private final BigDecimal centerLatitude;
    private final BigDecimal centerLongitude;
    private final int radiusMeters;

    @Autowired
    public KakaoLocalClient(RestClient.Builder builder,
            @Value("${kakao.rest-api-key}") String apiKey,
            @Value("${nearby.search-center-latitude}") BigDecimal centerLatitude,
            @Value("${nearby.search-center-longitude}") BigDecimal centerLongitude,
            @Value("${nearby.search-radius-meters}") int radiusMeters) {
        this(buildRestClient(builder, apiKey), centerLatitude, centerLongitude, radiusMeters);
    }

    KakaoLocalClient(RestClient restClient, BigDecimal centerLatitude, BigDecimal centerLongitude, int radiusMeters) {
        this.restClient = restClient;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.radiusMeters = radiusMeters;
    }

    private static RestClient buildRestClient(RestClient.Builder builder, String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        return builder.baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .requestFactory(requestFactory)
                .build();
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

    public List<KakaoPlace> searchViewport(double minLatitude, double minLongitude,
            double maxLatitude, double maxLongitude) {
        String rect = String.join(",", Double.toString(minLongitude), Double.toString(minLatitude),
                Double.toString(maxLongitude), Double.toString(maxLatitude));
        return List.of("FD6", "CE7").stream()
                .flatMap(category -> searchCategory(category, rect).stream())
                .toList();
    }

    public boolean isWithinCampusArea(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && distanceMeters(centerLatitude.doubleValue(), centerLongitude.doubleValue(), latitude, longitude)
                        <= radiusMeters;
    }

    public boolean isViewportWithinSearchArea(double minLatitude, double minLongitude,
            double maxLatitude, double maxLongitude) {
        return validCoordinates(minLatitude, minLongitude) && validCoordinates(maxLatitude, maxLongitude)
                && minLatitude < maxLatitude && minLongitude < maxLongitude
                && distanceMeters(minLatitude, minLongitude, maxLatitude, maxLongitude) <= 2.0 * radiusMeters
                && isWithinCampusArea(minLatitude, minLongitude)
                && isWithinCampusArea(minLatitude, maxLongitude)
                && isWithinCampusArea(maxLatitude, minLongitude)
                && isWithinCampusArea(maxLatitude, maxLongitude);
    }

    public static double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                        * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(Math.min(1, a)), Math.sqrt(1 - Math.min(1, a)));
    }

    private List<KakaoPlace> searchCategory(String category, String rect) {
        try {
            KakaoResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/local/search/category.json")
                            .queryParam("category_group_code", category)
                            .queryParam("rect", rect)
                            .queryParam("size", CATEGORY_PAGE_SIZE)
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

    private static boolean validCoordinates(double latitude, double longitude) {
        return Double.isFinite(latitude) && latitude >= -90 && latitude <= 90
                && Double.isFinite(longitude) && longitude >= -180 && longitude <= 180;
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
