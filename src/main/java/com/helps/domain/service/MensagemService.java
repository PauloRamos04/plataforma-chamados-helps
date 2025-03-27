package com.helps.domain.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.Mensagem;
import com.helps.domain.model.User;
import com.helps.domain.repository.ChamadoRepository;
import com.helps.domain.repository.MensagemRepository;
import com.helps.dto.MensagemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private UserContextService userContextService;

    @Autowired
    private ChamadoAccessService chamadoAccessService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Mensagem> listarMensagensPorChamado(Long chamadoId) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));

        if (!chamadoAccessService.podeAcessarChamado(chamado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para acessar as mensagens deste chamado");
        }

        return mensagemRepository.findByChamadoOrderByDataEnvioAsc(chamado);
    }

    @Transactional
    public Mensagem enviarMensagem(Long chamadoId, MensagemDto mensagemDTO) {
        User remetente = userContextService.getCurrentUser();

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));

        chamadoAccessService.verificarPermissaoEnviarMensagem(chamado);

        Mensagem mensagem = new Mensagem();
        mensagem.setChamado(chamado);
        mensagem.setRemetente(remetente);
        mensagem.setConteudo(mensagemDTO.conteudo());
        mensagem.setDataEnvio(LocalDateTime.now());

        if (mensagemDTO.image() != null && !mensagemDTO.image().isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(mensagemDTO.image());
                mensagem.setImagePath(fileName);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Could not process the image: " + e.getMessage());
            }
        }

        Mensagem mensagemSalva = mensagemRepository.save(mensagem);

        webSocketService.enviarMensagemChat(mensagemSalva);

        String conteudoResumido = resumirConteudo(mensagemDTO.conteudo(), 50);
        notificationService.notificarMensagemRecebida(chamadoId, remetente.getId(), conteudoResumido);

        return mensagemSalva;
    }

    private String resumirConteudo(String conteudo, int maxLength) {
        if (conteudo == null) return "";
        if (conteudo.length() <= maxLength) return conteudo;

        return conteudo.substring(0, maxLength - 3) + "...";
    }
}