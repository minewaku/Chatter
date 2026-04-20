package com.minewaku.chatter.identityaccess.presentation.web.request;

import java.time.LocalDate;

public record RegisterRequest(String email, String username, LocalDate birthday, String password) {
}
