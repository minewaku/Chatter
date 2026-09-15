package com.minewaku.chatter.identityaccess.domain.aggregate.session.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class DeviceInfo {

    private final String ipAddress;

    private final String country;

    private final String rawUserAgent;

    private final String deviceType;

    private final String deviceBrand;

    private final String osName;

    private final String osVersion;

    private final String browserName;

    private final String browserVersion;

    public DeviceInfo(
            @NonNull String ipAddress,
            @NonNull String country,
            @NonNull String rawUserAgent,
            @NonNull String deviceType,
            @NonNull String deviceBrand,
            @NonNull String osName,
            @NonNull String osVersion,
            @NonNull String browserName,
            @NonNull String browserVersion) {
        this.ipAddress = ipAddress;
        this.country = country;
        this.rawUserAgent = rawUserAgent;
        this.deviceType = deviceType;
        this.deviceBrand = deviceBrand;
        this.osName = osName;
        this.osVersion = osVersion;
        this.browserName = browserName;
        this.browserVersion = browserVersion;
    }

    public void validateDeviceInfo(DeviceInfo incomingDeviceInfo) {
        if (!this.equals(incomingDeviceInfo)) {
            throw new IllegalArgumentException("Device information mismatch. Possible token theft detected.");
        }
    }
}