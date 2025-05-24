package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.domain.repository.UserSessionRepository;
import com.helps.dto.DashboardStatsDto;
import com.helps.dto.TicketTrendDto;
import com.helps.dto.HelperPerformanceDto;
import com.helps.dto.CategoryStatsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private UserContextService userContextService;

    public DashboardStatsDto getDashboardStats() {
        User currentUser = userContextService.getCurrentUser();
        boolean isAdmin = userContextService.hasAnyRole("ADMIN");
        boolean isHelper = userContextService.hasAnyRole("HELPER");

        List<Ticket> allTickets = isAdmin || isHelper ?
                ticketRepository.findAll() :
                ticketRepository.findByUser(currentUser);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(24);
        LocalDateTime last7days = now.minusDays(7);
        LocalDateTime last30days = now.minusDays(30);

        Map<String, Object> generalStats = calculateGeneralStats(allTickets, now);
        Map<String, Object> timeStats = calculateTimeStats(allTickets, last24h, last7days, last30days);
        Map<String, Object> statusDistribution = calculateStatusDistribution(allTickets);
        Map<String, Object> categoryStats = calculateCategoryStats(allTickets);
        Map<String, Object> priorityStats = calculatePriorityStats(allTickets);
        List<TicketTrendDto> weeklyTrends = calculateWeeklyTrends();
        List<HelperPerformanceDto> helperPerformance = isAdmin ? calculateHelperPerformance() : null;
        Map<String, Object> slaStats = calculateSlaStats(allTickets);
        Map<String, Object> workloadStats = calculateWorkloadStats(currentUser, isAdmin, isHelper);

        return new DashboardStatsDto(
                generalStats,
                timeStats,
                statusDistribution,
                categoryStats,
                priorityStats,
                weeklyTrends,
                helperPerformance,
                slaStats,
                workloadStats
        );
    }

    private Map<String, Object> calculateGeneralStats(List<Ticket> tickets, LocalDateTime now) {
        Map<String, Object> stats = new HashMap<>();

        long totalTickets = tickets.size();
        long openTickets = tickets.stream().filter(t -> "ABERTO".equals(t.getStatus())).count();
        long inProgressTickets = tickets.stream().filter(t -> "EM_ATENDIMENTO".equals(t.getStatus())).count();
        long closedTickets = tickets.stream().filter(t -> "FECHADO".equals(t.getStatus())).count();

        double resolutionRate = totalTickets > 0 ? (double) closedTickets / totalTickets * 100 : 0;

        OptionalDouble avgResolutionTime = tickets.stream()
                .filter(t -> t.getClosingDate() != null && t.getOpeningDate() != null)
                .mapToLong(t -> ChronoUnit.HOURS.between(t.getOpeningDate(), t.getClosingDate()))
                .average();

        long urgentTickets = tickets.stream()
                .filter(t -> !"FECHADO".equals(t.getStatus()))
                .filter(t -> ChronoUnit.HOURS.between(t.getOpeningDate(), now) > 24)
                .count();

        stats.put("totalTickets", totalTickets);
        stats.put("openTickets", openTickets);
        stats.put("inProgressTickets", inProgressTickets);
        stats.put("closedTickets", closedTickets);
        stats.put("resolutionRate", Math.round(resolutionRate * 100.0) / 100.0);
        stats.put("avgResolutionTime", avgResolutionTime.orElse(0.0));
        stats.put("urgentTickets", urgentTickets);

        return stats;
    }

    private Map<String, Object> calculateTimeStats(List<Ticket> tickets, LocalDateTime last24h, LocalDateTime last7days, LocalDateTime last30days) {
        Map<String, Object> stats = new HashMap<>();

        long tickets24h = tickets.stream().filter(t -> t.getOpeningDate().isAfter(last24h)).count();
        long tickets7days = tickets.stream().filter(t -> t.getOpeningDate().isAfter(last7days)).count();
        long tickets30days = tickets.stream().filter(t -> t.getOpeningDate().isAfter(last30days)).count();

        long resolved24h = tickets.stream()
                .filter(t -> t.getClosingDate() != null && t.getClosingDate().isAfter(last24h))
                .count();

        long resolved7days = tickets.stream()
                .filter(t -> t.getClosingDate() != null && t.getClosingDate().isAfter(last7days))
                .count();

        stats.put("created24h", tickets24h);
        stats.put("created7days", tickets7days);
        stats.put("created30days", tickets30days);
        stats.put("resolved24h", resolved24h);
        stats.put("resolved7days", resolved7days);

        return stats;
    }

    private Map<String, Object> calculateStatusDistribution(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        Ticket::getStatus,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private Map<String, Object> calculateCategoryStats(List<Ticket> tickets) {
        Map<String, Long> categoryCount = tickets.stream()
                .collect(Collectors.groupingBy(
                        Ticket::getCategory,
                        Collectors.counting()
                ));

        Map<String, Double> categoryAvgTime = new HashMap<>();
        for (String category : categoryCount.keySet()) {
            OptionalDouble avgTime = tickets.stream()
                    .filter(t -> category.equals(t.getCategory()))
                    .filter(t -> t.getClosingDate() != null && t.getOpeningDate() != null)
                    .mapToLong(t -> ChronoUnit.HOURS.between(t.getOpeningDate(), t.getClosingDate()))
                    .average();
            categoryAvgTime.put(category, avgTime.orElse(0.0));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("distribution", categoryCount);
        stats.put("avgResolutionTime", categoryAvgTime);

        return stats;
    }

    private Map<String, Object> calculatePriorityStats(List<Ticket> tickets) {
        Map<String, Object> stats = new HashMap<>();

        Map<String, Long> priorityDistribution = new HashMap<>();
        priorityDistribution.put("HIGH", tickets.stream().filter(t -> isHighPriority(t)).count());
        priorityDistribution.put("MEDIUM", tickets.stream().filter(t -> isMediumPriority(t)).count());
        priorityDistribution.put("LOW", tickets.stream().filter(t -> isLowPriority(t)).count());

        stats.put("distribution", priorityDistribution);
        stats.put("highPriorityOpen", tickets.stream()
                .filter(t -> !"FECHADO".equals(t.getStatus()))
                .filter(this::isHighPriority)
                .count());

        return stats;
    }

    private List<TicketTrendDto> calculateWeeklyTrends() {
        LocalDateTime now = LocalDateTime.now();
        List<TicketTrendDto> trends = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDateTime day = now.minusDays(i);
            LocalDateTime startOfDay = day.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfDay = day.withHour(23).withMinute(59).withSecond(59);

            List<Ticket> allTickets = ticketRepository.findAll();

            long created = allTickets.stream()
                    .filter(t -> t.getOpeningDate().isAfter(startOfDay) && t.getOpeningDate().isBefore(endOfDay))
                    .count();

            long resolved = allTickets.stream()
                    .filter(t -> t.getClosingDate() != null)
                    .filter(t -> t.getClosingDate().isAfter(startOfDay) && t.getClosingDate().isBefore(endOfDay))
                    .count();

            trends.add(new TicketTrendDto(
                    day.toLocalDate().toString(),
                    day.getDayOfWeek().toString(),
                    created,
                    resolved
            ));
        }

        return trends;
    }

    private List<HelperPerformanceDto> calculateHelperPerformance() {
        List<User> helpers = userRepository.findByRoleName("HELPER");
        LocalDateTime last30days = LocalDateTime.now().minusDays(30);

        return helpers.stream().map(helper -> {
            List<Ticket> helperTickets = ticketRepository.findByHelper(helper);

            long totalAssigned = helperTickets.size();
            long recentTickets = helperTickets.stream()
                    .filter(t -> t.getStartDate() != null && t.getStartDate().isAfter(last30days))
                    .count();

            long resolvedTickets = helperTickets.stream()
                    .filter(t -> "FECHADO".equals(t.getStatus()))
                    .count();

            double resolutionRate = totalAssigned > 0 ? (double) resolvedTickets / totalAssigned * 100 : 0;

            OptionalDouble avgResolutionTime = helperTickets.stream()
                    .filter(t -> t.getClosingDate() != null && t.getStartDate() != null)
                    .mapToLong(t -> ChronoUnit.HOURS.between(t.getStartDate(), t.getClosingDate()))
                    .average();

            long activeTickets = helperTickets.stream()
                    .filter(t -> !"FECHADO".equals(t.getStatus()))
                    .count();

            return new HelperPerformanceDto(
                    helper.getId(),
                    helper.getName() != null ? helper.getName() : helper.getUsername(),
                    totalAssigned,
                    recentTickets,
                    resolvedTickets,
                    Math.round(resolutionRate * 100.0) / 100.0,
                    Math.round(avgResolutionTime.orElse(0.0) * 100.0) / 100.0,
                    activeTickets
            );
        }).collect(Collectors.toList());
    }

    private Map<String, Object> calculateSlaStats(List<Ticket> tickets) {
        Map<String, Object> stats = new HashMap<>();

        long totalWithSla = tickets.size();
        long slaCompliant = tickets.stream()
                .filter(this::isSlaCompliant)
                .count();

        double slaComplianceRate = totalWithSla > 0 ? (double) slaCompliant / totalWithSla * 100 : 100;

        long slaViolations = tickets.stream()
                .filter(t -> !"FECHADO".equals(t.getStatus()))
                .filter(this::isSlaViolated)
                .count();

        stats.put("complianceRate", Math.round(slaComplianceRate * 100.0) / 100.0);
        stats.put("violations", slaViolations);
        stats.put("compliantTickets", slaCompliant);
        stats.put("totalTickets", totalWithSla);

        return stats;
    }

    private Map<String, Object> calculateWorkloadStats(User currentUser, boolean isAdmin, boolean isHelper) {
        Map<String, Object> stats = new HashMap<>();

        if (isAdmin) {
            long totalActiveUsers = sessionRepository.countActiveSessions();
            long totalHelpers = userRepository.findByRoleName("HELPER").size();
            long totalUsers = userRepository.count();

            stats.put("activeUsers", totalActiveUsers);
            stats.put("totalHelpers", totalHelpers);
            stats.put("totalUsers", totalUsers);
            stats.put("systemLoad", calculateSystemLoad());
        }

        if (isHelper) {
            List<Ticket> myTickets = ticketRepository.findByHelper(currentUser);
            long activeTickets = myTickets.stream()
                    .filter(t -> !"FECHADO".equals(t.getStatus()))
                    .count();

            stats.put("myActiveTickets", activeTickets);
            stats.put("myTotalTickets", myTickets.size());
        }

        if (!isAdmin && !isHelper) {
            List<Ticket> myTickets = ticketRepository.findByUser(currentUser);
            long openTickets = myTickets.stream()
                    .filter(t -> "ABERTO".equals(t.getStatus()))
                    .count();
            long inProgressTickets = myTickets.stream()
                    .filter(t -> "EM_ATENDIMENTO".equals(t.getStatus()))
                    .count();

            stats.put("myOpenTickets", openTickets);
            stats.put("myInProgressTickets", inProgressTickets);
            stats.put("myTotalTickets", myTickets.size());
        }

        return stats;
    }

    private boolean isHighPriority(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        long hoursOpen = ChronoUnit.HOURS.between(ticket.getOpeningDate(), now);
        return hoursOpen > 4 && !"FECHADO".equals(ticket.getStatus());
    }

    private boolean isMediumPriority(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        long hoursOpen = ChronoUnit.HOURS.between(ticket.getOpeningDate(), now);
        return hoursOpen > 1 && hoursOpen <= 4 && !"FECHADO".equals(ticket.getStatus());
    }

    private boolean isLowPriority(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        long hoursOpen = ChronoUnit.HOURS.between(ticket.getOpeningDate(), now);
        return hoursOpen <= 1 || "FECHADO".equals(ticket.getStatus());
    }

    private boolean isSlaCompliant(Ticket ticket) {
        if ("FECHADO".equals(ticket.getStatus()) && ticket.getClosingDate() != null) {
            long resolutionHours = ChronoUnit.HOURS.between(ticket.getOpeningDate(), ticket.getClosingDate());
            return resolutionHours <= 24;
        }

        long openHours = ChronoUnit.HOURS.between(ticket.getOpeningDate(), LocalDateTime.now());
        return openHours <= 24;
    }

    private boolean isSlaViolated(Ticket ticket) {
        return !isSlaCompliant(ticket);
    }

    private double calculateSystemLoad() {
        List<Ticket> activeTickets = ticketRepository.findAll().stream()
                .filter(t -> !"FECHADO".equals(t.getStatus()))
                .collect(Collectors.toList());

        long helpersCount = userRepository.findByRoleName("HELPER").size();
        if (helpersCount == 0) return 100.0;

        double avgTicketsPerHelper = (double) activeTickets.size() / helpersCount;
        return Math.min(100.0, avgTicketsPerHelper * 10);
    }
}