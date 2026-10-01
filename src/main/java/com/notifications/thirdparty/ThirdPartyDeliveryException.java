package com.notifications.thirdparty;

/**
 * Thrown when a third-party notification service fails to deliver a notification.
 *
 * <p>Workers catch this exception, increment {@code retryCount}, and either
 * re-queue the payload or mark it {@code FAILED} once the maximum retry
 * count is exceeded.</p>
 */
public class ThirdPartyDeliveryException extends RuntimeException {

    public ThirdPartyDeliveryException(String message) {
        super(message);
    }

    public ThirdPartyDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
