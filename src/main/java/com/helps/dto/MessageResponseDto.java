package com.helps.dto;

import com.helps.domain.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {
    private Long id;
    private Long ticketId;
    private Long senderId;
    private String senderName;
    private String senderUsername;
    private String content;
    private String imagePath;
    private LocalDateTime sentDate;

    public static MessageResponseDto fromEntity(Message message) {
        MessageResponseDto dto = new MessageResponseDto();
        dto.setId(message.getId());
        dto.setTicketId(message.getTicket().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getName());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setContent(message.getContent());
        dto.setImagePath(message.getImagePath());
        dto.setSentDate(message.getSentDate());
        return dto;
    }
}