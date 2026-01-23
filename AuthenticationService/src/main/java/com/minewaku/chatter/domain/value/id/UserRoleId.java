package com.minewaku.chatter.domain.value.id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class UserRoleId {
	
	@NonNull
	private final UserId userId;
	
	@NonNull
	private final RoleId roleId;
	
	public UserRoleId(
			@NonNull UserId userId, 
			@NonNull RoleId roleId) {
		
		this.userId = userId;
		this.roleId = roleId;
	}
}
