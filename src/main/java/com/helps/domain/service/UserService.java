package com.helps.domain.service;

import com.helps.domain.model.Role;
import com.helps.domain.model.User;
import com.helps.domain.repository.RoleRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.CreateUserDto;
import com.helps.dto.UpdateUserDto;
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
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de usuário já existe");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Role não encontrada: " + roleName));

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

    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (dto.name() != null && !dto.name().trim().isEmpty()) {
            user.setName(dto.name());
        }

        if (dto.role() != null && !dto.role().trim().isEmpty()) {
            boolean roleExists = user.getRoles().stream()
                    .anyMatch(role -> role.getName().equals(dto.role()) || role.getName().equals("ROLE_" + dto.role()));

            if (!roleExists) {
                user.getRoles().clear();

                Role role = roleRepository.findByName(dto.role())
                        .orElseGet(() -> {
                            return roleRepository.findByName("ROLE_" + dto.role())
                                    .orElseThrow(() -> new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, "Papel não encontrado: " + dto.role()));
                        });

                user.getRoles().add(role);
            }
        }

        if (dto.password() != null && !dto.password().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
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