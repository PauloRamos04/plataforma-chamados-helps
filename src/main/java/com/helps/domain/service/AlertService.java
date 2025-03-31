package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.AlertDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    private static final int MAX_OPEN_TICKETS = 20;
    private static final int MAX_HELPER_TICKETS = 10;
    private static final int TICKET_SLA_WARNING_MINUTES = 360;
    private static final int TICKET_SLA_CRITICAL_MINUTES = 480;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 300000)
    public void checkForAlerts() {
        logger.debug("Verificando condições para alertas");

        List<AlertDto> alerts = new ArrayList<>();

        long openTicketsCount = ticketRepository.findByStatus("ABERTO").size();
        if (openTicketsCount > MAX_OPEN_TICKETS) {
            alerts.add(new AlertDto(
                    "HIGH_OPEN_TICKETS",
                    "Alerta: Muitos tickets abertos",
                    "Existem " + openTicketsCount + " tickets abertos no sistema. Considere alocar mais helpers.",
                    "HIGH",
                    LocalDateTime.now()
            ));
        }

        // Alerta 2: Tickets próximos do SLA
        List<Ticket> ticketsNearSla = findTicketsNearSla();
        if (!ticketsNearSla.isEmpty()) {
            alerts.add(new AlertDto(
                    "SLA_WARNING",
                    "Alerta: Tickets próximos do prazo SLA",
                    ticketsNearSla.size() + " tickets estão próximos de violar o SLA. Requer atenção imediata.",
                    "MEDIUM",
                    LocalDateTime.now()
            ));
        }

        // Alerta 3: Tickets com SLA violado
        List<Ticket> ticketsWithSlaViolation = findTicketsWithSlaViolation();
        if (!ticketsWithSlaViolation.isEmpty()) {
            alerts.add(new AlertDto(
                    "SLA_VIOLATION",
                    "Alerta CRÍTICO: Tickets com SLA violado",
                    ticketsWithSlaViolation.size() + " tickets já violaram o SLA. Requer intervenção imediata.",
                    "CRITICAL",
                    LocalDateTime.now()
            ));
        }

        // Alerta 4: Helpers sobrecarregados
        List<User> overloadedHelpers = findOverloadedHelpers();
        if (!overloadedHelpers.isEmpty()) {
            alerts.add(new AlertDto(
                    "OVERLOADED_HELPERS",
                    "Alerta: Helpers sobrecarregados",
                    overloadedHelpers.size() + " helpers estão com mais tickets do que o recomendado.",
                    "MEDIUM",
                    LocalDateTime.now()
            ));
        }

        if (!alerts.isEmpty()) {
            logger.info("Enviando {} alertas para os administradores", alerts.size());
            alerts.forEach(alert -> {
                messagingTemplate.convertAndSend("/topic/admin/alerts", alert);
                logger.debug("Enviado alerta: {}", alert.title());
            });
        }
    }

    private List<Ticket> findTicketsNearSla() {
        List<Ticket> tickets = ticketRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        return tickets.stream()
                .filter(ticket -> {
                    if (!"EM_ATENDIMENTO".equals(ticket.getStatus()) || ticket.getOpeningDate() == null) {
                        return false;
                    }

                    long minutesOpen = Duration.between(ticket.getOpeningDate(), now).toMinutes();

                    // Obter o SLA em minutos do tipo de ticket
                    int slaMinutes;
                    if (ticket.getType() != null && ticket.getType().getSlaMinutes() > 0) {
                        slaMinutes = ticket.getType().getSlaMinutes();
                    } else {
                        // SLA padrão se não estiver definido: 8 horas (480 minutos)
                        slaMinutes = 480;
                    }

                    return minutesOpen >= (slaMinutes * 0.75) && minutesOpen < slaMinutes;
                })
                .collect(Collectors.toList());
    }

    private List<Ticket> findTicketsWithSlaViolation() {
        List<Ticket> tickets = ticketRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        return tickets.stream()
                .filter(ticket -> {
                    if (!"EM_ATENDIMENTO".equals(ticket.getStatus()) || ticket.getOpeningDate() == null) {
                        return false;
                    }

                    long minutesOpen = Duration.between(ticket.getOpeningDate(), now).toMinutes();

                    // Obter o SLA em minutos do tipo de ticket
                    int slaMinutes;
                    if (ticket.getType() != null && ticket.getType().getSlaMinutes() > 0) {
                        slaMinutes = ticket.getType().getSlaMinutes();
                    } else {
                        // SLA padrão se não estiver definido: 8 horas (480 minutos)
                        slaMinutes = 480;
                    }

                    return minutesOpen >= slaMinutes;
                })
                .collect(Collectors.toList());
    }


    private List<User> findOverloadedHelpers() {
        List<User> helpers = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> "HELPER".equals(role.getName()) || "ROLE_HELPER".equals(role.getName())))
                .collect(Collectors.toList());

        return helpers.stream()
                .filter(helper -> {
                    List<Ticket> helperTickets = ticketRepository.findByHelper(helper);
                    long activeTickets = helperTickets.stream()
                            .filter(ticket -> "EM_ATENDIMENTO".equals(ticket.getStatus()))
                            .count();

                    return activeTickets > MAX_HELPER_TICKETS;
                })
                .collect(Collectors.toList());
    }
}