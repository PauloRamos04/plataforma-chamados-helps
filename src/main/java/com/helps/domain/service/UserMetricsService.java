package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import com.helps.domain.repository.AuditLogRepository;
import com.helps.domain.repository.MessageRepository;
import com.helps.domain.repository.TicketRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.UserMetricsDto;
import com.helps.dto.UserActivitySummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(UserMetricsService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Cacheable(value = "userMetrics", key = "#userId")
    public UserMetricsDto getUserMetrics(Long userId) {
        logger.debug("Calculating metrics for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with ID: " + userId));

        // Tickets opened by the user
        List<Ticket> userTickets = ticketRepository.findByUser(user);

        // Tickets helped by the user (if a helper)
        List<Ticket> helpedTickets = ticketRepository.findByHelper(user);

        // Count by status of tickets opened by the user
        Map<String, Long> ticketsByStatus = userTickets.stream()
                .collect(Collectors.groupingBy(Ticket::getStatus, Collectors.counting()));

        // Count by status of tickets helped by the user
        Map<String, Long> helpedTicketsByStatus = helpedTickets.stream()
                .collect(Collectors.groupingBy(Ticket::getStatus, Collectors.counting()));

        // Average response time (for helpers)
        double avgResponseTime = calculateAverageResponseTime(helpedTickets);

        // Average resolution time (for helpers)
        double avgResolutionTime = calculateAverageResolutionTime(helpedTickets);

        // Total number of tickets created
        long totalTicketsCreated = userTickets.size();

        // Total number of tickets helped
        long totalTicketsHelped = helpedTickets.size();

        return new UserMetricsDto(
                userId,
                user.getUsername(),
                user.getName(),
                totalTicketsCreated,
                totalTicketsHelped,
                ticketsByStatus,
                helpedTicketsByStatus,
                avgResponseTime,
                avgResolutionTime
        );
    }

    @Cacheable(value = "userActivity", key = "#userId")
    public UserActivitySummaryDto getUserActivitySummary(Long userId) {
        logger.debug("Getting activity summary for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with ID: " + userId));

        // Currently open tickets by the user
        List<Ticket> openTickets = ticketRepository.findByUser(user).stream()
                .filter(ticket -> !"CLOSED".equals(ticket.getStatus()))
                .collect(Collectors.toList());

        // Tickets currently in progress by the user (if helper)
        List<Ticket> ticketsInProgress = ticketRepository.findByHelper(user).stream()
                .filter(ticket -> "IN_PROGRESS".equals(ticket.getStatus()))
                .collect(Collectors.toList());

        // Recent actions by the user
        Page<com.helps.domain.model.AuditLog> recentActions =
                auditLogRepository.findByChangedById(userId, Pageable.ofSize(10));

        // Calculate average tickets created per day in the last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);

        List<Ticket> userTickets = ticketRepository.findByUser(user);
        long ticketsLastMonth = userTickets.stream()
                .filter(ticket -> ticket.getOpeningDate() != null &&
                        ticket.getOpeningDate().isAfter(thirtyDaysAgo))
                .count();

        double avgTicketsPerDay = ticketsLastMonth / 30.0;

        return new UserActivitySummaryDto(
                userId,
                user.getUsername(),
                user.getName(),
                openTickets.size(),
                ticketsInProgress.size(),
                recentActions.getContent().size(),
                avgTicketsPerDay,
                openTickets,
                ticketsInProgress,
                recentActions.getContent()
        );
    }

    public Page<Ticket> getUserTickets(Long userId, Pageable pageable) {
        logger.debug("Getting tickets for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with ID: " + userId));

        return ticketRepository.findByUser(user, pageable);
    }

    public Page<Ticket> getUserHelperTickets(Long userId, Pageable pageable) {
        logger.debug("Getting tickets helped by user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with ID: " + userId));

        return ticketRepository.findByHelper(user, pageable);
    }

    private double calculateAverageResponseTime(List<Ticket> tickets) {
        // Calculate average time between opening and start of attention
        return tickets.stream()
                .filter(ticket -> ticket.getOpeningDate() != null && ticket.getStartDate() != null)
                .mapToLong(ticket -> Duration.between(ticket.getOpeningDate(), ticket.getStartDate()).toMinutes())
                .average()
                .orElse(0);
    }

    private double calculateAverageResolutionTime(List<Ticket> tickets) {
        // Calculate average time between opening and closing
        return tickets.stream()
                .filter(ticket -> ticket.getOpeningDate() != null && ticket.getClosingDate() != null)
                .mapToLong(ticket -> Duration.between(ticket.getOpeningDate(), ticket.getClosingDate()).toMinutes())
                .average()
                .orElse(0);
    }
}