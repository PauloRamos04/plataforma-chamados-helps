package com.helps.infra.config;

import com.helps.domain.model.Role;
import com.helps.domain.model.User;
import com.helps.domain.repository.RoleRepository;
import com.helps.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public DatabaseInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
        initializeAdminUser();
    }

    private void initializeRoles() {
        for (Role.Values roleValue : Role.Values.values()) {
            String roleName = roleValue.name();
            roleRepository.findByName(roleName).orElseGet(() -> {
                Role role = new Role();
                role.setName(roleName);
                role = roleRepository.save(role);
                System.out.println("Role " + roleName + " criada com sucesso.");
                return role;
            });
        }
    }

    private void initializeAdminUser() {
        userRepository.findByUsername("admin").ifPresentOrElse(
                user -> System.out.println("Usuário admin já existe."),
                () -> {
                    initializeRoles();

                    Role adminRole = roleRepository.findByName(Role.Values.ADMIN.name())
                            .orElseGet(() -> {
                                Role role = new Role();
                                role.setName(Role.Values.ADMIN.name());
                                return roleRepository.save(role);
                            });

                    User adminUser = new User();
                    adminUser.setUsername("admin");
                    adminUser.setPassword(passwordEncoder.encode("123"));
                    adminUser.setName("Administrador");
                    adminUser.setEnabled(true);
                    adminUser.setRoles(Set.of(adminRole));

                    userRepository.save(adminUser);
                    System.out.println("Usuário admin criado com sucesso.");
                }
        );
    }
}