package com.helps.infra.websocket;

import org.springframework.lang.Nullable;

import java.util.Set;

/**
 * Interface para gerenciamento de sessões WebSocket ativas.
 * Permite rastrear quais usuários estão conectados e suas respectivas sessões.
 */
public interface WebSocketSessionRegistry {

    /**
     * Adiciona uma sessão para um usuário.
     *
     * @param username  Nome do usuário
     * @param sessionId ID da sessão WebSocket
     */
    void addSession(String username, String sessionId);

    /**
     * Remove uma sessão de um usuário.
     *
     * @param username  Nome do usuário
     * @param sessionId ID da sessão WebSocket
     */
    void removeSession(String username, String sessionId);

    /**
     * Obtém todas as sessões ativas de um usuário.
     *
     * @param username Nome do usuário
     * @return Set com os IDs das sessões ativas
     */
    Set<String> getSessions(String username);

    /**
     * Verifica se um usuário tem uma sessão específica ativa.
     *
     * @param username  Nome do usuário
     * @param sessionId ID da sessão
     * @return true se a sessão estiver ativa
     */
    boolean hasSession(String username, String sessionId);

    /**
     * Encontra o nome do usuário por ID de sessão.
     *
     * @param sessionId ID da sessão
     * @return Nome do usuário ou null se não encontrado
     */
    @Nullable
    String findUsernameBySessionId(String sessionId);

    /**
     * Obtém o número total de sessões ativas.
     *
     * @return Número de sessões ativas
     */
    int getTotalActiveSessions();

    /**
     * Obtém o número de usuários únicos conectados.
     *
     * @return Número de usuários conectados
     */
    int getTotalConnectedUsers();
}
