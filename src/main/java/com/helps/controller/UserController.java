package com.helps.controller;

import com.helps.domain.model.Role;
import com.helps.domain.service.UserService;
import com.helps.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponseDto>> newUser(@Valid @RequestBody CreateUserDto dto) {
        UserResponseDto user = userService.createUser(dto, Role.Values.OPERADOR.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "Usuário criado com sucesso"));
    }

    @PostMapping("/register/helper")
    public ResponseEntity<ApiResponse<UserResponseDto>> newHelper(@Valid @RequestBody CreateUserDto dto) {
        UserResponseDto user = userService.createUser(dto, Role.Values.HELPER.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "Helper criado com sucesso"));
    }

    @PostMapping("/register/admin")
    public ResponseEntity<ApiResponse<UserResponseDto>> newAdmin(@Valid @RequestBody CreateUserDto dto) {
        UserResponseDto user = userService.createUser(dto, Role.Values.ADMIN.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "Administrador criado com sucesso"));
    }

    @PostMapping("/admin/users")
    public ResponseEntity<ApiResponse<UserResponseDto>> createUserWithRole(@Valid @RequestBody CreateUserWithRoleDto dto) {
        try {
            CreateUserDto userDto = new CreateUserDto(dto.username(), dto.password(), dto.name());
            UserResponseDto user = userService.createUser(userDto, dto.role());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(user, "Usuário criado com sucesso"));
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Usuário deletado com sucesso"));
    }

    @PutMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDto dto) {
        try {
            UserResponseDto updatedUser = userService.updateUser(id, dto);
            return ResponseEntity.ok(ApiResponse.success(updatedUser, "Usuário atualizado com sucesso"));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro interno ao atualizar usuário: " + e.getMessage());
        }
    }

    @PatchMapping("/admin/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusDto dto) {
        UserResponseDto user = userService.updateUserStatus(id, dto.enabled());
        return ResponseEntity.ok(ApiResponse.success(user, "Status do usuário atualizado com sucesso"));
    }
}