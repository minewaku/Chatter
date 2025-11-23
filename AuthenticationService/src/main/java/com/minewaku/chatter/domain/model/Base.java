package com.minewaku.chatter.domain.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public abstract class Base {
    @NonNull
    private final Instant createdAt;
    @Setter
    private Instant modifiedAt;

    protected Base() {
        this.createdAt = Instant.now();
        this.modifiedAt = null;
    }

    protected void markUpdated() {
        Instant now = Instant.now();
        if (now.isBefore(this.createdAt)) {
            throw new IllegalStateException("Modified date cannot be before the created date");
        }
        this.modifiedAt = now;
    }
}
