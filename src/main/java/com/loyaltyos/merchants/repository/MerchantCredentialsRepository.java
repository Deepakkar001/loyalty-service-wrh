package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.MerchantCredentials;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantCredentialsRepository extends JpaRepository<MerchantCredentials, Long> {

    Optional<MerchantCredentials> findByUsernameAndTenantId(String username, String tenantId);

    Optional<MerchantCredentials> findByTenantIdAndMerchantUid(String tenantId, String merchantUid);
}
