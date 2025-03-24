package com.helps.domain.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.Mensagem;
import com.helps.domain.model.User;
import com.helps.domain.repository.ChamadoRepository;
import com.helps.domain.repository.MensagemRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.MensagemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MensagemService {

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Mensagem> listarMensagensPorChamado(Long chamadoId) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));

        return mensagemRepository.findByChamadoOrderByDataEnvioAsc(chamado);
    }

    public Mensagem enviarMensagem(Long chamadoId, MensagemDto mensagemDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        String username = auth.getName();
        System.out.println("Tentando enviar mensagem como usuário: " + username);
        System.out.println("Tipo de autenticação: " + auth.getClass().getName());
        System.out.println("Principal: " + auth.getPrincipal());

        User remetente = null;

        try {
            remetente = userRepository.findByUsername(username)
                    .orElse(null);
            System.out.println("Busca por username: " + (remetente != null ? "encontrado" : "não encontrado"));
        } catch (Exception e) {
            System.err.println("Erro ao buscar por username: " + e.getMessage());
        }

        if (remetente == null) {
            try {
                Long userId = Long.parseLong(username);
                remetente = userRepository.findById(userId).orElse(null);
                System.out.println("Busca por ID " + userId + ": " + (remetente != null ? "encontrado" : "não encontrado"));
            } catch (NumberFormatException e) {
                System.out.println("Username não é numérico: " + username);
            }
        }

        if (remetente == null && auth.getPrincipal() instanceof Jwt) {
            try {
                Jwt jwt = (Jwt) auth.getPrincipal();
                String sub = jwt.getClaimAsString("sub");
                if (sub != null && !sub.equals(username)) {
                    System.out.println("Claim 'sub' diferente do username: " + sub);

                    remetente = userRepository.findByUsername(sub).orElse(null);
                    System.out.println("Busca por sub como username: " + (remetente != null ? "encontrado" : "não encontrado"));

                    if (remetente == null) {
                        try {
                            Long subId = Long.parseLong(sub);
                            remetente = userRepository.findById(subId).orElse(null);
                            System.out.println("Busca por sub como ID: " + (remetente != null ? "encontrado" : "não encontrado"));
                        } catch (NumberFormatException e) {
                            System.out.println("Sub claim não é numérico: " + sub);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao extrair sub do JWT: " + e.getMessage());
            }
        }

        if (remetente == null) {
            System.err.println("NENHUMA estratégia encontrou o usuário: " + username);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Usuário não encontrado");
        }

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));

        System.out.println("Status do chamado #" + chamadoId + ": " + chamado.getStatus());

        if (!"EM_ATENDIMENTO".equals(chamado.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível enviar mensagens para um chamado que não está em atendimento");
        }

        boolean isSystemAdmin = remetente.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN") || role.getName().equals("ROLE_ADMIN"));

        boolean isSolicitante = chamado.getUsuario() != null &&
                remetente.getId().equals(chamado.getUsuario().getId());

        boolean isHelper = chamado.getHelper() != null &&
                remetente.getId().equals(chamado.getHelper().getId());

        System.out.println("Verificação de permissão:");
        System.out.println("- É admin: " + isSystemAdmin);
        System.out.println("- É solicitante: " + isSolicitante);
        System.out.println("- É helper: " + isHelper);

        if (!isSystemAdmin && !isSolicitante && !isHelper) {
            System.err.println("Usuário " + username + " não tem permissão para enviar mensagens no chamado " + chamadoId);
            System.err.println("ID do usuário: " + remetente.getId());
            System.err.println("ID do solicitante: " + (chamado.getUsuario() != null ? chamado.getUsuario().getId() : "null"));
            System.err.println("ID do helper: " + (chamado.getHelper() != null ? chamado.getHelper().getId() : "null"));

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para enviar mensagens neste chamado");
        }

        Mensagem mensagem = new Mensagem();
        mensagem.setChamado(chamado);
        mensagem.setRemetente(remetente);
        mensagem.setConteudo(mensagemDTO.conteudo());
        mensagem.setDataEnvio(LocalDateTime.now());

        Mensagem mensagemSalva = mensagemRepository.save(mensagem);
        System.out.println("Mensagem salva com sucesso, ID: " + mensagemSalva.getId());

        return mensagemSalva;
    }
}