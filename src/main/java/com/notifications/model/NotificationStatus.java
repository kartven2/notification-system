package com.notifications.model;

/**
 * Status of a dispatched notification.
 */
public enum NotificationStatus {
    QUEUED,
    SENT,
    FAILED,
    RETRYING
}
