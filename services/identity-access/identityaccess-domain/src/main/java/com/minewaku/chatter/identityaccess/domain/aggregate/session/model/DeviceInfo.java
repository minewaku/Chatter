package com.minewaku.chatter.identityaccess.domain.aggregate.session.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class DeviceInfo {
    // private final String userAgent;
    @NonNull
    private final String ipAddress;

    // used Yauaa to extract device info from http request header
    // private final String deviceType;  // VD: "WEB_BROWSER", "DESKTOP_APP", "MOBILE_APP"
    // private final String os;          // VD: "Windows 11", "macOS", "iOS"
    // private final String browser;     // VD: "Chrome", "Safari", "N/A" (nếu là Desktop App)

    // used GeoIP
    // // 4. Quản lý trạng thái và rủi ro (Security & Lifecycle)
    // private final String location;    // Map từ IP ra vị trí (VD: "Ho Chi Minh City, Vietnam")
    // private final Instant loginAt;    // Thời điểm thiết bị này đăng nhập lần đầu
    // private final Instant lastActive; // Cập nhật mỗi khi có request tới, dùng để check session idle

    public DeviceInfo(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    

    public void validateDeviceInfo(DeviceInfo incomingDeviceInfo) {
        if (!this.equals(incomingDeviceInfo)) {
            throw new IllegalArgumentException("Device information mismatch. Possible token theft detected.");
        }
    }
}
