package com.minewaku.chatter.message.infrastructure.config;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.lease.event.SecretLeaseRotatedEvent;

import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@AllArgsConstructor
//recheck: implements to the rest of services
public class VaultDatasourceCredentialRotator implements ApplicationListener<SecretLeaseRotatedEvent> {

    private final HikariDataSource hikariDataSource;

    @Override
    public void onApplicationEvent(SecretLeaseRotatedEvent event) {

        if (event.getSource().getPath().contains("message-postgresql-approle")) {
            Map<String, Object> secrets = event.getSecrets();
            String newUsername = (String) secrets.get("username");
            String newPassword = (String) secrets.get("password");

            log.info("Vault has rotated DB credentials. Updating HikariCP pool with new username: {}", newUsername);

            hikariDataSource.setUsername(newUsername);
            hikariDataSource.setPassword(newPassword);

            if (hikariDataSource.getHikariPoolMXBean() != null) {
                hikariDataSource.getHikariPoolMXBean().softEvictConnections();
                log.info("Successfully soft-evicted old connections.");
            }
        }
    }
}
