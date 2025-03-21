package com.helps.domain.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.Mensagem;
import com.helps.domain.model.User;
import com.helps.domain.repository.ChamadoRepository;
import com.helps.domain.repository.MensagemRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.MensagemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

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

    public Mensagem enviarMensagem(Long chamadoId, MensagemDTO mensagemDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User remetente = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Usuário não encontrado"));

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));

        if (!"EM_ATENDIMENTO".equals(chamado.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível enviar mensagens para um chamado que não está em atendimento");
        }

        if (!remetente.equals(chamado.getUsuario()) && !remetente.equals(chamado.getHelper())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para enviar mensagens neste chamado");
        }

        Mensagem mensagem = new Mensagem();
        mensagem.setChamado(chamado);
        mensagem.setRemetente(remetente);
        mensagem.setConteudo(mensagemDTO.conteudo());
        mensagem.setDataEnvio(LocalDateTime.now());

        return mensagemRepository.save(mensagem);
    }
}