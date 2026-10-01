package com.notifications.controller;

import com.notifications.dto.DeviceRequest;
import com.notifications.model.Device;
import com.notifications.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing user devices.
 * Base path: /api/devices
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<Device> registerDevice(@Valid @RequestBody DeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.registerDevice(request));
    }

    @GetMapping
    public ResponseEntity<List<Device>> getDevicesForUser(@RequestParam Long userId) {
        return ResponseEntity.ok(deviceService.getActiveDevicesForUser(userId));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deactivateDevice(@PathVariable Long deviceId,
                                                  @RequestParam Long userId) {
        deviceService.deactivateDevice(userId, deviceId);
        return ResponseEntity.noContent().build();
    }
}
