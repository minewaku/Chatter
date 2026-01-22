package com.minewaku.chatter.domain.model;

import com.minewaku.chatter.domain.value.id.UserId;
import com.minewaku.chatter.domain.value.id.UserRoleId;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class UserRole {

	@NonNull
	private final UserRoleId userRoleId;
	
	private UserId createdBy;
	
	// Private constructor
	private UserRole(
			@NonNull UserRoleId userRoleId, 
			UserId createdBy) {
		
		this.userRoleId = userRoleId;
		this.createdBy = createdBy;
	}
	

	 /*
    * STATIC FACTORIES
    */
	public static UserRole reconstitute(
				@NonNull UserRoleId userRoleId,
				UserId createdBy) {

        return new UserRole(userRoleId, createdBy);
    }


	public static UserRole createNew(
				@NonNull UserRoleId userRoleId,
				UserId createdBy) {
		
        return new UserRole(userRoleId, createdBy);
    }

	
	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (o == null || getClass() != o.getClass()) return false;
	    UserRole userRole = (UserRole) o;
	    return userRoleId.equals(userRole.userRoleId);
	}

	@Override
	public int hashCode() {
	    return userRoleId.hashCode();
	}

}
