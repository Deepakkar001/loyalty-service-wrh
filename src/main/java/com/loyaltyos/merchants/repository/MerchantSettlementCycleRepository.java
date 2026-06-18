package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.MerchantSettlementCycle;
import com.loyaltyos.merchants.enums.MerchantSettlementCycleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantSettlementCycleRepository extends JpaRepository<MerchantSettlementCycle, Long> {

    Optional<MerchantSettlementCycle> findByTenantIdAndCycleUid(String tenantId, String cycleUid);

    List<MerchantSettlementCycle> findByTenantIdAndMerchantUidOrderByPeriodEndDesc(
        String tenantId,
        String merchantUid
    );

    List<MerchantSettlementCycle> findByTenantIdAndStatusOrderByCreatedAtDesc(
        String tenantId,
        MerchantSettlementCycleStatus status
    );

    boolean existsByTenantIdAndMerchantUidAndPeriodStartAndPeriodEnd(
        String tenantId,
        String merchantUid,
        LocalDate periodStart,
        LocalDate periodEnd
    );
}
