package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import com.helps.dto.TicketDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserContextService userContextService;

    @Autowired
    private TicketAccessService ticketAccessService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Ticket> listTickets() {
        User currentUser = userContextService.getCurrentUser();

        boolean isHelper = userContextService.hasAnyRole("HELPER");
        boolean isAdmin = userContextService.hasAnyRole("ADMIN");

        System.out.println("Current user: " + currentUser.getUsername() +
                ", isHelper: " + isHelper +
                ", isAdmin: " + isAdmin);

        if (isAdmin) {
            return ticketRepository.findAll();
        } else if (isHelper) {
            List<Ticket> allTickets = ticketRepository.findAll();
            System.out.println("Found " + allTickets.size() + " total tickets");
            return allTickets;
        } else {
            List<Ticket> userTickets = ticketRepository.findByUser(currentUser);
            System.out.println("Found " + userTickets.size() + " user tickets");
            return userTickets;
        }
    }

    @Transactional
    public Ticket openTicket(Ticket ticket) {
        User solicitante = userContextService.getCurrentUser();

        ticket.setOpeningDate(LocalDateTime.now());
        ticket.setStatus("ABERTO");
        ticket.setUser(solicitante);

        Ticket saveTicket = ticketRepository.save(ticket);

        try {
            notificationService.notifyNewTickets(saveTicket);
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificações para o novo chamado: " + e.getMessage());
            e.printStackTrace();
        }

        return saveTicket;
    }

    @Transactional
    public Ticket openTicketImage(TicketDto ticketDto) {
        User solicitante = userContextService.getCurrentUser();

        Ticket ticket = new Ticket();
        ticket.setTitle(ticketDto.title());
        ticket.setDescription(ticketDto.description());
        ticket.setCategory(ticketDto.description());
        ticket.setType(ticketDto.description());
        ticket.setStartDate(LocalDateTime.now());
        ticket.setStatus("ABERTO");
        ticket.setUser(solicitante);

        if (ticketDto.image() != null && !ticketDto.image().isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(ticketDto.image());
                ticket.setImagePath(fileName);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Could not process the image: " + e.getMessage());
            }
        }

        Ticket saveTicket = ticketRepository.save(ticket);

        try {
            notificationService.notifyNewTickets(saveTicket);
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificações para o novo chamado: " + e.getMessage());
            e.printStackTrace();
        }

        return saveTicket;
    }

    @Transactional
    public Ticket updateTicket(Long id, Ticket updatedTicket) {
        return ticketRepository.findById(id)
                .map(ticket -> {
                    if (!ticketAccessService.canAccessTicket(ticket)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "You don't have permission to update this ticket");
                    }

                    ticket.setTitle(updatedTicket.getTitle());
                    ticket.setDescription(updatedTicket.getDescription());

                    if (userContextService.hasRole("ADMIN") && updatedTicket.getStatus() != null) {
                        String previousStatus = ticket.getStatus();
                        ticket.setStatus(updatedTicket.getStatus());

                        if (!previousStatus.equals(updatedTicket.getStatus())) {
                            webSocketService.notifyTicketStatus(ticket,
                                    "Status changed from " + previousStatus + " to " + ticket.getStatus());
                        }
                    }

                    return ticketRepository.save(ticket);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    @Transactional
    public void closeTicket(Long id) {
        ticketRepository.findById(id)
                .map(ticket -> {
                    ticketAccessService.verifyCloseTicketPermission(ticket);

                    ticket.setStatus("FECHADO");
                    ticket.setClosingDate(LocalDateTime.now());

                    Ticket closedTicket = ticketRepository.save(ticket);

                    webSocketService.notifyTicketStatus(closedTicket,
                            "Ticket finalized by " + userContextService.getCurrentUser().getName());

                    if (ticket.getUser() != null) {
                        notificationService.createNotificationForUser(
                                ticket.getUser().getId(),
                                "Your ticket \"" + ticket.getTitle() + "\" has been closed",
                                "CHAMADO_FECHADO",
                                ticket.getId());
                    }

                    return closedTicket;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    @Transactional
    public Ticket assignTicket(Long id) {
        User helper = userContextService.getCurrentUser();
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));

        ticketAccessService.verifyAssignTicketPermission(ticket);

        ticket.setHelper(helper);
        ticket.setStatus("EM_ATENDIMENTO");
        ticket.setStartDate(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        webSocketService.notifyTicketStatus(updatedTicket,
                helper.getName() + " começou a atender este chamado");

        if (ticket.getUser() != null) {
            notificationService.createNotificationForUser(
                    ticket.getUser().getId(),
                    "O seu chamado \"" + ticket.getTitle() + "\" começou a ser atendido por " + helper.getName(),
                    "CHAMADO_EM_ATENDIMENTO",
                    ticket.getId());
        }

        return updatedTicket;
    }

    public List<Ticket> listTicketsByHelper() {
        User helper = userContextService.getCurrentUser();

        if (!userContextService.hasAnyRole("HELPER", "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User doesn't have permission to list helper tickets");
        }

        return ticketRepository.findByHelper(helper);
    }

    public List<Ticket> listTicketsByUser() {
        User user = userContextService.getCurrentUser();
        return ticketRepository.findByUser(user);
    }

    public Optional<Ticket> findById(Long id) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);

        ticketOpt.ifPresent(ticket -> {
            if (!ticketAccessService.canAccessTicket(ticket)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You don't have permission to access this ticket");
            }
        });

        return ticketOpt;
    }
}