package com.loyaltyos.voucher.repository;

import com.loyaltyos.voucher.entity.VoucherRedemptionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRedemptionAuditRepository extends JpaRepository<VoucherRedemptionAudit, Long> {
}
