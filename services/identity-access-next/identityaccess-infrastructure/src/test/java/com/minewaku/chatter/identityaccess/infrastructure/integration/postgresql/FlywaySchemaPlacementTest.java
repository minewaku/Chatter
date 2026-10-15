package com.minewaku.chatter.identityaccess.infrastructure.integration.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.support.PostgreSqlTestSupport;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class FlywaySchemaPlacementTest extends PostgreSqlTestSupport {

    @Test
    void tablesAndMigrationHistoryLiveOnlyInTheServiceSchema() throws SQLException {
        assertThat(tablesIn("identity_access")).contains("user_account", "flyway_schema_history");
        assertThat(tablesIn("public")).doesNotContain("user_account", "flyway_schema_history");
    }

    @Test
    void anotherServiceSchemaMigratesInTheSameDatabaseWithoutConflict() throws SQLException {
        Flyway.configure()
                .dataSource(
                        jdbcUrl().replace("currentSchema=identity_access", "currentSchema=other_service"),
                        username(),
                        password())
                .schemas("other_service")
                .defaultSchema("other_service")
                .createSchemas(true)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(tablesIn("other_service")).contains("user_account", "flyway_schema_history");
        assertThat(tablesIn("identity_access")).contains("user_account", "flyway_schema_history");
    }

    private static List<String> tablesIn(String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(jdbcUrl(), username(), password());
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(
                        "select table_name from information_schema.tables where table_schema = '" + schema + "'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }
}
