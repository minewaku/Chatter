package com.minewaku.chatter.adapter.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

@Entity
@Table(name = "role")
@SuperBuilder
public class JpaRoleEntity extends BaseEntity {
    
	// This relationship exists only for join/cascade mapping, not for direct access
	@Getter(AccessLevel.NONE)
	@Builder.Default
	@OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<JpaUserRoleEntity> userRoles = new HashSet<>();

    @Column(name = "name", length = 255, nullable = false, unique = true)
    @NotBlank(message = "Name is required")
    @NotNull(message = "Name cannot be null")
    private String name;
    
    @Column(name = "code", length = 255, nullable = false, unique = true, updatable = false)
    @NotBlank(message = "Code is required")
    @NotNull(message = "Code cannot be null")
    private String code;

    @Column(name = "description", length = 255, nullable = false)
    @NotBlank(message = "Description is required")
    @NotNull(message = "Description cannot be null")
    private String description;
    
	@Column(name = "is_deleted", nullable = false)
	@NotNull(message = "deleted is required")
	private Boolean deleted;

	@Column(name = "deleted_at", nullable = true)
	private Instant deletedAt;
}