package com.loyaltyos.onboarding.rules.repository;

import com.loyaltyos.onboarding.rules.entity.EarnRule;
import com.loyaltyos.onboarding.rules.enums.RuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EarnRuleRepository extends JpaRepository<EarnRule, Long> {

    @Query("""
        select distinct r from EarnRule r
        left join fetch r.actions
        left join fetch r.condition
        where r.tenantId = :tenantId
          and r.programmeUid = :programmeUid
          and r.triggerEventType = :eventType
          and r.status = :status
          and (r.effectiveAt is null or r.effectiveAt <= :now)
          and (r.endAt is null or r.endAt > :now)
        order by r.priority desc
        """)
    List<EarnRule> findActiveForEvaluation(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("eventType") String eventType,
        @Param("status") RuleStatus status,
        @Param("now") Instant now
    );

    Optional<EarnRule> findByTenantIdAndProgrammeUidAndRuleUid(String tenantId, String programmeUid, String ruleUid);

    @Query("""
        select distinct r from EarnRule r
        left join fetch r.actions
        left join fetch r.condition
        where r.tenantId = :tenantId
          and r.programmeUid = :programmeUid
          and r.ruleUid = :ruleUid
        """)
    Optional<EarnRule> loadForAdminEdit(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("ruleUid") String ruleUid
    );

    List<EarnRule> findByTenantIdAndProgrammeUidOrderByPriorityDesc(String tenantId, String programmeUid);
}
