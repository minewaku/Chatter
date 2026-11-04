package com.minewaku.chatter.profilenext.profile.internal.model;

/** A provider file waiting to be deleted, with the number of failed attempts so far. */
public record PendingRemoval(String publicId, ImageSlot slot, int attempts) {}
