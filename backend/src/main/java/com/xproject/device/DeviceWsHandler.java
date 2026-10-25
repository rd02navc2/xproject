package com.xproject.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;

/** 前端 WebSocket 端點:連線時推 snapshot;接收前端指令 {"type":"send","deviceId":"..","body":{..}} 並下行 */
@Component
public class DeviceWsHandler extends TextWebSocketHandler {
    private final WsHub hub;
    private final DeviceGatewayHelper helper;
    private final DeviceMessageConnector connector;
    private final MqttServerDeviceGateway gateway;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeviceWsHandler(WsHub hub, DeviceGatewayHelper helper, DeviceMessageConnector connector, MqttServerDeviceGateway gateway) {
        this.hub = hub; this.helper = helper; this.connector = connector; this.gateway = gateway;
    }

    @Override public void afterConnectionEstablished(WebSocketSession s) {
        hub.add(s);
        hub.sendTo(s, "snapshot", Map.of("devices", helper.devices.values(), "messages", connector.latest(20)));
    }

    @Override protected void handleTextMessage(WebSocketSession s, TextMessage msg) throws Exception {
        JsonNode n = mapper.readTree(msg.getPayload());
        if ("send".equals(n.path("type").asText()) && n.hasNonNull("deviceId")) {
            gateway.send(n.get("deviceId").asText(), n.path("body").toString());      // 下行 send()
        }
    }

    @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus status) { hub.remove(s); }
    @Override public void handleTransportError(WebSocketSession s, Throwable e) { hub.remove(s); }
}
