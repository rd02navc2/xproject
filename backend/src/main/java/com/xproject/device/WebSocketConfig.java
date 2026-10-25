package com.xproject.device;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final DeviceWsHandler handler;
    public WebSocketConfig(DeviceWsHandler handler) { this.handler = handler; }

    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry r) {
        r.addHandler(handler, "/ws").setAllowedOrigins("*");   // 正式環境請改成實際網域
    }
}
