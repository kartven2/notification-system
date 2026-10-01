package com.notifications.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A reusable notification message template.
 * Supports {{variableName}} token substitution.
 */
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "title_template", length = 500)
    private String titleTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateChannel channel = TemplateChannel.ALL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public NotificationTemplate() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Builder ---
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String titleTemplate;
        private String bodyTemplate;
        private TemplateChannel channel = TemplateChannel.ALL;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder titleTemplate(String t) { this.titleTemplate = t; return this; }
        public Builder bodyTemplate(String b) { this.bodyTemplate = b; return this; }
        public Builder channel(TemplateChannel c) { this.channel = c; return this; }

        public NotificationTemplate build() {
            NotificationTemplate t = new NotificationTemplate();
            t.id = this.id;
            t.name = this.name;
            t.titleTemplate = this.titleTemplate;
            t.bodyTemplate = this.bodyTemplate;
            t.channel = this.channel;
            return t;
        }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public TemplateChannel getChannel() { return channel; }
    public void setChannel(TemplateChannel channel) { this.channel = channel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
