package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByEmail(String email);
    Optional<AdminUser> findByAdminUid(String adminUid);
    boolean existsByEmail(String email);
}
