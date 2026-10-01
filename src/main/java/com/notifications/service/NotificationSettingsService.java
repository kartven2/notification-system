package com.notifications.service;

import com.notifications.config.RedisConfig;
import com.notifications.dto.NotificationSettingsRequest;
import com.notifications.model.NotificationChannel;
import com.notifications.model.NotificationSettings;
import com.notifications.model.User;
import com.notifications.repository.NotificationSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing per-user, per-channel notification opt-in settings.
 */
@Service
public class NotificationSettingsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSettingsService.class);

    private final NotificationSettingsRepository settingsRepository;
    private final UserService userService;

    public NotificationSettingsService(NotificationSettingsRepository settingsRepository,
                                       UserService userService) {
        this.settingsRepository = settingsRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<NotificationSettings> getSettingsForUser(Long userId) {
        userService.getUserById(userId); // validate user exists
        return settingsRepository.findByUserId(userId);
    }

    /**
     * Returns true if the user has opted in for the given channel.
     * Defaults to true if no record exists.
     */
    @Transactional(readOnly = true)
    public boolean isOptedIn(Long userId, NotificationChannel channel) {
        return settingsRepository
                .findByUserIdAndChannel(userId, channel)
                .map(NotificationSettings::getOptIn)
                .orElse(true);
    }

    @CacheEvict(value = RedisConfig.CACHE_USERS, key = "#request.userId")
    @Transactional
    public NotificationSettings upsertSettings(NotificationSettingsRequest request) {
        User user = userService.getUserById(request.getUserId());

        NotificationSettings settings = settingsRepository
                .findByUserIdAndChannel(request.getUserId(), request.getChannel())
                .orElseGet(() -> {
                    NotificationSettings ns = NotificationSettings.builder()
                            .user(user)
                            .channel(request.getChannel())
                            .build();
                    return ns;
                });

        settings.setOptIn(request.getOptIn());
        NotificationSettings saved = settingsRepository.save(settings);
        log.info("Updated notification settings: userId={} channel={} optIn={}",
                request.getUserId(), request.getChannel(), request.getOptIn());
        return saved;
    }
}
