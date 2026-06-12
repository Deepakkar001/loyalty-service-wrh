package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.MerchantApiKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, Long> {

    List<MerchantApiKey> findByTenantIdAndMerchantUidOrderByCreatedAtDesc(String tenantId, String merchantUid);
}
