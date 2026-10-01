package com.notifications.service;

import com.notifications.config.RedisConfig;
import com.notifications.dto.DeviceRequest;
import com.notifications.model.Device;
import com.notifications.model.DevicePlatform;
import com.notifications.model.User;
import com.notifications.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service for managing Device registration and lookup.
 */
@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository deviceRepository;
    private final UserService userService;

    public DeviceService(DeviceRepository deviceRepository, UserService userService) {
        this.deviceRepository = deviceRepository;
        this.userService = userService;
    }

    @CacheEvict(value = RedisConfig.CACHE_DEVICES, key = "#request.userId")
    @Transactional
    public Device registerDevice(DeviceRequest request) {
        User user = userService.getUserById(request.getUserId());

        // Re-register if token already exists
        var existing = deviceRepository.findByToken(request.getToken());
        if (existing.isPresent()) {
            Device d = existing.get();
            d.setUser(user);
            d.setPlatform(request.getPlatform());
            d.setActive(true);
            Device saved = deviceRepository.save(d);
            log.info("Re-registered existing device token for user id={}", request.getUserId());
            return saved;
        }

        Device device = Device.builder()
                .user(user)
                .token(request.getToken())
                .platform(request.getPlatform())
                .active(true)
                .build();
        Device saved = deviceRepository.save(device);
        log.info("Registered device id={} platform={} for user id={}",
                saved.getId(), saved.getPlatform(), request.getUserId());
        return saved;
    }

    @Cacheable(value = RedisConfig.CACHE_DEVICES, key = "#userId")
    @Transactional(readOnly = true)
    public List<Device> getActiveDevicesForUser(Long userId) {
        return deviceRepository.findByUserIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<Device> getActiveDevicesForUserByPlatform(Long userId, DevicePlatform platform) {
        return deviceRepository.findByUserIdAndPlatformAndActiveTrue(userId, platform);
    }

    @CacheEvict(value = RedisConfig.CACHE_DEVICES, key = "#userId")
    @Transactional
    public void deactivateDevice(Long userId, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Device not found: " + deviceId));
        if (!device.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Device does not belong to user " + userId);
        }
        device.setActive(false);
        deviceRepository.save(device);
        log.info("Deactivated device id={} for user id={}", deviceId, userId);
    }
}
