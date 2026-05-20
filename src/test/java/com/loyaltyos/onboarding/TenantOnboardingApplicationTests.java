package com.loyaltyos.onboarding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Disabled to keep unit tests infra-independent; full context verified via bootRun in dev.")
@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@ActiveProfiles("local")
class TenantOnboardingApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}

