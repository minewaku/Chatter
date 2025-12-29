package com.minewaku.chatter.adapter.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.domain.port.out.service.LinkGenerator;
import com.minewaku.chatter.domain.value.MailType;

@Service
public class LinkProvider implements LinkGenerator {

	@Override
	public String generate(MailType mailType, Map<String, String> parameters) {
	    StringBuilder linkBuilder = new StringBuilder();
	    linkBuilder.append(mailTypeToPath(mailType));

	    if (parameters != null && !parameters.isEmpty()) {
	        linkBuilder.append("?");
	        parameters.forEach((key, value) -> {
	            if (key != null && value != null) {
	                try {
	                    String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
	                    String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
	                    linkBuilder.append(encodedKey).append("=").append(encodedValue).append("&");
	                } catch (Exception exception) {
	                    throw new RuntimeException("Unable to encode URL parameter: " + key + "=" + value, exception);
	                }
	            }
	        });
	        // Remove the trailing '&' only if one was added
	        if (linkBuilder.charAt(linkBuilder.length() - 1) == '&') {
	            linkBuilder.setLength(linkBuilder.length() - 1);
	        }
	    }

	    return linkBuilder.toString();
	}

	private String mailTypeToPath(MailType mailType) {
	    return switch (mailType) {
	        case EMAIL_CONFIRMATION -> "http://localhost:5001/auth-service/v1/auth/verify";
	        default -> throw new IllegalArgumentException("Invalid email type: " + mailType);
	    };
	}


}
