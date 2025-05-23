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

    @Autowired
    private UserContextService userContextService;

    @Autowired
    private ActivityLogService activityLogService;

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

        // Registrar log de atividade
        try {
            User currentUser = userContextService.getCurrentUser();
            activityLogService.logActivity(currentUser, "USER_CREATED", null,
                    "Criou usuário: " + dto.username() + " com perfil " + roleName);
        } catch (Exception e) {
            // Se não conseguir obter o usuário atual (ex: primeiro usuário sendo criado), registra como sistema
            System.out.println("Usuário criado: " + dto.username() + " (registro de log falhou: " + e.getMessage() + ")");
        }

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

        String deletedUsername = user.getUsername();
        userRepository.delete(user);

        // Registrar log de atividade
        try {
            User currentUser = userContextService.getCurrentUser();
            activityLogService.logActivity(currentUser, "USER_DELETED", null,
                    "Excluiu usuário: " + deletedUsername);
        } catch (Exception e) {
            System.err.println("Erro ao registrar log de exclusão de usuário: " + e.getMessage());
        }
    }

    @Transactional
    public UserResponseDto updateUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        boolean wasEnabled = user.isEnabled();
        user.setEnabled(enabled);
        User savedUser = userRepository.save(user);

        // Registrar log de atividade
        try {
            User currentUser = userContextService.getCurrentUser();
            String action = enabled ? "USER_ENABLED" : "USER_DISABLED";
            String description = (enabled ? "Ativou" : "Desativou") + " usuário: " + user.getUsername();

            activityLogService.logActivity(currentUser, action, null, description);
        } catch (Exception e) {
            System.err.println("Erro ao registrar log de alteração de status: " + e.getMessage());
        }

        return convertToDto(savedUser);
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        StringBuilder changes = new StringBuilder();

        if (dto.name() != null && !dto.name().trim().isEmpty()) {
            String oldName = user.getName();
            user.setName(dto.name());
            if (!dto.name().equals(oldName)) {
                changes.append("Nome: '").append(oldName).append("' → '").append(dto.name()).append("'; ");
            }
        }

        if (dto.role() != null && !dto.role().trim().isEmpty()) {
            boolean roleExists = user.getRoles().stream()
                    .anyMatch(role -> role.getName().equals(dto.role()) || role.getName().equals("ROLE_" + dto.role()));

            if (!roleExists) {
                String oldRoles = user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.joining(", "));

                user.getRoles().clear();

                Role role = roleRepository.findByName(dto.role())
                        .orElseGet(() -> {
                            return roleRepository.findByName("ROLE_" + dto.role())
                                    .orElseThrow(() -> new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, "Papel não encontrado: " + dto.role()));
                        });

                user.getRoles().add(role);
                changes.append("Perfil: '").append(oldRoles).append("' → '").append(dto.role()).append("'; ");
            }
        }

        if (dto.password() != null && !dto.password().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
            changes.append("Senha alterada; ");
        }

        User savedUser = userRepository.save(user);

        // Registrar log de atividade
        try {
            User currentUser = userContextService.getCurrentUser();
            String description = "Atualizou usuário: " + user.getUsername();
            if (changes.length() > 0) {
                description += " - Alterações: " + changes.toString().trim();
            }

            activityLogService.logActivity(currentUser, "USER_UPDATED", null, description);
        } catch (Exception e) {
            System.err.println("Erro ao registrar log de atualização de usuário: " + e.getMessage());
        }

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