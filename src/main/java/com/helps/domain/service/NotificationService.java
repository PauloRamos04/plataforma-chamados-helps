package com.helps.domain.service;
import com.helps.domain.model.Role;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.Notification;
import com.helps.domain.model.User;
import com.helps.domain.repository.ChamadoRepository;
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
    private ChamadoRepository chamadoRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private UserContextService userContextService;

    @Transactional
    public NotificationDto criarNotificacaoParaUsuario(Long userId, String message, String type, Long chamadoId) {
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
        NotificationDto notificationDto = convertToDto(notification);

        webSocketService.enviarNotificacao(notificationDto, user);

        return notificationDto;
    }

    public void notificarNovosChamados(Chamado chamado) {
        List<User> helpers = new ArrayList<>();

        try {
            List<User> helpersWithoutPrefix = userContextService.findUsersWithRole("HELPER");
            System.out.println("Helpers encontrados sem prefixo ROLE_: " + helpersWithoutPrefix.size());
            helpers.addAll(helpersWithoutPrefix);
        } catch (Exception e) {
            System.err.println("Erro ao buscar helpers sem prefixo: " + e.getMessage());
        }

        try {
            List<User> helpersWithPrefix = userContextService.findUsersWithRole("ROLE_HELPER");
            System.out.println("Helpers encontrados com prefixo ROLE_: " + helpersWithPrefix.size());
            helpers.addAll(helpersWithPrefix);
        } catch (Exception e) {
            System.err.println("Erro ao buscar helpers com prefixo: " + e.getMessage());
        }

        helpers = helpers.stream().distinct().collect(Collectors.toList());

        System.out.println("Total de helpers únicos encontrados: " + helpers.size());

        try {
            List<User> admins = userContextService.findUsersWithRole("ADMIN");
            System.out.println("Admins encontrados: " + admins.size());
            helpers.addAll(admins);
        } catch (Exception e) {
            System.err.println("Erro ao buscar admins: " + e.getMessage());
        }

        try {
            List<User> adminsWithPrefix = userContextService.findUsersWithRole("ROLE_ADMIN");
            System.out.println("Admins encontrados com prefixo ROLE_: " + adminsWithPrefix.size());
            helpers.addAll(adminsWithPrefix);
        } catch (Exception e) {
            System.err.println("Erro ao buscar admins com prefixo: " + e.getMessage());
        }

        helpers = helpers.stream().distinct().collect(Collectors.toList());

        System.out.println("Total de usuários a notificar (helpers + admins): " + helpers.size());

        for (User user : helpers) {
            System.out.println("Usuário: " + user.getUsername() + ", ID: " + user.getId());
            System.out.println("  Papéis: " + user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.joining(", ")));

            try {
                criarNotificacaoParaUsuario(
                        user.getId(),
                        "Novo chamado disponível: " + chamado.getTitulo(),
                        "NOVO_CHAMADO",
                        chamado.getId()
                );
                System.out.println("  Notificação enviada com sucesso!");
            } catch (Exception e) {
                System.err.println("  Erro ao enviar notificação: " + e.getMessage());
            }
        }
    }

    public void notificarMensagemRecebida(Long chamadoId, Long remetenteId, String conteudoResumido) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado não encontrado"));

        User remetente = userRepository.findById(remetenteId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (chamado.getUsuario() != null && !chamado.getUsuario().getId().equals(remetenteId)) {
            criarNotificacaoParaUsuario(
                    chamado.getUsuario().getId(),
                    "Nova mensagem de " + remetente.getName() + ": " + conteudoResumido,
                    "NOVA_MENSAGEM",
                    chamadoId
            );
        }

        if (chamado.getHelper() != null && !chamado.getHelper().getId().equals(remetenteId)) {
            criarNotificacaoParaUsuario(
                    chamado.getHelper().getId(),
                    "Nova mensagem de " + remetente.getName() + ": " + conteudoResumido,
                    "NOVA_MENSAGEM",
                    chamadoId
            );
        }
    }

    @Transactional
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return convertToDto(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Notification> notifications = notificationRepository.findByUserAndReadFalse(user);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getChamadoId(),
                notification.getCreatedAt()
        );

        System.out.println("DTO criado: " + dto);
        return dto;
    }
}