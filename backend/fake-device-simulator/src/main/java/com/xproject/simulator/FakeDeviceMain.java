package com.xproject.simulator;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.*;

public class FakeDeviceMain {

    private static final Map<String, MqttClient> clients = new LinkedHashMap<>();
    private static final Random rnd = new Random();

    public static void main(String[] args) throws Exception {
        int port  = args.length > 0 ? Integer.parseInt(args[0]) : 1883;
        int count = args.length > 1 ? Integer.parseInt(args[1]) : 3;

        for (int i = 1; i <= count; i++) {
            connect(String.format("dev-%03d", i), port);
        }

        System.out.println("[simulator] started " + count
                + " devices, pid=" + ProcessHandle.current().pid()
                + ", broker=127.0.0.1:" + port);

        while (true) {
            for (int i = 1; i <= count; i++) {
                String id = String.format("dev-%03d", i);
                try {
                    MqttClient c = clients.get(id);
                    if (c == null || !c.isConnected()) c = connect(id, port);

                    String json = String.format(Locale.ROOT,
                            "{\"temperature\":%.1f,\"humidity\":%.1f,\"ts\":%d}",
                            20 + rnd.nextDouble() * 15,
                            40 + rnd.nextDouble() * 30,
                            System.currentTimeMillis());

                    c.publish("devices/" + id + "/up", json.getBytes(), 1, false);
                } catch (Exception e) {
                    System.err.println("[simulator] " + id + ": " + e.getMessage());
                }
            }
            Thread.sleep(3000);
        }
    }

    private static MqttClient connect(String id, int port) throws MqttException {
        MqttClient c = new MqttClient("tcp://127.0.0.1:" + port, id, new MemoryPersistence());

        MqttConnectOptions o = new MqttConnectOptions();
        o.setCleanSession(true);
        o.setAutomaticReconnect(false);     // 測試遺囑時關閉
        o.setKeepAliveInterval(10);         // 縮短 Keep Alive

        String willPayload = "{\"status\":\"offline\",\"reason\":\"unexpected_disconnect\"}";
        o.setWill("devices/" + id + "/status", willPayload.getBytes(), 1, false);

        c.connect(o);
        c.subscribe("devices/" + id + "/down",
                (t, m) -> System.out.println("[simulator] " + id
                        + " got downlink: " + new String(m.getPayload())));

        clients.put(id, c);
        return c;
    }
}