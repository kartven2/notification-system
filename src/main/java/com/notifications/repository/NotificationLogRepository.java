package com.notifications.repository;

import com.notifications.model.NotificationLog;
import com.notifications.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for {@link NotificationLog} entities.
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<NotificationLog> findByStatus(NotificationStatus status);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);
}
