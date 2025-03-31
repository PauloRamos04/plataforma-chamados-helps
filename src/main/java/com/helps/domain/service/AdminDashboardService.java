package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.AdminDashboardDto;
import com.helps.dto.HelperPerformanceDto;
import com.helps.dto.TicketStatisticsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AdminDashboardService {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardService.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Cacheable(value = "adminDashboard")
    public AdminDashboardDto getAdminDashboard() {
        logger.debug("Gerando dados para dashboard de administração");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        List<Ticket> todayTickets = ticketRepository.findByOpeningDateBetween(startOfDay, now);
        List<Ticket> weekTickets = ticketRepository.findByOpeningDateBetween(startOfWeek, now);
        List<Ticket> monthTickets = ticketRepository.findByOpeningDateBetween(startOfMonth, now);

        List<Ticket> allTickets = ticketRepository.findAll();

        // Estatísticas gerais
        TicketStatisticsDto statistics = new TicketStatisticsDto(
                allTickets.size(),
                allTickets.stream().filter(t -> "ABERTO".equals(t.getStatus())).count(),
                allTickets.stream().filter(t -> "EM_ATENDIMENTO".equals(t.getStatus())).count(),
                allTickets.stream().filter(t -> "FECHADO".equals(t.getStatus())).count(),
                todayTickets.size(),
                weekTickets.size(),
                monthTickets.size(),
                calculateAverageResolutionTime(allTickets),
                calculateAverageResponseTime(allTickets),
                getTicketsByCategory(allTickets),
                getTicketsByDay(monthTickets)
        );

        // Performance dos helpers
        List<HelperPerformanceDto> helperPerformance = getHelperPerformance();

        return new AdminDashboardDto(
                statistics,
                helperPerformance,
                monthTickets.stream().filter(t -> "FECHADO".equals(t.getStatus())).count(),
                monthTickets.stream().filter(t -> !"FECHADO".equals(t.getStatus())).count()
        );
    }

    private List<HelperPerformanceDto> getHelperPerformance() {
        List<User> helpers = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> "HELPER".equals(role.getName()) || "ROLE_HELPER".equals(role.getName())))
                .collect(Collectors.toList());

        return helpers.stream()
                .map(helper -> {
                    long totalTickets = ticketRepository.countByHelperId(helper.getId());
                    Double avgResolutionTime = ticketRepository.getAverageResolutionTimeByHelper(helper.getId());

                    return new HelperPerformanceDto(
                            helper.getId(),
                            helper.getUsername(),
                            helper.getName(),
                            totalTickets,
                            avgResolutionTime != null ? avgResolutionTime : 0.0,
                            calculateHelperEfficiency(helper)
                    );
                })
                .collect(Collectors.toList());
    }

    private Map<String, Long> getTicketsByCategory(List<Ticket> tickets) {
        return tickets.stream()
                .filter(ticket -> ticket.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Ticket::getCategoryName,
                        Collectors.counting()));
    }

    private Map<String, Long> getTicketsByDay(List<Ticket> tickets) {
        // Criando um mapa para os últimos 30 dias
        LocalDate today = LocalDate.now();

        Map<String, Long> ticketsByDay = IntStream.rangeClosed(1, 30)
                .mapToObj(i -> today.minusDays(i))
                .collect(Collectors.toMap(
                        date -> date.toString(),
                        date -> 0L,
                        (a, b) -> a,
                        HashMap::new
                ));

        // Contando tickets por dia
        tickets.stream()
                .filter(ticket -> ticket.getOpeningDate() != null)
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getOpeningDate().toLocalDate().toString(),
                        Collectors.counting()))
                .forEach((date, count) -> {
                    if (ticketsByDay.containsKey(date)) {
                        ticketsByDay.put(date, count);
                    }
                });

        return ticketsByDay;
    }

    private double calculateAverageResponseTime(List<Ticket> tickets) {
        return tickets.stream()
                .filter(ticket -> ticket.getOpeningDate() != null && ticket.getStartDate() != null)
                .mapToDouble(ticket -> ChronoUnit.MINUTES.between(ticket.getOpeningDate(), ticket.getStartDate()))
                .average()
                .orElse(0);
    }

    private double calculateAverageResolutionTime(List<Ticket> tickets) {
        return tickets.stream()
                .filter(ticket -> ticket.getOpeningDate() != null && ticket.getClosingDate() != null)
                .mapToDouble(ticket -> ChronoUnit.MINUTES.between(ticket.getOpeningDate(), ticket.getClosingDate()))
                .average()
                .orElse(0);
    }

    private double calculateHelperEfficiency(User helper) {
        List<Ticket> helperTickets = ticketRepository.findByHelper(helper);

        if (helperTickets.isEmpty()) {
            return 0.0;
        }

        // Contar tickets resolvidos dentro do SLA (considerando tipo do ticket)
        long resolvedWithinSla = helperTickets.stream()
                .filter(ticket -> {
                    if (!"FECHADO".equals(ticket.getStatus()) || ticket.getOpeningDate() == null || ticket.getClosingDate() == null) {
                        return false;
                    }

                    // Obter o SLA em minutos do tipo de ticket
                    int slaMinutos;
                    if (ticket.getType() != null && ticket.getType().getSlaMinutes() > 0) {
                        slaMinutos = ticket.getType().getSlaMinutes();
                    } else {
                        // SLA padrão se não estiver definido: 8 horas (480 minutos)
                        slaMinutos = 480;
                    }

                    // Calcular tempo de resolução em minutos
                    long minutosResolucao = ChronoUnit.MINUTES.between(ticket.getOpeningDate(), ticket.getClosingDate());

                    return minutosResolucao <= slaMinutos;
                })
                .count();

        // Contar tickets fechados
        long closedTickets = helperTickets.stream()
                .filter(ticket -> "FECHADO".equals(ticket.getStatus()))
                .count();

        // Se não tem tickets fechados, eficiência é 0
        if (closedTickets == 0) {
            return 0.0;
        }

        // Calcular eficiência: % resolvidos dentro do SLA
        return (double) resolvedWithinSla / closedTickets * 100.0;
    }
}