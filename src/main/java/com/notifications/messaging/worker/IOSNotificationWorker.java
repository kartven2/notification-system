package com.notifications.messaging.worker;

import com.notifications.dto.NotificationPayload;
import com.notifications.messaging.NotificationPublisher;
import com.notifications.model.NotificationStatus;
import com.notifications.repository.NotificationLogRepository;
import com.notifications.thirdparty.APNsService;
import com.notifications.thirdparty.ThirdPartyDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for the iOS push notification queue.
 */
@Component
public class IOSNotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(IOSNotificationWorker.class);

    private final APNsService apnsService;
    private final NotificationLogRepository logRepository;
    private final NotificationPublisher publisher;

    @Value("${notification.retry.max-attempts:5}")
    private int maxRetries;

    public IOSNotificationWorker(APNsService apnsService,
                                  NotificationLogRepository logRepository,
                                  NotificationPublisher publisher) {
        this.apnsService = apnsService;
        this.logRepository = logRepository;
        this.publisher = publisher;
    }

    @RabbitListener(queues = "${notification.queues.ios}",
                    containerFactory = "rabbitListenerContainerFactory")
    public void consume(NotificationPayload payload) {
        log.info("[IOS Worker] Processing logId={} userId={} attempt={}/{}",
                payload.getLogId(), payload.getUserId(),
                payload.getRetryCount() + 1, maxRetries);
        try {
            apnsService.send(payload);
            markSent(payload.getLogId());
        } catch (Exception ex) {
            log.error("[IOS Worker] Delivery failed for logId={}: {}", payload.getLogId(), ex.getMessage());
            handleFailure(payload, ex.getMessage());
        }
    }

    private void markSent(Long logId) {
        logRepository.findById(logId).ifPresent(entry -> {
            entry.setStatus(NotificationStatus.SENT);
            entry.setErrorMessage(null);
            logRepository.save(entry);
            log.info("[IOS Worker] Marked logId={} as SENT", logId);
        });
    }

    private void handleFailure(NotificationPayload payload, String errorMessage) {
        int newRetryCount = payload.getRetryCount() + 1;
        logRepository.findById(payload.getLogId()).ifPresent(entry -> {
            entry.setRetryCount(newRetryCount);
            entry.setErrorMessage(errorMessage);
            if (newRetryCount >= maxRetries) {
                entry.setStatus(NotificationStatus.FAILED);
                logRepository.save(entry);
                log.warn("[IOS Worker] Exhausted retries for logId={}. Marked FAILED.", payload.getLogId());
            } else {
                entry.setStatus(NotificationStatus.RETRYING);
                logRepository.save(entry);
                publisher.requeue(cloneWithRetry(payload, newRetryCount));
                log.info("[IOS Worker] Re-queued logId={} for retry #{}", payload.getLogId(), newRetryCount);
            }
        });
    }

    private NotificationPayload cloneWithRetry(NotificationPayload o, int retryCount) {
        return NotificationPayload.builder()
                .logId(o.getLogId()).userId(o.getUserId()).userName(o.getUserName())
                .userEmail(o.getUserEmail()).userPhone(o.getUserPhone())
                .deviceToken(o.getDeviceToken()).devicePlatform(o.getDevicePlatform())
                .channel(o.getChannel()).templateName(o.getTemplateName())
                .title(o.getTitle()).body(o.getBody()).retryCount(retryCount).build();
    }
}
