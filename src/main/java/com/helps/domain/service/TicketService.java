package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import com.helps.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public Page<TicketResponseDto> findAllWithPagination(Pageable pageable, TicketFilterDto filters) {
        User currentUser = userContextService.getCurrentUser();
        Specification<Ticket> spec = createSpecification(currentUser, filters);
        Page<Ticket> tickets = ticketRepository.findAll(spec, pageable);
        return tickets.map(this::convertToResponseDto);
    }

    public List<Ticket> listTickets() {
        User currentUser = userContextService.getCurrentUser();
        boolean isHelper = userContextService.hasAnyRole("HELPER");
        boolean isAdmin = userContextService.hasAnyRole("ADMIN");

        if (isAdmin || isHelper) {
            return ticketRepository.findAll();
        } else {
            return ticketRepository.findByUser(currentUser);
        }
    }

    @Transactional
    public TicketResponseDto createTicket(CreateTicketDto dto) {
        User currentUser = userContextService.getCurrentUser();

        Ticket ticket = new Ticket();
        ticket.setTitle(dto.title());
        ticket.setDescription(dto.description());
        ticket.setCategory(dto.category());
        ticket.setStatus("ABERTO");
        ticket.setOpeningDate(LocalDateTime.now());
        ticket.setUser(currentUser);

        if (dto.image() != null && !dto.image().isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(dto.image());
                ticket.setImagePath(fileName);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Não foi possível processar a imagem: " + e.getMessage());
            }
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        try {
            notificationService.notifyNewTickets(savedTicket);
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificações: " + e.getMessage());
        }

        return convertToResponseDto(savedTicket);
    }

    @Transactional
    public TicketResponseDto assignTicket(Long id) {
        User helper = userContextService.getCurrentUser();
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));

        ticketAccessService.verifyAssignTicketPermission(ticket);

        ticket.setHelper(helper);
        ticket.setStatus("EM_ATENDIMENTO");
        ticket.setStartDate(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        // Notificar via WebSocket
        webSocketService.notifyTicketStatus(updatedTicket,
                helper.getName() + " começou a atender este ticket");

        // Notificar apenas o solicitante via sistema de notificações
        notificationService.notifyTicketAssigned(ticket, helper);

        return convertToResponseDto(updatedTicket);
    }

    @Transactional
    public void closeTicket(Long id) {
        User currentUser = userContextService.getCurrentUser();
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));

        ticketAccessService.verifyCloseTicketPermission(ticket);

        ticket.setStatus("FECHADO");
        ticket.setClosingDate(LocalDateTime.now());

        Ticket closedTicket = ticketRepository.save(ticket);

        // Notificar via WebSocket
        webSocketService.notifyTicketStatus(closedTicket,
                "Ticket finalizado por " + currentUser.getName());

        // Notificar participantes relevantes (exceto quem fechou)
        notificationService.notifyTicketClosed(ticket, currentUser);
    }

    public Optional<TicketResponseDto> findById(Long id) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);

        ticketOpt.ifPresent(ticket -> {
            if (!ticketAccessService.canAccessTicket(ticket)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem permissão para acessar este ticket");
            }
        });

        return ticketOpt.map(this::convertToResponseDto);
    }

    @Transactional
    public Ticket openTicket(Ticket ticket) {
        User solicitante = userContextService.getCurrentUser();
        ticket.setOpeningDate(LocalDateTime.now());
        ticket.setStatus("ABERTO");
        ticket.setUser(solicitante);

        Ticket savedTicket = ticketRepository.save(ticket);

        try {
            notificationService.notifyNewTickets(savedTicket);
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificações: " + e.getMessage());
        }

        return savedTicket;
    }

    @Transactional
    public Ticket openTicketImage(TicketDto ticketDto) {
        User solicitante = userContextService.getCurrentUser();

        Ticket ticket = new Ticket();
        ticket.setTitle(ticketDto.title());
        ticket.setDescription(ticketDto.description());
        ticket.setCategory(ticketDto.category());
        ticket.setOpeningDate(LocalDateTime.now());
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

        Ticket savedTicket = ticketRepository.save(ticket);

        try {
            notificationService.notifyNewTickets(savedTicket);
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificações: " + e.getMessage());
        }

        return savedTicket;
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

    private Specification<Ticket> createSpecification(User currentUser, TicketFilterDto filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!userContextService.hasAnyRole("ADMIN", "HELPER")) {
                predicates.add(criteriaBuilder.equal(root.get("user"), currentUser));
            }

            if (filters != null) {
                if (filters.status() != null && !filters.status().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filters.status()));
                }

                if (filters.category() != null && !filters.category().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("category"), filters.category()));
                }

                if (filters.search() != null && !filters.search().isEmpty()) {
                    String searchPattern = "%" + filters.search().toLowerCase() + "%";
                    Predicate titlePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")), searchPattern);
                    Predicate descriptionPredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("description")), searchPattern);
                    predicates.add(criteriaBuilder.or(titlePredicate, descriptionPredicate));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TicketResponseDto convertToResponseDto(Ticket ticket) {
        return new TicketResponseDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCategory(),
                ticket.getOpeningDate(),
                ticket.getStartDate(),
                ticket.getClosingDate(),
                ticket.getImagePath(),
                ticket.getUser() != null ? convertUserToDto(ticket.getUser()) : null,
                ticket.getHelper() != null ? convertUserToDto(ticket.getHelper()) : null
        );
    }

    private UserResponseDto convertUserToDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> role.getName())
                        .toList()
        );
    }
}