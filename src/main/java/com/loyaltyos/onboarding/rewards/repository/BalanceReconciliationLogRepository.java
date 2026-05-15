package com.loyaltyos.onboarding.rewards.repository;

import com.loyaltyos.onboarding.rewards.entity.BalanceReconciliationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceReconciliationLogRepository extends JpaRepository<BalanceReconciliationLog, Long> {
}
