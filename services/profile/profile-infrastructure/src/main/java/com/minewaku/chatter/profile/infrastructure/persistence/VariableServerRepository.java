package com.minewaku.chatter.profile.infrastructure.persistence;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VariableServerRepository {

    private final JdbcTemplate jdbcTemplate;

    public VariableServerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(value = "server_config", key = "'serverId'")
    public int getServerId() {
        String sql = "SELECT (current_setting('port')::int - 5432) AS server_id";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
