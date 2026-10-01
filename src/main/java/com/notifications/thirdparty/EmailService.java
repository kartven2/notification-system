package com.notifications.thirdparty;

import com.notifications.dto.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * STUB: Email delivery client (SendGrid / Mailchimp-compatible).
 * In production, use the SendGrid or Mailchimp transactional email API.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${notification.third-party.email.from-address:notifications@example.com}")
    private String fromAddress;

    @Value("${notification.third-party.email.from-name:Notification System}")
    private String fromName;

    @Value("${notification.third-party.email.api-key:STUB_KEY}")
    private String apiKey;

    public void send(NotificationPayload payload) {
        String toAddress = payload.getUserEmail();
        log.info("[EMAIL STUB] Sending email from='{}' <{}> to='{}' | subject='{}' | body='{}'",
                fromName, fromAddress, toAddress, payload.getTitle(), payload.getBody());

        if (toAddress != null && toAddress.toLowerCase().contains("fail")) {
            log.error("[EMAIL STUB] Simulated delivery failure for email={}", toAddress);
            throw new ThirdPartyDeliveryException(
                    "EMAIL: Simulated delivery failure for email " + toAddress);
        }

        simulateLatency(100, 400);
        log.info("[EMAIL STUB] Successfully delivered email notification logId={}", payload.getLogId());
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + (long) (Math.random() * (maxMs - minMs)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
