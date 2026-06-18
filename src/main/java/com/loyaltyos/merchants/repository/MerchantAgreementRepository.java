package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.MerchantAgreement;
import com.loyaltyos.merchants.enums.MerchantAgreementStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantAgreementRepository extends JpaRepository<MerchantAgreement, Long> {

    Optional<MerchantAgreement> findTopByTenantIdAndMerchantUidAndStatusOrderByCreatedAtDesc(
        String tenantId,
        String merchantUid,
        MerchantAgreementStatus status
    );

    Optional<MerchantAgreement> findByTenantIdAndAgreementUid(String tenantId, String agreementUid);

    List<MerchantAgreement> findByTenantIdAndStatusOrderByCreatedAtDesc(
        String tenantId,
        MerchantAgreementStatus status
    );
}
