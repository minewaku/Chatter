package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.DeviceInfo;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.dto.JdbcDeviceInfo;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity.JdbcSessionEntity;

@Component
public class JdbcSessionMapper {

    public JdbcSessionEntity domainToEntity(Session domain) {
        if (domain == null) {
            return null;
        }

        return JdbcSessionEntity.builder()
            .id(domain.getSessionId() != null ? UUID.fromString(domain.getSessionId().getValue()) : null)
            .userId(domain.getUserId() != null ? domain.getUserId().getValue() : null)
            .deviceInfo(mapDeviceInfo(domain.getDeviceInfo()))
            .generation(domain.getGeneration())
            .issuedAt(domain.getIssuedAt())
            .lastRefreshedAt(domain.getLastRefreshedAt())
            .expiresAt(domain.getExpiresAt())
            .revoked(domain.isRevoked())
            .revokedAt(domain.getRevokedAt())
            .version(domain.getVersion())
            .build();
    }

    public Session entityToDomain(JdbcSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        SessionId sessionId = entity.getId() != null ? new SessionId(entity.getId().toString()) : null;
        UserId userId = entity.getUserId() != null ? new UserId(entity.getUserId()) : null;

        return Session.reconstitute(
            sessionId,
            userId,
            mapDeviceInfo(entity.getDeviceInfo()),
            entity.getGeneration() != null ? entity.getGeneration() : 1,
            entity.getIssuedAt(),
            entity.getLastRefreshedAt(),
            entity.getExpiresAt(),
            entity.getRevoked() != null ? entity.getRevoked() : false,
            entity.getRevokedAt(),
            entity.getVersion()
        );
    }

    public Optional<Session> entityToDomain(Optional<JdbcSessionEntity> entityOptional) {
        return entityOptional.map(this::entityToDomain);
    }

    // --- New Methods for List Mapping ---

    public List<JdbcSessionEntity> domainListToEntityList(List<Session> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        return domains.stream()
            .map(this::domainToEntity)
            .toList(); 
    }

    public List<Session> entityListToDomainList(List<JdbcSessionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
            .map(this::entityToDomain)
            .toList();
    }

    // --- Private Helper Methods ---

    private JdbcDeviceInfo mapDeviceInfo(DeviceInfo domainDeviceInfo) {
        if (domainDeviceInfo == null) {
            return null;
        }
        
        return new JdbcDeviceInfo(
            domainDeviceInfo.getIpAddress(),
            domainDeviceInfo.getCountry(),
            domainDeviceInfo.getRawUserAgent(),
            domainDeviceInfo.getDeviceType(),
            domainDeviceInfo.getDeviceBrand(),
            domainDeviceInfo.getOsName(),
            domainDeviceInfo.getOsVersion(),
            domainDeviceInfo.getBrowserName(),
            domainDeviceInfo.getBrowserVersion()
        );
    }

    private DeviceInfo mapDeviceInfo(JdbcDeviceInfo jdbcDeviceInfo) {
        if (jdbcDeviceInfo == null) {
            return null;
        }

        return DeviceInfo.builder()
            .ipAddress(jdbcDeviceInfo.ipAddress())
            .country(jdbcDeviceInfo.country())
            .rawUserAgent(jdbcDeviceInfo.rawUserAgent())
            .deviceType(jdbcDeviceInfo.deviceType())
            .deviceBrand(jdbcDeviceInfo.deviceBrand())
            .osName(jdbcDeviceInfo.osName())
            .osVersion(jdbcDeviceInfo.osVersion())
            .browserName(jdbcDeviceInfo.browserName())
            .browserVersion(jdbcDeviceInfo.browserVersion())
            .build();
    }
}