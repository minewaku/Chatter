package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.event.jdbc;

import java.time.Instant;

/**
 * The message value of {@code identityaccess.user.v1} for a soft-deleted account. The field names are the contract
 * with consumers (see specs/013-account-deleted-outbox/data-model.md); it carries no personal data.
 */
record UserDeletedMessage(String eventId, String userId, Instant occurredAt) {}
