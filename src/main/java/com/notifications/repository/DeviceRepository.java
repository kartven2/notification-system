package com.notifications.repository;

import com.notifications.model.Device;
import com.notifications.model.DevicePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Device} entities.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.active = TRUE")
    List<Device> findByUserIdAndActiveTrue(Long userId);

    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.platform = :platform AND d.active = TRUE")
    List<Device> findByUserIdAndPlatformAndActiveTrue(Long userId, DevicePlatform platform);

    @Query("SELECT d FROM Device d WHERE d.token = :token")
    Optional<Device> findByToken(String token);

    boolean existsByToken(String token);
}
