package com.loyaltyos.rewards.repository;

import com.loyaltyos.rewards.entity.BalanceReconciliationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceReconciliationLogRepository extends JpaRepository<BalanceReconciliationLog, Long> {
}
