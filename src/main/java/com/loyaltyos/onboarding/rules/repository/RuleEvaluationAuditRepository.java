package com.loyaltyos.onboarding.rules.repository;

import com.loyaltyos.onboarding.rules.entity.RuleEvaluationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleEvaluationAuditRepository extends JpaRepository<RuleEvaluationAudit, Long> {
}
