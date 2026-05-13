package com.loyaltyos.onboarding.rules.repository;

import com.loyaltyos.onboarding.rules.entity.RuleChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleChangeLogRepository extends JpaRepository<RuleChangeLog, Long> {
    List<RuleChangeLog> findByTenantIdAndRule_IdOrderByChangedAtDesc(String tenantId, Long ruleId);
}
