package com.helps.domain.service;

import com.helps.domain.model.Role;
import com.helps.domain.model.User;
import com.helps.domain.repository.RoleRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.CreateUserDto;
import com.helps.dto.UserResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto createUser(CreateUserDto dto, String roleName) {
        // Verificar se o usuário já existe
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de usuário já existe");
        }

        // Buscar a role pelo nome SEM adicionar o prefixo "ROLE_"
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Role não encontrada: " + roleName));

        // Criar o novo usuário
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setName(dto.name());
        user.setEnabled(true);
        user.setRoles(Collections.singleton(role));

        User savedUser = userRepository.save(user);

        return convertToDto(savedUser);
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<UserResponseDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        userRepository.delete(user);
    }

    @Transactional
    public UserResponseDto updateUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setEnabled(enabled);
        return convertToDto(userRepository.save(user));
    }

    private UserResponseDto convertToDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList())
        );
    }
}