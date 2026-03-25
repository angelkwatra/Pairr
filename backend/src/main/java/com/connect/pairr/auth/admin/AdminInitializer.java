package com.connect.pairr.auth.admin;

import com.connect.pairr.model.entity.User;
import com.connect.pairr.model.enums.Role;
import com.connect.pairr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.display-name}")
    private String adminDisplayName;

    @Value("${admin.password}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void createAdmin() {

        boolean adminExists = userRepository.existsByRole(Role.ADMIN);

        if (!adminExists) {

            User admin = User.builder()
                    .email(adminEmail)
                    .username("admin")
                    .displayName(adminDisplayName)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("Default admin created.");
        }
    }
}
