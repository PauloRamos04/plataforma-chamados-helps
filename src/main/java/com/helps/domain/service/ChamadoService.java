package com.helps.domain.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.User;
import com.helps.domain.repository.ChamadoRepository;
import com.helps.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public List<Chamado> listarChamadosPorStatus(String status) {
        return chamadoRepository.findByStatus(status);
    }

    public Chamado abrirChamado(Chamado chamado) {
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setStatus("ABERTO");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        String username = auth.getName();
        System.out.println("Tentando encontrar usuário: " + username);

        // Verificar se o token contém 'sub' numérico (caso seja o ID em vez do username)
        Long userId = null;
        try {
            userId = Long.parseLong(username);
            System.out.println("O token contém um ID numérico: " + userId);
        } catch (NumberFormatException e) {
            System.out.println("O token contém um username, não um ID numérico");
        }

        User solicitante = null;
        if (userId != null) {
            solicitante = userRepository.findById(userId).orElse(null);
        }

        if (solicitante == null) {
            solicitante = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Usuário não encontrado para o token. Username/ID: " + username));
        }

        chamado.setUsuario(solicitante);
        return chamadoRepository.save(chamado);
    }

    public Optional<Chamado> buscarPorId(Long id) {
        return chamadoRepository.findById(id);
    }

    public Chamado atualizarChamado(Long id, Chamado chamadoAtualizado) {
        return chamadoRepository.findById(id)
                .map(chamado -> {
                    chamado.setTitulo(chamadoAtualizado.getTitulo());
                    chamado.setDescricao(chamadoAtualizado.getDescricao());
                    chamado.setStatus(chamadoAtualizado.getStatus());
                    return chamadoRepository.save(chamado);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));
    }

    public void fecharChamado(Long id) {
        chamadoRepository.findById(id)
                .map(chamado -> {
                    chamado.setStatus("FECHADO");
                    chamado.setDataFechamento(LocalDateTime.now());
                    return chamadoRepository.save(chamado);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));
    }

    public Chamado aderirChamado(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User helper = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Helper não encontrado"));

        return chamadoRepository.findById(id)
                .map(chamado -> {
                    // Verificar se o chamado já está em atendimento
                    if (!"ABERTO".equals(chamado.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Chamado não está disponível para atendimento. Status atual: " + chamado.getStatus());
                    }

                    chamado.setHelper(helper);
                    chamado.setStatus("EM_ATENDIMENTO");
                    chamado.setDataInicio(LocalDateTime.now());
                    return chamadoRepository.save(chamado);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));
    }

    public List<Chamado> listarChamadosPorHelper() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User helper = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Helper não encontrado"));

        return chamadoRepository.findByHelper(helper);
    }
}