package com.minewaku.chatter.message.domain.model.invite.model;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.invite.exception.InviteExpiredException;
import com.minewaku.chatter.message.domain.model.invite.exception.InviteUsedUpException;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.domain.sharedkernel.value.BaseEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Table("invite")
@ToString
public class Invite extends BaseEntity<InviteId> implements Persistable<InviteId> {

    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(30);

    @Id
    @Embedded.Nullable
    private InviteId id;

    @Embedded.Nullable
    private Code code;

    @Embedded.Nullable
    private GuildId guildId;

    @Embedded.Nullable
    private UserId inviterId;

    @Column("max_uses")
    private int maxUses;

    @Embedded.Nullable
    private int uses;

    @Column("duration")
    private Duration duration;

    @Column("created_at")
    private Instant createdAt;

    @Column("expired_at")
    private Instant expiredAt;

    private Invite(
        @NonNull InviteId id,
        @NonNull Code code,
        @NonNull GuildId guildId,
        @NonNull UserId inviterId,
        int maxUses,
        int uses,
        Duration duration,
        Instant createdAt,
        Instant expiredAt
    ) {
        this.id = id;
        this.code = code;
        this.guildId = guildId;
        this.inviterId = inviterId;
        this.maxUses = maxUses;
        this.uses = uses;
        this.duration = duration;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
    }

    public static Invite createNew(
        @NonNull InviteId id,
        @NonNull Code code,
        @NonNull GuildId guildId,
        @NonNull UserId inviterId,
        int maxUses,
        Duration duration
    ) {
        return new Invite(
            id,
            code, 
            guildId, 
            inviterId, 
            maxUses, 
            0, 
            duration != null ? duration : DEFAULT_DURATION, 
            Instant.now(), 
            Instant.now().plus(duration));
    }

    public static Invite reconstitute(
        @NonNull InviteId id,
        @NonNull Code code,
        @NonNull GuildId guildId,
        @NonNull UserId inviterId,
        int maxUses,
        int uses,
        Duration duration,
        Instant createdAt,
        Instant expiredAt
    ) {
        return new Invite(
            id,
            code, 
            guildId, 
            inviterId, 
            maxUses, 
            uses, 
            duration, 
            createdAt, 
            expiredAt);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    public boolean validateExpiration() {
        // Kiểm tra điều kiện hết hạn về mặt thời gian
        if (expiredAt != null && Instant.now().isAfter(expiredAt)) {
            throw new InviteExpiredException("invite expired at: " + expiredAt);
        }
        
        // Kiểm tra điều kiện hết lượt sử dụng
        if (maxUses > 0 && uses >= maxUses) {
            throw new InviteUsedUpException("invite used up at: " + Instant.now());
        }

        return false;
    }
}
