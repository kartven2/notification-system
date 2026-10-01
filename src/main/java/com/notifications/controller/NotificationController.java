package com.notifications.controller;

import com.notifications.dto.NotificationRequest;
import com.notifications.model.NotificationLog;
import com.notifications.repository.NotificationLogRepository;
import com.notifications.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for triggering notifications and querying notification logs.
 * Base path: /api/notifications
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationLogRepository logRepository;

    public NotificationController(NotificationService notificationService,
                                   NotificationLogRepository logRepository) {
        this.notificationService = notificationService;
        this.logRepository = logRepository;
    }

    /**
     * POST /api/notifications/send
     * Trigger a notification for a user.
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        List<Long> logIds = notificationService.send(request);
        return ResponseEntity.accepted().body(Map.of(
                "status", "QUEUED",
                "queuedLogIds", logIds,
                "count", logIds.size()
        ));
    }

    /**
     * GET /api/notifications/logs?userId={userId}&page=0&size=20
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<NotificationLog>> getLogsForUser(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                logRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)));
    }

    /**
     * GET /api/notifications/logs/{logId}
     */
    @GetMapping("/logs/{logId}")
    public ResponseEntity<NotificationLog> getLog(@PathVariable Long logId) {
        return logRepository.findById(logId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
