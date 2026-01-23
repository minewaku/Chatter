package com.minewaku.chatter.adapter.service.impl;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.minewaku.chatter.adapter.service.IPdpService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PdpService implements IPdpService {

    private final static String PDP_URL = "http://CERBOS-BRIDGE-SERVICE/api/v1/resources/check";
    private final RestTemplate restTemplate;

    public PdpService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static class PdpResponse {
        public boolean allowed;
    }

    // private static record PdpRequest(String principal, String[] roles, String
    // resource, String[] actions) {}
    private static record PdpRequest(
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> resourceAttrs) {
    }

    public boolean isAccessAllowed(
            String resourceType, 
            String resourceId, 
            String action,
            Map<String, Object> resourceAttrs) {
        PdpRequest requestBody = new PdpRequest(resourceType, resourceId, action, resourceAttrs);

        log.info("PDP request: {}", requestBody);

        // get auth object from SecurityContextHolder
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String jwt = auth.getToken().getTokenValue();

        // create httpEntity with authorization header abd pdpRequest
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PdpRequest> entity = new HttpEntity<>(requestBody, headers);

        // call Pdp service with restTemplate
        ResponseEntity<PdpResponse> response = restTemplate.exchange(PDP_URL, HttpMethod.POST, entity,
                PdpResponse.class);

        return response.getBody() != null && response.getBody().allowed;
    }
}
