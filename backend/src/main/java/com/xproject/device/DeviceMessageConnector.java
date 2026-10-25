package com.xproject.device;

import com.xproject.device.Models.DeviceMessage;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** DeviceMessageConnector / EventBus:訂閱者(此處示範 時序庫 + 規則引擎) */
@Component
public class DeviceMessageConnector {
    private final Deque<DeviceMessage> timeSeries = new ArrayDeque<>();   // 假時序庫(記憶體環形緩衝)
    private final MqttServerDeviceGateway gateway;
    private final WsHub hub;

    public DeviceMessageConnector(MqttServerDeviceGateway gateway, WsHub hub) { this.gateway = gateway; this.hub = hub; }

    @EventListener  // 對應流程圖 @Subscribe
    public synchronized void onMessage(DeviceMessage m) {
        timeSeries.addFirst(m);
        while (timeSeries.size() > 200) timeSeries.removeLast();
        hub.broadcast("message", m);                                      // 即時推送前端
        Object t = m.payload.get("temperature");                           // 規則引擎:溫度 > 30 下發告警指令
        if (t instanceof Number && ((Number) t).doubleValue() > 30) {
            gateway.send(m.deviceId, "{\"cmd\":\"alarm\",\"reason\":\"temperature>30\"}");
        }
    }

    public synchronized List<DeviceMessage> latest(int n) { return new ArrayList<>(timeSeries).subList(0, Math.min(n, timeSeries.size())); }
}
