package com.minewaku.chatter.identityaccess.presentation.authentication;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Component;

/**
 * Turns a {@code User-Agent} header into the short device label stored with a session, for example
 * {@code Chrome on Linux (Desktop)}. The label is never blank and never longer than 200 characters.
 */
@Component
public class DeviceLabelResolver {

    static final String UNKNOWN_DEVICE = "Unknown device";
    static final int MAX_LABEL_LENGTH = 200;
    private static final int MAX_PARSED_LENGTH = 1_000;

    private final UserAgentAnalyzer analyzer;

    public DeviceLabelResolver(UserAgentAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String resolve(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN_DEVICE;
        }
        String bounded = userAgent.length() > MAX_PARSED_LENGTH ? userAgent.substring(0, MAX_PARSED_LENGTH) : userAgent;
        UserAgent parsed = analyzer.parse(bounded);
        String agent = known(parsed.getValue("AgentName"));
        String system = known(parsed.getValue("OperatingSystemName"));
        String deviceClass = known(parsed.getValue("DeviceClass"));

        StringBuilder label = new StringBuilder();
        if (agent != null) {
            label.append(agent);
        }
        if (system != null) {
            label.append(label.isEmpty() ? "" : " on ").append(system);
        }
        if (deviceClass != null) {
            label.append(label.isEmpty() ? deviceClass : " (" + deviceClass + ")");
        }
        if (label.isEmpty()) {
            return UNKNOWN_DEVICE;
        }
        return label.length() > MAX_LABEL_LENGTH ? label.substring(0, MAX_LABEL_LENGTH) : label.toString();
    }

    private static String known(String value) {
        if (value == null || value.isBlank() || "Unknown".equalsIgnoreCase(value) || "??".equals(value)) {
            return null;
        }
        return value;
    }
}
