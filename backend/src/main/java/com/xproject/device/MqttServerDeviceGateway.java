package com.xproject.device;

import com.xproject.device.Models.*;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/** MqttServer(內嵌 Moquette)+ MqttServerDeviceGateway */
@Component
public class MqttServerDeviceGateway {
    private final Server mqttServer = new Server();
    private final DeviceGatewayHelper helper;
    private final WsHub hub;
    @Value("${mqtt.port}") private int port;

    public MqttServerDeviceGateway(DeviceGatewayHelper helper, WsHub hub) { this.helper = helper; this.hub = hub; }

    @PostConstruct
    public void start() throws Exception {
        Properties p = new Properties();
        p.setProperty("port", String.valueOf(port));
        p.setProperty("host", "0.0.0.0");
        p.setProperty("allow_anonymous", "true");
        mqttServer.startServer(new MemoryConfig(p), List.of(new AbstractInterceptHandler() {
            @Override public String getID() { return "xproject-gateway"; }
            @Override public void onConnect(InterceptConnectMessage m) { handleConnection(m.getClientID()); }
            @Override public void onDisconnect(InterceptDisconnectMessage m) { disconnected(m.getClientID()); }
            @Override public void onPublish(InterceptPublishMessage m) {            // 消息回調
                if (!m.getTopicName().endsWith("/up")) return;                       // 只處理上行
                ByteBuf buf = m.getPayload(); byte[] raw = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), raw);
                helper.handleDeviceMessage(m.getClientID(), m.getTopicName(), raw);
            }
			@Override
			public void onSessionLoopError(Throwable error) {
				// TODO Auto-generated method stub
				
			}
        }));
        System.out.println("[mqtt] server started on :" + port);
    }

    /** handleConnection() → doStartup() 建立 DeviceSession → monitor.connected() */
    public void handleConnection(String clientId) {
        Device d = helper.findOrCreate(clientId);
        DeviceSession s = new DeviceSession(); s.deviceId = clientId;               // doStartup()
        helper.sessions.put(clientId, s);
        d.online = true;
        hub.broadcast("device", d);
        System.out.println("[monitor] connected: " + clientId);                      // monitor.connected()
    }

    private void disconnected(String clientId) {
        helper.sessions.remove(clientId);
        Device d = helper.devices.get(clientId); if (d != null) { d.online = false; hub.broadcast("device", d); }
        System.out.println("[monitor] disconnected: " + clientId);
    }

    /** 下行消息 send() + monitor.sentMessage() */
    public void send(String deviceId, String json) {
        MqttPublishMessage msg = MqttMessageBuilders.publish().topicName("devices/" + deviceId + "/down")
                .retained(false).qos(MqttQoS.AT_LEAST_ONCE)
                .payload(Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)).build();
        mqttServer.internalPublish(msg, "xproject-gateway");
        System.out.println("[monitor] sentMessage -> " + deviceId + " " + json);
    }

    @PreDestroy public void stop() { mqttServer.stopServer(); }
}
