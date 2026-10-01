package com.yongjibus.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FlywayMigrationTest {

    @Test
    void seedsEveryInitialTimetableRow() throws Exception {
        Flyway flyway = flyway("jdbc:h2:mem:flyway_timetable_seed;MODE=MySQL;DB_CLOSE_DELAY=-1");

        flyway.migrate();

        try (var connection = DriverManager.getConnection("jdbc:h2:mem:flyway_timetable_seed;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
                var result = connection.createStatement().executeQuery("SELECT payload FROM timetable_release")) {
            assertThat(result.next()).isTrue();
            JsonNode payload = new ObjectMapper().readTree(result.getString("payload"));
            assertThat(payload.get("myongjiWeekday")).hasSize(64);
            assertThat(payload.get("myongjiWeekend")).hasSize(10);
            assertThat(payload.get("giheungWeekday")).hasSize(14);
            assertThat(payload.get("myongjiWeekday").get(0).get("type").asText()).isEqualTo("명지대역");
            assertThat(payload.get("myongjiWeekday").get(63).get("startTime").asText()).isEqualTo("20:00");
            assertThat(payload.get("giheungWeekday").get(13).get("schoolArrival").asText()).isEqualTo("19:45");
        }
    }

    @Test
    void migratesEmptyDatabaseFromV1() {
        Flyway flyway = flyway("jdbc:h2:mem:flyway_empty;MODE=MySQL;DB_CLOSE_DELAY=-1");

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("7");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void migratesExistingSchemaWhetherCheckpointTableAlreadyExists(boolean checkpointAlreadyExists) throws Exception {
        String url = "jdbc:h2:mem:flyway_" + checkpointAlreadyExists + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE legacy_marker (id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE member (id BIGINT AUTO_INCREMENT PRIMARY KEY)");
            statement.execute("""
                    CREATE TABLE fcmtoken (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        token VARCHAR(255) NOT NULL,
                        member_id BIGINT NOT NULL,
                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                        last_updated_at TIMESTAMP(6),
                        CONSTRAINT uk_fcmtoken_member UNIQUE (member_id),
                        CONSTRAINT fk_fcmtoken_member FOREIGN KEY (member_id)
                            REFERENCES member(id) ON DELETE CASCADE
                    )
                    """);
            statement.execute("INSERT INTO member (id) VALUES (1)");
            statement.execute("INSERT INTO fcmtoken (id, token, member_id) VALUES (1, 'legacy-token', 1)");
            if (checkpointAlreadyExists) {
                statement.execute("""
                        CREATE TABLE gmail_history_checkpoint (
                            checkpoint_key VARCHAR(64) PRIMARY KEY,
                            last_history_id DECIMAL(39,0) NOT NULL,
                            updated_at TIMESTAMP(6) NOT NULL
                        )
                        """);
            }
        }

        Flyway flyway = flyway(url);

        flyway.migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var tables = connection.getMetaData().getTables(null, null, "GMAIL_HISTORY_CHECKPOINT", null)) {
            assertThat(tables.next()).isTrue();
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var tables = connection.getMetaData().getTables(null, null, "PLACE_IMAGE", null)) {
            assertThat(tables.next()).isTrue();
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
                var result = connection.createStatement().executeQuery(
                        "SELECT version, payload FROM timetable_release")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getLong("version")).isEqualTo(1L);
            assertThat(result.getString("payload")).contains("\"myongjiWeekday\"")
                    .contains("\"giheungWeekday\"");
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
                var statement = connection.createStatement()) {
            try (var result = statement.executeQuery(
                    "SELECT token, member_id FROM fcmtoken WHERE id = 1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("token")).isEqualTo("legacy-token");
                assertThat(result.getLong("member_id")).isEqualTo(1L);
            }
            statement.executeUpdate("INSERT INTO fcmtoken (token, member_id) VALUES ('second-token', 1)");
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM fcmtoken WHERE member_id = 1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(2);
            }
            statement.executeUpdate("DELETE FROM member WHERE id = 1");
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM fcmtoken")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
        }
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("7");
    }

    @Test
    void backfillsLegacyImageRowsBeforeAddingTheThumbnailConstraint() throws Exception {
        String url = "jdbc:h2:mem:flyway_legacy_image;MODE=MySQL;DB_CLOSE_DELAY=-1";
        flyway(url, "4").migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO member (name, username, password, email, role, is_deleted)
                    VALUES ('maker', 'maker', 'password', 'maker@example.com', 'USER', FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO place (display_name, address_text, latitude, longitude,
                        juso_building_management_number, category, kakao_place_id, kakao_place_url, status)
                    VALUES ('place', 'address', 37.2242000, 127.1876600,
                        '1234567890123456789012345', 'CAFE', 'legacy-image-place',
                        'https://place.map.kakao.com/legacy-image-place', 'APPROVED')
                    """);
            statement.executeUpdate("""
                    INSERT INTO place_image (place_id, storage_key, sort_order)
                    SELECT id, '00000000-0000-4000-8000-000000000001.jpg', 0
                    FROM place WHERE kakao_place_id = 'legacy-image-place'
                    """);
        }

        flyway(url).migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
                var result = connection.createStatement().executeQuery("""
                        SELECT storage_key, thumbnail_storage_key
                        FROM place_image
                        WHERE storage_key = '00000000-0000-4000-8000-000000000001.jpg'
                        """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("thumbnail_storage_key"))
                    .isEqualTo(result.getString("storage_key"));
        }
    }

    private Flyway flyway(String url) {
        return flyway(url, null);
    }

    private Flyway flyway(String url, String target) {
        var configuration = Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("1");
        if (target != null) {
            configuration.target(target);
        }
        return configuration
                .load();
    }
}
