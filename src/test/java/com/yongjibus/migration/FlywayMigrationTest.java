package com.yongjibus.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FlywayMigrationTest {

    @Test
    void migratesEmptyDatabaseFromV1() {
        Flyway flyway = flyway("jdbc:h2:mem:flyway_empty;MODE=MySQL;DB_CLOSE_DELAY=-1");

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void migratesExistingSchemaWhetherCheckpointTableAlreadyExists(boolean checkpointAlreadyExists) throws Exception {
        String url = "jdbc:h2:mem:flyway_" + checkpointAlreadyExists + ";DB_CLOSE_DELAY=-1";
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
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    private Flyway flyway(String url) {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }
}
