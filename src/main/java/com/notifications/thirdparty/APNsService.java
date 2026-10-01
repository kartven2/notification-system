package com.notifications.thirdparty;

import com.notifications.dto.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * STUB: Apple Push Notification service (APNs) client.
 * In production, use the APNs HTTP/2 API (e.g., pushy or java-apns library).
 * Tokens containing "fail" simulate delivery failures for testing retry paths.
 */
@Service
public class APNsService {

    private static final Logger log = LoggerFactory.getLogger(APNsService.class);

    @Value("${notification.third-party.apns.endpoint:https://api.sandbox.push.apple.com}")
    private String endpoint;

    @Value("${notification.third-party.apns.bundle-id:com.example.app}")
    private String bundleId;

    public void send(NotificationPayload payload) {
        log.info("[APNs STUB] Sending push to iOS device token={} | title='{}' | body='{}'",
                maskToken(payload.getDeviceToken()), payload.getTitle(), payload.getBody());

        if (payload.getDeviceToken() != null
                && payload.getDeviceToken().toLowerCase().contains("fail")) {
            log.error("[APNs STUB] Simulated delivery failure for token={}",
                    maskToken(payload.getDeviceToken()));
            throw new ThirdPartyDeliveryException(
                    "APNs: Simulated delivery failure for token " + maskToken(payload.getDeviceToken()));
        }

        simulateLatency(50, 150);
        log.info("[APNs STUB] Successfully delivered iOS push notification logId={}", payload.getLogId());
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) return "****";
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + (long) (Math.random() * (maxMs - minMs)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
