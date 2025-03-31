package com.helps.domain.service;

import com.helps.domain.model.Role;
import com.helps.domain.repository.RoleRepository;
import com.helps.dto.RoleDto;
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
public class RoleService {
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    @Autowired
    private RoleRepository roleRepository;

    public List<RoleDto> getAllRoles() {
        logger.debug("Buscando todas as roles");
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RoleDto getRoleById(Long id) {
        logger.debug("Buscando role por ID: {}", id);
        return roleRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Role não encontrada com ID: " + id));
    }

    @Transactional
    public RoleDto createRole(RoleDto roleDto) {
        if (roleRepository.findByName(roleDto.name()).isPresent()) {
            logger.warn("Tentativa de criar role com nome já existente: {}", roleDto.name());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Já existe uma role com este nome");
        }

        Role role = new Role();
        role.setName(roleDto.name());

        Role savedRole = roleRepository.save(role);
        logger.info("Role criada com sucesso: {}", savedRole.getName());
        return toDto(savedRole);
    }

    @Transactional
    public RoleDto updateRole(Long id, RoleDto roleDto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Role não encontrada com ID: " + id));

        roleRepository.findByName(roleDto.name())
                .ifPresent(existingRole -> {
                    if (!existingRole.getId().equals(id)) {
                        logger.warn("Tentativa de atualizar role para um nome já em uso: {}", roleDto.name());
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Já existe uma role com este nome");
                    }
                });

        role.setName(roleDto.name());

        Role updatedRole = roleRepository.save(role);
        logger.info("Role atualizada com sucesso: {}", updatedRole.getName());
        return toDto(updatedRole);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Role não encontrada com ID: " + id));

        if (isSystemRole(role.getName())) {
            logger.warn("Tentativa de excluir role do sistema: {}", role.getName());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Não é possível excluir roles do sistema");
        }

        roleRepository.delete(role);
        logger.info("Role excluída com sucesso: {}", role.getName());
    }

    private boolean isSystemRole(String roleName) {
        return "ADMIN".equals(roleName) ||
                "HELPER".equals(roleName) ||
                "USUARIO".equals(roleName) ||
                "OPERADOR".equals(roleName);
    }

    private RoleDto toDto(Role role) {
        return new RoleDto(
                role.getId(),
                role.getName(),
                null,
                true
        );
    }
}