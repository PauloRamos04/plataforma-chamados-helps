package com.helps.controller;

import com.helps.domain.model.User;
import com.helps.domain.repository.UserRepository;
import com.helps.domain.service.NotificationService;
import com.helps.domain.service.UserContextService;
import com.helps.dto.NotificationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserContextService userContextService;

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications() {
        try {
            User user = userContextService.getCurrentUser();
            List<NotificationDto> notifications = notificationService.getUnreadNotifications(user.getId());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            System.err.println("Erro ao buscar notificações não lidas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getAllNotifications() {
        try {
            User user = userContextService.getCurrentUser();
            List<NotificationDto> notifications = notificationService.getUnreadNotifications(user.getId());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            System.err.println("Erro ao buscar todas as notificações: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long id) {
        try {
            NotificationDto notification = notificationService.markAsRead(id);
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            System.err.println("Erro ao marcar notificação como lida: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead() {
        try {
            User user = userContextService.getCurrentUser();
            notificationService.markAllAsRead(user.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Erro ao marcar todas as notificações como lidas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getNotificationCount() {
        try {
            User user = userContextService.getCurrentUser();
            List<NotificationDto> unreadNotifications = notificationService.getUnreadNotifications(user.getId());

            Map<String, Integer> result = Map.of(
                    "total", unreadNotifications.size(),
                    "unread", unreadNotifications.size()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Erro ao contar notificações: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("total", 0, "unread", 0));
        }
    }

    @PostMapping("/test")
    public ResponseEntity<NotificationDto> createTestNotification(@RequestBody Map<String, Object> requestBody) {
        try {
            User user = userContextService.getCurrentUser();

            String message = (String) requestBody.getOrDefault("message", "Notificação de teste");
            String type = (String) requestBody.getOrDefault("type", "TEST");
            Long chamadoId = requestBody.get("chamadoId") instanceof Number ?
                    ((Number) requestBody.get("chamadoId")).longValue() : null;

            NotificationDto notification = notificationService.criarNotificacaoParaUsuario(
                    user.getId(),
                    message,
                    type,
                    chamadoId
            );

            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            System.err.println("Erro ao criar notificação de teste: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}