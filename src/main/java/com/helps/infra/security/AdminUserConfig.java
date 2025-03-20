package com.helps.infra.security;

import com.helps.domain.model.Role;
import com.helps.domain.model.User;
import com.helps.domain.repository.RoleRepository;
import com.helps.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

@Configuration
public class AdminUserConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminUserConfig(RoleRepository roleRepository,
                           UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Criar todas as roles necessárias
        for (Role.Values roleValue : Role.Values.values()) {
            roleRepository.findByName(roleValue.name()).orElseGet(() -> {
                Role role = new Role();
                role.setName(roleValue.name());
                roleRepository.save(role);
                System.out.println("Role " + roleValue.name() + " criada.");
                return role;
            });
        }

        // Criar usuário admin se não existir
        userRepository.findByUsername("admin").ifPresentOrElse(
                user -> System.out.println("Admin já existe."),
                () -> {
                    var roleAdmin = roleRepository.findByName(Role.Values.ADMIN.name()).orElseThrow();
                    var user = new User();
                    user.setUsername("admin");
                    user.setPassword(passwordEncoder.encode("123"));
                    user.setRoles(Set.of(roleAdmin));
                    userRepository.save(user);
                    System.out.println("Admin criado com sucesso.");
                }
        );
    }
}
