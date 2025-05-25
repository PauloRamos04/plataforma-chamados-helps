package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.Notification;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import com.helps.domain.repository.NotificationRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.NotificationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private UserContextService userContextService;

    @Transactional
    public NotificationDto createNotificationForUser(Long userId, String message, String type, Long ticketId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setTicketId(ticketId);
        notification.setCreatedAt(LocalDateTime.now());

        notification = notificationRepository.save(notification);
        NotificationDto notificationDto = convertToDto(notification);

        webSocketService.sendNotification(notificationDto, user);

        return notificationDto;
    }

    public void notifyNewTickets(Ticket ticket) {
        User ticketCreator = ticket.getUser();
        Long ticketCreatorId = ticketCreator != null ? ticketCreator.getId() : null;

        List<User> usersToNotify = new ArrayList<>();

        try {
            List<User> helpers = userRepository.findAll().stream()
                    .filter(user -> user.isEnabled() && hasHelperRole(user))
                    .collect(Collectors.toList());
            usersToNotify.addAll(helpers);

            List<User> admins = userRepository.findAll().stream()
                    .filter(user -> user.isEnabled() && hasAdminRole(user))
                    .collect(Collectors.toList());
            usersToNotify.addAll(admins);

            usersToNotify = usersToNotify.stream()
                    .distinct()
                    .collect(Collectors.toList());

            if (ticketCreatorId != null) {
                usersToNotify = usersToNotify.stream()
                        .filter(user -> !user.getId().equals(ticketCreatorId))
                        .collect(Collectors.toList());
            }

            for (User user : usersToNotify) {
                try {
                    String creatorName = ticketCreator != null ?
                            (ticketCreator.getName() != null ? ticketCreator.getName() : ticketCreator.getUsername()) :
                            "Usuário";

                    createNotificationForUser(
                            user.getId(),
                            "Novo ticket criado por " + creatorName + ": " + ticket.getTitle(),
                            "NEW_TICKET",
                            ticket.getId()
                    );
                } catch (Exception e) {
                    System.err.println("Erro ao enviar notificação para " + user.getUsername() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar notificações de novo ticket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean hasAdminRole(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN") ||
                        role.getName().equalsIgnoreCase("ROLE_ADMIN"));
    }

    private boolean hasHelperRole(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("HELPER") ||
                        role.getName().equalsIgnoreCase("ROLE_HELPER"));
    }

    public void notifyMessageReceived(Long ticketId, Long senderId, String summarizedContent) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String senderName = sender.getName() != null ? sender.getName() : sender.getUsername();

        if (ticket.getUser() != null && !ticket.getUser().getId().equals(senderId)) {
            createNotificationForUser(
                    ticket.getUser().getId(),
                    "Nova mensagem de " + senderName + ": " + summarizedContent,
                    "NEW_MESSAGE",
                    ticketId
            );
        }

        if (ticket.getHelper() != null && !ticket.getHelper().getId().equals(senderId)) {
            createNotificationForUser(
                    ticket.getHelper().getId(),
                    "Nova mensagem de " + senderName + ": " + summarizedContent,
                    "NEW_MESSAGE",
                    ticketId
            );
        }
    }

    public void notifyTicketAssigned(Ticket ticket, User helper) {
        if (ticket.getUser() != null && !ticket.getUser().getId().equals(helper.getId())) {
            String helperName = helper.getName() != null ? helper.getName() : helper.getUsername();
            createNotificationForUser(
                    ticket.getUser().getId(),
                    "Seu ticket \"" + ticket.getTitle() + "\" começou a ser atendido por " + helperName,
                    "TICKET_ASSIGNED",
                    ticket.getId()
            );
        }
    }

    public void notifyTicketClosed(Ticket ticket, User closer) {
        String closerName = closer.getName() != null ? closer.getName() : closer.getUsername();

        if (ticket.getUser() != null && !ticket.getUser().getId().equals(closer.getId())) {
            createNotificationForUser(
                    ticket.getUser().getId(),
                    "Seu ticket \"" + ticket.getTitle() + "\" foi finalizado por " + closerName,
                    "TICKET_CLOSED",
                    ticket.getId()
            );
        }

        if (ticket.getHelper() != null && !ticket.getHelper().getId().equals(closer.getId())) {
            createNotificationForUser(
                    ticket.getHelper().getId(),
                    "O ticket \"" + ticket.getTitle() + "\" foi finalizado por " + closerName,
                    "TICKET_CLOSED",
                    ticket.getId()
            );
        }
    }

    @Transactional
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return convertToDto(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Notification> notifications = notificationRepository.findByUserAndReadFalse(user);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    private NotificationDto convertToDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getTicketId(),
                notification.getCreatedAt()
        );
    }
}