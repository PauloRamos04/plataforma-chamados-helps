package com.helps.domain.service;

import com.helps.domain.model.User;
import com.helps.domain.model.UserActivityLog;
import com.helps.domain.model.UserSession;
import com.helps.domain.repository.UserActivityLogRepository;
import com.helps.domain.repository.UserSessionRepository;
import com.helps.dto.ActivityLogDto;
import com.helps.dto.ActivityStatsDto;
import com.helps.dto.UserSessionDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter READABLE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Autowired
    private UserActivityLogRepository activityLogRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private UserContextService userContextService;

    @Transactional
    public void logActivity(User user, String activity, HttpServletRequest request, String additionalInfo) {
        UserActivityLog log = new UserActivityLog();
        log.setUser(user);
        log.setActivity(activity);
        log.setCreatedAt(getCurrentBrazilTime());
        log.setAdditionalInfo(formatActivityMessage(activity, user, additionalInfo));

        if (request != null) {
            log.setIpAddress(getClientIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setSessionId(request.getSession().getId());
        }

        activityLogRepository.save(log);
    }

    @Transactional
    public String createUserSession(User user, HttpServletRequest request) {
        LocalDateTime currentTime = getCurrentBrazilTime();

        List<UserSession> activeSessions = sessionRepository.findByUserAndIsActiveTrueOrderByLoginTimeDesc(user);

        for (UserSession activeSession : activeSessions) {
            activeSession.setLogoutTime(currentTime);
            activeSession.setIsActive(false);
            sessionRepository.save(activeSession);

            logActivity(user, "SESSION_REPLACED", request,
                    "Sessão anterior finalizada automaticamente por novo login");
        }

        String sessionId = UUID.randomUUID().toString();

        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionId(sessionId);
        session.setLoginTime(currentTime);
        session.setLastActivity(currentTime);
        session.setIsActive(true);

        if (request != null) {
            session.setIpAddress(getClientIpAddress(request));
            session.setUserAgent(request.getHeader("User-Agent"));
        }

        sessionRepository.save(session);
        logActivity(user, "LOGIN", request, "Login realizado com sucesso");

        return sessionId;
    }

    @Transactional
    public void endUserSession(String sessionId) {
        LocalDateTime currentTime = getCurrentBrazilTime();

        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }

        Optional<UserSession> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);

        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setLogoutTime(currentTime);
            session.setIsActive(false);
            sessionRepository.save(session);

            Duration sessionDuration = Duration.between(session.getLoginTime(), currentTime);
            String durationText = formatDuration(sessionDuration);

            logActivity(session.getUser(), "LOGOUT", null,
                    "Logout realizado - Tempo de sessão: " + durationText);
        } else {
            try {
                User currentUser = userContextService.getCurrentUser();
                List<UserSession> activeSessions = sessionRepository.findByUserAndIsActiveTrueOrderByLoginTimeDesc(currentUser);

                for (UserSession activeSession : activeSessions) {
                    activeSession.setLogoutTime(currentTime);
                    activeSession.setIsActive(false);
                    sessionRepository.save(activeSession);

                    logActivity(currentUser, "LOGOUT", null,
                            "Logout realizado (sessão encontrada por usuário)");
                }
            } catch (Exception e) {
                System.err.println("Erro no fallback de finalização de sessão: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void updateLastActivity(String sessionId) {
        sessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                .ifPresent(session -> {
                    session.setLastActivity(getCurrentBrazilTime());
                    sessionRepository.save(session);
                });
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupInactiveSessions() {
        try {
            LocalDateTime timeout = getCurrentBrazilTime().minusHours(2);

            List<UserSession> inactiveSessions = sessionRepository.findActiveSessions()
                    .stream()
                    .filter(session -> session.getLastActivity().isBefore(timeout))
                    .collect(Collectors.toList());

            for (UserSession session : inactiveSessions) {
                session.setLogoutTime(getCurrentBrazilTime());
                session.setIsActive(false);
                sessionRepository.save(session);

                Duration sessionDuration = Duration.between(session.getLoginTime(), session.getLogoutTime());
                String durationText = formatDuration(sessionDuration);

                logActivity(session.getUser(), "SESSION_TIMEOUT", null,
                        "Sessão expirada por inatividade - Duração: " + durationText);
            }

            if (!inactiveSessions.isEmpty()) {
                System.out.println("Limpeza de sessões: " + inactiveSessions.size() + " sessões inativas finalizadas");
            }
        } catch (Exception e) {
            System.err.println("Erro na limpeza de sessões: " + e.getMessage());
        }
    }

    public Page<ActivityLogDto> getActivityLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::convertToActivityLogDto);
    }

    public Page<ActivityLogDto> getActivityLogsByUser(User user, Pageable pageable) {
        return activityLogRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToActivityLogDto);
    }

    public Page<ActivityLogDto> getActivityLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return activityLogRepository.findByDateRange(startDate, endDate, pageable)
                .map(this::convertToActivityLogDto);
    }

    public Page<UserSessionDto> getUserSessions(Pageable pageable) {
        return sessionRepository.findAllByOrderByLoginTimeDesc(pageable)
                .map(this::convertToUserSessionDto);
    }

    public List<UserSessionDto> getActiveSessions() {
        return sessionRepository.findActiveSessions()
                .stream()
                .map(this::convertToUserSessionDto)
                .collect(Collectors.toList());
    }

    public ActivityStatsDto getActivityStats() {
        LocalDateTime now = getCurrentBrazilTime();
        LocalDateTime oneDayAgo = now.minusDays(1);

        Long totalSessions = sessionRepository.count();
        Long activeSessions = sessionRepository.countActiveSessions();

        // Otimização: usar contagem direta em vez de carregar todos os logs
        Long totalLogins24h = activityLogRepository.countByActivityAndCreatedAtAfter("LOGIN", oneDayAgo);
        Long uniqueUsers24h = activityLogRepository.countDistinctUserByActivityAndCreatedAtAfter("LOGIN", oneDayAgo);

        // Otimização: usar consultas específicas para agregações
        Map<String, Long> loginsByHour = convertToLoginsByHour(activityLogRepository.findLoginsByHourRaw(oneDayAgo));
        Map<String, Long> activitiesByType = convertToActivitiesByType(activityLogRepository.findActivitiesByTypeRaw(oneDayAgo, now));

        return new ActivityStatsDto(
                totalSessions,
                activeSessions,
                totalLogins24h,
                uniqueUsers24h,
                activeSessions,
                loginsByHour,
                activitiesByType
        );
    }

    private ActivityLogDto convertToActivityLogDto(UserActivityLog log) {
        return new ActivityLogDto(
                log.getId(),
                log.getUser().getId(),
                log.getUser().getUsername(),
                log.getUser().getName() != null ? log.getUser().getName() : log.getUser().getUsername(),
                getReadableActivityName(log.getActivity()),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getSessionId(),
                log.getCreatedAt(),
                log.getAdditionalInfo()
        );
    }

    private UserSessionDto convertToUserSessionDto(UserSession session) {
        Long durationMinutes = null;
        if (session.getLoginTime() != null) {
            LocalDateTime endTime = session.getLogoutTime() != null ? session.getLogoutTime() : getCurrentBrazilTime();
            durationMinutes = Duration.between(session.getLoginTime(), endTime).toMinutes();
        }

        return new UserSessionDto(
                session.getId(),
                session.getUser().getId(),
                session.getUser().getUsername(),
                session.getUser().getName() != null ? session.getUser().getName() : session.getUser().getUsername(),
                session.getSessionId(),
                session.getLoginTime(),
                session.getLogoutTime(),
                session.getLastActivity(),
                session.getIpAddress(),
                session.getUserAgent(),
                session.getIsActive(),
                durationMinutes
        );
    }

    private LocalDateTime getCurrentBrazilTime() {
        return LocalDateTime.now(BRAZIL_ZONE);
    }

    /**
     * Converte resultado da consulta para Map de logins por hora
     */
    private Map<String, Long> convertToLoginsByHour(List<Object[]> results) {
        Map<String, Long> loginsByHour = new HashMap<>();
        for (Object[] result : results) {
            String hour = String.valueOf(result[0]);
            Long count = (Long) result[1];
            loginsByHour.put(hour, count);
        }
        return loginsByHour;
    }

    /**
     * Converte resultado da consulta para Map de atividades por tipo
     */
    private Map<String, Long> convertToActivitiesByType(List<Object[]> results) {
        Map<String, Long> activitiesByType = new HashMap<>();
        for (Object[] result : results) {
            String activity = (String) result[0];
            Long count = (Long) result[1];
            activitiesByType.put(getReadableActivityName(activity), count);
        }
        return activitiesByType;
    }

    private String formatActivityMessage(String activity, User user, String additionalInfo) {
        String userName = user.getName() != null ? user.getName() : user.getUsername();

        switch (activity) {
            case "LOGIN":
                return "Usuário " + userName + " fez login no sistema";
            case "LOGOUT":
                return "Usuário " + userName + " fez logout do sistema";
            case "LOGIN_FAILED":
                return "Tentativa de login falhou para " + userName;
            case "SESSION_TIMEOUT":
                return "Sessão de " + userName + " expirou por inatividade";
            case "SESSION_REPLACED":
                return "Sessão anterior de " + userName + " foi substituída por novo login";
            case "TICKET_CREATED":
                return additionalInfo != null ? additionalInfo : "Criou um novo chamado";
            case "TICKET_ASSIGNED":
                return additionalInfo != null ? additionalInfo : "Assumiu um chamado";
            case "TICKET_CLOSED":
                return additionalInfo != null ? additionalInfo : "Finalizou um chamado";
            case "TICKET_UPDATED":
                return additionalInfo != null ? additionalInfo : "Atualizou um chamado";
            case "TICKET_STATUS_CHANGED":
                return additionalInfo != null ? additionalInfo : "Alterou status de um chamado";
            case "USER_CREATED":
                return additionalInfo != null ? additionalInfo : "Criou um novo usuário";
            case "USER_UPDATED":
                return additionalInfo != null ? additionalInfo : "Atualizou um usuário";
            case "USER_DELETED":
                return additionalInfo != null ? additionalInfo : "Excluiu um usuário";
            case "USER_ENABLED":
                return additionalInfo != null ? additionalInfo : "Ativou um usuário";
            case "USER_DISABLED":
                return additionalInfo != null ? additionalInfo : "Desativou um usuário";
            default:
                return additionalInfo != null ? additionalInfo : "Ação realizada: " + activity;
        }
    }

    private String getReadableActivityName(String activity) {
        switch (activity) {
            case "LOGIN":
                return "Login";
            case "LOGOUT":
                return "Logout";
            case "LOGIN_FAILED":
                return "Login Falhou";
            case "SESSION_TIMEOUT":
                return "Sessão Expirada";
            case "SESSION_REPLACED":
                return "Sessão Substituída";
            case "TICKET_CREATED":
                return "Chamado Criado";
            case "TICKET_ASSIGNED":
                return "Chamado Atribuído";
            case "TICKET_CLOSED":
                return "Chamado Finalizado";
            case "TICKET_UPDATED":
                return "Chamado Atualizado";
            case "TICKET_STATUS_CHANGED":
                return "Status Alterado";
            case "USER_CREATED":
                return "Usuário Criado";
            case "USER_UPDATED":
                return "Usuário Atualizado";
            case "USER_DELETED":
                return "Usuário Excluído";
            case "USER_ENABLED":
                return "Usuário Ativado";
            case "USER_DISABLED":
                return "Usuário Desativado";
            default:
                return activity;
        }
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}