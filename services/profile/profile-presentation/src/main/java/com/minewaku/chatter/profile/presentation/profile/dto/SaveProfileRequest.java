package com.minewaku.chatter.profile.presentation.profile.dto;

/**
 * Both fields are optional: a field that is left out is {@code null} and means "unchanged". There are no Bean
 * Validation annotations on purpose; the domain validates, so the rules and their messages live in one place.
 */
public record SaveProfileRequest(String displayName, String bio) {}
