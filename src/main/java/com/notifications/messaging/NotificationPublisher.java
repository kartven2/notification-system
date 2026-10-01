package com.notifications.messaging;

import com.notifications.dto.NotificationPayload;
import com.notifications.model.DevicePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publishes notification payloads to the appropriate RabbitMQ queues.
 */
@Component
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.queues.ios}")
    private String iosQueue;

    @Value("${notification.queues.android}")
    private String androidQueue;

    @Value("${notification.queues.sms}")
    private String smsQueue;

    @Value("${notification.queues.email}")
    private String emailQueue;

    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPush(NotificationPayload payload, DevicePlatform platform) {
        String queue = (platform == DevicePlatform.IOS) ? iosQueue : androidQueue;
        rabbitTemplate.convertAndSend(queue, payload);
        log.debug("Published PUSH payload to queue={} logId={}", queue, payload.getLogId());
    }

    public void publishSms(NotificationPayload payload) {
        rabbitTemplate.convertAndSend(smsQueue, payload);
        log.debug("Published SMS payload to queue={} logId={}", smsQueue, payload.getLogId());
    }

    public void publishEmail(NotificationPayload payload) {
        rabbitTemplate.convertAndSend(emailQueue, payload);
        log.debug("Published EMAIL payload to queue={} logId={}", emailQueue, payload.getLogId());
    }

    public void requeue(NotificationPayload payload) {
        switch (payload.getChannel()) {
            case PUSH  -> publishPush(payload, payload.getDevicePlatform());
            case SMS   -> publishSms(payload);
            case EMAIL -> publishEmail(payload);
        }
        log.info("Re-queued payload logId={} attempt={}", payload.getLogId(), payload.getRetryCount());
    }
}
