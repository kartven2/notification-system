package com.notifications.dto;

import com.notifications.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Inbound request from a calling service to trigger a notification.
 */
public class NotificationRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "channel is required (PUSH, SMS, or EMAIL)")
    private NotificationChannel channel;

    @NotBlank(message = "templateName is required")
    private String templateName;

    private Map<String, String> variables = new HashMap<>();

    public NotificationRequest() {}

    public NotificationRequest(Long userId, NotificationChannel channel,
                                String templateName, Map<String, String> variables) {
        this.userId = userId;
        this.channel = channel;
        this.templateName = templateName;
        this.variables = variables != null ? variables : new HashMap<>();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long userId;
        private NotificationChannel channel;
        private String templateName;
        private Map<String, String> variables = new HashMap<>();
        public Builder userId(Long u) { this.userId = u; return this; }
        public Builder channel(NotificationChannel c) { this.channel = c; return this; }
        public Builder templateName(String t) { this.templateName = t; return this; }
        public Builder variables(Map<String, String> v) { this.variables = v; return this; }
        public NotificationRequest build() { return new NotificationRequest(userId, channel, templateName, variables); }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
}
