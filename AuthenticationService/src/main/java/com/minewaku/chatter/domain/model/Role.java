package com.minewaku.chatter.domain.model;

import java.time.Instant;

import com.minewaku.chatter.domain.exception.DomainValidationException;
import com.minewaku.chatter.domain.exception.StateAlreadySatisfiedException;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Code;
import com.minewaku.chatter.domain.value.id.RoleId;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class Role {

    @NonNull
    private final RoleId id;

    @Nonnull
    private String name;

    @NonNull
    private final Code code;

    @NonNull
    private String description;

    @NonNull
    private final AuditMetadata auditMetadata;

    @Setter
    private boolean isDeleted;

    private Instant deletedAt;

    // Private constructor
    private Role(
            @NonNull RoleId id,
            @NonNull String name,
            @NonNull Code code,
            @NonNull String description,
            @NonNull AuditMetadata auditMetadata,
            boolean isDeleted,
            Instant deletedAt
        ) {

        if (name.isBlank()) {
            throw new DomainValidationException("Name cannot be blank");
        }
        if (description.isBlank()) {
            throw new DomainValidationException("Description cannot be blank");
        }
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.auditMetadata = auditMetadata;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
    }

    // Static factory for creating new data
    public static Role createNew(
            @NonNull RoleId id,
            @NonNull String name,
            @NonNull Code code,
            @NonNull String description) {

        return new Role(id, name, code, description, new AuditMetadata(), false, null);
    }

    // Static factory for loading existing data
    public static Role reconstitute(
            @NonNull RoleId id,
            @NonNull String name,
            @NonNull Code code,
            @NonNull String description,
            @NonNull AuditMetadata auditMetadata,
            boolean isDeleted,
            Instant deletedAt) {

        return new Role(id, name, code, description, auditMetadata, isDeleted, deletedAt);
    }

    public void setName(@Nonnull String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if(this.name.equals(name)) {
            return;
        }
        this.name = name;
        this.auditMetadata.markUpdated();
    }

    public void setDescription(@Nonnull String description) {
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if(this.description.equals(description)) {
            return;
        }
        this.description = description;
        this.auditMetadata.markUpdated();
    }

    public void softDelete() {
        if(this.isDeleted) {
            throw new StateAlreadySatisfiedException("Role is already soft deleted");
        }
        this.isDeleted = true;
        this.deletedAt = Instant.now();
    }

    public void restore() {
        if(!this.isDeleted) {
            throw new StateAlreadySatisfiedException("Role is not soft deleted");
        }
        this.isDeleted = false;
        this.deletedAt = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Role role = (Role) o;
        return id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
