package com.loyaltyos.rules.repository;

import com.loyaltyos.rules.entity.RuleEvaluationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleEvaluationAuditRepository extends JpaRepository<RuleEvaluationAudit, Long> {
}
