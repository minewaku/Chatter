package com.minewaku.chatter.adapter.entity;

import com.minewaku.chatter.adapter.entity.embededKey.JpaUserRoleId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
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
@EqualsAndHashCode()

@Entity
@Table(name = "user_role")
@Builder
public class JpaUserRoleEntity {
	
    @EmbeddedId
    private JpaUserRoleId id;
    
    //these references are used only for joins, not for direct access
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private JpaUserEntity user;

    //these references are used only for joins, not for direct access
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private JpaRoleEntity role;
    
    @Column(name = "created_by", updatable = false)
    private Long createdBy;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;
}
