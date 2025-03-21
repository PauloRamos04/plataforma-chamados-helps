package com.helps.controller;

import com.helps.domain.model.Role;
import com.helps.domain.service.UserService;
import com.helps.dto.CreateUserDto;
import com.helps.dto.CreateUserWithRoleDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/users")
    public ResponseEntity<Void> newUser(@RequestBody CreateUserDto dto) {
        userService.createUser(dto, Role.Values.OPERADOR.name());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/helper")
    public ResponseEntity<Void> newHelper(@RequestBody CreateUserDto dto) {
        userService.createUser(dto, Role.Values.HELPER.name());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> newAdmin(@RequestBody CreateUserDto dto) {
        userService.createUser(dto, Role.Values.ADMIN.name());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> createUserWithRole(@RequestBody CreateUserWithRoleDto dto) {
        userService.createUser(new CreateUserDto(dto.username(), dto.password()), dto.role());
        return ResponseEntity.ok().build();
    }
}