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

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void migratesExistingSchemaWhetherCheckpointTableAlreadyExists(boolean checkpointAlreadyExists) throws Exception {
        String url = "jdbc:h2:mem:flyway_" + checkpointAlreadyExists + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE legacy_marker (id BIGINT PRIMARY KEY)");
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
                var result = connection.createStatement().executeQuery(
                        "SELECT version, payload FROM timetable_release")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getLong("version")).isEqualTo(1L);
            assertThat(result.getString("payload")).contains("\"myongjiWeekday\"")
                    .contains("\"giheungWeekday\"");
        }
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");
    }

    private Flyway flyway(String url) {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }
}
