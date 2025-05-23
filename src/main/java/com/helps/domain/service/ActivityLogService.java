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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

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
        log.setCreatedAt(LocalDateTime.now());
        log.setAdditionalInfo(additionalInfo);

        if (request != null) {
            log.setIpAddress(getClientIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setSessionId(request.getSession().getId());
        }

        activityLogRepository.save(log);
    }

    @Transactional
    public String createUserSession(User user, HttpServletRequest request) {
        System.out.println("Usuário: " + user.getUsername());

        List<UserSession> activeSessions = sessionRepository.findByUserAndIsActiveTrueOrderByLoginTimeDesc(user);
        System.out.println("Sessões ativas encontradas para finalizar: " + activeSessions.size());

        for (UserSession activeSession : activeSessions) {
            activeSession.setLogoutTime(LocalDateTime.now());
            activeSession.setIsActive(false);
            sessionRepository.save(activeSession);

            logActivity(user, "SESSION_REPLACED", request, "Sessão anterior finalizada por novo login");
            System.out.println("Sessão finalizada: " + activeSession.getSessionId());
        }

        String sessionId = UUID.randomUUID().toString();
        System.out.println("Novo sessionId gerado: " + sessionId);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionId(sessionId);
        session.setLoginTime(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        session.setIsActive(true);

        if (request != null) {
            session.setIpAddress(getClientIpAddress(request));
            session.setUserAgent(request.getHeader("User-Agent"));
        }

        sessionRepository.save(session);

        logActivity(user, "LOGIN", request, "Usuário fez login no sistema");

        return sessionId;
    }

    @Transactional
    public void endUserSession(String sessionId) {
        System.out.println("SessionId: " + sessionId);

        if (sessionId == null || sessionId.isEmpty()) {
            System.err.println("SessionId vazio ou nulo para finalizar sessão");
            return;
        }

        Optional<UserSession> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);

        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setLogoutTime(LocalDateTime.now());
            session.setIsActive(false);
            sessionRepository.save(session);

            logActivity(session.getUser(), "LOGOUT", null, "Usuário fez logout do sistema");

            System.out.println("Sessão finalizada com sucesso: " + sessionId + " para usuário: " + session.getUser().getUsername());
        } else {
            System.err.println("Sessão não encontrada ou já inativa: " + sessionId);

            try {
                User currentUser = userContextService.getCurrentUser();
                List<UserSession> activeSessions = sessionRepository.findByUserAndIsActiveTrueOrderByLoginTimeDesc(currentUser);

                System.out.println("Fallback: encontradas " + activeSessions.size() + " sessões ativas para finalizar");

                for (UserSession activeSession : activeSessions) {
                    activeSession.setLogoutTime(LocalDateTime.now());
                    activeSession.setIsActive(false);
                    sessionRepository.save(activeSession);

                    logActivity(currentUser, "LOGOUT", null, "Logout realizado (sessão encontrada por usuário)");
                }

                System.out.println("Finalizadas " + activeSessions.size() + " sessões ativas como fallback");
            } catch (Exception e) {
                System.err.println("Erro no fallback de finalização de sessão: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void updateLastActivity(String sessionId) {
        sessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                .ifPresent(session -> {
                    session.setLastActivity(LocalDateTime.now());
                    sessionRepository.save(session);
                });
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupInactiveSessions() {
        try {
            LocalDateTime timeout = LocalDateTime.now().minusHours(2);

            List<UserSession> inactiveSessions = sessionRepository.findActiveSessions()
                    .stream()
                    .filter(session -> session.getLastActivity().isBefore(timeout))
                    .collect(Collectors.toList());

            for (UserSession session : inactiveSessions) {
                session.setLogoutTime(LocalDateTime.now());
                session.setIsActive(false);
                sessionRepository.save(session);

                logActivity(session.getUser(), "SESSION_TIMEOUT", null,
                        "Sessão expirada por inatividade");
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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);

        Long totalSessions = sessionRepository.count();
        Long activeSessions = sessionRepository.countActiveSessions();

        List<UserActivityLog> logins24h = activityLogRepository.findByActivityAndCreatedAtAfter("LOGIN", oneDayAgo);
        Long totalLogins24h = (long) logins24h.size();

        Map<String, Long> loginsByHour = logins24h.stream()
                .collect(Collectors.groupingBy(
                        log -> String.valueOf(log.getCreatedAt().getHour()),
                        Collectors.counting()
                ));

        List<UserActivityLog> activities24h = activityLogRepository.findByDateRange(oneDayAgo, now, Pageable.unpaged()).getContent();
        Map<String, Long> activitiesByType = activities24h.stream()
                .collect(Collectors.groupingBy(
                        UserActivityLog::getActivity,
                        Collectors.counting()
                ));

        return new ActivityStatsDto(
                totalSessions,
                activeSessions,
                totalLogins24h,
                0L,
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
                log.getActivity(),
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
            LocalDateTime endTime = session.getLogoutTime() != null ? session.getLogoutTime() : LocalDateTime.now();
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

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
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