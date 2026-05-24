package com.loyaltyos.rewards.repository;

import com.loyaltyos.rewards.entity.RewardIssuanceAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardIssuanceAuditRepository extends JpaRepository<RewardIssuanceAudit, Long> {
}
