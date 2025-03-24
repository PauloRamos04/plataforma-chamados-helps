package com.helps.domain.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChamadoAccessService {

    @Autowired
    private UserContextService userContextService;

    public boolean podeAcessarChamado(Chamado chamado) {
        User currentUser = userContextService.getCurrentUser();

        if (userContextService.hasRole("ADMIN")) {
            return true;
        }

        if (chamado.getUsuario() != null &&
                chamado.getUsuario().getId().equals(currentUser.getId())) {
            return true;
        }

        if (chamado.getHelper() != null &&
                chamado.getHelper().getId().equals(currentUser.getId())) {
            return true;
        }

        if ("ABERTO".equals(chamado.getStatus()) &&
                userContextService.hasRole("HELPER")) {
            return true;
        }

        return false;
    }

    public void verificarPermissaoAderirChamado(Chamado chamado) {
        if (!userContextService.hasAnyRole("HELPER", "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário não tem permissão para aderir a chamados. Papéis necessários: HELPER ou ADMIN");
        }

        if (!"ABERTO".equals(chamado.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chamado não está disponível para atendimento. Status atual: " + chamado.getStatus());
        }
    }

    public void verificarPermissaoFecharChamado(Chamado chamado) {
        User currentUser = userContextService.getCurrentUser();

        if (userContextService.hasRole("ADMIN")) {
            return;
        }

        if (chamado.getHelper() == null ||
                !chamado.getHelper().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas o helper designado pode fechar este chamado");
        }

        if (!"EM_ATENDIMENTO".equals(chamado.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Apenas chamados em atendimento podem ser fechados. Status atual: " + chamado.getStatus());
        }
    }

    public void verificarPermissaoEnviarMensagem(Chamado chamado) {
        User currentUser = userContextService.getCurrentUser();

        if (userContextService.hasRole("ADMIN")) {
            return;
        }

        boolean isSolicitante = chamado.getUsuario() != null &&
                currentUser.getId().equals(chamado.getUsuario().getId());

        boolean isHelper = chamado.getHelper() != null &&
                currentUser.getId().equals(chamado.getHelper().getId());

        if (!isSolicitante && !isHelper) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para enviar mensagens neste chamado");
        }

        if (!"EM_ATENDIMENTO".equals(chamado.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível enviar mensagens para um chamado que não está em atendimento");
        }
    }
}