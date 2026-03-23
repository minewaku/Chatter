package com.minewaku.chatter.identityaccess.infrastructure.cache.mapper;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.DeviceInfo;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.infrastructure.cache.dto.SessionCacheDto;
import com.minewaku.chatter.identityaccess.infrastructure.cache.dto.SessionCacheDto.DeviceInfoDto;

@Component
public class SessionMapper {

    public SessionCacheDto domainToDto(Session domain) {
        if (domain == null) return null;

        return new SessionCacheDto(
            unwrapValue(domain.getSessionId(), SessionId::getValue),
            unwrapValue(domain.getUserId(), UserId::getValue),
            mapToDeviceInfoDto(domain.getDeviceInfo()),
            domain.getGeneration(),
            domain.getIssuedAt(),
            domain.getExpiresAt(),
            domain.getLastRefreshedAt(),
            domain.isRevoked(),
            domain.getRevokedAt()
        );
    }

    public Session dtoToDomain(SessionCacheDto dto) {
        if (dto == null) return null;

        return Session.reconstitute(
            mapToSessionId(dto.sessionId()),
            mapToUserId(dto.userId()),
            mapToDeviceInfo(dto.deviceInfo()),
            dto.generation(),
            dto.issuedAt(),
            dto.lastRefreshedAt(),
            dto.expiresAt(),
            dto.revoked(),
            dto.revokedAt()
        );
    }

    public Optional<SessionCacheDto> domainToDto(Optional<Session> domain) {
        return domain.map(this::domainToDto);
    }

    public Optional<Session> dtoToDomain(Optional<SessionCacheDto> dto) {
        return dto.map(this::dtoToDomain);
    }

    // --- Private Helper Methods ---

    private SessionId mapToSessionId(String sessionId) {
        return sessionId != null ? new SessionId(sessionId) : null;
    }

    private UserId mapToUserId(Long userId) {
        return userId != null ? new UserId(userId) : null;
    }


    private DeviceInfoDto mapToDeviceInfoDto(DeviceInfo deviceInfo) {
        if (deviceInfo == null) return null;
        return new DeviceInfoDto(deviceInfo.getIpAddress());
    }


    private DeviceInfo mapToDeviceInfo(DeviceInfoDto dto) {
        if (dto == null) return null;
        return new DeviceInfo(dto.ipAddress());
    }

    private <T, R> R unwrapValue(T valueObject, Function<T, R> extractor) {
        return valueObject != null ? extractor.apply(valueObject) : null;
    }
}