package com.helps.domain.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.User;
import com.helps.domain.repository.ChamadoRepository;
import com.helps.dto.ChamadoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private UserContextService userContextService;

    @Autowired
    private ChamadoAccessService chamadoAccessService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Chamado> listarChamados() {
        User currentUser = userContextService.getCurrentUser();

        if (userContextService.hasRole("ADMIN")) {
            return chamadoRepository.findAll();
        } else if (userContextService.hasRole("HELPER")) {
            return chamadoRepository.findByHelperOrStatus(currentUser, "ABERTO");
        } else {
            return chamadoRepository.findByUsuario(currentUser);
        }
    }

    public List<Chamado> listarChamadosPorStatus(String status) {
        return chamadoRepository.findByStatus(status);
    }

    @Transactional
    public Chamado abrirChamado(Chamado chamado) {
        User solicitante = userContextService.getCurrentUser();

        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setStatus("ABERTO");
        chamado.setUsuario(solicitante);

        Chamado chamadoSalvo = chamadoRepository.save(chamado);

        try {
            notificationService.notificarNovosChamados(chamadoSalvo);
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificações para o novo chamado: " + e.getMessage());
            e.printStackTrace();
        }

        return chamadoSalvo;
    }

    @Transactional
    public Chamado abrirChamado(ChamadoDto chamadoDto) {
        User solicitante = userContextService.getCurrentUser();

        Chamado chamado = new Chamado();
        chamado.setTitulo(chamadoDto.titulo());
        chamado.setDescricao(chamadoDto.descricao());
        chamado.setCategoria(chamadoDto.categoria());
        chamado.setTipo(chamadoDto.tipo());
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setStatus("ABERTO");
        chamado.setUsuario(solicitante);

        if (chamadoDto.image() != null && !chamadoDto.image().isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(chamadoDto.image());
                chamado.setImagePath(fileName);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Could not process the image: " + e.getMessage());
            }
        }

        Chamado chamadoSalvo = chamadoRepository.save(chamado);

        try {
            notificationService.notificarNovosChamados(chamadoSalvo);
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificações para o novo chamado: " + e.getMessage());
            e.printStackTrace();
        }

        return chamadoSalvo;
    }

    @Transactional
    public Chamado atualizarChamado(Long id, Chamado chamadoAtualizado) {
        return chamadoRepository.findById(id)
                .map(chamado -> {
                    if (!chamadoAccessService.podeAcessarChamado(chamado)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Você não tem permissão para atualizar este chamado");
                    }

                    chamado.setTitulo(chamadoAtualizado.getTitulo());
                    chamado.setDescricao(chamadoAtualizado.getDescricao());

                    if (userContextService.hasRole("ADMIN") && chamadoAtualizado.getStatus() != null) {
                        String statusAnterior = chamado.getStatus();
                        chamado.setStatus(chamadoAtualizado.getStatus());

                        if (!statusAnterior.equals(chamadoAtualizado.getStatus())) {
                            webSocketService.notificarStatusChamado(chamado,
                                    "Status alterado de " + statusAnterior + " para " + chamado.getStatus());
                        }
                    }

                    return chamadoRepository.save(chamado);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));
    }

    @Transactional
    public void fecharChamado(Long id) {
        chamadoRepository.findById(id)
                .map(chamado -> {
                    chamadoAccessService.verificarPermissaoFecharChamado(chamado);

                    chamado.setStatus("FECHADO");
                    chamado.setDataFechamento(LocalDateTime.now());

                    Chamado chamadoFechado = chamadoRepository.save(chamado);

                    webSocketService.notificarStatusChamado(chamadoFechado,
                            "Chamado finalizado por " + userContextService.getCurrentUser().getName());

                    if (chamado.getUsuario() != null) {
                        notificationService.criarNotificacaoParaUsuario(
                                chamado.getUsuario().getId(),
                                "Seu chamado \"" + chamado.getTitulo() + "\" foi finalizado",
                                "CHAMADO_FECHADO",
                                chamado.getId());
                    }

                    return chamadoFechado;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));
    }

    @Transactional
    public Chamado aderirChamado(Long id) {
        User helper = userContextService.getCurrentUser();
        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));

        chamadoAccessService.verificarPermissaoAderirChamado(chamado);

        chamado.setHelper(helper);
        chamado.setStatus("EM_ATENDIMENTO");
        chamado.setDataInicio(LocalDateTime.now());

        Chamado chamadoAtualizado = chamadoRepository.save(chamado);

        webSocketService.notificarStatusChamado(chamadoAtualizado,
                helper.getName() + " começou a atender este chamado");

        if (chamado.getUsuario() != null) {
            notificationService.criarNotificacaoParaUsuario(
                    chamado.getUsuario().getId(),
                    "Seu chamado \"" + chamado.getTitulo() + "\" começou a ser atendido por " + helper.getName(),
                    "CHAMADO_EM_ATENDIMENTO",
                    chamado.getId());
        }

        return chamadoAtualizado;
    }

    public List<Chamado> listarChamadosPorHelper() {
        User helper = userContextService.getCurrentUser();

        if (!userContextService.hasAnyRole("HELPER", "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário não tem permissão para listar chamados de helper");
        }

        return chamadoRepository.findByHelper(helper);
    }

    public List<Chamado> listarChamadosPorUsuario() {
        User usuario = userContextService.getCurrentUser();
        return chamadoRepository.findByUsuario(usuario);
    }

    public Optional<Chamado> buscarPorId(Long id) {
        Optional<Chamado> chamadoOpt = chamadoRepository.findById(id);

        chamadoOpt.ifPresent(chamado -> {
            if (!chamadoAccessService.podeAcessarChamado(chamado)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem permissão para acessar este chamado");
            }
        });

        return chamadoOpt;
    }
}