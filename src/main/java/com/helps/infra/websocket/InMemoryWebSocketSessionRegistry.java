package com.helps.infra.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação em memória do registro de sessões WebSocket.
 * Thread-safe e adequada para uso em aplicações de pequeno a médio porte.
 */
@Slf4j
@Component
public class InMemoryWebSocketSessionRegistry implements WebSocketSessionRegistry {

    // Mapeia username -> set de sessionIds
    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    // Mapeia sessionId -> username (para lookup reverso)
    private final ConcurrentHashMap<String, String> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void addSession(String username, String sessionId) {
        if (username == null || username.trim().isEmpty() || sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("Tentativa de adicionar sessão com dados inválidos: username={}, sessionId={}", username, sessionId);
            return;
        }

        final String cleanUsername = username.trim();
        final String cleanSessionId = sessionId.trim();

        userSessions.compute(cleanUsername, (user, currentSessions) -> {
            Set<String> sessions = (currentSessions == null) 
                ? ConcurrentHashMap.newKeySet() 
                : currentSessions;
            
            sessions.add(cleanSessionId);
            return sessions;
        });

        sessionToUser.put(cleanSessionId, cleanUsername);
        
        log.debug("Sessão adicionada: user={} session={} totalSessions={}", 
                cleanUsername, cleanSessionId, getTotalActiveSessions());
    }

    @Override
    public void removeSession(String username, String sessionId) {
        if (username == null || sessionId == null) {
            log.warn("Tentativa de remover sessão com dados nulos: username={}, sessionId={}", username, sessionId);
            return;
        }

        final String cleanUsername = username.trim();
        final String cleanSessionId = sessionId.trim();

        // Remove da mapping username -> sessions
        userSessions.computeIfPresent(cleanUsername, (user, sessions) -> {
            sessions.remove(cleanSessionId);
            // Se não há mais sessões, remove o usuário completamente
            return sessions.isEmpty() ? null : sessions;
        });

        // Remove da mapping sessionId -> username
        sessionToUser.remove(cleanSessionId);
        
        log.debug("Sessão removida: user={} session={} totalSessions={}", 
                cleanUsername, cleanSessionId, getTotalActiveSessions());
    }

    @Override
    public Set<String> getSessions(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> sessions = userSessions.get(username.trim());
        return sessions == null ? Collections.emptySet() : Collections.unmodifiableSet(sessions);
    }

    @Override
    public boolean hasSession(String username, String sessionId) {
        if (username == null || sessionId == null) {
            return false;
        }

        Set<String> sessions = userSessions.get(username.trim());
        return sessions != null && sessions.contains(sessionId.trim());
    }

    @Override
    @Nullable
    public String findUsernameBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }

        return sessionToUser.get(sessionId.trim());
    }

    @Override
    public int getTotalActiveSessions() {
        return sessionToUser.size();
    }

    @Override
    public int getTotalConnectedUsers() {
        return userSessions.size();
    }

    /**
     * Remove todas as sessões de um usuário (útil para logout).
     *
     * @param username Nome do usuário
     */
    public void removeAllUserSessions(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        final String cleanUsername = username.trim();
        Set<String> sessions = userSessions.remove(cleanUsername);
        
        if (sessions != null) {
            // Remove todas as sessões do mapeamento reverso
            sessions.forEach(sessionToUser::remove);
            log.debug("Todas as sessões removidas para usuário: {} (total: {})", cleanUsername, sessions.size());
        }
    }

    /**
     * Obtém estatísticas do registry (útil para monitoring).
     */
    public String getStatistics() {
        return String.format("Connected Users: %d, Active Sessions: %d", 
                getTotalConnectedUsers(), getTotalActiveSessions());
    }

    /**
     * Limpa todas as sessões (útil para testes ou shutdown).
     */
    public void clearAll() {
        userSessions.clear();
        sessionToUser.clear();
        log.info("Todas as sessões WebSocket foram removidas do registry");
    }
}
