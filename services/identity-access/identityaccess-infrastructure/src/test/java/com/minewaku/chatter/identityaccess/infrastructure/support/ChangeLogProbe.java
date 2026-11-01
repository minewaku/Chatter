package com.minewaku.chatter.identityaccess.infrastructure.support;

import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reads the database change log the way the relay would, without Debezium: a throwaway logical slot with the
 * built-in {@code test_decoding} plugin. The container must run with {@code wal_level=logical}.
 */
public final class ChangeLogProbe implements AutoCloseable {

    private final JdbcTemplate jdbc;
    private final String slot;

    private ChangeLogProbe(JdbcTemplate jdbc, String slot) {
        this.jdbc = jdbc;
        this.slot = slot;
    }

    public static ChangeLogProbe start(DataSource dataSource) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String slot = "probe_" + UUID.randomUUID().toString().replace('-', '_');
        jdbc.queryForList("SELECT pg_create_logical_replication_slot(?, 'test_decoding')", slot);
        return new ChangeLogProbe(jdbc, slot);
    }

    /** Returns and consumes every decoded change since the last call, e.g. {@code table identity_access.outbox: INSERT: ...}. */
    public List<String> drain() {
        return jdbc.queryForList("SELECT data FROM pg_logical_slot_get_changes(?, NULL, NULL)", String.class, slot);
    }

    /** Only the lines that concern the announcements table. */
    public List<String> drainOutbox() {
        return drain().stream().filter(line -> line.contains("identity_access.outbox")).toList();
    }

    @Override
    public void close() {
        jdbc.queryForList("SELECT pg_drop_replication_slot(?)", slot);
    }
}
