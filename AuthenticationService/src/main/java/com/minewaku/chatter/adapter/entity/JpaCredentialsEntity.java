package com.minewaku.chatter.adapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
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
@Table(name = "credentials")
@SuperBuilder
public class JpaCredentialsEntity extends BaseEntity {
	
    //these references are used only for joins, not for direct access
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private JpaUserEntity user;
    
    @Column(name = "user_id", nullable = false, updatable = false)
    private Integer userId;
    
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

}
