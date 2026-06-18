package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.MerchantCredentials;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantCredentialsRepository extends JpaRepository<MerchantCredentials, Long> {

    Optional<MerchantCredentials> findByUsernameAndTenantId(String username, String tenantId);

    List<MerchantCredentials> findByUsernameIgnoreCaseAndActiveTrue(String username);

    Optional<MerchantCredentials> findByTenantIdAndMerchantUid(String tenantId, String merchantUid);

    Optional<MerchantCredentials> findByInviteTokenHashAndActiveTrue(String inviteTokenHash);

    boolean existsByTenantIdAndUsernameIgnoreCase(String tenantId, String username);

    boolean existsByTenantIdAndUsernameIgnoreCaseAndMerchantUidNot(
        String tenantId,
        String username,
        String merchantUid
    );
}
