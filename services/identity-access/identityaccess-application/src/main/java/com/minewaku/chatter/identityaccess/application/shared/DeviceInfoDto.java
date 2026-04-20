package com.minewaku.chatter.identityaccess.application.shared;

public record DeviceInfoDto(
    String ipAddress,
    String country,
    String deviceType,
    String osName,
    String browserName
) {
}
