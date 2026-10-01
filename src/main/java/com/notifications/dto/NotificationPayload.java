package com.notifications.dto;

import com.notifications.model.DevicePlatform;
import com.notifications.model.NotificationChannel;

import java.io.Serializable;

/**
 * The fully-rendered notification payload placed on a RabbitMQ queue.
 */
public class NotificationPayload implements Serializable {

    private Long logId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String deviceToken;
    private DevicePlatform devicePlatform;
    private NotificationChannel channel;
    private String templateName;
    private String title;
    private String body;
    private int retryCount = 0;

    public NotificationPayload() {}

    // --- Builder ---
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long logId;
        private Long userId;
        private String userName;
        private String userEmail;
        private String userPhone;
        private String deviceToken;
        private DevicePlatform devicePlatform;
        private NotificationChannel channel;
        private String templateName;
        private String title;
        private String body;
        private int retryCount = 0;

        public Builder logId(Long l) { this.logId = l; return this; }
        public Builder userId(Long u) { this.userId = u; return this; }
        public Builder userName(String n) { this.userName = n; return this; }
        public Builder userEmail(String e) { this.userEmail = e; return this; }
        public Builder userPhone(String p) { this.userPhone = p; return this; }
        public Builder deviceToken(String t) { this.deviceToken = t; return this; }
        public Builder devicePlatform(DevicePlatform p) { this.devicePlatform = p; return this; }
        public Builder channel(NotificationChannel c) { this.channel = c; return this; }
        public Builder templateName(String t) { this.templateName = t; return this; }
        public Builder title(String t) { this.title = t; return this; }
        public Builder body(String b) { this.body = b; return this; }
        public Builder retryCount(int r) { this.retryCount = r; return this; }

        public NotificationPayload build() {
            NotificationPayload p = new NotificationPayload();
            p.logId = this.logId;
            p.userId = this.userId;
            p.userName = this.userName;
            p.userEmail = this.userEmail;
            p.userPhone = this.userPhone;
            p.deviceToken = this.deviceToken;
            p.devicePlatform = this.devicePlatform;
            p.channel = this.channel;
            p.templateName = this.templateName;
            p.title = this.title;
            p.body = this.body;
            p.retryCount = this.retryCount;
            return p;
        }
    }

    // --- Getters & Setters ---
    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    public DevicePlatform getDevicePlatform() { return devicePlatform; }
    public void setDevicePlatform(DevicePlatform devicePlatform) { this.devicePlatform = devicePlatform; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    @Override
    public String toString() {
        return "NotificationPayload{logId=" + logId + ", userId=" + userId +
               ", channel=" + channel + ", title='" + title + "', body='" + body + "'}";
    }
}
