package com.helps.infra.websocket;

import com.helps.domain.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private WebSocketService webSocketService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        logger.debug("Recebida nova conexão WebSocket");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        Long chamadoId = (Long) headerAccessor.getSessionAttributes().get("chamadoId");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

        if(username != null && chamadoId != null) {
            logger.debug("Usuário desconectado: {}", username);

            webSocketService.notificarSaidaUsuario(chamadoId, userId, username);
        }
    }
}