package com.notifications.thirdparty;

import com.notifications.dto.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * STUB: Firebase Cloud Messaging (FCM) client.
 * In production, use the FCM HTTP v1 API or Firebase Admin SDK.
 */
@Service
public class FCMService {

    private static final Logger log = LoggerFactory.getLogger(FCMService.class);

    @Value("${notification.third-party.fcm.endpoint:https://fcm.googleapis.com/fcm/send}")
    private String endpoint;

    @Value("${notification.third-party.fcm.server-key:STUB_KEY}")
    private String serverKey;

    public void send(NotificationPayload payload) {
        log.info("[FCM STUB] Sending push to Android token={} | title='{}' | body='{}'",
                maskToken(payload.getDeviceToken()), payload.getTitle(), payload.getBody());

        if (payload.getDeviceToken() != null
                && payload.getDeviceToken().toLowerCase().contains("fail")) {
            log.error("[FCM STUB] Simulated delivery failure for token={}",
                    maskToken(payload.getDeviceToken()));
            throw new ThirdPartyDeliveryException(
                    "FCM: Simulated delivery failure for token " + maskToken(payload.getDeviceToken()));
        }

        simulateLatency(30, 100);
        log.info("[FCM STUB] Successfully delivered Android push notification logId={}", payload.getLogId());
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
