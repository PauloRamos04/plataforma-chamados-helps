package com.helps.dto;

import org.springframework.web.multipart.MultipartFile;

public record MensagemDto(String conteudo, MultipartFile image) {
    public MensagemDto(String conteudo) {
        this(conteudo, null);
    }
}