package com.minewaku.chatter.adapter.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

import org.hibernate.annotations.DynamicUpdate;
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

	@Column(name = "avatar_key", unique = true)
	private String avatarKey;
	@Column(name = "avatar", unique = true)
	private String avatar;

	@Column(name = "cover_key", unique = true)
	private String coverKey;
	@Column(name = "cover", unique = true)
	private String cover;

	@Column(name = "username", length = 255, nullable = false, unique = true, updatable = false)
	@NotBlank(message = "Username is required")
	@NotNull(message = "Username cannot be null")
	private String username;

	@Column(name = "display_name", length = 255)
	private String displayName;

	@Column(name = "bio", length = 255)
	private String bio;

	@Column(name = "birthday", length = 255, nullable = false)
	@Past(message = "Birthday must be in the past")
	@NotNull(message = "Birthday cannot be null")
	private LocalDate birthday;

	@Column(name = "discoverable", nullable = false)
	@NotNull(message = "Discoverable is required")
	private Boolean discoverable;

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

		if(discoverable == null) {
			discoverable = true;
		}

		if (isEnabled == null) {
			isEnabled = false;
		}

		if (isLocked == null) {
			isLocked = false;
		}

		if (isDeleted == null) {
			isDeleted = false;
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
