package com.helps.dto;

import org.springframework.web.multipart.MultipartFile;

public record ChamadoDto(
        String titulo,
        String descricao,
        String categoria,
        String tipo,
        MultipartFile image
) {}