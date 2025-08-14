package com.helps.controller;

import com.helps.domain.model.Message;
import com.helps.domain.service.MessageService;
import com.helps.dto.ChatMessageDto;
import com.helps.dto.MessageDto;
import com.helps.dto.MessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/tickets/{ticketId}/mensagens")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<MessageResponseDto>> listMessages(@PathVariable Long ticketId) {
        try {
            List<Message> messages = messageService.listMessagesByTicket(ticketId);
            List<MessageResponseDto> messageDtos = messages.stream()
                    .map(MessageResponseDto::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(messageDtos);
        } catch (Exception e) {
            log.error("Error listing messages for ticket {}: {}", ticketId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@PathVariable Long ticketId, @RequestBody Map<String, Object> requestBody) {
        try {
            String content = extractContent(requestBody);

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Bad Request",
                                "message", "Message content is required"
                        ));
            }

            MessageDto messageDTO = new MessageDto(content);
            Message message = messageService.sendMessage(ticketId, messageDTO);

            // Retorna DTO ao invés da entidade
            MessageResponseDto responseDto = MessageResponseDto.fromEntity(message);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

        } catch (Exception e) {
            log.error("Error sending message to ticket {}: {}", ticketId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Bad Request",
                            "message", "Error processing message: " + e.getMessage()
                    ));
        }
    }

    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessageWithImage(
            @PathVariable Long ticketId,
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            if ((content == null || content.trim().isEmpty()) && (image == null || image.isEmpty())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Bad Request",
                                "message", "A mensagem deve ter texto ou imagem"
                        ));
            }

            if (content == null || content.trim().isEmpty()) {
                content = "[Imagem]";
            }

            MessageDto messageDTO = new MessageDto(content);
            Message message = messageService.sendMessageWithImage(ticketId, messageDTO, image);

            // Retorna DTO ao invés da entidade
            MessageResponseDto responseDto = MessageResponseDto.fromEntity(message);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

        } catch (Exception e) {
            log.error("Error sending message with image to ticket {}: {}", ticketId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Bad Request",
                            "message", "Error processing message: " + e.getMessage()
                    ));
        }
    }

    @GetMapping("/chat-history")
    public @ResponseBody List<ChatMessageDto> getChatHistory(@PathVariable Long ticketId) {
        List<Message> messages = messageService.listMessagesByTicket(ticketId);
        return messages.stream()
                .map(this::convertToChatMessageDto)
                .collect(Collectors.toList());
    }

    @MessageMapping("/chat.sendMessage/{ticketId}")
    public void sendMessageViaWebSocket(
            @DestinationVariable Long ticketId,
            @Payload ChatMessageDto chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            String sessionId = headerAccessor.getSessionId();
            headerAccessor.getSessionAttributes().put("ticketId", ticketId);
            headerAccessor.getSessionAttributes().put("userId", chatMessage.senderId());
            headerAccessor.getSessionAttributes().put("username", chatMessage.senderName());

            MessageDto messageDto = new MessageDto(chatMessage.content());
            messageService.sendMessage(ticketId, messageDto);

            log.debug("WebSocket message processed: user={} ticket={} session={}",
                    chatMessage.senderName(), ticketId, sessionId);

        } catch (Exception e) {
            log.error("Error processing WebSocket message: user={} ticket={} error={}",
                    chatMessage.senderName(), ticketId, e.getMessage());
        }
    }

    @MessageMapping("/chat.addUser/{ticketId}")
    public void addUser(
            @DestinationVariable Long ticketId,
            @Payload ChatMessageDto chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            headerAccessor.getSessionAttributes().put("username", chatMessage.senderName());
            headerAccessor.getSessionAttributes().put("ticketId", ticketId);
            headerAccessor.getSessionAttributes().put("userId", chatMessage.senderId());

            log.debug("User joined chat: user={} ticket={} session={}",
                    chatMessage.senderName(), ticketId, headerAccessor.getSessionId());

        } catch (Exception e) {
            log.error("Error adding user to chat: user={} ticket={} error={}",
                    chatMessage.senderName(), ticketId, e.getMessage());
        }
    }

    private String extractContent(Map<String, Object> requestBody) {
        if (requestBody.containsKey("content")) {
            return (String) requestBody.get("content");
        } else if (requestBody.containsKey("conteudo")) {
            return (String) requestBody.get("conteudo");
        } else if (requestBody.containsKey("message")) {
            return (String) requestBody.get("message");
        } else if (requestBody.containsKey("texto")) {
            return (String) requestBody.get("texto");
        }
        return null;
    }

    private ChatMessageDto convertToChatMessageDto(Message message) {
        return new ChatMessageDto(
                "CHAT",
                message.getTicket().getId(),
                message.getSender().getId(),
                message.getSender().getName() != null ?
                        message.getSender().getName() :
                        message.getSender().getUsername(),
                message.getContent(),
                message.getSentDate()
        );
    }
}