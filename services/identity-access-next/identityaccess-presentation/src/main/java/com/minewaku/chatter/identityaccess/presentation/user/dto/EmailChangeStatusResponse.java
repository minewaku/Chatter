package com.minewaku.chatter.identityaccess.presentation.user.dto;

/** {@code status} is {@code confirmed} or {@code already_confirmed}. */
public record EmailChangeStatusResponse(String status) {

    public static EmailChangeStatusResponse confirmed() {
        return new EmailChangeStatusResponse("confirmed");
    }

    public static EmailChangeStatusResponse alreadyConfirmed() {
        return new EmailChangeStatusResponse("already_confirmed");
    }
}
