package com.helps.domain.service;

import com.helps.domain.model.Notification;
import com.helps.domain.model.User;
import com.helps.domain.repository.NotificationRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.NotificationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Criar e enviar uma notificação para um usuário
    public NotificationDto createAndSendNotification(Long userId, String message, String type, Long chamadoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setChamadoId(chamadoId);
        notification.setCreatedAt(LocalDateTime.now());

        notification = notificationRepository.save(notification);

        // Converter para DTO
        NotificationDto notificationDto = convertToDto(notification);

        // Enviar notificação via WebSocket
        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                notificationDto
        );

        return notificationDto;
    }

    // Buscar notificações não lidas de um usuário
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Marcar uma notificação como lida
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return convertToDto(notification);
    }

    // Marcar todas as notificações de um usuário como lidas
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Notification> notifications = notificationRepository.findByUserAndReadFalse(user);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    // Método auxiliar para converter a entidade para DTO
    private NotificationDto convertToDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getChamadoId(),
                notification.getCreatedAt()
        );
    }
}