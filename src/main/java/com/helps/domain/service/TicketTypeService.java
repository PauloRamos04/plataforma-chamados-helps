package com.helps.domain.service;

import com.helps.domain.model.TicketType;
import com.helps.domain.repository.TicketTypeRepository;
import com.helps.dto.TicketTypeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketTypeService {
    private static final Logger logger = LoggerFactory.getLogger(TicketTypeService.class);

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    public List<TicketTypeDto> getAllTicketTypes() {
        logger.debug("Buscando todos os tipos de tickets");
        List<TicketType> types = ticketTypeRepository.findAll();
        return types.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<TicketTypeDto> getActiveTicketTypes() {
        logger.debug("Buscando tipos de tickets ativos");
        List<TicketType> types = ticketTypeRepository.findByActiveTrue();
        return types.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public TicketTypeDto getTicketTypeById(Long id) {
        logger.debug("Buscando tipo de ticket por ID: {}", id);
        return ticketTypeRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tipo de ticket não encontrado com ID: " + id));
    }

    @Transactional
    public TicketTypeDto createTicketType(TicketTypeDto typeDto) {
        if (ticketTypeRepository.findByName(typeDto.name()).isPresent()) {
            logger.warn("Tentativa de criar tipo de ticket com nome já existente: {}", typeDto.name());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Já existe um tipo de ticket com este nome");
        }

        TicketType type = new TicketType();
        type.setName(typeDto.name());
        type.setDescription(typeDto.description());
        type.setActive(typeDto.active());
        type.setPriorityLevel(typeDto.priorityLevel());
        type.setSlaMinutes(typeDto.slaMinutes());

        TicketType savedType = ticketTypeRepository.save(type);
        logger.info("Tipo de ticket criado com sucesso: {}", savedType.getName());
        return toDto(savedType);
    }

    @Transactional
    public TicketTypeDto updateTicketType(Long id, TicketTypeDto typeDto) {
        TicketType type = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tipo de ticket não encontrado com ID: " + id));

        ticketTypeRepository.findByName(typeDto.name())
                .ifPresent(existingType -> {
                    if (!existingType.getId().equals(id)) {
                        logger.warn("Tentativa de atualizar tipo de ticket para um nome já em uso: {}", typeDto.name());
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Já existe um tipo de ticket com este nome");
                    }
                });

        type.setName(typeDto.name());
        type.setDescription(typeDto.description());
        type.setActive(typeDto.active());
        type.setPriorityLevel(typeDto.priorityLevel());
        type.setSlaMinutes(typeDto.slaMinutes());

        TicketType updatedType = ticketTypeRepository.save(type);
        logger.info("Tipo de ticket atualizado com sucesso: {}", updatedType.getName());
        return toDto(updatedType);
    }

    @Transactional
    public void deleteTicketType(Long id) {
        TicketType type = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tipo de ticket não encontrado com ID: " + id));

        ticketTypeRepository.delete(type);
        logger.info("Tipo de ticket excluído com sucesso: {}", type.getName());
    }

    @Transactional
    public TicketTypeDto toggleTicketTypeActive(Long id) {
        TicketType type = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tipo de ticket não encontrado com ID: " + id));

        type.setActive(!type.isActive());
        TicketType updatedType = ticketTypeRepository.save(type);

        String status = type.isActive() ? "ativado" : "desativado";
        logger.info("Tipo de ticket {} com sucesso: {}", status, type.getName());

        return toDto(updatedType);
    }

    private TicketTypeDto toDto(TicketType type) {
        return new TicketTypeDto(
                type.getId(),
                type.getName(),
                type.getDescription(),
                type.isActive(),
                type.getPriorityLevel(),
                type.getSlaMinutes()
        );
    }
}