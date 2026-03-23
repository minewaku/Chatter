package com.minewaku.chatter.identityaccess.infrastructure.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
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
@Table(name = "\"user\"")
@DynamicUpdate
@SuperBuilder
public class JpaUserEntity extends BaseEntity implements UserDetails, CredentialsContainer {

	private static final long serialVersionUID = 1L;

	@Column(name = "email", nullable = false, unique = true, updatable = false)
	@Email(message = "Invalid email")
	@NotBlank(message = "Email is required")
	@NotNull(message = "Email cannot be null")
	private String email;

	@Column(name = "username", length = 255, nullable = false, unique = true, updatable = false)
	@NotBlank(message = "Username is required")
	@NotNull(message = "Username cannot be null")
	private String username;

	@Column(name = "birthday", length = 255, nullable = false)
	@Past(message = "Birthday must be in the past")
	@NotNull(message = "Birthday cannot be null")
	private LocalDate birthday;

	@Column(name = "is_enabled", nullable = false)
	@NotNull(message = "enabled is required")
	private Boolean enabled;

	@Column(name = "is_locked", nullable = false)
	@NotNull(message = "locked is required")
	private Boolean locked;

	@Column(name = "is_deleted", nullable = false)
	@NotNull(message = "deleted is required")
	private Boolean deleted;

	@Column(name = "deleted_at", nullable = true)
	private Instant deletedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "hashed_password", nullable = false)
	@NotBlank(message = "hashedPassword is required")
	@NotNull(message = "hashedPassword cannot be null")
	private String hashedPassword;

	@Column(name = "password_modified_at")
	private Instant passwordModifiedAt;

	@Column(name = "algorithm",  nullable = false)
    private String algorithm;
	
	@Column(name = "hash",  nullable = false)
    private String hash;
	
	@JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "salt", nullable = false, length = 16)
    private byte[] salt;




	@PrePersist
	protected void onCreate() {
		super.onCreate();

		if (enabled == null) {
			enabled = false;
		}

		if (locked == null) {
			locked = false;
		}

		if (deleted == null) {
			deleted = false;
		}
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !locked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void eraseCredentials() {
		this.deleted = true;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return null;
	}

	@Override
	public String getPassword() {
		return null;
	}
}
