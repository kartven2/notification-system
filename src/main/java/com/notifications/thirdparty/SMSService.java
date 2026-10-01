package com.notifications.thirdparty;

import com.notifications.dto.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * STUB: SMS delivery client (Twilio / Nexmo-compatible).
 * In production, use the Twilio REST API or Nexmo (Vonage) SDK.
 */
@Service
public class SMSService {

    private static final Logger log = LoggerFactory.getLogger(SMSService.class);

    @Value("${notification.third-party.sms.from-number:+15005550006}")
    private String fromNumber;

    @Value("${notification.third-party.sms.account-sid:STUB_SID}")
    private String accountSid;

    public void send(NotificationPayload payload) {
        String toNumber = payload.getUserPhone();
        log.info("[SMS STUB] Sending SMS from={} to={} | body='{}'",
                fromNumber, toNumber, payload.getBody());

        if (toNumber != null && toNumber.toLowerCase().contains("fail")) {
            log.error("[SMS STUB] Simulated delivery failure for phone={}", toNumber);
            throw new ThirdPartyDeliveryException(
                    "SMS: Simulated delivery failure for phone " + toNumber);
        }

        simulateLatency(80, 300);
        log.info("[SMS STUB] Successfully delivered SMS logId={}", payload.getLogId());
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + (long) (Math.random() * (maxMs - minMs)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
