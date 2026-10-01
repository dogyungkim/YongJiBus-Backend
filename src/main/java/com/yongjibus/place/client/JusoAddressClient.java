package com.yongjibus.place.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JusoAddressClient {
    private static final String SEARCH_URL = "https://business.juso.go.kr/addrlink/addrLinkApi.do";
    private static final String COORDINATE_URL = "https://business.juso.go.kr/addrlink/addrCoordApi.do";
    private static final Duration COORDINATE_WINDOW = Duration.ofSeconds(5);
    private static final int COORDINATE_LIMIT = 10;
    private static final Map<String, String> ADMINISTRATIVE_PREFIXES = Map.ofEntries(
            Map.entry("서울", "서울특별시"), Map.entry("서울특별시", "서울특별시"),
            Map.entry("부산", "부산광역시"), Map.entry("부산광역시", "부산광역시"),
            Map.entry("대구", "대구광역시"), Map.entry("대구광역시", "대구광역시"),
            Map.entry("인천", "인천광역시"), Map.entry("인천광역시", "인천광역시"),
            Map.entry("광주", "광주광역시"), Map.entry("광주광역시", "광주광역시"),
            Map.entry("대전", "대전광역시"), Map.entry("대전광역시", "대전광역시"),
            Map.entry("울산", "울산광역시"), Map.entry("울산광역시", "울산광역시"),
            Map.entry("세종", "세종특별자치시"), Map.entry("세종특별자치시", "세종특별자치시"),
            Map.entry("경기", "경기도"), Map.entry("경기도", "경기도"),
            Map.entry("강원", "강원"), Map.entry("강원도", "강원"),
            Map.entry("강원특별자치도", "강원"),
            Map.entry("충북", "충청북도"), Map.entry("충청북도", "충청북도"),
            Map.entry("충남", "충청남도"), Map.entry("충청남도", "충청남도"),
            Map.entry("전북", "전북"), Map.entry("전라북도", "전북"),
            Map.entry("전북특별자치도", "전북"),
            Map.entry("전남", "전라남도"), Map.entry("전라남도", "전라남도"),
            Map.entry("경북", "경상북도"), Map.entry("경상북도", "경상북도"),
            Map.entry("경남", "경상남도"), Map.entry("경상남도", "경상남도"),
            Map.entry("제주", "제주특별자치도"), Map.entry("제주특별자치도", "제주특별자치도"));

    private final RestClient restClient;
    private final String searchKey;
    private final String coordinateKey;
    private final double centerLatitude;
    private final double centerLongitude;
    private final int radiusMeters;
    private final ArrayDeque<Instant> coordinateCalls = new ArrayDeque<>();
    private final CoordinateTransform utmKToWgs84;

    public JusoAddressClient(RestClient.Builder builder,
            @Value("${juso.search-confirmation-key}") String searchKey,
            @Value("${juso.coordinate-confirmation-key}") String coordinateKey,
            @Value("${nearby.search-center-latitude}") double centerLatitude,
            @Value("${nearby.search-center-longitude}") double centerLongitude,
            @Value("${nearby.search-radius-meters}") int radiusMeters) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder.requestFactory(requestFactory).build();
        this.searchKey = searchKey;
        this.coordinateKey = coordinateKey;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.radiusMeters = radiusMeters;

        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem utmK = crsFactory.createFromParameters("EPSG:5179",
                "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 "
                        + "+ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");
        CoordinateReferenceSystem wgs84 = crsFactory.createFromParameters("EPSG:4326",
                "+proj=longlat +datum=WGS84 +no_defs");
        this.utmKToWgs84 = new CoordinateTransformFactory().createTransform(utmK, wgs84);
    }

    public ResolvedAddress resolve(String roadAddress, String jibunAddress) {
        validateSearchAddress(roadAddress, jibunAddress);
        boolean roadPresent = !isBlank(roadAddress);
        boolean jibunPresent = !isBlank(jibunAddress);
        Juso candidate = null;
        if (roadPresent) {
            List<Juso> results = search(roadAddress, "road", roadPresent, jibunPresent);
            int matchCount = matchingCount(results, roadAddress, true);
            candidate = exactMatch(results, roadAddress, true);
            if (candidate == null) {
                logFailure("search", searchFailureReason(results.size(), matchCount), "road",
                        roadPresent, jibunPresent, results.size(), matchCount, null, null);
            }
        }
        if (candidate == null && jibunPresent) {
            List<Juso> results = search(jibunAddress, "jibun", roadPresent, jibunPresent);
            int matchCount = matchingCount(results, jibunAddress, false);
            candidate = exactMatch(results, jibunAddress, false);
            if (candidate == null) {
                logFailure("search", searchFailureReason(results.size(), matchCount), "jibun",
                        roadPresent, jibunPresent, results.size(), matchCount, null, null);
            }
        }
        if (candidate == null) {
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
        }
        if (candidate.bdMgtSn() == null || !candidate.bdMgtSn().matches("\\d{25}")) {
            logFailure("resolve", "invalid_bd_mgt_sn", null, roadPresent, jibunPresent, 1, 1, null, null);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
        }

        CoordinateJuso coordinate = coordinate(candidate, roadPresent, jibunPresent);
        if (!candidate.bdMgtSn().equals(coordinate.bdMgtSn())) {
            logFailure("coordinate", "bd_mgt_sn_mismatch", null, roadPresent, jibunPresent, 1, 1, null, null);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
        }
        ProjCoordinate target = new ProjCoordinate();
        try {
            ProjCoordinate source = new ProjCoordinate(parseCoordinate(coordinate.entX()),
                    parseCoordinate(coordinate.entY()));
            utmKToWgs84.transform(source, target);
        } catch (PlaceException e) {
            logFailure("coordinate", "invalid_coordinate", null, roadPresent, jibunPresent, 1, 1, null, null);
            throw e;
        } catch (RuntimeException e) {
            logFailure("coordinate", "invalid_coordinate", null, roadPresent, jibunPresent, 1, 1, null, null);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
        }
        if (!Double.isFinite(target.x) || !Double.isFinite(target.y)) {
            logFailure("coordinate", "invalid_coordinate", null, roadPresent, jibunPresent, 1, 1, null, null);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
        }
        if (distanceMeters(centerLatitude, centerLongitude, target.y, target.x) > radiusMeters) {
            logFailure("coordinate", "out_of_radius", null, roadPresent, jibunPresent, 1, 1, null, null);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
        }

        String addressText = firstNonBlank(candidate.roadAddrPart1(), candidate.roadAddr(), candidate.jibunAddr());
        return new ResolvedAddress(addressText, candidate.bdMgtSn(),
                BigDecimal.valueOf(target.y).setScale(7, RoundingMode.HALF_UP),
                BigDecimal.valueOf(target.x).setScale(7, RoundingMode.HALF_UP));
    }

    private List<Juso> search(String address, String addressType, boolean roadPresent, boolean jibunPresent) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("confmKey", searchKey)
                    .queryParam("currentPage", 1)
                    .queryParam("countPerPage", 10)
                    .queryParam("resultType", "json")
                    .queryParam("keyword", address)
                    .build().encode().toUri();
            JusoResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JusoResponse.class);
            Results results = response == null ? null : response.results();
            Common common = results == null ? null : results.common();
            if (common == null || !isSuccess(common.errorCode())) {
                String apiErrorCode = common == null ? null : common.errorCode();
                logFailure("search", upstreamReason(apiErrorCode), addressType, roadPresent, jibunPresent,
                        0, 0, apiErrorCode, null);
                throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_SEARCH_UNAVAILABLE);
            }
            return results.juso() == null ? List.of() : results.juso();
        } catch (PlaceException e) {
            throw e;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            logFailure("search", status == 429 ? "upstream_rate_limited" : "upstream_http_failure", addressType,
                    roadPresent, jibunPresent, 0, 0, null, status);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_SEARCH_UNAVAILABLE);
        } catch (RestClientException | IllegalArgumentException e) {
            logFailure("search", "transport_failure", addressType, roadPresent, jibunPresent, 0, 0, null, null);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_SEARCH_UNAVAILABLE);
        }
    }

    private CoordinateJuso coordinate(Juso address, boolean roadPresent, boolean jibunPresent) {
        acquireCoordinatePermit(roadPresent, jibunPresent);
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(COORDINATE_URL)
                    .queryParam("confmKey", coordinateKey)
                    .queryParam("admCd", address.admCd())
                    .queryParam("rnMgtSn", address.rnMgtSn())
                    .queryParam("udrtYn", address.udrtYn())
                    .queryParam("buldMnnm", address.buldMnnm())
                    .queryParam("buldSlno", address.buldSlno())
                    .queryParam("resultType", "json")
                    .build().encode().toUri();
            CoordinateResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(CoordinateResponse.class);
            CoordinateResults results = response == null ? null : response.results();
            Common common = results == null ? null : results.common();
            if (common == null || !isSuccess(common.errorCode())) {
                String apiErrorCode = common == null ? null : common.errorCode();
                logFailure("coordinate", upstreamReason(apiErrorCode), null, roadPresent, jibunPresent,
                        0, 0, apiErrorCode, null);
                throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_RESOLVE_UNAVAILABLE);
            }
            List<CoordinateJuso> values = results.juso() == null ? List.of() : results.juso();
            List<CoordinateJuso> matches = values.stream()
                    .filter(item -> item != null && address.bdMgtSn().equals(item.bdMgtSn()))
                    .toList();
            if (matches.isEmpty()) {
                logFailure("coordinate", "no_matching_result", null, roadPresent, jibunPresent,
                        values.size(), 0, null, null);
                throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
            }
            return matches.get(0);
        } catch (PlaceException e) {
            throw e;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            logFailure("coordinate", status == 429 ? "upstream_rate_limited" : "upstream_http_failure", null,
                    roadPresent, jibunPresent, 0, 0, null, status);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_RESOLVE_UNAVAILABLE);
        } catch (RestClientException | IllegalArgumentException e) {
            logFailure("coordinate", "transport_failure", null, roadPresent, jibunPresent, 0, 0, null, null);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_RESOLVE_UNAVAILABLE);
        }
    }

    private synchronized void acquireCoordinatePermit(boolean roadPresent, boolean jibunPresent) {
        Instant now = Instant.now();
        while (!coordinateCalls.isEmpty() && !coordinateCalls.peekFirst().isAfter(now.minus(COORDINATE_WINDOW))) {
            coordinateCalls.removeFirst();
        }
        if (coordinateCalls.size() >= COORDINATE_LIMIT) {
            logFailure("coordinate", "rate_limited", null, roadPresent, jibunPresent, 0, 0, null, null);
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_RESOLVE_UNAVAILABLE);
        }
        coordinateCalls.addLast(now);
    }

    private static boolean isSuccess(String code) {
        return "0".equals(code) || "00".equals(code);
    }

    static Juso exactMatch(List<Juso> values, String requested, boolean road) {
        List<Juso> matches = matchingValues(values, requested, road);
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private static int matchingCount(List<Juso> values, String requested, boolean road) {
        return matchingValues(values, requested, road).size();
    }

    static String searchFailureReason(int resultCount, int matchCount) {
        if (resultCount == 0) {
            return "no_results";
        }
        return matchCount == 0 ? "no_exact_match" : "ambiguous_match";
    }

    private static List<Juso> matchingValues(List<Juso> values, String requested, boolean road) {
        String normalized = normalize(requested);
        return (values == null ? List.<Juso>of() : values).stream()
                .filter(value -> value != null && (road
                        ? normalized.equals(normalize(firstNonBlank(value.roadAddrPart1(), value.roadAddr())))
                        : matchesJibun(normalized, value)))
                .toList();
    }

    private static boolean matchesJibun(String normalizedRequested, Juso value) {
        if (normalizedRequested.equals(normalize(value.jibunAddr()))) {
            return true;
        }
        String structured = structuredJibun(value);
        return !structured.isEmpty() && normalizedRequested.equals(normalize(structured));
    }

    private static String structuredJibun(Juso value) {
        if (isBlank(value.siNm()) || isBlank(value.sggNm()) || isBlank(value.emdNm())
                || isBlank(value.lnbrMnnm()) || isBlank(value.mtYn())) {
            return "";
        }
        String parcelNumber = (isMountain(value.mtYn()) ? "산 " : "") + value.lnbrMnnm().trim();
        if (!isBlank(value.lnbrSlno()) && !"0".equals(value.lnbrSlno().trim())) {
            parcelNumber += "-" + value.lnbrSlno().trim();
        }
        return String.join(" ", value.siNm().trim(), value.sggNm().trim(), value.emdNm().trim(),
                blankToEmpty(value.liNm()), parcelNumber).replaceAll("\\s+", " ").trim();
    }

    private static boolean isMountain(String value) {
        return "1".equals(value.trim()) || "Y".equalsIgnoreCase(value.trim());
    }

    static String normalize(String address) {
        if (address == null) {
            return "";
        }
        String normalized = address.replaceAll("\\([^)]*\\)", " ").replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        int separator = normalized.indexOf(' ');
        if (separator < 0) {
            return ADMINISTRATIVE_PREFIXES.getOrDefault(normalized, normalized);
        }
        String prefix = normalized.substring(0, separator);
        return ADMINISTRATIVE_PREFIXES.getOrDefault(prefix, prefix) + normalized.substring(separator);
    }

    private static void validateSearchAddress(String roadAddress, String jibunAddress) {
        if ((isBlank(roadAddress) && isBlank(jibunAddress))
                || (roadAddress != null && roadAddress.length() > 255)
                || (jibunAddress != null && jibunAddress.length() > 255)) {
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_INVALID);
        }
    }

    private static double parseCoordinate(String value) {
        try {
            return Double.parseDouble(value);
        } catch (RuntimeException e) {
            throw new PlaceException(ErrorCode.PUBLIC_ADDRESS_NOT_RESOLVED);
        }
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double latDelta = Math.toRadians(lat2 - lat1);
        double lonDelta = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToEmpty(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    static String upstreamReason(String apiErrorCode) {
        if (isBlank(apiErrorCode)) {
            return "upstream_api_unsuccessful";
        }
        String normalized = apiErrorCode.trim().toUpperCase(Locale.ROOT);
        return normalized.contains("429") || normalized.contains("RATE")
                || normalized.contains("LIMIT") || normalized.contains("QUOTA")
                ? "upstream_rate_limited" : "upstream_api_unsuccessful";
    }

    private static void logFailure(String operation, String reason, String addressType,
            boolean roadPresent, boolean jibunPresent, int resultCount, int matchCount,
            String apiErrorCode, Integer httpStatus) {
        log.warn("Juso address resolution failed: operation={}, reason={}, addressType={}, roadPresent={}, "
                        + "jibunPresent={}, resultCount={}, matchCount={}, apiErrorCode={}, httpStatus={}",
                operation, reason, addressType, roadPresent, jibunPresent, resultCount, matchCount,
                apiErrorCode, httpStatus);
    }

    public record ResolvedAddress(String addressText, String buildingManagementNumber,
            BigDecimal latitude, BigDecimal longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JusoResponse(Results results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Results(Common common, List<Juso> juso) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CoordinateResponse(CoordinateResults results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CoordinateResults(Common common, List<CoordinateJuso> juso) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Common(String errorCode, String errorMessage, String totalCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Juso(String roadAddr, String roadAddrPart1, String jibunAddr, String bdMgtSn,
            String admCd, String rnMgtSn, String udrtYn, String buldMnnm, String buldSlno,
            String siNm, String sggNm, String emdNm, String liNm, String lnbrMnnm,
            String lnbrSlno, String mtYn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CoordinateJuso(String bdMgtSn, String entX, String entY) {
    }
}
