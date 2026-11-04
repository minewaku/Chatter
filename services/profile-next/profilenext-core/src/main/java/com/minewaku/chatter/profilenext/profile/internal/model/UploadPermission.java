package com.minewaku.chatter.profilenext.profile.internal.model;

import java.time.Instant;
import java.util.Map;

/** What a client needs to upload one file straight to the provider: send {@code fields} plus the file to the URL. */
public record UploadPermission(String uploadUrl, String method, Map<String, String> fields, Instant expiresAt) {}
