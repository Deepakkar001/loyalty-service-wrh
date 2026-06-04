package com.loyaltyos.referrals.repository;

import com.loyaltyos.referrals.entity.ReferralAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralAuditLogRepository extends JpaRepository<ReferralAuditLog, Long> {}

