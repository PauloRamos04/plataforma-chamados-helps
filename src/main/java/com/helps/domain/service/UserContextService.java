package com.helps.domain.service;

import com.helps.domain.model.Role;
import com.helps.domain.model.User;
import com.helps.domain.repository.RoleRepository;
import com.helps.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserContextService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        String identifier = auth.getName();
        User user = null;

        Optional<User> userByUsername = userRepository.findByUsername(identifier);
        if (userByUsername.isPresent()) {
            return userByUsername.get();
        }

        try {
            Long userId = Long.parseLong(identifier);
            Optional<User> userById = userRepository.findById(userId);
            if (userById.isPresent()) {
                return userById.get();
            }
        } catch (NumberFormatException ignored) {}

        if (auth.getPrincipal() instanceof Jwt) {
            try {
                Jwt jwt = (Jwt) auth.getPrincipal();
                String subject = jwt.getClaimAsString("sub");

                if (subject != null) {
                    userByUsername = userRepository.findByUsername(subject);
                    if (userByUsername.isPresent()) {
                        return userByUsername.get();
                    }

                    try {
                        Long subjectId = Long.parseLong(subject);
                        Optional<User> userBySubjectId = userRepository.findById(subjectId);
                        if (userBySubjectId.isPresent()) {
                            return userBySubjectId.get();
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } catch (Exception ignored) {}
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Usuário não encontrado para o identificador: " + identifier);
    }

    public boolean hasRole(String role) {
        User user = getCurrentUser();
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(role));
    }

    public boolean hasAnyRole(String... roles) {
        User user = getCurrentUser();
        return user.getRoles().stream()
                .anyMatch(r -> {
                    String roleName = r.getName();
                    for (String role : roles) {
                        if (roleName.equals(role) || roleName.equals(role)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    public List<User> findUsersWithRole(String roleName) {
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (!roleOpt.isPresent()) {
            roleOpt = roleRepository.findByName(roleName);
        }

        if (!roleOpt.isPresent()) {
            return Collections.emptyList();
        }

        Role role = roleOpt.get();
        List<User> users = userRepository.findAll().stream()
                .filter(user -> {
                    boolean hasRole = user.isEnabled() &&
                            user.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId()));
                    System.out.println("Usuário: " + user.getUsername() + ", Tem papel: " + hasRole);
                    return hasRole;
                })
                .collect(Collectors.toList());

        System.out.println("Total de usuários encontrados: " + users.size());
        return users;
    }
}