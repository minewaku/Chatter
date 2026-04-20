package com.minewaku.chatter.identityaccess.domain.aggregate.session.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class DeviceInfo {

    @NonNull
    private final String ipAddress;

    @NonNull
    private final String country;

    @NonNull
    private final String rawUserAgent;

    @NonNull
    private final String deviceType;       // Desktop | Phone | Tablet | Robot
    
    @NonNull
    private final String deviceBrand;      // Apple | Samsung | ...
    
    @NonNull
    private final String osName;           // Windows | macOS | Android | iOS
    
    @NonNull
    private final String osVersion;        // 14.0 | 11 | ...
    
    @NonNull
    private final String browserName;      // Chrome | Firefox | Safari | ...
    
    @NonNull
    private final String browserVersion;   // 120.0.0 | ...

    public void validateDeviceInfo(DeviceInfo incomingDeviceInfo) {
        if (!this.equals(incomingDeviceInfo)) {
            throw new IllegalArgumentException("Device information mismatch. Possible token theft detected.");
        }
    }
}