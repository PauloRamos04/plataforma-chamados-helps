package com.helps.util;

import com.helps.domain.model.*;
import com.helps.dto.*;

import java.util.List;
import java.util.stream.Collectors;

public class EntityDtoConverter {

    public static UserDto toDto(User user) {
        if (user == null) return null;

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEnabled(),
                roles
        );
    }

    public static CategoryDto toDto(Category category) {
        if (category == null) return null;

        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }

    public static TicketTypeDto toDto(TicketType type) {
        if (type == null) return null;

        return new TicketTypeDto(
                type.getId(),
                type.getName(),
                type.getDescription(),
                type.isActive(),
                type.getPriorityLevel(),
                type.getSlaMinutes()
        );
    }

    public static TicketDto toDto(Ticket ticket) {
        if (ticket == null) return null;

        return new TicketDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                toDto(ticket.getCategory()),
                toDto(ticket.getType()),
                ticket.getOpeningDate(),
                ticket.getStartDate(),
                ticket.getClosingDate(),
                toDto(ticket.getUser()),
                toDto(ticket.getHelper()),
                ticket.getImagePath()
        );
    }

    public static List<TicketDto> toTicketDtoList(List<Ticket> tickets) {
        if (tickets == null) return List.of();
        return tickets.stream()
                .map(EntityDtoConverter::toDto)
                .collect(Collectors.toList());
    }

    public static MessageDto toMessageDto(Message message) {
        if (message == null) return null;

        return new MessageDto(
                message.getId(),
                message.getTicket().getId(),
                toDto(message.getSender()),
                message.getContent(),
                message.getSentDate(),
                message.getTicket().getImagePath()
        );
    }

    public static List<MessageDto> toMessageDtoList(List<Message> messages) {
        if (messages == null) return List.of();
        return messages.stream()
                .map(EntityDtoConverter::toMessageDto)
                .collect(Collectors.toList());
    }

    public static NotificationDto toDto(Notification notification) {
        if (notification == null) return null;

        return new NotificationDto(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getTicketId(),
                notification.getCreatedAt()
        );
    }

    public static RoleDto toDto(Role role) {
        if (role == null) return null;

        return new RoleDto(
                role.getId(),
                role.getName(),
                null,
                true
        );
    }
}