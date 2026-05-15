package com.loyaltyos.onboarding.rewards.repository;

import com.loyaltyos.onboarding.rewards.entity.RewardIssuanceAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardIssuanceAuditRepository extends JpaRepository<RewardIssuanceAudit, Long> {
}
