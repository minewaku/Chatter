package com.minewaku.chatter.domain.value.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
	
	@JsonCreator
	public UserRoleId(
			@JsonProperty("userId") @NonNull UserId userId, 
			@JsonProperty("roleId") @NonNull RoleId roleId) {
		
		this.userId = userId;
		this.roleId = roleId;
	}
}
