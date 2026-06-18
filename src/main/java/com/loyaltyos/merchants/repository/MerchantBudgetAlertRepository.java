package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.MerchantBudgetAlert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantBudgetAlertRepository extends JpaRepository<MerchantBudgetAlert, Long> {

    List<MerchantBudgetAlert> findByTenantIdAndMerchantUidOrderByNotifiedAtDesc(
        String tenantId,
        String merchantUid
    );
}
