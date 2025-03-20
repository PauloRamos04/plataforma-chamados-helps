package com.helps.infra.security;

import com.helps.domain.model.Role;
import com.helps.domain.model.User;
import com.helps.domain.model.repository.RoleRepository;
import com.helps.domain.model.repository.UserRepository;
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
    public void run(String... args) throws Exception {
        // Busca a role ADMIN no banco de dados
        var roleAdmin = roleRepository.findByName(Role.Values.ADMIN.name());

        // Se a role ADMIN não existir, cria uma nova
        if (roleAdmin == null) {
            roleAdmin = new Role();
            roleAdmin.setName(Role.Values.ADMIN.name());
            roleRepository.save(roleAdmin);
            System.out.println("Role ADMIN criada com sucesso.");
        }

        // Busca o usuário admin no banco de dados
        var userAdmin = userRepository.findByUsername("admin");

        Role finalRoleAdmin = roleAdmin;
        userAdmin.ifPresentOrElse(
                user -> {
                    System.out.println("Admin já existe.");
                },
                () -> {
                    // Cria um novo usuário admin
                    var user = new User();
                    user.setUsername("admin");
                    user.setPassword(passwordEncoder.encode("123"));
                    user.setRoles(Set.of(finalRoleAdmin)); // Garantimos que roleAdmin não é null
                    userRepository.save(user);
                    System.out.println("Admin criado com sucesso.");
                }
        );
    }
}