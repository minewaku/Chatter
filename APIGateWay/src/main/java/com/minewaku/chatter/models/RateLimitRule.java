package com.minewaku.chatter.models;

import java.time.Duration;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

@Builder
public class RateLimitRule {
	private String pathPattern;
	private Boolean isApplied;
	private List<String> roles;
	private Integer initialTokens;
	private Integer capacity;
	private Integer refilltokens;
	private Duration period;

	
	public boolean pathMatches(String requestPath) {
        return requestPath.matches(pathPattern);
    }
	
	public boolean rolesMatches(List<String> requestRoles) {
		if (requestRoles == null || requestRoles.isEmpty()) {
            return true;
        }
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return requestRoles.stream().anyMatch(roles::contains);
	}
}
