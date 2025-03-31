package com.helps.controller;

import com.helps.domain.service.RoleService;
import com.helps.dto.RoleDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Gerenciamento de Perfis/Roles", description = "APIs para gerenciar perfis de usuários")
public class RoleController {
    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleService roleService;

    @GetMapping
    @Operation(summary = "Listar todos os perfis")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        logger.debug("Listando todos os perfis");
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar perfil por ID")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id) {
        logger.debug("Buscando perfil com ID: {}", id);
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Criar novo perfil")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleDto roleDto) {
        logger.info("Criando novo perfil: {}", roleDto.name());
        RoleDto createdRole = roleService.createRole(roleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualizar perfil existente")
    public ResponseEntity<RoleDto> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleDto roleDto) {
        logger.info("Atualizando perfil com ID: {}", id);
        RoleDto updatedRole = roleService.updateRole(id, roleDto);
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Excluir perfil")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        logger.info("Excluindo perfil com ID: {}", id);
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}