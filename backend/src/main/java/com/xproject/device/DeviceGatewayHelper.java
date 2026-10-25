package com.xproject.device;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xproject.device.Models.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** DeviceGatewayHelper:解碼 codec / 查找 Device / 寫 Session / 投遞 DeviceMessage */
@Component
public class DeviceGatewayHelper {
    public final Map<String, Device> devices = new ConcurrentHashMap<>();
    public final Map<String, DeviceSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ApplicationEventPublisher eventBus;

    public DeviceGatewayHelper(ApplicationEventPublisher eventBus) { this.eventBus = eventBus; }

    public Device findOrCreate(String id) {
        return devices.computeIfAbsent(id, k -> { Device d = new Device(); d.id = k; return d; });
    }

    public void handleDeviceMessage(String deviceId, String topic, byte[] raw) {
        try {
            Map<String, Object> data = mapper.readValue(raw, new TypeReference<Map<String, Object>>() {}); // 1. 解碼 codec
            Device device = findOrCreate(deviceId);                                                          // 2. 查找 Device
            device.lastData = data;
            DeviceSession s = sessions.get(deviceId);                                                        // 3. 寫 Session
            if (s != null) { s.lastActiveAt = System.currentTimeMillis(); s.uplinkCount++; }
            eventBus.publishEvent(new DeviceMessage(deviceId, topic, data));                                 // 4. 投遞 DeviceMessage
        } catch (Exception e) {
            System.err.println("[helper] decode failed: " + e.getMessage());
        }
    }
}
