package com.helps.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins:http://localhost:3000}")
    private String websocketAllowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = websocketAllowedOrigins.split(",");

        // Endpoints with SockJS (for browsers without native WebSocket support)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000);

        // Native WebSocket endpoints
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable message broker for topic and queue channels
        registry.enableSimpleBroker(
                "/topic",
                "/queue",
                "/user"
        );

        // Set prefixes for application routes
        registry.setApplicationDestinationPrefixes("/app");

        // Configure prefix for messages directed to specific users
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure limits for WebSocket messages
        registration.setMessageSizeLimit(128 * 1024)        // 128KB per message
                .setSendBufferSizeLimit(1024 * 1024)      // 1MB send buffer
                .setSendTimeLimit(20000);                 // 20 seconds timeout
    }
}