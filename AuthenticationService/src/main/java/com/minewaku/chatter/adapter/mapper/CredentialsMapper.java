package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.entity.JpaCredentialsEntity;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.value.HashedPassword;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class CredentialsMapper {

    public Credentials entityToDomain(JpaCredentialsEntity entity) {
        if (entity == null) return null;

        return Credentials.reconstitute(
            mapToUserId(entity.getUserId()),
            mapToHashedPassword(entity),
            entity.getModifiedAt()
        );
    }

    
    public JpaCredentialsEntity domainToEntity(Credentials domain) {
        if (domain == null) return null;

        HashedPassword hp = domain.getHashedPassword();

        return JpaCredentialsEntity.builder()
            .userId(unwrapValue(domain.getUserId(), UserId::getValue))
            .algorithm(hp != null ? hp.algorithm() : null)
            .hashedPassword(hp != null ? hp.hash() : null)
            .salt(hp != null ? hp.salt() : null)
            .modifiedAt(domain.getModifiedAt())
            .build();
    }

    
    public Optional<Credentials> entityToDomain(Optional<JpaCredentialsEntity> entity) {
        return entity.map(this::entityToDomain);
    }



    private UserId mapToUserId(Long id) {
        return id != null ? new UserId(id) : null;
    }

    private HashedPassword mapToHashedPassword(JpaCredentialsEntity entity) {
        byte[] saltBytes = (entity.getSalt() != null && entity.getSalt().length > 0)
                ? entity.getSalt()
                : new byte[] { 0x00 };

        return new HashedPassword(
            entity.getAlgorithm(),
            entity.getHashedPassword(),
            saltBytes
        );
    }

    private <T, R> R unwrapValue(T valueObject, Function<T, R> extractor) {
        return valueObject != null ? extractor.apply(valueObject) : null;
    }
}