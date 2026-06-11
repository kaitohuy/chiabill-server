package com.kaitohuy.chiabill.config;

import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class AdminUserSeeder {

    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdminUser() {
        String adminEmail = "adminchiabill@gmail.com";
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists with email: {}", adminEmail);
            return;
        }

        log.info("Seeding default Admin user with email: {}", adminEmail);
        User admin = User.builder()
                .email(adminEmail)
                .name("DuliVie Admin")
                .role("ADMIN")
                .isAnonymous(false)
                .isGhost(false)
                .provider("GOOGLE")
                .providerId("ADMIN_SEED")
                .build();

        userRepository.save(admin);
    }
}
