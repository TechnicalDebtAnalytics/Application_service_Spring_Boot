package com.debtlens.backend.config;

import com.debtlens.backend.websocket.AnalysisProgressPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AnalysisProgressPublisher analysisProgressPublisher;

    public WebSocketConfig(AnalysisProgressPublisher analysisProgressPublisher) {
        this.analysisProgressPublisher = analysisProgressPublisher;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(analysisProgressPublisher, "/ws/analysis", "/ws/analysis/")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");
    }
}