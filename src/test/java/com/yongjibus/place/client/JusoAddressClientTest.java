package com.yongjibus.place.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JusoAddressClientTest {
    private static final String BD_MGT_SN = "1168010100101230000000001";

    @Test
    void matchesStandardProvinceAndCityAbbreviationsExactly() {
        Map.ofEntries(
                Map.entry("서울", "서울특별시"),
                Map.entry("부산", "부산광역시"),
                Map.entry("대구", "대구광역시"),
                Map.entry("인천", "인천광역시"),
                Map.entry("광주", "광주광역시"),
                Map.entry("대전", "대전광역시"),
                Map.entry("울산", "울산광역시"),
                Map.entry("세종", "세종특별자치시"),
                Map.entry("경기", "경기도"),
                Map.entry("충북", "충청북도"),
                Map.entry("충남", "충청남도"),
                Map.entry("전북", "전북특별자치도"),
                Map.entry("전남", "전라남도"),
                Map.entry("경북", "경상북도"),
                Map.entry("경남", "경상남도"),
                Map.entry("제주", "제주특별자치도"))
                .forEach((shortName, fullName) -> assertThat(JusoAddressClient.normalize(shortName + " 용인시")
                        .equals(JusoAddressClient.normalize(fullName + " 용인시"))).isTrue());

        assertThat(JusoAddressClient.normalize("강원도 원주시"))
                .isEqualTo(JusoAddressClient.normalize("강원특별자치도 원주시"));
    }

    @Test
    void matchesJibunFromStructuredParcelFieldsWithoutSubstringMatching() {
        JusoAddressClient.Juso value = jusoWithJibun("경기도 용인시 처인구 남동 555 명지대학교용인캠퍼스");

        assertThat(JusoAddressClient.exactMatch(List.of(value), "경기 용인시 처인구 남동 555", false))
                .isSameAs(value);
        assertThat(JusoAddressClient.exactMatch(List.of(value), "경기 용인시 처인구 남동 5550", false))
                .isNull();
    }

    @Test
    void returnsNoMatchWhenNormalizedAddressIsAmbiguous() {
        JusoAddressClient.Juso first = jusoWithRoad("경기도 용인시");
        JusoAddressClient.Juso second = jusoWithRoad("경기도 용인시");

        assertThat(JusoAddressClient.exactMatch(List.of(first, second), "경기 용인시", true))
                .isNull();
    }

    @Test
    void classifiesOnlySafeUpstreamCodesForDiagnostics() {
        assertThat(JusoAddressClient.upstreamReason("429")).isEqualTo("upstream_rate_limited");
        assertThat(JusoAddressClient.upstreamReason("E0001")).isEqualTo("upstream_api_unsuccessful");
    }

    @Test
    void distinguishesEmptySearchNoExactMatchAndAmbiguousSearch() {
        assertThat(JusoAddressClient.searchFailureReason(0, 0)).isEqualTo("no_results");
        assertThat(JusoAddressClient.searchFailureReason(3, 0)).isEqualTo("no_exact_match");
        assertThat(JusoAddressClient.searchFailureReason(3, 2)).isEqualTo("ambiguous_match");
    }

    private static JusoAddressClient.Juso jusoWithRoad(String roadAddress) {
        return new JusoAddressClient.Juso(roadAddress, roadAddress, null, BD_MGT_SN,
                "4146110100", "414612345678", "0", "116", "0",
                null, null, null, null, null, null, null);
    }

    private static JusoAddressClient.Juso jusoWithJibun(String jibunAddress) {
        return new JusoAddressClient.Juso(null, null, jibunAddress, BD_MGT_SN,
                "4146110100", "414612345678", "0", "116", "0",
                "경기도", "용인시 처인구", "남동", null, "555", "0", "0");
    }
}
