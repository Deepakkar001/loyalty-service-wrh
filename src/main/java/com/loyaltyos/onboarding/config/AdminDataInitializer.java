package com.loyaltyos.onboarding.config;

import com.loyaltyos.onboarding.domain.entity.AdminUser;
import com.loyaltyos.onboarding.domain.enums.AdminRole;
import com.loyaltyos.onboarding.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminUserRepository.existsByEmail("admin@loyaltyos.com")) {
            return;
        }

        AdminUser admin = AdminUser.builder()
                .adminUid("a0000000-0000-0000-0000-000000000001")
                .email("admin@loyaltyos.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Platform Admin")
                .role(AdminRole.PLATFORM_ADMIN)
                .build();

        adminUserRepository.save(admin);
        log.info("Seeded local dev admin: admin@loyaltyos.com / admin123");
    }
}
