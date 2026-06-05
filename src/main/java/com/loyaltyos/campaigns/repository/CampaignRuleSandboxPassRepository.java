package com.loyaltyos.campaigns.repository;

import com.loyaltyos.campaigns.entity.CampaignRuleSandboxPass;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRuleSandboxPassRepository extends JpaRepository<CampaignRuleSandboxPass, Long> {

    Optional<CampaignRuleSandboxPass> findByTenantIdAndRuleUid(String tenantId, String ruleUid);

    boolean existsByTenantIdAndRuleUid(String tenantId, String ruleUid);
}
