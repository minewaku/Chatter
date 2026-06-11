package com.minewaku.chatter.message.infrastructure.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    Flyway flywayPostgres(DataSource postgresDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(postgresDataSource)
                .cleanDisabled(false)
                .locations("classpath:db/migration/postgresql")
                .table("flyway_schema_history_postgres")
                .load();
        // flyway.clean();
        return flyway;
    }

    @Bean(initMethod = "migrate")
    public Flyway flywayCassandra(
            @Value("${spring.cassandra.contact-points}") String host,
            @Value("${spring.cassandra.port}") String port,
            @Value("${spring.cassandra.keyspace-name}") String keyspace,
            @Value("${spring.cassandra.local-datacenter}") String datacenter,
            @Value("${spring.cassandra.username}") String username,
            @Value("${spring.cassandra.password}") String password
    ) {
        //cassandra flyway doesn't use Jdbc but cassandra-jdbc-wrapper instead so we append username/password directly into the url instead of through dataSource() params
        String url = String.format(
            "jdbc:cassandra://%s:%s/%s?localdatacenter=%s&user=%s&password=%s",
            host, port, keyspace, datacenter, username, password
        );
        Flyway flyway = Flyway.configure()
                .dataSource(url, null, null)
                .cleanDisabled(false)
                .locations("classpath:db/migration/cassandra")
                .table("flyway_schema_history_cassandra")
                .load();
        // flyway.clean();
        return flyway;
    }
}
