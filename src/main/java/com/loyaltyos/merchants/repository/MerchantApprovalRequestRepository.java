package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.MerchantApprovalRequest;
import com.loyaltyos.merchants.enums.MerchantApprovalRequestType;
import com.loyaltyos.merchants.enums.MerchantApprovalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantApprovalRequestRepository extends JpaRepository<MerchantApprovalRequest, Long> {

    Optional<MerchantApprovalRequest> findByRequestUid(String requestUid);

    List<MerchantApprovalRequest> findByTenantIdAndStatusOrderByRequestedAtDesc(
        String tenantId,
        MerchantApprovalStatus status
    );

    List<MerchantApprovalRequest> findByTenantIdAndMerchantUidAndRequestTypeAndStatus(
        String tenantId,
        String merchantUid,
        MerchantApprovalRequestType requestType,
        MerchantApprovalStatus status
    );
}
