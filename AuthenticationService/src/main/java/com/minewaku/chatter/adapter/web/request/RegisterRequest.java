package com.minewaku.chatter.adapter.web.request;

import java.time.LocalDate;

public record RegisterRequest(String email, String username, LocalDate birthday, String password) {
}
