package com.notifications.service;

import com.notifications.dto.NotificationPayload;
import com.notifications.dto.NotificationRequest;
import com.notifications.messaging.NotificationPublisher;
import com.notifications.model.*;
import com.notifications.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Core orchestration service: opt-in check → payload build → queue publish.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserService userService;
    private final DeviceService deviceService;
    private final NotificationSettingsService settingsService;
    private final NotificationPayloadBuilderService payloadBuilder;
    private final NotificationPublisher publisher;
    private final NotificationLogRepository logRepository;

    public NotificationService(UserService userService,
                                DeviceService deviceService,
                                NotificationSettingsService settingsService,
                                NotificationPayloadBuilderService payloadBuilder,
                                NotificationPublisher publisher,
                                NotificationLogRepository logRepository) {
        this.userService = userService;
        this.deviceService = deviceService;
        this.settingsService = settingsService;
        this.payloadBuilder = payloadBuilder;
        this.publisher = publisher;
        this.logRepository = logRepository;
    }

    @Transactional
    public List<Long> send(NotificationRequest request) {
        User user = userService.getUserById(request.getUserId());
        List<Long> queuedLogIds = new ArrayList<>();

        if (!settingsService.isOptedIn(user.getId(), request.getChannel())) {
            log.info("User id={} has opted out of channel={}. Skipping.",
                    user.getId(), request.getChannel());
            return queuedLogIds;
        }

        switch (request.getChannel()) {
            case PUSH  -> queuedLogIds.addAll(dispatchPushNotifications(request, user));
            case SMS   -> queuedLogIds.add(dispatchSmsNotification(request, user));
            case EMAIL -> queuedLogIds.add(dispatchEmailNotification(request, user));
        }

        return queuedLogIds;
    }

    private List<Long> dispatchPushNotifications(NotificationRequest request, User user) {
        List<Device> devices = deviceService.getActiveDevicesForUser(user.getId());
        List<Long> logIds = new ArrayList<>();

        if (devices.isEmpty()) {
            log.warn("No active devices found for userId={}, skipping PUSH.", user.getId());
            return logIds;
        }

        for (Device device : devices) {
            NotificationLog logEntry = createLogEntry(user, request.getChannel(),
                    device.getPlatform(), request.getTemplateName());

            NotificationPayload payload = payloadBuilder.buildPushPayload(
                    request, user, device, logEntry.getId());

            logEntry.setPayload(payload.toString());
            logRepository.save(logEntry);

            publisher.publishPush(payload, device.getPlatform());
            logIds.add(logEntry.getId());
            log.info("Queued PUSH notification logId={} platform={} userId={}",
                    logEntry.getId(), device.getPlatform(), user.getId());
        }
        return logIds;
    }

    private Long dispatchSmsNotification(NotificationRequest request, User user) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            log.warn("userId={} has no phone number. Skipping SMS.", user.getId());
            return -1L;
        }
        NotificationLog logEntry = createLogEntry(user, NotificationChannel.SMS,
                null, request.getTemplateName());

        NotificationPayload payload = payloadBuilder.buildChannelPayload(
                request, user, logEntry.getId());

        logEntry.setPayload(payload.toString());
        logRepository.save(logEntry);

        publisher.publishSms(payload);
        log.info("Queued SMS notification logId={} userId={}", logEntry.getId(), user.getId());
        return logEntry.getId();
    }

    private Long dispatchEmailNotification(NotificationRequest request, User user) {
        NotificationLog logEntry = createLogEntry(user, NotificationChannel.EMAIL,
                null, request.getTemplateName());

        NotificationPayload payload = payloadBuilder.buildChannelPayload(
                request, user, logEntry.getId());

        logEntry.setPayload(payload.toString());
        logRepository.save(logEntry);

        publisher.publishEmail(payload);
        log.info("Queued EMAIL notification logId={} userId={}", logEntry.getId(), user.getId());
        return logEntry.getId();
    }

    private NotificationLog createLogEntry(User user, NotificationChannel channel,
                                            DevicePlatform platform, String templateName) {
        NotificationLog entry = NotificationLog.builder()
                .user(user)
                .channel(channel)
                .platform(platform)
                .templateName(templateName)
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();
        return logRepository.save(entry);
    }
}
