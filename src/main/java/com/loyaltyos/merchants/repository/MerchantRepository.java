package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByTenantIdAndMerchantUid(String tenantId, String merchantUid);

    Page<Merchant> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<Merchant> findByTenantIdAndOnboardingStageOrderByCreatedAtDesc(
        String tenantId,
        MerchantOnboardingStage stage,
        Pageable pageable
    );

    List<Merchant> findByTenantIdAndOnboardingStage(String tenantId, MerchantOnboardingStage stage);

    boolean existsByTenantIdAndMerchantUid(String tenantId, String merchantUid);
}
