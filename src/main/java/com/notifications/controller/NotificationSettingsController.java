package com.notifications.controller;

import com.notifications.dto.NotificationSettingsRequest;
import com.notifications.model.NotificationSettings;
import com.notifications.service.NotificationSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing per-user notification opt-in preferences.
 * Base path: /api/notification-settings
 */
@RestController
@RequestMapping("/api/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    public NotificationSettingsController(NotificationSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationSettings>> getSettingsForUser(
            @RequestParam Long userId) {
        return ResponseEntity.ok(settingsService.getSettingsForUser(userId));
    }

    @PutMapping
    public ResponseEntity<NotificationSettings> upsertSettings(
            @Valid @RequestBody NotificationSettingsRequest request) {
        return ResponseEntity.ok(settingsService.upsertSettings(request));
    }
}
