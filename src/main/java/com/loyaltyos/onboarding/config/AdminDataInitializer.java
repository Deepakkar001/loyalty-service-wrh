package com.loyaltyos.onboarding.config;

import com.loyaltyos.onboarding.domain.entity.AdminUser;
import com.loyaltyos.onboarding.domain.enums.AdminRole;
import com.loyaltyos.onboarding.repository.AdminUserRepository;
import java.util.Objects;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataInitializer(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = Objects.requireNonNull(adminUserRepository, "adminUserRepository");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
    }

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

        adminUserRepository.save(Objects.requireNonNull(admin, "admin"));
        log.info("Seeded local dev admin user: admin@loyaltyos.com");
    }
}
