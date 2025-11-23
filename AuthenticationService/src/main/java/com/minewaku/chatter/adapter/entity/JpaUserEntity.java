package com.minewaku.chatter.adapter.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
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
@Table(name = "user")
@DynamicUpdate
@SuperBuilder
public class JpaUserEntity extends BaseEntity implements UserDetails, CredentialsContainer {	
	
	private static final long serialVersionUID = 1L;
	
	// This relationship exists only for join/cascade mapping, not for direct access
	@Getter(AccessLevel.NONE)
	@Builder.Default
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<JpaUserRoleEntity> userRoles = new HashSet<>();

	@Column(name = "email", nullable = false, unique = true, updatable = false)
	@Email(message = "Invalid email")
	@NotBlank(message = "Email is required")
	@NotNull(message = "Email cannot be null")
	private String email;
	
	@Column(name = "username", length = 255, nullable = false, unique = true, updatable = false)
	@NotBlank(message = "Username is required")
	@NotNull(message = "Username cannot be null")
	private String username;

	@Column(name = "birthday", length = 255, nullable = false, unique = true)
	@NotBlank(message = "Birthday is required")
	@NotNull(message = "Birthday cannot be null")
	private LocalDate birthday;

	@Column(name = "is_enabled", nullable = false)
	@NotNull(message = "isEnabled is required")
	private Boolean isEnabled;
	
	@Column(name = "is_locked", nullable = false)
	@NotNull(message = "isLocked is required")
	private Boolean isLocked;
	
	@Column(name = "is_deleted", nullable = false)
	@NotNull(message = "isDeleted is required")
	private Boolean isDeleted;
	
	@Column(name = "deleted_at", nullable = true)
	private Instant deletedAt;
	
	
	@PrePersist
	protected void onCreate() {
		super.onCreate();

		if (isEnabled == null) {
			isEnabled = false;
		}
		
		if (isLocked == null) {			
			isLocked = false;
		}
		
		if( isDeleted == null) {
			isDeleted = false;			
		}
	}
	
	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	
	
	@Override
	public boolean isAccountNonLocked() {
		return !isLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void eraseCredentials() {
		this.isDeleted = true;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return null;
	}

	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return null;
	}
}
