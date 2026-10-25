package com.xproject.device;

import java.util.Map;

/** 領域模型(public 欄位,Jackson 2.11 可直接序列化) */
public class Models {
    public static class Device {
        public String id; public boolean online; public long createdAt = System.currentTimeMillis();
        public Map<String, Object> lastData;
    }
    /** DeviceSession:設備連線期間的狀態 */
    public static class DeviceSession {
        public String deviceId; public long connectedAt = System.currentTimeMillis();
        public long lastActiveAt = connectedAt; public long uplinkCount;
    }
    public static class DeviceMessage {
        public String deviceId; public String topic; public Map<String, Object> payload; public long ts = System.currentTimeMillis();
        public DeviceMessage(String d, String t, Map<String, Object> p) { deviceId = d; topic = t; payload = p; }
    }
}
