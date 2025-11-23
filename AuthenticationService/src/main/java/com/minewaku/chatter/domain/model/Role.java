package com.minewaku.chatter.domain.model;

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

    // Private constructor
    private Role(@NonNull RoleId id, @NonNull String name, @NonNull Code code, @NonNull String description, @NonNull AuditMetadata auditMetadata, boolean isDeleted) {
        
    	if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.auditMetadata = auditMetadata;
		this.isDeleted = isDeleted;
    }

    // Static factory for creating new data
    public static Role createNew(@NonNull RoleId id, @NonNull String name, @NonNull Code code, @NonNull String description) {
        return new Role(id, name, code, description, new AuditMetadata(), false);
    }

    // Static factory for loading existing data
    public static Role reconstitute(@NonNull RoleId id, @NonNull String name, @NonNull Code code, @NonNull String description, @NonNull AuditMetadata auditMetadata, boolean isDeleted) {
        return new Role(id, name, code, description, auditMetadata, isDeleted);
    }

    public void setName(@Nonnull String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        this.name = name;
        this.auditMetadata.markUpdated();
    }

    public void setDescription(@Nonnull String description) {
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        this.description = description;
        this.auditMetadata.markUpdated();
    }
    
    public void softDelete() {
        this.isDeleted = true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
