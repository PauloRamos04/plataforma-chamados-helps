package com.helps.controller;

import com.helps.domain.model.Role;
import com.helps.domain.service.UserService;
import com.helps.dto.CreateUserDto;
import com.helps.dto.CreateUserWithRoleDto;
import com.helps.dto.UpdateUserStatusDto;
import com.helps.dto.UserResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    // Endpoint para criar um operador (acesso público para registro)
    @PostMapping("/users")
    public ResponseEntity<UserResponseDto> newUser(@RequestBody CreateUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(dto, Role.Values.OPERADOR.name()));
    }

    // Endpoint para criar um helper (acesso público para registro)
    @PostMapping("/register/helper")
    public ResponseEntity<UserResponseDto> newHelper(@RequestBody CreateUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(dto, Role.Values.HELPER.name()));
    }

    // Endpoint para criar um admin (somente admins podem criar outros admins)
    @PostMapping("/register/admin")
    public ResponseEntity<UserResponseDto> newAdmin(@RequestBody CreateUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(dto, Role.Values.ADMIN.name()));
    }

    @PostMapping("/admin/users")
    public ResponseEntity<UserResponseDto> createUserWithRole(@RequestBody CreateUserWithRoleDto dto) {
        System.out.println("Recebido DTO: " + dto);
        try {
            CreateUserDto userDto = new CreateUserDto(dto.username(), dto.password(), dto.name());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(userService.createUser(userDto, dto.role()));
        } catch (Exception e) {
            System.err.println("Erro ao criar usuário: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Listar todos os usuários (somente admins)
    @GetMapping("/admin/users")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Obter usuário pelo ID (somente admins)
    @GetMapping("/admin/users/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Deletar usuário (somente admins)
    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Atualizar status do usuário (ativar/desativar) (somente admins)
    @PatchMapping("/admin/users/{id}/status")
    public ResponseEntity<UserResponseDto> updateUserStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusDto dto) {
        return ResponseEntity.ok(userService.updateUserStatus(id, dto.enabled()));
    }
}