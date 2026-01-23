package com.minewaku.chatter.adapter.entity;

import java.time.Instant;

import org.springframework.data.annotation.Version;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode

@Entity
@Table(name = "credentials")
@Builder
public class JpaCredentialsEntity {

	@Id
	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

	// these references are used only for joins, not for direct access
	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private JpaUserEntity user;

	@Column(name = "hashed_password", length = 255, nullable = false)
	@NotBlank(message = "hashedPassword is required")
	@NotNull(message = "hashedPassword cannot be null")
	private String hashedPassword;

	@Column(name = "algorithm", length = 50, nullable = false)
	@NotBlank(message = "algorithm is required")
	@NotNull(message = "algorithm cannot be null")
	private String algorithm;

	@Column(name = "salt", nullable = false, length = 128)
	@NotNull(message = "salt cannot be null")
	private byte[] salt;

	@Column(name = "created_at", nullable = false, updatable = false)
	@NotNull(message = "Created date cannot be null")
	private Instant createdAt;

	@Column(name = "modified_at")
	private Instant modifiedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
	}

	@PreUpdate
	protected void onUpdate() {
		modifiedAt = Instant.now();
	}

}
