package com.xproject.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 管理所有前端 WebSocket 連線並廣播事件,訊息格式 {"type":"...","data":...} */
@Component
public class WsHub {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper = new ObjectMapper();

    public void add(WebSocketSession s) { sessions.add(s); }
    public void remove(WebSocketSession s) { sessions.remove(s); }

    public void sendTo(WebSocketSession s, String type, Object data) {
        try {
            String json = mapper.writeValueAsString(Map.of("type", type, "data", data));
            synchronized (s) { if (s.isOpen()) s.sendMessage(new TextMessage(json)); }   // 同一 session 不可並行寫入
        } catch (Exception e) { sessions.remove(s); }
    }

    public void broadcast(String type, Object data) { sessions.forEach(s -> sendTo(s, type, data)); }

    @Scheduled(fixedRate = 25000)   // WebSocket ping,避免 nginx/代理閒置斷線
    public void heartbeat() {
        sessions.forEach(s -> {
            try { synchronized (s) { if (s.isOpen()) s.sendMessage(new PingMessage()); } }
            catch (Exception e) { sessions.remove(s); }
        });
    }
}
