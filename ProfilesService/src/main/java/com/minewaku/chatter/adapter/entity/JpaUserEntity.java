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

	@Column(name = "banner_key", unique = true)
	private String bannerKey;
	@Column(name = "banner", unique = true)
	private String banner;

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

	@Column(name = "is_enabled", nullable = false)
	@NotNull(message = "isEnabled is required")
	private Boolean enabled;

	@Column(name = "is_locked", nullable = false)
	@NotNull(message = "isLocked is required")
	private Boolean locked;

	@Column(name = "is_deleted", nullable = false)
	@NotNull(message = "isDeleted is required")
	private Boolean deleted;

	@Column(name = "deleted_at", nullable = true)
	private Instant deletedAt;

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
		// TODO Auto-generated method stub
		return null;
	}
}
