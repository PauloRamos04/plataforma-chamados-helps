package com.helps.domain.service;

import com.helps.domain.model.AuditLog;
import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import com.helps.domain.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportExportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportExportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserMetricsService userMetricsService;

    public byte[] exportTicketsCSV(String status, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Exportando tickets para CSV - Status: {}, Período: {} a {}",
                status, startDate, endDate);

        List<Ticket> tickets;
        if (status != null && !status.isEmpty()) {
            tickets = ticketRepository.findAll().stream()
                    .filter(t -> status.equals(t.getStatus()) &&
                            t.getOpeningDate() != null &&
                            t.getOpeningDate().isAfter(startDate) &&
                            t.getOpeningDate().isBefore(endDate))
                    .toList();
        } else {
            tickets = ticketRepository.findByOpeningDateBetween(startDate, endDate);
        }

        return generateTicketsCsv(tickets);
    }

    public byte[] exportUserTicketsCSV(Long userId) {
        logger.debug("Exportando tickets do usuário para CSV - User ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Ticket> tickets = ticketRepository.findByUser(user);

        return generateTicketsCsv(tickets);
    }

    public byte[] exportHelperTicketsCSV(Long userId) {
        logger.debug("Exportando tickets atendidos pelo helper para CSV - Helper ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Ticket> tickets = ticketRepository.findByHelper(user);

        return generateTicketsCsv(tickets);
    }

    public byte[] exportAuditLogsCSV(String entityType, Long entityId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Exportando logs de auditoria para CSV - Tipo: {}, ID: {}, Período: {} a {}",
                entityType, entityId, startDate, endDate);

        Page<AuditLog> auditLogs;

        if (entityType != null && entityId != null) {
            auditLogs = auditService.getEntityAuditLogs(entityType, entityId, Pageable.unpaged());
        } else if (entityType != null) {
            auditLogs = auditService.getAuditLogsByEntityType(entityType, Pageable.unpaged());
        } else if (startDate != null && endDate != null) {
            auditLogs = auditService.getAuditLogsByDateRange(startDate, endDate, Pageable.unpaged());
        } else {
            throw new IllegalArgumentException("Parâmetros insuficientes para exportação");
        }

        return generateAuditLogsCsv(auditLogs.getContent());
    }

    private byte[] generateTicketsCsv(List<Ticket> tickets) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("ID", "Título", "Descrição", "Status", "Categoria", "Tipo",
                             "Data de Abertura", "Data de Início", "Data de Fechamento",
                             "Usuário", "Helper"))) {

            for (Ticket ticket : tickets) {
                csvPrinter.printRecord(
                        ticket.getId(),
                        ticket.getTitle(),
                        ticket.getDescription(),
                        ticket.getStatus(),
                        ticket.getCategory() != null ? ticket.getCategory().getName() : "",
                        ticket.getType() != null ? ticket.getType().getName() : "",
                        ticket.getOpeningDate() != null ? ticket.getOpeningDate().format(DATE_FORMAT) : "",
                        ticket.getStartDate() != null ? ticket.getStartDate().format(DATE_FORMAT) : "",
                        ticket.getClosingDate() != null ? ticket.getClosingDate().format(DATE_FORMAT) : "",
                        ticket.getUser() != null ? ticket.getUser().getUsername() : "",
                        ticket.getHelper() != null ? ticket.getHelper().getUsername() : ""
                );
            }

            csvPrinter.flush();
            return out.toByteArray();

        } catch (IOException e) {
            logger.error("Erro ao gerar CSV de tickets", e);
            throw new RuntimeException("Erro ao gerar relatório de tickets", e);
        }
    }

    private byte[] generateAuditLogsCsv(List<AuditLog> auditLogs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("ID", "Tipo de Entidade", "ID da Entidade", "Ação",
                             "Usuário", "Data e Hora", "Valor Anterior", "Novo Valor"))) {

            for (AuditLog log : auditLogs) {
                csvPrinter.printRecord(
                        log.getId(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getAction(),
                        log.getChangedByUsername(),
                        log.getTimestamp().format(DATE_FORMAT),
                        log.getPreviousValue(),
                        log.getNewValue()
                );
            }

            csvPrinter.flush();
            return out.toByteArray();

        } catch (IOException e) {
            logger.error("Erro ao gerar CSV de logs de auditoria", e);
            throw new RuntimeException("Erro ao gerar relatório de auditoria", e);
        }
    }
}