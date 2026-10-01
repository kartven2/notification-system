package com.notifications.dto;

import com.notifications.model.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for registering a device.
 */
public class DeviceRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Device token is required")
    private String token;

    @NotNull(message = "Platform is required (IOS or ANDROID)")
    private DevicePlatform platform;

    public DeviceRequest() {}

    public DeviceRequest(Long userId, String token, DevicePlatform platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long userId;
        private String token;
        private DevicePlatform platform;
        public Builder userId(Long u) { this.userId = u; return this; }
        public Builder token(String t) { this.token = t; return this; }
        public Builder platform(DevicePlatform p) { this.platform = p; return this; }
        public DeviceRequest build() { return new DeviceRequest(userId, token, platform); }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public DevicePlatform getPlatform() { return platform; }
    public void setPlatform(DevicePlatform platform) { this.platform = platform; }
}
