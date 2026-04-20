package com.minewaku.chatter.identityaccess.infrastructure.cache.mapper;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;
import com.minewaku.chatter.identityaccess.application.shared.DeviceInfoDto;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.dto.JdbcDeviceInfo;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity.JdbcSessionEntity;

@Component
public class RedisSessionMapper {
    public SessionReadModel entityToModel(JdbcSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        return new SessionReadModel(
                entity.getId() != null ? entity.getId().toString() : "unknown-session-id",
                
                entity.getUserId(),
                
                mapDeviceInfo(entity.getDeviceInfo()),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getLastRefreshedAt()
        );
    }

    private DeviceInfoDto mapDeviceInfo(JdbcDeviceInfo jdbcDeviceInfo) {
        if (jdbcDeviceInfo == null) {
            return new DeviceInfoDto(null, null, null, null, null);
        }

        return new DeviceInfoDto(
                jdbcDeviceInfo.ipAddress(),
                jdbcDeviceInfo.country(),
                jdbcDeviceInfo.deviceType(),
                jdbcDeviceInfo.osName(),
                jdbcDeviceInfo.browserName()
        );
    }
}
