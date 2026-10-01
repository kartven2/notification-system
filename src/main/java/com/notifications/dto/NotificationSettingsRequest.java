package com.notifications.dto;

import com.notifications.model.NotificationChannel;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a user's notification opt-in preference.
 */
public class NotificationSettingsRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Channel is required (PUSH, SMS, or EMAIL)")
    private NotificationChannel channel;

    @NotNull(message = "optIn flag is required")
    private Boolean optIn;

    public NotificationSettingsRequest() {}

    public NotificationSettingsRequest(Long userId, NotificationChannel channel, Boolean optIn) {
        this.userId = userId;
        this.channel = channel;
        this.optIn = optIn;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long userId;
        private NotificationChannel channel;
        private Boolean optIn;
        public Builder userId(Long u) { this.userId = u; return this; }
        public Builder channel(NotificationChannel c) { this.channel = c; return this; }
        public Builder optIn(Boolean o) { this.optIn = o; return this; }
        public NotificationSettingsRequest build() { return new NotificationSettingsRequest(userId, channel, optIn); }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public Boolean getOptIn() { return optIn; }
    public void setOptIn(Boolean optIn) { this.optIn = optIn; }
}
