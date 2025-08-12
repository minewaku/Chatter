package com.minewaku.chatter.routes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {

    @PostMapping("/api/auth/login")
    ResponseEntity<?> login(@RequestBody Map<String, Object> body);

    @PostMapping("/api/auth/register")
    ResponseEntity<?> register(@RequestBody Map<String, Object> body);

    @PostMapping("/api/auth/refresh")
    ResponseEntity<?> refresh(@RequestBody Map<String, Object> body);
}