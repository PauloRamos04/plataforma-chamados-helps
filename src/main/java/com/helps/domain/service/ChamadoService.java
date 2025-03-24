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
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public Chamado abrirChamado(Chamado chamado) {
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setStatus("ABERTO");

        User solicitante = getCurrentUser();

        chamado.setUsuario(solicitante);

        return chamadoRepository.save(chamado);
    }

    public Optional<Chamado> buscarPorId(Long id) {
        return chamadoRepository.findById(id);
    }

    @Transactional
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

    @Transactional
    public void fecharChamado(Long id) {
        chamadoRepository.findById(id)
                .map(chamado -> {
                    if (!"EM_ATENDIMENTO".equals(chamado.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Apenas chamados em atendimento podem ser fechados. Status atual: " + chamado.getStatus());
                    }

                    chamado.setStatus("FECHADO");
                    chamado.setDataFechamento(LocalDateTime.now());
                    return chamadoRepository.save(chamado);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));
    }

    @Transactional
    public Chamado aderirChamado(Long id) {
        User helper = getCurrentUser();

        boolean isAuthorized = helper.getRoles().stream()
                .anyMatch(role -> role.getName().equals("HELPER") || role.getName().equals("ADMIN") ||
                        role.getName().equals("ROLE_HELPER") || role.getName().equals("ROLE_ADMIN"));

        if (!isAuthorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário não tem permissão para aderir a chamados. Papéis necessários: HELPER ou ADMIN");
        }

        return chamadoRepository.findById(id)
                .map(chamado -> {
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
        User helper = getCurrentUser();

        boolean isAuthorized = helper.getRoles().stream()
                .anyMatch(role -> role.getName().equals("HELPER") || role.getName().equals("ADMIN") ||
                        role.getName().equals("ROLE_HELPER") || role.getName().equals("ROLE_ADMIN"));

        if (!isAuthorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário não tem permissão para listar chamados de helper. Papéis necessários: HELPER ou ADMIN");
        }

        return chamadoRepository.findByHelper(helper);
    }
    public List<Chamado> listarChamadosPorUsuario() {
        User usuario = getCurrentUser();

        return chamadoRepository.findByUsuario(usuario);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        String username = auth.getName();
        System.out.println("Tentando encontrar usuário: " + username);

        Long userId = null;
        try {
            userId = Long.parseLong(username);
            System.out.println("O token contém um ID numérico: " + userId);
        } catch (NumberFormatException e) {
            System.out.println("O token contém um username, não um ID numérico");
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
            System.out.println("Busca por ID " + userId + ": " + (user != null ? "encontrado" : "não encontrado"));
        }

        if (user == null) {
            user = userRepository.findByUsername(username).orElse(null);
            System.out.println("Busca por username " + username + ": " + (user != null ? "encontrado" : "não encontrado"));
        }

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Usuário não encontrado para o token. Username/ID: " + username);
        }

        return user;
    }
}