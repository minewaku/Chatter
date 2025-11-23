package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.entity.JpaCredentialsEntity;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.value.HashedPassword;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class CredentialsMapper {
    public Credentials entityToDomain(JpaCredentialsEntity entity) {
        if (entity == null) return null;
        UserId userId = entity.getUserId() != null ? new UserId(entity.getUserId().longValue()) : null;
        byte[] saltBytes = (entity.getSalt() != null && entity.getSalt().length > 0)
                ? entity.getSalt()
                : new byte[] { 0x00 }; // minimal non-empty placeholder to satisfy domain constraint
        HashedPassword hashedPassword = new HashedPassword(
                entity.getAlgorithm(),
                entity.getHashedPassword(),
                saltBytes
        );
        return Credentials.createNew(userId, hashedPassword);
    }

    public JpaCredentialsEntity domainToEntity(Credentials domain) {
        if (domain == null) return null;
        JpaCredentialsEntity entity = new JpaCredentialsEntity();
        // set foreign key userId; the JpaUserEntity reference can be set by caller if needed
        if (domain.getUserId() != null) {
            entity.setUserId(Math.toIntExact(domain.getUserId().getValue()));
        }
        entity.setAlgorithm(domain.getHashedPassword().algorithm());
        entity.setHashedPassword(domain.getHashedPassword().hash());
        entity.setSalt(domain.getHashedPassword().salt());
        return entity;
    }

    public Optional<Credentials> entityToDomain(Optional<JpaCredentialsEntity> entity) {
        return entity.map(this::entityToDomain);
    }
}
