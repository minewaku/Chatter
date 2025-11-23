package com.minewaku.chatter.adapter.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

@MappedSuperclass
public abstract class BaseEntity {
    
    @Id
	@Column(name = "id")
	private Long id;

    @Column(name = "created_date", nullable = false, updatable = false)
    @NotNull(message = "Created date cannot be null")
    private Instant createdDate;

    @Column(name = "modified_date")
    private Instant modifiedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = Instant.now();
    }
}
