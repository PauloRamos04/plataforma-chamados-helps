package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.repository.TicketRepository;
import com.helps.dto.RealTimeMetricsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RealTimeMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(RealTimeMetricsService.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final AtomicLong ticketCounter = new AtomicLong(0);
    private RealTimeMetricsDto currentMetrics;

    @Scheduled(fixedRate = 60000) // A cada 1 minuto
    public void calculateAndSendRealTimeMetrics() {
        logger.debug("Calculando métricas em tempo real");

        try {
            currentMetrics = calculateRealTimeMetrics();
            messagingTemplate.convertAndSend("/topic/metrics", currentMetrics);
            logger.debug("Métricas em tempo real enviadas");
        } catch (Exception e) {
            logger.error("Erro ao calcular ou enviar métricas em tempo real", e);
        }
    }

    public void incrementTicketCounter() {
        ticketCounter.incrementAndGet();
    }

    public RealTimeMetricsDto getCurrentMetrics() {
        if (currentMetrics == null) {
            currentMetrics = calculateRealTimeMetrics();
        }
        return currentMetrics;
    }

    private RealTimeMetricsDto calculateRealTimeMetrics() {
        // Contar tickets por status
        List<Ticket> allTickets = ticketRepository.findAll();
        long openTickets = allTickets.stream().filter(t -> "ABERTO".equals(t.getStatus())).count();
        long inProgressTickets = allTickets.stream().filter(t -> "EM_ATENDIMENTO".equals(t.getStatus())).count();

        // Contar tickets criados na última hora
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Ticket> recentTickets = ticketRepository.findByOpeningDateBetween(oneHourAgo, LocalDateTime.now());

        // Calcular tickets por minuto (média da última hora)
        double ticketsPerMinute = recentTickets.size() / 60.0;

        // Contar tickets sem helper atribuído
        long unassignedTickets = allTickets.stream()
                .filter(t -> "ABERTO".equals(t.getStatus()) && t.getHelper() == null)
                .count();

        // Obter total desde a última reinicialização do contador
        long totalSinceRestart = ticketCounter.get();

        return new RealTimeMetricsDto(
                openTickets,
                inProgressTickets,
                recentTickets.size(),
                ticketsPerMinute,
                unassignedTickets,
                totalSinceRestart,
                LocalDateTime.now()
        );
    }
}