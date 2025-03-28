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
        notification.setTicketId(ticketId); // Previously chamadoId
        notification.setCreatedAt(LocalDateTime.now());

        notification = notificationRepository.save(notification);
        NotificationDto notificationDto = convertToDto(notification);

        webSocketService.sendNotification(notificationDto, user);

        return notificationDto;
    }

    public void notifyNewTickets(Ticket ticket) { // previously notificarNovosChamados
        // Get the current user who created the ticket
        User ticketCreator = ticket.getUser();
        Long ticketCreatorId = ticketCreator != null ? ticketCreator.getId() : null;

        // Find all users with role HELPER or ADMIN (with or without ROLE_ prefix)
        List<User> usersToNotify = new ArrayList<>();

        try {
            // Find helpers
            List<User> helpers = userRepository.findAll().stream()
                    .filter(user -> user.isEnabled() &&
                            (hasRole(user, "HELPER") || hasRole(user, "ROLE_HELPER")))
                    .collect(Collectors.toList());
            usersToNotify.addAll(helpers);

            // Find admins
            List<User> admins = userRepository.findAll().stream()
                    .filter(user -> user.isEnabled() &&
                            (hasRole(user, "ADMIN") || hasRole(user, "ROLE_ADMIN")))
                    .collect(Collectors.toList());
            usersToNotify.addAll(admins);

            // Remove duplicates (in case a user has multiple roles)
            usersToNotify = usersToNotify.stream()
                    .distinct()
                    .collect(Collectors.toList());

            // Remove the user who created the ticket from the list (if present)
            if (ticketCreatorId != null) {
                usersToNotify = usersToNotify.stream()
                        .filter(user -> !user.getId().equals(ticketCreatorId))
                        .collect(Collectors.toList());
            }

            System.out.println("Total users to notify: " + usersToNotify.size());

            // Send notifications to each recipient
            for (User user : usersToNotify) {
                try {
                    createNotificationForUser(
                            user.getId(),
                            "New ticket available: " + ticket.getTitle(),
                            "NEW_TICKET", // previously NOVO_CHAMADO
                            ticket.getId()
                    );
                    System.out.println("Notification sent to: " + user.getUsername());
                } catch (Exception e) {
                    System.err.println("Error sending notification to " + user.getUsername() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing new ticket notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    public void notifyMessageReceived(Long ticketId, Long senderId, String summarizedContent) { // previously notificarMensagemRecebida
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (ticket.getUser() != null && !ticket.getUser().getId().equals(senderId)) {
            createNotificationForUser(
                    ticket.getUser().getId(),
                    "New message from " + sender.getName() + ": " + summarizedContent,
                    "NEW_MESSAGE", // previously NOVA_MENSAGEM
                    ticketId
            );
        }

        if (ticket.getHelper() != null && !ticket.getHelper().getId().equals(senderId)) {
            createNotificationForUser(
                    ticket.getHelper().getId(),
                    "New message from " + sender.getName() + ": " + summarizedContent,
                    "NEW_MESSAGE", // previously NOVA_MENSAGEM
                    ticketId
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