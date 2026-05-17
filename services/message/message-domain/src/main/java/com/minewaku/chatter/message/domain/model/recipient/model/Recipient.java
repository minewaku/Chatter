package com.minewaku.chatter.message.domain.model.recipient.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.message.domain.sharedkernel.value.BaseEntity;

import io.micrometer.common.lang.NonNull;
import lombok.Getter;
import lombok.ToString;

@Getter
@Table("Recipient")
@ToString
public class Recipient extends BaseEntity<RecipientId> {

    @Id
    @Embedded.Nullable
    private RecipientId id;

    @Column("join_date")
    private Instant joinDate;

    @PersistenceCreator
    private Recipient(
            @NonNull RecipientId id, 
            @NonNull Instant joinDate) {
                
        this.id = id;
        this.joinDate = joinDate;
    }

    static public Recipient createNew(
            @NonNull RecipientId id
    ) {
        return new Recipient(id, Instant.now());
    }
}
