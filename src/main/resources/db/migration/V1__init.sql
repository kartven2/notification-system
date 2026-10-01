-- V1__init.sql
-- Initial schema for notification-system

-- ======================================================
-- Users
-- ======================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    phone       VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ======================================================
-- Devices
-- ======================================================
CREATE TABLE IF NOT EXISTS devices (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    platform    VARCHAR(20) NOT NULL CHECK (platform IN ('IOS', 'ANDROID')),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_devices_token ON devices(token);

-- ======================================================
-- Notification Settings (opt-in per channel per user)
-- ======================================================
CREATE TABLE IF NOT EXISTS notification_settings (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel     VARCHAR(20) NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'EMAIL')),
    opt_in      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, channel)
);

CREATE INDEX IF NOT EXISTS idx_notification_settings_user_id ON notification_settings(user_id);

-- ======================================================
-- Notification Templates
-- ======================================================
CREATE TABLE IF NOT EXISTS notification_templates (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    title_template  VARCHAR(500) NOT NULL,
    body_template   TEXT NOT NULL,
    channel         VARCHAR(20) NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'EMAIL', 'ALL')),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed a default template
INSERT INTO notification_templates (name, title_template, body_template, channel)
VALUES
    ('default', 'Hello, {{userName}}!', 'You have a new notification: {{message}}', 'ALL'),
    ('promo_email', 'Exclusive offer for {{userName}}', 'Hi {{userName}}, check out our latest deal: {{message}}', 'EMAIL'),
    ('alert_push', 'Alert', '{{message}}', 'PUSH'),
    ('alert_sms', NULL, 'Alert for {{userName}}: {{message}}', 'SMS')
ON CONFLICT (name) DO NOTHING;

-- ======================================================
-- Notification Logs
-- ======================================================
CREATE TABLE IF NOT EXISTS notification_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel         VARCHAR(20) NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'EMAIL')),
    platform        VARCHAR(20) CHECK (platform IN ('IOS', 'ANDROID')),
    template_name   VARCHAR(100),
    status          VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'SENT', 'FAILED', 'RETRYING')),
    retry_count     INT NOT NULL DEFAULT 0,
    error_message   TEXT,
    payload         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_logs_user_id ON notification_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_status ON notification_logs(status);
CREATE INDEX IF NOT EXISTS idx_notification_logs_created_at ON notification_logs(created_at);
