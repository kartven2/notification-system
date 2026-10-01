package com.notifications.repository;

import com.notifications.model.NotificationChannel;
import com.notifications.model.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link NotificationSettings} entities.
 */
@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    List<NotificationSettings> findByUserId(Long userId);

    Optional<NotificationSettings> findByUserIdAndChannel(Long userId, NotificationChannel channel);

    boolean existsByUserIdAndChannel(Long userId, NotificationChannel channel);
}
