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

        // Endpoints com SockJS (para navegadores sem suporte WebSocket nativo)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000);

        // Endpoints WebSocket nativos
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Habilitar broker de mensagens para canais de tópicos e filas
        registry.enableSimpleBroker(
                "/topic",      // Para mensagens públicas (ex: chat)
                "/queue",      // Para mensagens privadas
                "/user"        // Para mensagens específicas do usuário
        );

        // Estabelecer prefixos para rotas de aplicação
        registry.setApplicationDestinationPrefixes("/app");

        // Configurar prefixo para mensagens direcionadas a usuários específicos
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configurar limites para mensagens WebSocket
        registration.setMessageSizeLimit(128 * 1024)        // 128KB por mensagem
                .setSendBufferSizeLimit(1024 * 1024)      // 1MB buffer de envio
                .setSendTimeLimit(20000);                 // 20 segundos timeout
    }
}