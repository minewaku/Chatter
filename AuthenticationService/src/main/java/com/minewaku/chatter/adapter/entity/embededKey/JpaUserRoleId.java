package com.minewaku.chatter.adapter.entity.embededKey;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JpaUserRoleId {

	@Column(name = "user_id")
    private Long userId;

    @Column(name = "role_id")
    private Long roleId;
}

