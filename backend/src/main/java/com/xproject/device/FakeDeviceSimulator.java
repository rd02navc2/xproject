package com.xproject.device;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;

/** 「設備」:假資料產生器,MQTT CONNECT → PUBLISH 上行 → 接收下行 */
@Component
public class FakeDeviceSimulator {
    private final Map<String, MqttClient> clients = new LinkedHashMap<>();
    private final Random rnd = new Random();
    @Value("${mqtt.port}") private int port;
    @Value("${mqtt.fake-device-count}") private int count;

    @Scheduled(initialDelay = 3000, fixedRate = 3000)
    public void tick() {
        for (int i = 1; i <= count; i++) {
            String id = String.format("dev-%03d", i);
            try {
                MqttClient c = clients.get(id);
                if (c == null || !c.isConnected()) c = connect(id);
                String json = String.format(Locale.ROOT, "{\"temperature\":%.1f,\"humidity\":%.1f,\"ts\":%d}",
                        20 + rnd.nextDouble() * 15, 40 + rnd.nextDouble() * 30, System.currentTimeMillis());
                c.publish("devices/" + id + "/up", json.getBytes(), 1, false);   // PUBLISH 上行消息
            } catch (Exception e) { System.err.println("[fake] " + id + ": " + e.getMessage()); }
        }
    }

    private MqttClient connect(String id) throws MqttException {
        MqttClient c = new MqttClient("tcp://127.0.0.1:" + port, id, new MemoryPersistence());
        MqttConnectOptions o = new MqttConnectOptions(); o.setCleanSession(true); o.setAutomaticReconnect(true);
        c.connect(o);                                                           // MQTT CONNECT
        c.subscribe("devices/" + id + "/down", (t, m) -> System.out.println("[fake] " + id + " got downlink: " + new String(m.getPayload())));
        clients.put(id, c);
        return c;
    }
}
