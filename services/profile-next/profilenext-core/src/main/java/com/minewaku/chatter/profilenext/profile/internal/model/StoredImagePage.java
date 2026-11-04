package com.minewaku.chatter.profilenext.profile.internal.model;

import java.util.List;

/** One page of the provider's listing; {@code nextCursor} is null on the last page. */
public record StoredImagePage(List<StoredImageInfo> items, String nextCursor) {}
