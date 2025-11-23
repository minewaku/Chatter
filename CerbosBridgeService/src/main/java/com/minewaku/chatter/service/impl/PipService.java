package com.minewaku.chatter.service.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.minewaku.chatter.exception.ApiException;
import com.minewaku.chatter.service.IPipService;

@Service
public class PipService implements IPipService {

    private final String PIP_URL = "http://localhost:5005/pip/api/v1/attributes";

    private final RestTemplate restTemplate;

    public PipService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public PipResponse sendPipRequest(PipRequest request) {
        try {
            ResponseEntity<PipResponse> response = restTemplate.postForEntity(
                    PIP_URL,
                    request,
                    PipResponse.class);
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ApiException("Error response from PIP service", e.getStatusCode().value());
        } catch (Exception e) {
            throw new ApiException("Error response from PIP service", 500);
        }
    }

}
