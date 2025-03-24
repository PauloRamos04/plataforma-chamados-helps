package com.helps.controller;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.Mensagem;
import com.helps.domain.model.User;
import com.helps.domain.repository.ChamadoRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.domain.service.MensagemService;
import com.helps.dto.ChatMessageDto;
import com.helps.dto.MensagemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MensagemService mensagemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @MessageMapping("/chat.sendMessage/{chamadoId}")
    @SendTo("/topic/chamado/{chamadoId}")
    public ChatMessageDto sendMessage(@DestinationVariable Long chamadoId, @Payload ChatMessageDto chatMessage) {
        MensagemDto mensagemDto = new MensagemDto(chatMessage.content());
        Mensagem mensagem = mensagemService.enviarMensagem(chamadoId, mensagemDto);

        return new ChatMessageDto(
                "CHAT",
                chamadoId,
                mensagem.getRemetente().getId(),
                mensagem.getRemetente().getName() != null ? mensagem.getRemetente().getName() : mensagem.getRemetente().getUsername(),
                mensagem.getConteudo(),
                mensagem.getDataEnvio()
        );
    }

    @MessageMapping("/chat.addUser/{chamadoId}")
    @SendTo("/topic/chamado/{chamadoId}")
    public ChatMessageDto addUser(@DestinationVariable Long chamadoId,
                                  @Payload ChatMessageDto chatMessage,
                                  SimpMessageHeaderAccessor headerAccessor) {

        headerAccessor.getSessionAttributes().put("username", chatMessage.senderName());
        headerAccessor.getSessionAttributes().put("chamadoId", chamadoId);

        return new ChatMessageDto(
                "JOIN",
                chamadoId,
                chatMessage.senderId(),
                chatMessage.senderName(),
                chatMessage.senderName() + " entrou no chat",
                LocalDateTime.now()
        );
    }

    @GetMapping("/chamados/{chamadoId}/chat-history")
    public @ResponseBody List<ChatMessageDto> getChatHistory(@PathVariable Long chamadoId) {
        // Busca as mensagens do chamado e converte para o formato de chat
        List<Mensagem> mensagens = mensagemService.listarMensagensPorChamado(chamadoId);

        return mensagens.stream().map(mensagem -> new ChatMessageDto(
                "CHAT",
                chamadoId,
                mensagem.getRemetente().getId(),
                mensagem.getRemetente().getName() != null ? mensagem.getRemetente().getName() : mensagem.getRemetente().getUsername(),
                mensagem.getConteudo(),
                mensagem.getDataEnvio()
        )).collect(Collectors.toList());
    }
}