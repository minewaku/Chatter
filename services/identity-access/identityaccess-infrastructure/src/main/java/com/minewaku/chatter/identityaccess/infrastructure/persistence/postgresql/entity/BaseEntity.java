package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseEntity {
    
    @Id
    private Long id;

    @Version
    private Integer version;

    @Column("created_at")
    @CreatedDate
    @NotNull(message = "Created date cannot be null")
    private Instant createdAt;

    @Column("modified_at")
    @LastModifiedDate 
    private Instant modifiedAt;
}