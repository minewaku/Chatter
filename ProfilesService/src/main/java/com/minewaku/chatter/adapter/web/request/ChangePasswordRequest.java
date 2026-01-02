package com.minewaku.chatter.adapter.web.request;

public record ChangePasswordRequest(String email, String password, String newPassword) {};
